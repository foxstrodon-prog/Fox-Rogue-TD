package com.example.engine

import android.content.Context
import android.content.SharedPreferences
import com.example.model.GlobalBoardStatus
import com.example.model.LeaderboardEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.UUID

class LeaderboardService(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("rogue_td_leaderboard_prefs", Context.MODE_PRIVATE)

    companion object {
        // Zero-setup default public Dreamlo leaderboard key for Rogue TD Dual Front
        const val DEFAULT_DREAMLO_PUBLIC: String = "672fda008f40bb11681283bf"
        private const val BASE_URL: String = "https://dreamlo.com/lb"
    }

    private val okHttpClient by lazy {
        OkHttpClient.Builder()
            .followRedirects(true)
            .build()
    }

    /**
     * Gets or creates an anonymous persistent device UUID.
     */
    fun getOrCreateDeviceUuid(): String {
        var uuid = prefs.getString("device_uuid", null)
        if (uuid.isNullOrBlank()) {
            uuid = UUID.randomUUID().toString().take(6).uppercase()
            prefs.edit().putString("device_uuid", uuid).apply()
        }
        return uuid
    }

    fun getDefaultPlayerName(): String {
        return "Commander-${getOrCreateDeviceUuid()}"
    }

    private fun getPublicKey(): String {
        return prefs.getString("custom_dreamlo_public", DEFAULT_DREAMLO_PUBLIC) ?: DEFAULT_DREAMLO_PUBLIC
    }

    fun getPrivateKey(): String? {
        return prefs.getString("custom_dreamlo_private", null)
    }

    /**
     * Submits a run score to the online leaderboard using OkHttp.
     */
    suspend fun submitScore(
        playerName: String,
        score: Int,
        wavesCleared: Int,
        scrapEarned: Int,
        isDevTuned: Boolean
    ): Boolean = withContext(Dispatchers.IO) {
        if (score <= 0 && wavesCleared <= 0) return@withContext false
        val privateKey = getPrivateKey() ?: return@withContext false
        if (privateKey.isBlank()) return@withContext false

        try {
            val uuid = getOrCreateDeviceUuid()

            var safeName = playerName.trim().ifBlank { getDefaultPlayerName() }
            if (isDevTuned && !safeName.contains("[DEV]", ignoreCase = true)) {
                safeName = "$safeName [DEV]"
            }
            // Dreamlo names cannot have slashes or spaces
            val cleanName = safeName.replace("/", "-").replace("\\", "-").take(20)
            val encodedName = URLEncoder.encode(cleanName, StandardCharsets.UTF_8.toString()).replace("+", "%20")

            val meta = "scrap=$scrapEarned|dev=$isDevTuned|uuid=$uuid|waves=$wavesCleared"
            val encodedMeta = URLEncoder.encode(meta, StandardCharsets.UTF_8.toString()).replace("+", "%20")

            // Dreamlo sorts ties in ascending order of seconds. Map tiebreaker so higher waves rank higher.
            val tieBreaker = (1000 - wavesCleared).coerceAtLeast(0)
            val url = "$BASE_URL/$privateKey/add/$encodedName/$score/$tieBreaker/$encodedMeta"

            val request = Request.Builder().url(url).build()
            val response = okHttpClient.newCall(request).execute()
            response.isSuccessful
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Fetches top 50 global scores using OkHttp and deserializes into List<LeaderboardEntry>.
     * Falls back to locally cached scores if network fails.
     */
    suspend fun fetchLeaderboard(): Pair<List<LeaderboardEntry>, GlobalBoardStatus> =
        withContext(Dispatchers.IO) {
            val publicKey = getPublicKey()
            val url = "$BASE_URL/$publicKey/json/50"

            try {
                val request = Request.Builder().url(url).build()
                val response = okHttpClient.newCall(request).execute()
                if (response.isSuccessful) {
                    val bodyText = response.body?.string().orEmpty()
                    val entries = parseDreamloJson(bodyText)
                    saveCachedLeaderboard(entries)
                    return@withContext Pair(entries, GlobalBoardStatus.ONLINE)
                }
                // Fallback to cached
                val cached = getCachedLeaderboard()
                Pair(cached, if (cached.isNotEmpty()) GlobalBoardStatus.OFFLINE_CACHED else GlobalBoardStatus.ONLINE)
            } catch (_: Exception) {
                val cached = getCachedLeaderboard()
                Pair(cached, GlobalBoardStatus.OFFLINE_CACHED)
            }
        }

    private fun parseDreamloJson(jsonText: String): List<LeaderboardEntry> {
        val list = mutableListOf<LeaderboardEntry>()
        try {
            val root = JSONObject(jsonText)
            val dreamlo = root.optJSONObject("dreamlo") ?: return emptyList()
            val lb = dreamlo.optJSONObject("leaderboard") ?: return emptyList()
            val entryObj = lb.opt("entry") ?: return emptyList()

            when (entryObj) {
                is JSONArray -> {
                    for (i in 0 until entryObj.length()) {
                        val item = entryObj.getJSONObject(i)
                        parseEntry(item, i + 1)?.let { list.add(it) }
                    }
                }
                is JSONObject -> {
                    parseEntry(entryObj, 1)?.let { list.add(it) }
                }
            }
        } catch (_: Exception) {}
        return list
    }

    private fun parseEntry(obj: JSONObject, rank: Int): LeaderboardEntry? {
        val rawName = obj.optString("name", "Unknown Commander")
        val decodedName = try {
            URLDecoder.decode(rawName, StandardCharsets.UTF_8.toString())
        } catch (_: Exception) {
            rawName
        }
        val score = obj.optInt("score", 0)
        val seconds = obj.optInt("seconds", 0) // Used for wavesCleared
        val rawText = obj.optString("text", "")
        val text = try {
            URLDecoder.decode(rawText, StandardCharsets.UTF_8.toString())
        } catch (_: Exception) {
            rawText
        }
        val date = obj.optString("date", "")

        val isDev = decodedName.contains("[DEV]", ignoreCase = true) || text.contains("dev=true", ignoreCase = true)
        var scrap = 0
        var uuid = ""
        var extractedWaves: Int? = null
        if (text.isNotBlank()) {
            val parts = text.split("|")
            for (p in parts) {
                if (p.startsWith("scrap=")) {
                    scrap = p.removePrefix("scrap=").toIntOrNull() ?: 0
                } else if (p.startsWith("uuid=")) {
                    uuid = p.removePrefix("uuid=")
                } else if (p.startsWith("waves=")) {
                    extractedWaves = p.removePrefix("waves=").toIntOrNull()
                }
            }
        }

        val wavesCleared = extractedWaves ?: (if (seconds > 0 && seconds <= 1000) (1000 - seconds).coerceAtLeast(0) else seconds)
        val displayName = decodedName.replace("[DEV]", "").trim().ifBlank { "Commander" }

        return LeaderboardEntry(
            rank = rank,
            name = displayName,
            score = score,
            wavesCleared = wavesCleared,
            scrapEarned = scrap,
            isDevTuned = isDev,
            date = date,
            deviceUuid = uuid
        )
    }

    fun getCachedLeaderboard(): List<LeaderboardEntry> {
        val jsonStr = prefs.getString("cached_online_leaderboard", null) ?: return emptyList()
        val list = mutableListOf<LeaderboardEntry>()
        try {
            val arr = JSONArray(jsonStr)
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                list.add(
                    LeaderboardEntry(
                        rank = o.optInt("rank", i + 1),
                        name = o.optString("name", "Commander"),
                        score = o.optInt("score", 0),
                        wavesCleared = o.optInt("wavesCleared", 0),
                        scrapEarned = o.optInt("scrapEarned", 0),
                        isDevTuned = o.optBoolean("isDevTuned", false),
                        date = o.optString("date", ""),
                        deviceUuid = o.optString("deviceUuid", "")
                    )
                )
            }
        } catch (_: Exception) {}
        return list
    }

    private fun saveCachedLeaderboard(entries: List<LeaderboardEntry>) {
        val arr = JSONArray()
        for (e in entries) {
            val o = JSONObject().apply {
                put("rank", e.rank)
                put("name", e.name)
                put("score", e.score)
                put("wavesCleared", e.wavesCleared)
                put("scrapEarned", e.scrapEarned)
                put("isDevTuned", e.isDevTuned)
                put("date", e.date)
                put("deviceUuid", e.deviceUuid)
            }
            arr.put(o)
        }
        prefs.edit().putString("cached_online_leaderboard", arr.toString()).apply()
    }
}
