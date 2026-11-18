package com.example.engine.network

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import com.example.BuildConfig
import com.example.model.FailureKind
import com.example.model.LeaderboardResult
import com.example.model.OnlineScoreEntry
import com.example.model.PendingSubmission
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.security.MessageDigest

class FirestoreLeaderboardApi @JvmOverloads constructor(
  private val client: OkHttpClient = OkHttpClientProvider.client,
  projectId: String = resolveProjectId(BuildConfig.LEADERBOARD_PROJECT_ID),
  apiKey: String = resolveDefaultApiKey(),
  collection: String = BuildConfig.LEADERBOARD_COLLECTION,
  context: Context? = null
) {
  val projectId: String = projectId.trim()
    .takeUnless(::isPlaceholderProjectId)
    .orEmpty()
    .ifBlank {
      context?.let { resolveProjectId("", it) }.orEmpty()
    }
  private val apiKey: String = sanitizeApiKey(apiKey)
    .takeUnless(::isPlaceholderApiKey)
    .orEmpty()
    .ifBlank {
      context?.let { resolveDefaultApiKey(it) }.orEmpty()
    }
  private val androidPackageName: String = context?.packageName ?: BuildConfig.APPLICATION_ID
  private val androidCertSha1: String? = resolveSigningCertSha1(context)
  val canonicalCollection: String = normalizeCollection(collection)
  val alternateCollection: String = if (canonicalCollection == "leaderboards") "leaderboard" else "leaderboards"
  val collection: String get() = canonicalCollection

  val isConfigured: Boolean
    get() = projectId.isNotBlank() && this.apiKey.isNotBlank()

  suspend fun submitScore(
    item: PendingSubmission,
    idToken: String? = null
  ): LeaderboardResult<Unit> = withContext(Dispatchers.IO) {
    if (!isConfigured) {
      val missing = when {
        projectId.isBlank() -> "project ID"
        apiKey.isBlank() -> "API key"
        else -> "configuration"
      }
      Log.w(TAG, "[FirestoreLeaderboardApi] Cannot submit score: $missing is unconfigured")
      return@withContext LeaderboardResult.Failure(
        FailureKind.UNCONFIGURED,
        "Leaderboard $missing is not configured"
      )
    }

    val sanitizedCallsign = sanitizeCallsign(item.callsign)
    val effectiveUserId = item.userId.ifBlank { "anonymous" }

    val fieldsObj = JSONObject().apply {
      // Primary fields strictly validated by Firestore Security Rules
      put("userId", JSONObject().put("stringValue", effectiveUserId))
      put("username", JSONObject().put("stringValue", sanitizedCallsign))
      put("highScore", JSONObject().put("integerValue", item.score.coerceAtLeast(0).toString()))
      put("maxWave", JSONObject().put("integerValue", item.wavesCleared.coerceAtLeast(1).toString()))
      put("timestamp", JSONObject().put("integerValue", item.timestampMs.toString()))
      put("isDevTuned", JSONObject().put("booleanValue", false))
      put("clientVersion", JSONObject().put("stringValue", item.clientVersion.ifBlank { BuildConfig.VERSION_NAME }))

      // Legacy fields preserved for backward compatibility
      put("callsign", JSONObject().put("stringValue", sanitizedCallsign))
      put("score", JSONObject().put("integerValue", item.score.coerceAtLeast(0).toString()))
      put("wavesCleared", JSONObject().put("integerValue", item.wavesCleared.coerceAtLeast(1).toString()))
      put("scrapEarned", JSONObject().put("integerValue", item.scrapEarned.toString()))
      put("timestampMs", JSONObject().put("integerValue", item.timestampMs.toString()))
    }
    val requestBodyJson = JSONObject().apply {
      put("fields", fieldsObj)
    }

    // Primary attempt on canonicalCollection
    val primaryAttempt = executeSubmitScore(canonicalCollection, item.runId, requestBodyJson, idToken)
    var result = primaryAttempt.result

    // Cross-path failover to alternateCollection ONLY on path/database UNCONFIGURED (404), never on 403 or API key/service errors
    if (result is LeaderboardResult.Failure &&
        result.kind == FailureKind.UNCONFIGURED &&
        primaryAttempt.allowCollectionFailover &&
        canonicalCollection != alternateCollection) {
      Log.w(TAG, "[FirestoreLeaderboardApi] Primary submit on '$canonicalCollection' failed with ${result.kind}. Attempting cross-path failover to '$alternateCollection'")
      val fallbackResult = executeSubmitScore(alternateCollection, item.runId, requestBodyJson, idToken).result
      if (fallbackResult is LeaderboardResult.Success) {
        Log.i(TAG, "[FirestoreLeaderboardApi] Cross-path failover to '$alternateCollection' succeeded for runId=${item.runId}")
        result = fallbackResult
      }
    }

    result
  }

  /**
   * Non-blocking whole-run balance telemetry upload to the `run_telemetry` collection for Balance-by-Data analysis.
   * Never blocks or alters strict `leaderboards` security rules.
   */
  suspend fun submitRunTelemetry(
    record: com.example.model.RunRecord,
    difficultyId: String = "medium",
    towerPerf: List<com.example.model.TowerPerformanceRecord> = emptyList(),
    idToken: String? = null
  ): LeaderboardResult<Unit> = withContext(Dispatchers.IO) {
    if (!isConfigured || record.isDevTuned) {
      return@withContext LeaderboardResult.Failure(
        FailureKind.UNCONFIGURED,
        "Telemetry upload skipped (unconfigured or dev-tuned)"
      )
    }

    val towerSummaryJson = JSONArray().apply {
      for (perf in towerPerf.take(16)) {
        put(
          JSONObject().apply {
            put("name", perf.towerName)
            put("class", perf.towerClass.name)
            put("dmg", perf.totalDamage.toLong())
            put("kills", perf.totalKills)
            put("built", perf.totalBuilt)
            put("cost", perf.totalCostInvested)
            put("dpsPerCost", perf.dpsCostEfficiency.toDouble())
          }
        )
      }
    }.toString()

    val fieldsObj = JSONObject().apply {
      put("runId", JSONObject().put("stringValue", record.id))
      put("seed", JSONObject().put("integerValue", record.seed.toString()))
      put("difficulty", JSONObject().put("stringValue", difficultyId))
      put("wavesCleared", JSONObject().put("integerValue", record.wavesCleared.toString()))
      put("isVictory", JSONObject().put("booleanValue", record.isVictory))
      put("totalCollapses", JSONObject().put("integerValue", record.totalCollapses.toString()))
      put("unspentGoldAtEnd", JSONObject().put("integerValue", record.unspentGoldAtEnd.toString()))
      put("uniqueAurasBanked", JSONObject().put("integerValue", record.uniqueAurasBanked.toString()))
      put("fatalEnemyType", JSONObject().put("stringValue", record.fatalEnemyType))
      put("collapseWaves", JSONObject().put("stringValue", record.collapseWaves.joinToString(",")))
      put("mvpTower", JSONObject().put("stringValue", record.mvpTower))
      put("scrapEarned", JSONObject().put("integerValue", record.scrapEarned.toString()))
      put("totalDamage", JSONObject().put("integerValue", record.totalDamage.toLong().toString()))
      put("towerLedgerJson", JSONObject().put("stringValue", towerSummaryJson))
      put("clientVersion", JSONObject().put("stringValue", BuildConfig.VERSION_NAME))
      put("timestamp", JSONObject().put("integerValue", record.timestamp.toString()))
    }

    val requestBodyJson = JSONObject().apply {
      put("fields", fieldsObj)
    }

    executeSubmitScore("run_telemetry", record.id, requestBodyJson, idToken).result
  }

  private data class AttemptOutcome<T>(
    val result: LeaderboardResult<T>,
    val allowCollectionFailover: Boolean = false
  )

  private data class ParsedError(
    val kind: FailureKind,
    val message: String,
    val allowCollectionFailover: Boolean = false
  )

  private suspend fun executeSubmitScore(
    targetCollection: String,
    runId: String,
    requestBodyJson: JSONObject,
    idToken: String?
  ): AttemptOutcome<Unit> {
    val encodedRunId = android.net.Uri.encode(runId)
    val keyParam = if (apiKey.isNotBlank()) "&key=$apiKey" else ""
    val url = "https://firestore.googleapis.com/v1/projects/$projectId/databases/(default)/documents/$targetCollection?documentId=$encodedRunId$keyParam"
    val payloadString = requestBodyJson.toString()
    val mediaType = "application/json; charset=utf-8".toMediaType()

    val redactedUrl = "https://firestore.googleapis.com/v1/projects/$projectId/databases/(default)/documents/$targetCollection?documentId=$encodedRunId"
    Log.d(TAG, "[FirestoreLeaderboardApi] Outgoing submitScore -> URL: $redactedUrl | collection: $targetCollection | runId: $runId | payloadSize: ${payloadString.toByteArray().size} bytes | hasAuthToken: ${!idToken.isNullOrBlank()}")

    val requestBuilder = Request.Builder()
      .url(url)
      .apply {
        if (apiKey.isNotBlank()) addHeader("X-Goog-Api-Key", apiKey)
        if (!androidCertSha1.isNullOrBlank()) {
          addHeader("X-Android-Package", androidPackageName)
          addHeader("X-Android-Cert", androidCertSha1)
        }
      }

    if (!idToken.isNullOrBlank()) {
      requestBuilder.addHeader("Authorization", "Bearer $idToken")
    }

    val request = requestBuilder
      .post(payloadString.toRequestBody(mediaType))
      .build()

    return try {
      client.newCall(request).awaitResponse().use { response ->
        val code = response.code
        Log.d(TAG, "[FirestoreLeaderboardApi] submitScore HTTP response code: $code for collection: $targetCollection, runId: $runId")
        when (code) {
          200, 201 -> {
            Log.i(TAG, "[FirestoreLeaderboardApi] submitScore SUCCESS ($code) on collection: $targetCollection for runId: $runId")
            AttemptOutcome(LeaderboardResult.Success(Unit))
          }
          409 -> {
            Log.i(TAG, "[FirestoreLeaderboardApi] submitScore idempotent 409 (already exists) on collection: $targetCollection for runId: $runId")
            AttemptOutcome(LeaderboardResult.Success(Unit))
          }
          400, 403, 404 -> {
            val errBody = response.body?.string().orEmpty()
            Log.e(TAG, "[FirestoreLeaderboardApi] submitScore HTTP $code error body on collection: $targetCollection: $errBody")
            val parsed = parseFirestoreError(code, errBody)
            AttemptOutcome(LeaderboardResult.Failure(parsed.kind, parsed.message), parsed.allowCollectionFailover)
          }
          429 -> {
            Log.w(TAG, "[FirestoreLeaderboardApi] submitScore HTTP 429 Rate Limited on collection: $targetCollection")
            AttemptOutcome(
              LeaderboardResult.Failure(
                FailureKind.RATE_LIMITED,
                "Firestore rate limit reached (429)"
              )
            )
          }
          in 500..599 -> {
            val errBody = response.body?.string().orEmpty()
            Log.e(TAG, "[FirestoreLeaderboardApi] submitScore server error HTTP $code on collection: $targetCollection: $errBody")
            AttemptOutcome(
              LeaderboardResult.Failure(
                FailureKind.SERVER_ERROR,
                "Firestore server error ($code)"
              )
            )
          }
          else -> {
            AttemptOutcome(
              LeaderboardResult.Failure(
                FailureKind.SERVER_ERROR,
                "Unexpected HTTP response code $code"
              )
            )
          }
        }
      }
    } catch (e: CancellationException) {
      throw e
    } catch (e: SocketTimeoutException) {
      Log.w(TAG, "[FirestoreLeaderboardApi] submitScore TIMEOUT on collection: $targetCollection: ${e.message}")
      AttemptOutcome(LeaderboardResult.Failure(FailureKind.TIMEOUT, "Connection timed out", e))
    } catch (e: UnknownHostException) {
      Log.w(TAG, "[FirestoreLeaderboardApi] submitScore NETWORK_UNAVAILABLE (DNS) on collection: $targetCollection: ${e.message}")
      AttemptOutcome(LeaderboardResult.Failure(FailureKind.NETWORK_UNAVAILABLE, "Network unavailable / DNS failed", e))
    } catch (e: IOException) {
      Log.w(TAG, "[FirestoreLeaderboardApi] submitScore I/O exception on collection: $targetCollection: ${e.message}")
      AttemptOutcome(LeaderboardResult.Failure(FailureKind.NETWORK_UNAVAILABLE, "Network I/O failed: ${e.message}", e))
    } catch (e: Exception) {
      Log.e(TAG, "[FirestoreLeaderboardApi] submitScore unexpected exception on collection: $targetCollection: ${e.message}", e)
      AttemptOutcome(LeaderboardResult.Failure(FailureKind.SERVER_ERROR, "Unexpected error: ${e.message}", e))
    }
  }

  suspend fun fetchTopScores(
    limit: Int = 50,
    idToken: String? = null
  ): LeaderboardResult<List<OnlineScoreEntry>> = withContext(Dispatchers.IO) {
    if (!isConfigured) {
      val missing = when {
        projectId.isBlank() -> "project ID"
        apiKey.isBlank() -> "API key"
        else -> "configuration"
      }
      Log.w(TAG, "[FirestoreLeaderboardApi] Cannot fetch scores: $missing is unconfigured")
      return@withContext LeaderboardResult.Failure(
        FailureKind.UNCONFIGURED,
        "Leaderboard $missing is not configured"
      )
    }

    var primaryAttempt = executeRunQuery(canonicalCollection, limit, singleMetricOnly = false, idToken = idToken)
    var result = primaryAttempt.result

    // Fallback within canonicalCollection if index is building or requires an index
    if (result is LeaderboardResult.Failure && result.kind == FailureKind.UNCONFIGURED &&
        result.message.contains("index", ignoreCase = true)) {
      Log.w(TAG, "[FirestoreLeaderboardApi] Composite query requires index on '$canonicalCollection', falling back to single-metric query")
      primaryAttempt = executeRunQuery(canonicalCollection, limit, singleMetricOnly = true, idToken = idToken)
      result = primaryAttempt.result
    }

    // Cross-path failover to alternateCollection ONLY on path/database UNCONFIGURED (404), never on 403 or API key/service errors
    if (result is LeaderboardResult.Failure &&
        result.kind == FailureKind.UNCONFIGURED &&
        primaryAttempt.allowCollectionFailover &&
        canonicalCollection != alternateCollection) {
      Log.w(TAG, "[FirestoreLeaderboardApi] Fetch on canonical collection '$canonicalCollection' failed with ${result.kind}. Attempting cross-path failover to '$alternateCollection'")
      val altAttempt = executeRunQuery(alternateCollection, limit, singleMetricOnly = false, idToken = idToken)
      val altResult = altAttempt.result
      val finalAltResult = if (altResult is LeaderboardResult.Failure && altResult.kind == FailureKind.UNCONFIGURED &&
          altResult.message.contains("index", ignoreCase = true)) {
        Log.w(TAG, "[FirestoreLeaderboardApi] Composite query requires index on alternate '$alternateCollection', falling back to single-metric query")
        executeRunQuery(alternateCollection, limit, singleMetricOnly = true, idToken = idToken).result
      } else {
        altResult
      }

      if (finalAltResult is LeaderboardResult.Success) {
        Log.i(TAG, "[FirestoreLeaderboardApi] Cross-path failover fetch on '$alternateCollection' succeeded with ${finalAltResult.data.size} entries")
        result = finalAltResult
      }
    }

    result
  }

  private suspend fun executeRunQuery(
    targetCollection: String,
    limit: Int,
    singleMetricOnly: Boolean,
    idToken: String? = null
  ): AttemptOutcome<List<OnlineScoreEntry>> {
    val keyParam = if (apiKey.isNotBlank()) "?key=$apiKey" else ""
    val url = "https://firestore.googleapis.com/v1/projects/$projectId/databases/(default)/documents:runQuery$keyParam"

    val queryJson = JSONObject().apply {
      val structuredQuery = JSONObject().apply {
        put("from", JSONArray().put(JSONObject().put("collectionId", targetCollection)))
        if (!singleMetricOnly) {
          put("where", JSONObject().apply {
            put("fieldFilter", JSONObject().apply {
              put("field", JSONObject().put("fieldPath", "isDevTuned"))
              put("op", "EQUAL")
              put("value", JSONObject().put("booleanValue", false))
            })
          })
          put("orderBy", JSONArray().apply {
            put(JSONObject().apply {
              put("field", JSONObject().put("fieldPath", "maxWave"))
              put("direction", "DESCENDING")
            })
            put(JSONObject().apply {
              put("field", JSONObject().put("fieldPath", "highScore"))
              put("direction", "DESCENDING")
            })
            put(JSONObject().apply {
              put("field", JSONObject().put("fieldPath", "timestamp"))
              put("direction", "ASCENDING")
            })
          })
        } else {
          put("orderBy", JSONArray().apply {
            put(JSONObject().apply {
              put("field", JSONObject().put("fieldPath", "maxWave"))
              put("direction", "DESCENDING")
            })
          })
        }
        put("limit", limit)
      }
      put("structuredQuery", structuredQuery)
    }

    val payloadString = queryJson.toString()
    val mediaType = "application/json; charset=utf-8".toMediaType()

    val redactedUrl = "https://firestore.googleapis.com/v1/projects/$projectId/databases/(default)/documents:runQuery"
    Log.d(TAG, "[FirestoreLeaderboardApi] Outgoing runQuery -> URL: $redactedUrl | collection: $targetCollection | singleMetric: $singleMetricOnly | limit: $limit | payloadSize: ${payloadString.toByteArray().size} bytes")

    val requestBuilder = Request.Builder()
      .url(url)
      .apply {
        if (apiKey.isNotBlank()) addHeader("X-Goog-Api-Key", apiKey)
        if (!androidCertSha1.isNullOrBlank()) {
          addHeader("X-Android-Package", androidPackageName)
          addHeader("X-Android-Cert", androidCertSha1)
        }
        if (!idToken.isNullOrBlank()) {
          addHeader("Authorization", "Bearer $idToken")
        }
      }
    val request = requestBuilder
      .post(payloadString.toRequestBody(mediaType))
      .build()

    return try {
      client.newCall(request).awaitResponse().use { response ->
        val code = response.code
        Log.d(TAG, "[FirestoreLeaderboardApi] runQuery HTTP response code: $code for collection: $targetCollection")
        when (code) {
          200 -> {
            val responseBody = response.body?.string().orEmpty()
            Log.d(TAG, "[FirestoreLeaderboardApi] runQuery 200 OK: responseBodyLength=${responseBody.length} bytes for collection: $targetCollection")
            if (responseBody.isBlank()) {
              AttemptOutcome(LeaderboardResult.Success(emptyList()))
            } else {
              val results = parseQueryResults(responseBody)
              Log.i(TAG, "[FirestoreLeaderboardApi] Parsed ${results.size} entries from collection: $targetCollection")
              AttemptOutcome(LeaderboardResult.Success(results))
            }
          }
          400, 403, 404 -> {
            val errBody = response.body?.string().orEmpty()
            Log.e(TAG, "[FirestoreLeaderboardApi] runQuery HTTP $code error body on collection: $targetCollection: $errBody")
            val parsed = parseFirestoreError(code, errBody)
            AttemptOutcome(LeaderboardResult.Failure(parsed.kind, parsed.message), parsed.allowCollectionFailover)
          }
          429 -> {
            Log.w(TAG, "[FirestoreLeaderboardApi] runQuery HTTP 429 Rate Limited on collection: $targetCollection")
            AttemptOutcome(
              LeaderboardResult.Failure(
                FailureKind.RATE_LIMITED,
                "Firestore rate limit reached (429)"
              )
            )
          }
          in 500..599 -> {
            val errBody = response.body?.string().orEmpty()
            Log.e(TAG, "[FirestoreLeaderboardApi] runQuery server error HTTP $code on collection: $targetCollection: $errBody")
            AttemptOutcome(
              LeaderboardResult.Failure(
                FailureKind.SERVER_ERROR,
                "Firestore server error ($code)"
              )
            )
          }
          else -> {
            AttemptOutcome(
              LeaderboardResult.Failure(
                FailureKind.SERVER_ERROR,
                "Unexpected HTTP response code $code"
              )
            )
          }
        }
      }
    } catch (e: CancellationException) {
      throw e
    } catch (e: SocketTimeoutException) {
      Log.w(TAG, "[FirestoreLeaderboardApi] runQuery TIMEOUT on collection: $targetCollection: ${e.message}")
      AttemptOutcome(LeaderboardResult.Failure(FailureKind.TIMEOUT, "Connection timed out", e))
    } catch (e: UnknownHostException) {
      Log.w(TAG, "[FirestoreLeaderboardApi] runQuery NETWORK_UNAVAILABLE (DNS) on collection: $targetCollection: ${e.message}")
      AttemptOutcome(LeaderboardResult.Failure(FailureKind.NETWORK_UNAVAILABLE, "Network unavailable / DNS failed", e))
    } catch (e: IOException) {
      Log.w(TAG, "[FirestoreLeaderboardApi] runQuery I/O exception on collection: $targetCollection: ${e.message}")
      AttemptOutcome(LeaderboardResult.Failure(FailureKind.NETWORK_UNAVAILABLE, "Network I/O failed: ${e.message}", e))
    } catch (e: Exception) {
      Log.e(TAG, "[FirestoreLeaderboardApi] runQuery unexpected parse exception on collection: $targetCollection: ${e.message}", e)
      AttemptOutcome(LeaderboardResult.Failure(FailureKind.MALFORMED_PAYLOAD, "Failed to parse response: ${e.message}", e))
    }
  }

  private fun parseFirestoreError(code: Int, body: String): ParsedError {
    if (body.isBlank()) {
      return if (code == 404) {
        ParsedError(
          FailureKind.UNCONFIGURED,
          "Cloud Firestore database not found in Firebase project ($projectId). Please create the (default) database in Firebase Console. Scores are cached locally.",
          allowCollectionFailover = true
        )
      } else {
        ParsedError(FailureKind.SECURITY_REJECTED, "Request rejected by server ($code)")
      }
    }
    return try {
      val trimmed = body.trim()
      val json = if (trimmed.startsWith("[")) {
        JSONArray(trimmed).optJSONObject(0) ?: JSONObject()
      } else {
        JSONObject(trimmed)
      }
      val err = json.optJSONObject("error")
      val msg = err?.optString("message", "") ?: ""
      val status = err?.optString("status", "") ?: ""
      val details = err?.optJSONArray("details")
      var reason = ""
      if (details != null) {
        for (i in 0 until details.length()) {
          val d = details.optJSONObject(i)
          val r = d?.optString("reason", "") ?: ""
          if (r.isNotBlank()) reason = r
        }
      }
      when {
        code == 404 || status == "NOT_FOUND" || msg.contains("does not exist", ignoreCase = true) || msg.contains("add a Cloud Datastore or Cloud Firestore database", ignoreCase = true) -> {
          ParsedError(
            FailureKind.UNCONFIGURED,
            "Cloud Firestore database not created in Firebase project ($projectId). Please visit Firebase Console -> Firestore Database and click 'Create database'. Scores are safely cached locally.",
            allowCollectionFailover = true
          )
        }
        status == "FAILED_PRECONDITION" || msg.contains("requires an index", ignoreCase = true) || msg.contains("index is currently building", ignoreCase = true) -> {
          ParsedError(
            FailureKind.UNCONFIGURED,
            "Firestore composite index is building in Firebase Console ($projectId). Falling back to single-metric leaderboard.",
            allowCollectionFailover = true
          )
        }
        reason == "SERVICE_DISABLED" || msg.contains("Cloud Firestore API has not been used", ignoreCase = true) || msg.contains("is disabled", ignoreCase = true) -> {
          ParsedError(
            FailureKind.UNCONFIGURED,
            "Cloud Firestore API not enabled in Firebase project ($projectId). Scores cached in offline queue.",
            allowCollectionFailover = false
          )
        }
        reason == "API_KEY_ANDROID_APP_BLOCKED" || msg.contains("Requests from this Android client application", ignoreCase = true) -> {
          ParsedError(
            FailureKind.UNCONFIGURED,
            "Firebase API Key Android app restriction blocked ($androidPackageName). Check SHA-1 in Google Cloud Console.",
            allowCollectionFailover = false
          )
        }
        msg.contains("API key not valid", ignoreCase = true) || reason == "API_KEY_INVALID" -> {
          ParsedError(
            FailureKind.UNCONFIGURED,
            "Firebase API Key invalid or expired ($code). Verify google-services.json or LEADERBOARD_API_KEY.",
            allowCollectionFailover = false
          )
        }
        status == "PERMISSION_DENIED" -> {
          ParsedError(
            FailureKind.SECURITY_REJECTED,
            "Firestore Security Rules denied access. Ensure Anonymous Auth is enabled in Firebase Console. Scores cached in offline queue."
          )
        }
        else -> {
          ParsedError(FailureKind.SECURITY_REJECTED, if (msg.isNotBlank()) msg else "Request rejected ($code)")
        }
      }
    } catch (_: Exception) {
      if (code == 404) {
        ParsedError(
          FailureKind.UNCONFIGURED,
          "Cloud Firestore database not created in Firebase project ($projectId). Please create the (default) database in Firebase Console.",
          allowCollectionFailover = true
        )
      } else {
        ParsedError(FailureKind.SECURITY_REJECTED, "Request rejected by server ($code)")
      }
    }
  }

  private fun parseQueryResults(jsonStr: String): List<OnlineScoreEntry> {
    val array = JSONArray(jsonStr)
    val entries = mutableListOf<OnlineScoreEntry>()

    for (i in 0 until array.length()) {
      val item = array.optJSONObject(i) ?: continue
      val doc = item.optJSONObject("document") ?: continue
      val docName = doc.optString("name", "")
      val id = docName.substringAfterLast("/")
      val fields = doc.optJSONObject("fields") ?: continue

      val isDevTuned = fields.optJSONObject("isDevTuned")?.optBoolean("booleanValue", false) ?: false
      if (isDevTuned) continue

      val callsign = fields.optJSONObject("username")?.optString("stringValue")
        ?: fields.optJSONObject("callsign")?.optString("stringValue")
        ?: "Commander"
      val score = fields.optJSONObject("highScore")?.let {
        it.optString("integerValue").toIntOrNull() ?: it.optInt("integerValue", 0)
      } ?: fields.optJSONObject("score")?.let {
        it.optString("integerValue").toIntOrNull() ?: it.optInt("integerValue", 0)
      } ?: 0
      val waves = fields.optJSONObject("maxWave")?.let {
        it.optString("integerValue").toIntOrNull() ?: it.optInt("integerValue", 0)
      } ?: fields.optJSONObject("wavesCleared")?.let {
        it.optString("integerValue").toIntOrNull() ?: it.optInt("integerValue", 0)
      } ?: 0
      val scrap = fields.optJSONObject("scrapEarned")?.let {
        it.optString("integerValue").toIntOrNull() ?: it.optInt("integerValue", 0)
      } ?: 0
      val timestampMs = fields.optJSONObject("timestamp")?.let {
        it.optString("integerValue").toLongOrNull() ?: it.optLong("integerValue", 0L)
      } ?: fields.optJSONObject("timestampMs")?.let {
        it.optString("integerValue").toLongOrNull() ?: it.optLong("integerValue", 0L)
      } ?: 0L
      val userId = fields.optJSONObject("userId")?.optString("stringValue") ?: ""

      entries.add(
        OnlineScoreEntry(
          id = id,
          rank = 0,
          callsign = sanitizeCallsign(callsign),
          score = score,
          wavesCleared = waves,
          scrapEarned = scrap,
          timestampMs = timestampMs,
          userId = userId
        )
      )
    }

    return entries
      .sortedWith(
        compareByDescending<OnlineScoreEntry> { it.wavesCleared }
          .thenByDescending { it.score }
          .thenBy { it.timestampMs }
      )
      .mapIndexed { index, entry ->
        entry.copy(rank = index + 1)
      }
  }

  companion object {
    const val TAG = "FirestoreLeaderboardApi"

    fun sanitizeApiKey(raw: String): String {
      return raw.trim().removePrefix("key=").removeSurrounding("\"").trim()
    }

    private fun isPlaceholderApiKey(key: String): Boolean =
      key.equals("API_KEY", ignoreCase = true) ||
        key.equals("your_firestore_api_key", ignoreCase = true)

    private fun isPlaceholderProjectId(projectId: String): Boolean =
      projectId.equals("your_firestore_project_id", ignoreCase = true)

    @SuppressLint("DiscouragedApi")
    @JvmOverloads
    fun resolveDefaultApiKey(context: Context? = null): String =
      resolveDefaultApiKey(context = context, buildConfigApiKey = BuildConfig.LEADERBOARD_API_KEY)

    internal fun resolveDefaultApiKey(context: Context?, buildConfigApiKey: String): String {
      val fromBuildConfig = sanitizeApiKey(buildConfigApiKey)
      if (fromBuildConfig.isNotBlank() && !isPlaceholderApiKey(fromBuildConfig)) return fromBuildConfig

      val fromFirebaseApp = runCatching {
        FirebaseApp.getInstance().options.apiKey
      }.getOrNull()?.let(::sanitizeApiKey).orEmpty()
      if (fromFirebaseApp.isNotBlank() && !isPlaceholderApiKey(fromFirebaseApp)) return fromFirebaseApp

      if (context != null) {
        val fromOptions = runCatching {
          FirebaseOptions.fromResource(context)?.apiKey
        }.getOrNull()?.let(::sanitizeApiKey).orEmpty()
        if (fromOptions.isNotBlank() && !isPlaceholderApiKey(fromOptions)) return fromOptions

        val resId = runCatching {
          context.resources.getIdentifier("google_api_key", "string", context.packageName)
        }.getOrDefault(0)
        if (resId != 0) {
          val fromRes = runCatching { context.getString(resId) }.getOrNull()?.let(::sanitizeApiKey).orEmpty()
          if (fromRes.isNotBlank() && !isPlaceholderApiKey(fromRes)) return fromRes
        }
      }

      return ""
    }

    @SuppressLint("DiscouragedApi")
    @JvmOverloads
    fun resolveProjectId(explicitProjectId: String = "", context: Context? = null): String {
      val trimmed = explicitProjectId.trim()
      if (trimmed.isNotBlank() && !isPlaceholderProjectId(trimmed)) return trimmed

      val fromFirebaseApp = runCatching {
        FirebaseApp.getInstance().options.projectId
      }.getOrNull()?.trim().orEmpty()
      if (fromFirebaseApp.isNotBlank() && !isPlaceholderProjectId(fromFirebaseApp)) return fromFirebaseApp

      if (context != null) {
        val fromOptions = runCatching {
          FirebaseOptions.fromResource(context)?.projectId
        }.getOrNull()?.trim().orEmpty()
        if (fromOptions.isNotBlank() && !isPlaceholderProjectId(fromOptions)) return fromOptions

        val resId = runCatching {
          context.resources.getIdentifier("project_id", "string", context.packageName)
        }.getOrDefault(0)
        if (resId != 0) {
          val fromRes = runCatching { context.getString(resId) }.getOrNull()?.trim().orEmpty()
          if (fromRes.isNotBlank() && !isPlaceholderProjectId(fromRes)) return fromRes
        }
      }

      return ""
    }

    fun resolveSigningCertSha1(context: Context?): String? {
      if (context == null) return null
      return runCatching {
        val pm = context.packageManager
        val pkgName = context.packageName
        val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
          val info = pm.getPackageInfo(pkgName, PackageManager.GET_SIGNING_CERTIFICATES)
          val signingInfo = info.signingInfo
          signingInfo?.apkContentsSigners?.takeIf { it.isNotEmpty() }
            ?: signingInfo?.signingCertificateHistory
        } else {
          @Suppress("DEPRECATION")
          pm.getPackageInfo(pkgName, PackageManager.GET_SIGNATURES).signatures
        }
        val certBytes = signatures?.firstOrNull()?.toByteArray() ?: return@runCatching null
        val digest = MessageDigest.getInstance("SHA-1").digest(certBytes)
        digest.joinToString("") { "%02x".format(it) }
      }.getOrNull()
    }

    fun normalizeCollection(raw: String): String {
      val trimmed = raw.trim()
      return when {
        trimmed.equals("leaderboard", ignoreCase = true) -> "leaderboards"
        trimmed.equals("leaderboards", ignoreCase = true) -> "leaderboards"
        trimmed.isBlank() -> "leaderboards"
        else -> trimmed
      }
    }

    fun sanitizeCallsign(raw: String): String {
      val cleaned = raw.trim()
        .filter { it.isLetterOrDigit() || it == '_' || it == '-' || it == ' ' }
        .take(16)
        .trim()
      return if (cleaned.length < 3) "Commander" else cleaned
    }
  }
}
