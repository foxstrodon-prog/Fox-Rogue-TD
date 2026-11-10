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

  suspend fun submitScore(item: PendingSubmission): LeaderboardResult<Unit> = withContext(Dispatchers.IO) {
    if (!isConfigured) {
      return@withContext LeaderboardResult.Failure(
        FailureKind.UNCONFIGURED,
        "Leaderboard project ID or API key is not configured"
      )
    }

    val url = "https://firestore.googleapis.com/v1/projects/$projectId/databases/(default)/documents/$collection?documentId=${item.runId}&key=$apiKey"

    val fieldsObj = JSONObject().apply {
      put("callsign", JSONObject().put("stringValue", sanitizeCallsign(item.callsign)))
      put("score", JSONObject().put("integerValue", item.score.toString()))
      put("wavesCleared", JSONObject().put("integerValue", item.wavesCleared.toString()))
      put("scrapEarned", JSONObject().put("integerValue", item.scrapEarned.toString()))
      put("timestampMs", JSONObject().put("integerValue", item.timestampMs.toString()))
      put("isDevTuned", JSONObject().put("booleanValue", false))
      put("clientVersion", JSONObject().put("stringValue", item.clientVersion))
    }
    val requestBodyJson = JSONObject().apply {
      put("fields", fieldsObj)
    }

    val mediaType = "application/json; charset=utf-8".toMediaType()
    val request = Request.Builder()
      .url(url)
      .addHeader("X-Goog-Api-Key", apiKey)
      .addHeader("X-Android-Package", "com.aistudio.roguetd.xtkp")
      .post(requestBodyJson.toString().toRequestBody(mediaType))
      .build()

    try {
      client.newCall(request).awaitResponse().use { response ->
        when (response.code) {
          200, 201 -> LeaderboardResult.Success(Unit)
          409 -> LeaderboardResult.Success(Unit) // Idempotent: already exists
          400, 403 -> LeaderboardResult.Failure(
            FailureKind.SECURITY_REJECTED,
            "Submission rejected by security rules (${response.code})"
          )
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
            put("field", JSONObject().put("fieldPath", "score"))
            put("direction", "DESCENDING")
          })
          put(JSONObject().apply {
            put("field", JSONObject().put("fieldPath", "timestampMs"))
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
          400, 403 -> LeaderboardResult.Failure(
            FailureKind.SECURITY_REJECTED,
            "Query rejected by server (${response.code})"
          )
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

      val callsign = fields.optJSONObject("callsign")?.optString("stringValue") ?: "Commander"
      val score = fields.optJSONObject("score")?.let {
        it.optString("integerValue").toIntOrNull() ?: it.optInt("integerValue", 0)
      } ?: 0
      val waves = fields.optJSONObject("wavesCleared")?.let {
        it.optString("integerValue").toIntOrNull() ?: it.optInt("integerValue", 0)
      } ?: 0
      val scrap = fields.optJSONObject("scrapEarned")?.let {
        it.optString("integerValue").toIntOrNull() ?: it.optInt("integerValue", 0)
      } ?: 0
      val timestampMs = fields.optJSONObject("timestampMs")?.let {
        it.optString("integerValue").toLongOrNull() ?: it.optLong("integerValue", 0L)
      } ?: 0L

      entries.add(
        OnlineScoreEntry(
          id = id,
          rank = currentRank++,
          callsign = sanitizeCallsign(callsign),
          score = score,
          wavesCleared = waves,
          scrapEarned = scrap,
          timestampMs = timestampMs
        )
      )
    }
    return entries
  }

  companion object {
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
