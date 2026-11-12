package com.example.engine.storage

import android.content.Context
import androidx.core.util.AtomicFile
import com.example.model.OnlineScoreEntry
import com.example.model.PendingSubmission
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream

class LeaderboardStore(context: Context) {
  private val baseDir = File(context.filesDir, "leaderboard").apply {
    if (!exists()) mkdirs()
  }

  private val cacheFile = File(baseDir, "cache.json")
  private val pendingFile = File(baseDir, "pending_queue.json")
  private val submittedFile = File(baseDir, "submitted_ids.json")

  private val mutex = Mutex()

  suspend fun getCachedLeaderboard(): List<OnlineScoreEntry> = withContext(Dispatchers.IO) {
    mutex.withLock {
      if (!cacheFile.exists()) return@withContext emptyList()
      try {
        val atomicFile = AtomicFile(cacheFile)
        val jsonStr = String(atomicFile.readFully(), Charsets.UTF_8)
        val array = JSONArray(jsonStr)
        val list = ArrayList<OnlineScoreEntry>(array.length())
        for (i in 0 until array.length()) {
          val obj = array.optJSONObject(i) ?: continue
          list.add(
            OnlineScoreEntry(
              id = obj.optString("id", ""),
              rank = obj.optInt("rank", i + 1),
              callsign = obj.optString("callsign", "Commander"),
              score = obj.optInt("score", 0),
              wavesCleared = obj.optInt("wavesCleared", 0),
              scrapEarned = obj.optInt("scrapEarned", 0),
              timestampMs = obj.optLong("timestampMs", 0L),
              isLocalPlayer = obj.optBoolean("isLocalPlayer", false),
              userId = obj.optString("userId", "")
            )
          )
        }
        list
      } catch (e: Exception) {
        emptyList()
      }
    }
  }

  suspend fun saveCachedLeaderboard(entries: List<OnlineScoreEntry>) = withContext(Dispatchers.IO) {
    mutex.withLock {
      try {
        val array = JSONArray()
        for (entry in entries) {
          val obj = JSONObject().apply {
            put("id", entry.id)
            put("rank", entry.rank)
            put("callsign", entry.callsign)
            put("score", entry.score)
            put("wavesCleared", entry.wavesCleared)
            put("scrapEarned", entry.scrapEarned)
            put("timestampMs", entry.timestampMs)
            put("isLocalPlayer", entry.isLocalPlayer)
            put("userId", entry.userId)
          }
          array.put(obj)
        }
        writeAtomic(cacheFile, array.toString())
      } catch (_: Exception) {
      }
    }
  }

  suspend fun getPendingQueue(): List<PendingSubmission> = withContext(Dispatchers.IO) {
    mutex.withLock {
      readPendingListInternal()
    }
  }

  suspend fun enqueuePending(submission: PendingSubmission) = withContext(Dispatchers.IO) {
    mutex.withLock {
      val now = System.currentTimeMillis()
      val sevenDaysMs = 7L * 24 * 60 * 60 * 1000
      val current = readPendingListInternal()
        .filter { (now - it.timestampMs) < sevenDaysMs && it.runId != submission.runId }
        .toMutableList()

      // Insert new submission at the front
      current.add(0, submission)

      // Cap at MAX_PENDING_QUEUE
      val capped = if (current.size > MAX_PENDING_QUEUE) current.take(MAX_PENDING_QUEUE) else current
      writePendingListInternal(capped)
    }
  }

  suspend fun removePending(runId: String) = withContext(Dispatchers.IO) {
    mutex.withLock {
      val current = readPendingListInternal().filter { it.runId != runId }
      writePendingListInternal(current)
    }
  }

  suspend fun incrementAttempt(runId: String) = withContext(Dispatchers.IO) {
    mutex.withLock {
      val now = System.currentTimeMillis()
      val current = readPendingListInternal().map {
        if (it.runId == runId) {
          it.copy(attempts = it.attempts + 1, lastAttemptMs = now)
        } else {
          it
        }
      }
      writePendingListInternal(current)
    }
  }

