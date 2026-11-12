package com.example.engine.network

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
  private val collection: String = com.example.BuildConfig.LEADERBOARD_COLLECTION
) {
  val isConfigured: Boolean
    get() = projectId.isNotBlank() && apiKey.isNotBlank()

  suspend fun submitScore(
    item: PendingSubmission,
    idToken: String? = null
  ): LeaderboardResult<Unit> = withContext(Dispatchers.IO) {
    if (!isConfigured) {
      return@withContext LeaderboardResult.Failure(
        FailureKind.UNCONFIGURED,
        "Leaderboard project ID or API key is not configured"
      )
    }

    val url = "https://firestore.googleapis.com/v1/projects/$projectId/databases/(default)/documents/$collection?documentId=${item.runId}&key=$apiKey"

    val sanitizedCallsign = sanitizeCallsign(item.callsign)
    val effectiveUserId = item.userId.ifBlank { "anonymous" }

    val fieldsObj = JSONObject().apply {
      // Primary fields strictly validated by Firestore Security Rules
      put("userId", JSONObject().put("stringValue", effectiveUserId))
      put("username", JSONObject().put("stringValue", sanitizedCallsign))
      put("highScore", JSONObject().put("integerValue", item.score.toString()))
      put("maxWave", JSONObject().put("integerValue", item.wavesCleared.toString()))
      put("timestamp", JSONObject().put("integerValue", item.timestampMs.toString()))
      put("isDevTuned", JSONObject().put("booleanValue", false))
      put("clientVersion", JSONObject().put("stringValue", item.clientVersion))

      // Legacy fields preserved for backward compatibility
      put("callsign", JSONObject().put("stringValue", sanitizedCallsign))
      put("score", JSONObject().put("integerValue", item.score.toString()))
      put("wavesCleared", JSONObject().put("integerValue", item.wavesCleared.toString()))
      put("scrapEarned", JSONObject().put("integerValue", item.scrapEarned.toString()))
      put("timestampMs", JSONObject().put("integerValue", item.timestampMs.toString()))
    }
    val requestBodyJson = JSONObject().apply {
      put("fields", fieldsObj)
    }

    val mediaType = "application/json; charset=utf-8".toMediaType()
    val requestBuilder = Request.Builder()
      .url(url)
      .addHeader("X-Goog-Api-Key", apiKey)
      .addHeader("X-Android-Package", "com.aistudio.roguetd.xtkp")
      .addHeader("X-Android-Cert", ANDROID_CERT_SHA1)

    if (!idToken.isNullOrBlank()) {
      requestBuilder.addHeader("Authorization", "Bearer $idToken")
    }

    val request = requestBuilder
      .post(requestBodyJson.toString().toRequestBody(mediaType))
      .build()

    try {
      client.newCall(request).awaitResponse().use { response ->
        when (response.code) {
          200, 201 -> LeaderboardResult.Success(Unit)
          409 -> LeaderboardResult.Success(Unit) // Idempotent: already exists
          400, 403, 404 -> {
            val errBody = response.body?.string().orEmpty()
            val (kind, msg) = parseFirestoreError(response.code, errBody)
            LeaderboardResult.Failure(kind, msg)
          }
          429 -> LeaderboardResult.Failure(
            FailureKind.RATE_LIMITED,
            "Firestore rate limit reached (429)"
          )
          in 500..599 -> LeaderboardResult.Failure(
            FailureKind.SERVER_ERROR,
            "Firestore server error (${response.code})"
          )
          else -> LeaderboardResult.Failure(
            FailureKind.SERVER_ERROR,
            "Unexpected HTTP response code ${response.code}"
          )
        }
      }
    } catch (e: CancellationException) {
      throw e
    } catch (e: SocketTimeoutException) {
      LeaderboardResult.Failure(FailureKind.TIMEOUT, "Connection timed out", e)
    } catch (e: UnknownHostException) {
      LeaderboardResult.Failure(FailureKind.NETWORK_UNAVAILABLE, "Network unavailable / DNS failed", e)
    } catch (e: IOException) {
      LeaderboardResult.Failure(FailureKind.NETWORK_UNAVAILABLE, "Network I/O failed: ${e.message}", e)
    } catch (e: Exception) {
      LeaderboardResult.Failure(FailureKind.SERVER_ERROR, "Unexpected error: ${e.message}", e)
    }
  }

  suspend fun fetchTopScores(limit: Int = 50): LeaderboardResult<List<OnlineScoreEntry>> = withContext(Dispatchers.IO) {
    if (!isConfigured) {
      return@withContext LeaderboardResult.Failure(
        FailureKind.UNCONFIGURED,
        "Leaderboard project ID or API key is not configured"
      )
    }

    val url = "https://firestore.googleapis.com/v1/projects/$projectId/databases/(default)/documents:runQuery?key=$apiKey"

    val queryJson = JSONObject().apply {
      val structuredQuery = JSONObject().apply {
        put("from", JSONArray().put(JSONObject().put("collectionId", collection)))
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
        put("limit", limit)
      }
      put("structuredQuery", structuredQuery)
    }

    val mediaType = "application/json; charset=utf-8".toMediaType()
    val request = Request.Builder()
      .url(url)
      .addHeader("X-Goog-Api-Key", apiKey)
      .addHeader("X-Android-Package", "com.aistudio.roguetd.xtkp")
      .addHeader("X-Android-Cert", ANDROID_CERT_SHA1)
      .post(queryJson.toString().toRequestBody(mediaType))
      .build()

    try {
      client.newCall(request).awaitResponse().use { response ->
        when (response.code) {
          200 -> {
            val responseBody = response.body?.string().orEmpty()
            if (responseBody.isBlank()) {
              return@withContext LeaderboardResult.Success(emptyList())
            }
            val results = parseQueryResults(responseBody)
            LeaderboardResult.Success(results)
          }
          400, 403, 404 -> {
            val errBody = response.body?.string().orEmpty()
            val (kind, msg) = parseFirestoreError(response.code, errBody)
            LeaderboardResult.Failure(kind, msg)
          }
          429 -> LeaderboardResult.Failure(
            FailureKind.RATE_LIMITED,
            "Firestore rate limit reached (429)"
          )
          in 500..599 -> LeaderboardResult.Failure(
            FailureKind.SERVER_ERROR,
            "Firestore server error (${response.code})"
          )
          else -> LeaderboardResult.Failure(
            FailureKind.SERVER_ERROR,
            "Unexpected HTTP response code ${response.code}"
          )
        }
      }
    } catch (e: CancellationException) {
      throw e
    } catch (e: SocketTimeoutException) {
      LeaderboardResult.Failure(FailureKind.TIMEOUT, "Connection timed out", e)
    } catch (e: UnknownHostException) {
      LeaderboardResult.Failure(FailureKind.NETWORK_UNAVAILABLE, "Network unavailable / DNS failed", e)
    } catch (e: IOException) {
      LeaderboardResult.Failure(FailureKind.NETWORK_UNAVAILABLE, "Network I/O failed: ${e.message}", e)
    } catch (e: Exception) {
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
    var currentRank = 1

    for (i in 0 until array.length()) {
      val item = array.optJSONObject(i) ?: continue
      val doc = item.optJSONObject("document") ?: continue
      val docName = doc.optString("name", "")
      val id = docName.substringAfterLast("/")
      val fields = doc.optJSONObject("fields") ?: continue

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
          rank = currentRank++,
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
  }

  companion object {
    const val ANDROID_CERT_SHA1 = "174775252333dc2e8159f5ac6de1019661c3e897"

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
