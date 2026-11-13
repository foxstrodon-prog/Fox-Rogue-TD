package com.example.engine.network

import android.util.Log
import com.example.model.FailureKind
import com.example.model.LeaderboardResult
import com.example.model.OnlineScoreEntry
import com.example.model.PendingSubmission
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

class FirestoreLeaderboardApi @JvmOverloads constructor(
  private val client: OkHttpClient = OkHttpClientProvider.client,
  private val projectId: String = com.example.BuildConfig.LEADERBOARD_PROJECT_ID,
  private val apiKey: String = com.example.BuildConfig.LEADERBOARD_API_KEY,
  collection: String = com.example.BuildConfig.LEADERBOARD_COLLECTION
) {
  val canonicalCollection: String = normalizeCollection(collection)
  val alternateCollection: String = if (canonicalCollection == "leaderboards") "leaderboard" else "leaderboards"
  val collection: String get() = canonicalCollection

  val isConfigured: Boolean
    get() = projectId.isNotBlank() && apiKey.isNotBlank()

  suspend fun submitScore(
    item: PendingSubmission,
    idToken: String? = null
  ): LeaderboardResult<Unit> = withContext(Dispatchers.IO) {
    if (!isConfigured) {
      Log.w(TAG, "[FirestoreLeaderboardApi] Cannot submit score: projectId or apiKey is unconfigured")
      return@withContext LeaderboardResult.Failure(
        FailureKind.UNCONFIGURED,
        "Leaderboard project ID or API key is not configured"
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
      put("clientVersion", JSONObject().put("stringValue", item.clientVersion.ifBlank { "0.0.1.019" }))

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
    var result = executeSubmitScore(canonicalCollection, item.runId, requestBodyJson, idToken)

    // Cross-path failover to alternateCollection if 403 or 404 on canonical
    if (result is LeaderboardResult.Failure &&
        (result.kind == FailureKind.SECURITY_REJECTED || result.kind == FailureKind.UNCONFIGURED) &&
        canonicalCollection != alternateCollection) {
      Log.w(TAG, "[FirestoreLeaderboardApi] Primary submit on '$canonicalCollection' failed with ${result.kind}. Attempting cross-path failover to '$alternateCollection'")
      val fallbackResult = executeSubmitScore(alternateCollection, item.runId, requestBodyJson, idToken)
      if (fallbackResult is LeaderboardResult.Success) {
        Log.i(TAG, "[FirestoreLeaderboardApi] Cross-path failover to '$alternateCollection' succeeded for runId=${item.runId}")
        result = fallbackResult
      }
    }

    result
  }

  private suspend fun executeSubmitScore(
    targetCollection: String,
    runId: String,
    requestBodyJson: JSONObject,
    idToken: String?
  ): LeaderboardResult<Unit> {
    val url = "https://firestore.googleapis.com/v1/projects/$projectId/databases/(default)/documents/$targetCollection?documentId=$runId&key=$apiKey"
    val payloadString = requestBodyJson.toString()
    val mediaType = "application/json; charset=utf-8".toMediaType()

    Log.d(TAG, "[FirestoreLeaderboardApi] Outgoing submitScore -> URL: $url | collection: $targetCollection | runId: $runId | payloadSize: ${payloadString.toByteArray().size} bytes | hasAuthToken: ${!idToken.isNullOrBlank()}")

    val requestBuilder = Request.Builder()
      .url(url)
      .addHeader("X-Goog-Api-Key", apiKey)
      .addHeader("X-Android-Package", "com.aistudio.roguetd.xtkp")
      .addHeader("X-Android-Cert", ANDROID_CERT_SHA1)

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
            LeaderboardResult.Success(Unit)
          }
          409 -> {
            Log.i(TAG, "[FirestoreLeaderboardApi] submitScore idempotent 409 (already exists) on collection: $targetCollection for runId: $runId")
            LeaderboardResult.Success(Unit)
          }
          400, 403, 404 -> {
            val errBody = response.body?.string().orEmpty()
            Log.e(TAG, "[FirestoreLeaderboardApi] submitScore HTTP $code error body on collection: $targetCollection: $errBody")
            val (kind, msg) = parseFirestoreError(code, errBody)
            LeaderboardResult.Failure(kind, msg)
          }
          429 -> {
            Log.w(TAG, "[FirestoreLeaderboardApi] submitScore HTTP 429 Rate Limited on collection: $targetCollection")
            LeaderboardResult.Failure(
              FailureKind.RATE_LIMITED,
              "Firestore rate limit reached (429)"
            )
          }
          in 500..599 -> {
            val errBody = response.body?.string().orEmpty()
            Log.e(TAG, "[FirestoreLeaderboardApi] submitScore server error HTTP $code on collection: $targetCollection: $errBody")
            LeaderboardResult.Failure(
              FailureKind.SERVER_ERROR,
              "Firestore server error ($code)"
            )
          }
          else -> {
            LeaderboardResult.Failure(
              FailureKind.SERVER_ERROR,
              "Unexpected HTTP response code $code"
            )
          }
        }
      }
    } catch (e: CancellationException) {
      throw e
    } catch (e: SocketTimeoutException) {
      Log.w(TAG, "[FirestoreLeaderboardApi] submitScore TIMEOUT on collection: $targetCollection: ${e.message}")
      LeaderboardResult.Failure(FailureKind.TIMEOUT, "Connection timed out", e)
    } catch (e: UnknownHostException) {
      Log.w(TAG, "[FirestoreLeaderboardApi] submitScore NETWORK_UNAVAILABLE (DNS) on collection: $targetCollection: ${e.message}")
      LeaderboardResult.Failure(FailureKind.NETWORK_UNAVAILABLE, "Network unavailable / DNS failed", e)
    } catch (e: IOException) {
      Log.w(TAG, "[FirestoreLeaderboardApi] submitScore I/O exception on collection: $targetCollection: ${e.message}")
      LeaderboardResult.Failure(FailureKind.NETWORK_UNAVAILABLE, "Network I/O failed: ${e.message}", e)
    } catch (e: Exception) {
      Log.e(TAG, "[FirestoreLeaderboardApi] submitScore unexpected exception on collection: $targetCollection: ${e.message}", e)
      LeaderboardResult.Failure(FailureKind.SERVER_ERROR, "Unexpected error: ${e.message}", e)
    }
  }

  suspend fun fetchTopScores(limit: Int = 50): LeaderboardResult<List<OnlineScoreEntry>> = withContext(Dispatchers.IO) {
    if (!isConfigured) {
      Log.w(TAG, "[FirestoreLeaderboardApi] Cannot fetch scores: projectId or apiKey is unconfigured")
      return@withContext LeaderboardResult.Failure(
        FailureKind.UNCONFIGURED,
        "Leaderboard project ID or API key is not configured"
      )
    }

    var result = executeRunQuery(canonicalCollection, limit, singleMetricOnly = false)

    // Fallback within canonicalCollection if index is building or requires an index
    if (result is LeaderboardResult.Failure && result.kind == FailureKind.UNCONFIGURED &&
        result.message.contains("index", ignoreCase = true)) {
      Log.w(TAG, "[FirestoreLeaderboardApi] Composite query requires index on '$canonicalCollection', falling back to single-metric query")
      result = executeRunQuery(canonicalCollection, limit, singleMetricOnly = true)
    }

    // Cross-path failover to alternateCollection if 403 or 404 on canonical
    if (result is LeaderboardResult.Failure &&
        (result.kind == FailureKind.SECURITY_REJECTED || result.kind == FailureKind.UNCONFIGURED) &&
        canonicalCollection != alternateCollection) {
      Log.w(TAG, "[FirestoreLeaderboardApi] Fetch on canonical collection '$canonicalCollection' failed with ${result.kind}. Attempting cross-path failover to '$alternateCollection'")
      val altResult = executeRunQuery(alternateCollection, limit, singleMetricOnly = false)
      val finalAltResult = if (altResult is LeaderboardResult.Failure && altResult.kind == FailureKind.UNCONFIGURED &&
          altResult.message.contains("index", ignoreCase = true)) {
        Log.w(TAG, "[FirestoreLeaderboardApi] Composite query requires index on alternate '$alternateCollection', falling back to single-metric query")
        executeRunQuery(alternateCollection, limit, singleMetricOnly = true)
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
    singleMetricOnly: Boolean
  ): LeaderboardResult<List<OnlineScoreEntry>> {
    val url = "https://firestore.googleapis.com/v1/projects/$projectId/databases/(default)/documents:runQuery?key=$apiKey"

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

    Log.d(TAG, "[FirestoreLeaderboardApi] Outgoing runQuery -> URL: $url | collection: $targetCollection | singleMetric: $singleMetricOnly | limit: $limit | payloadSize: ${payloadString.toByteArray().size} bytes")

    val request = Request.Builder()
      .url(url)
      .addHeader("X-Goog-Api-Key", apiKey)
      .addHeader("X-Android-Package", "com.aistudio.roguetd.xtkp")
      .addHeader("X-Android-Cert", ANDROID_CERT_SHA1)
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
              LeaderboardResult.Success(emptyList())
            } else {
              val results = parseQueryResults(responseBody)
              Log.i(TAG, "[FirestoreLeaderboardApi] Parsed ${results.size} entries from collection: $targetCollection")
              LeaderboardResult.Success(results)
            }
          }
          400, 403, 404 -> {
            val errBody = response.body?.string().orEmpty()
            Log.e(TAG, "[FirestoreLeaderboardApi] runQuery HTTP $code error body on collection: $targetCollection: $errBody")
            val (kind, msg) = parseFirestoreError(code, errBody)
            LeaderboardResult.Failure(kind, msg)
          }
          429 -> {
            Log.w(TAG, "[FirestoreLeaderboardApi] runQuery HTTP 429 Rate Limited on collection: $targetCollection")
            LeaderboardResult.Failure(
              FailureKind.RATE_LIMITED,
              "Firestore rate limit reached (429)"
            )
          }
          in 500..599 -> {
            val errBody = response.body?.string().orEmpty()
            Log.e(TAG, "[FirestoreLeaderboardApi] runQuery server error HTTP $code on collection: $targetCollection: $errBody")
            LeaderboardResult.Failure(
              FailureKind.SERVER_ERROR,
              "Firestore server error ($code)"
            )
          }
          else -> {
            LeaderboardResult.Failure(
              FailureKind.SERVER_ERROR,
              "Unexpected HTTP response code $code"
            )
          }
        }
      }
    } catch (e: CancellationException) {
      throw e
    } catch (e: SocketTimeoutException) {
      Log.w(TAG, "[FirestoreLeaderboardApi] runQuery TIMEOUT on collection: $targetCollection: ${e.message}")
      LeaderboardResult.Failure(FailureKind.TIMEOUT, "Connection timed out", e)
    } catch (e: UnknownHostException) {
      Log.w(TAG, "[FirestoreLeaderboardApi] runQuery NETWORK_UNAVAILABLE (DNS) on collection: $targetCollection: ${e.message}")
      LeaderboardResult.Failure(FailureKind.NETWORK_UNAVAILABLE, "Network unavailable / DNS failed", e)
    } catch (e: IOException) {
      Log.w(TAG, "[FirestoreLeaderboardApi] runQuery I/O exception on collection: $targetCollection: ${e.message}")
      LeaderboardResult.Failure(FailureKind.NETWORK_UNAVAILABLE, "Network I/O failed: ${e.message}", e)
    } catch (e: Exception) {
      Log.e(TAG, "[FirestoreLeaderboardApi] runQuery unexpected parse exception on collection: $targetCollection: ${e.message}", e)
      LeaderboardResult.Failure(FailureKind.MALFORMED_PAYLOAD, "Failed to parse response: ${e.message}", e)
    }
  }

  private fun parseFirestoreError(code: Int, body: String): Pair<FailureKind, String> {
    if (body.isBlank()) {
      return if (code == 404) {
        FailureKind.UNCONFIGURED to "Cloud Firestore database not found in Firebase project ($projectId). Please create the (default) database in Firebase Console. Scores are cached locally."
      } else {
        FailureKind.SECURITY_REJECTED to "Request rejected by server ($code)"
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
          FailureKind.UNCONFIGURED to "Cloud Firestore database not created in Firebase project ($projectId). Please visit Firebase Console -> Firestore Database and click 'Create database'. Scores are safely cached locally."
        }
        status == "FAILED_PRECONDITION" || msg.contains("requires an index", ignoreCase = true) || msg.contains("index is currently building", ignoreCase = true) -> {
          FailureKind.UNCONFIGURED to "Firestore composite index is building in Firebase Console ($projectId). Falling back to single-metric leaderboard."
        }
        reason == "SERVICE_DISABLED" || msg.contains("Cloud Firestore API has not been used", ignoreCase = true) || msg.contains("is disabled", ignoreCase = true) -> {
          FailureKind.UNCONFIGURED to "Cloud Firestore API not enabled in Firebase project ($projectId). Scores cached in offline queue."
        }
        msg.contains("API key not valid", ignoreCase = true) || reason == "API_KEY_INVALID" -> {
          FailureKind.UNCONFIGURED to "Firebase API Key invalid or expired ($code). Scores cached in offline queue."
        }
        status == "PERMISSION_DENIED" -> {
          FailureKind.SECURITY_REJECTED to "Firestore Security Rules denied access. Ensure Anonymous Auth is enabled in Firebase Console. Scores cached in offline queue."
        }
        else -> {
          FailureKind.SECURITY_REJECTED to (if (msg.isNotBlank()) msg else "Request rejected ($code)")
        }
      }
    } catch (_: Exception) {
      if (code == 404) {
        FailureKind.UNCONFIGURED to "Cloud Firestore database not created in Firebase project ($projectId). Please create the (default) database in Firebase Console."
      } else {
        FailureKind.SECURITY_REJECTED to "Request rejected by server ($code)"
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
    const val ANDROID_CERT_SHA1 = "174775252333dc2e8159f5ac6de1019661c3e897"

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
      val cleaned = raw.trim().filter { it.isLetterOrDigit() || it == '_' || it == '-' || it == ' ' }
      return when {
        cleaned.length < 3 -> "Commander"
        cleaned.length > 16 -> cleaned.take(16).trim()
        else -> cleaned
      }
    }
  }
}