  suspend fun isAlreadySubmitted(runId: String): Boolean = withContext(Dispatchers.IO) {
    mutex.withLock {
      readSubmittedIdsInternal().contains(runId)
    }
  }

  suspend fun recordSubmittedId(runId: String) = withContext(Dispatchers.IO) {
    mutex.withLock {
      val ids = readSubmittedIdsInternal().toMutableList()
      ids.remove(runId)
      ids.add(0, runId)
      val capped = if (ids.size > MAX_SUBMITTED_IDS) ids.take(MAX_SUBMITTED_IDS) else ids
      try {
        val array = JSONArray()
        capped.forEach { array.put(it) }
        writeAtomic(submittedFile, array.toString())
      } catch (_: Exception) {
      }
    }
  }

  private fun readPendingListInternal(): List<PendingSubmission> {
    if (!pendingFile.exists()) return emptyList()
    return try {
      val atomicFile = AtomicFile(pendingFile)
      val jsonStr = String(atomicFile.readFully(), Charsets.UTF_8)
      val array = JSONArray(jsonStr)
      val list = ArrayList<PendingSubmission>(array.length())
      for (i in 0 until array.length()) {
        val obj = array.optJSONObject(i) ?: continue
        list.add(
          PendingSubmission(
            runId = obj.optString("runId", ""),
            callsign = obj.optString("callsign", "Commander"),
            score = obj.optInt("score", 0),
            wavesCleared = obj.optInt("wavesCleared", 0),
            scrapEarned = obj.optInt("scrapEarned", 0),
            totalDamage = obj.optLong("totalDamage", 0L),
            timestampMs = obj.optLong("timestampMs", 0L),
            clientVersion = obj.optString("clientVersion", "0.0.0.008"),
            attempts = obj.optInt("attempts", 0),
            lastAttemptMs = obj.optLong("lastAttemptMs", 0L),
            userId = obj.optString("userId", "")
          )
        )
      }
      list
    } catch (_: Exception) {
      emptyList()
    }
  }

  private fun writePendingListInternal(list: List<PendingSubmission>) {
    try {
      val array = JSONArray()
      for (item in list) {
        val obj = JSONObject().apply {
          put("runId", item.runId)
          put("callsign", item.callsign)
          put("score", item.score)
          put("wavesCleared", item.wavesCleared)
          put("scrapEarned", item.scrapEarned)
          put("totalDamage", item.totalDamage)
          put("timestampMs", item.timestampMs)
          put("clientVersion", item.clientVersion)
          put("attempts", item.attempts)
          put("lastAttemptMs", item.lastAttemptMs)
          put("userId", item.userId)
        }
        array.put(obj)
      }
      writeAtomic(pendingFile, array.toString())
    } catch (_: Exception) {
    }
  }

  private fun readSubmittedIdsInternal(): List<String> {
    if (!submittedFile.exists()) return emptyList()
    return try {
      val atomicFile = AtomicFile(submittedFile)
      val jsonStr = String(atomicFile.readFully(), Charsets.UTF_8)
      val array = JSONArray(jsonStr)
      val list = ArrayList<String>(array.length())
      for (i in 0 until array.length()) {
        list.add(array.optString(i))
      }
      list
    } catch (_: Exception) {
      emptyList()
    }
  }

  private fun writeAtomic(file: File, content: String) {
    val atomicFile = AtomicFile(file)
    var stream: FileOutputStream? = null
    try {
      stream = atomicFile.startWrite()
      stream.write(content.toByteArray(Charsets.UTF_8))
      atomicFile.finishWrite(stream)
    } catch (e: Exception) {
      if (stream != null) {
        atomicFile.failWrite(stream)
      }
      throw e
    }
  }

  companion object {
    private const val MAX_PENDING_QUEUE = 20
    private const val MAX_SUBMITTED_IDS = 100
  }
}
