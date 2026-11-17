package com.example.engine

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import com.example.engine.storage.PlayerIdentity
import com.example.model.DevBalanceConfig
import com.example.model.LifetimeTelemetry
import com.example.model.RunRecord
import com.example.model.TowerClass
import com.example.model.TowerInstance
import com.example.model.TowerPerformanceRecord
import com.google.firebase.analytics.FirebaseAnalytics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class TelemetryManager @JvmOverloads constructor(
    private val context: Context,
    private val analytics: FirebaseAnalytics? = runCatching { FirebaseAnalytics.getInstance(context) }.getOrNull()
) {
    private val prefs: SharedPreferences = context.getSharedPreferences("rogue_td_telemetry", Context.MODE_PRIVATE)
    private val lock = Any()

    fun getActivePlayerName(): String {
        return PlayerIdentity.getPlayerCallsign(context)
    }

    fun setActivePlayerName(name: String) {
        PlayerIdentity.setPlayerCallsign(context, name)
    }

    fun getLastRun(): RunRecord? = synchronized(lock) {
        val jsonStr = prefs.getString("last_run_record", null) ?: return null
        return try {
            val obj = JSONObject(jsonStr)
            RunRecord(
                id = obj.optString("id", ""),
                seed = obj.optLong("seed", 0L),
                sectorName = obj.optString("sectorName", "Sector Alpha"),
                playerName = obj.optString("playerName", getActivePlayerName()),
                wavesCleared = obj.optInt("wavesCleared", 0),
                enemiesKilled = obj.optInt("enemiesKilled", 0),
                totalDamage = obj.optDouble("totalDamage", 0.0).toFloat(),
                scrapEarned = obj.optInt("scrapEarned", 0),
                ascendedTowersCount = obj.optInt("ascendedTowersCount", 0),
                mvpTower = obj.optString("mvpTower", "Vulcan Minigun"),
                isVictory = obj.optBoolean("isVictory", false),
                timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                isDevTuned = obj.optBoolean("isDevTuned", false)
            )
        } catch (_: Exception) {
            null
        }
    }

    fun saveLastRun(record: RunRecord) = synchronized(lock) {
        val obj = JSONObject().apply {
            put("id", record.id)
            put("seed", record.seed)
            put("sectorName", record.sectorName)
            put("playerName", record.playerName)
            put("wavesCleared", record.wavesCleared)
            put("enemiesKilled", record.enemiesKilled)
            put("totalDamage", record.totalDamage.toDouble())
            put("scrapEarned", record.scrapEarned)
            put("ascendedTowersCount", record.ascendedTowersCount)
            put("mvpTower", record.mvpTower)
            put("isVictory", record.isVictory)
            put("timestamp", record.timestamp)
            put("isDevTuned", record.isDevTuned)
        }
        prefs.edit().putString("last_run_record", obj.toString()).apply()
    }

    fun updateLastRunPlayerName(newName: String) = synchronized(lock) {
        val trimmed = newName.trim().ifBlank { PlayerIdentity.getPlayerCallsign(context) }
        setActivePlayerName(trimmed)

        val lastRun = getLastRun()
        if (lastRun != null) {
            val updated = lastRun.copy(playerName = trimmed)
            saveLastRun(updated)

            // Update in leaderboard list as well
            val list = getLeaderboard().map {
                if (it.id == updated.id || (it.timestamp == updated.timestamp && it.seed == updated.seed)) {
                    it.copy(playerName = trimmed)
                } else {
                    it
                }
            }
            saveLeaderboardList(list)
        }
    }

    fun getLeaderboard(): List<RunRecord> = synchronized(lock) {
        val jsonStr = prefs.getString("leaderboard_runs", "[]") ?: "[]"
        val list = mutableListOf<RunRecord>()
        try {
            val arr = JSONArray(jsonStr)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val rec = RunRecord(
                    id = obj.optString("id", ""),
                    seed = obj.optLong("seed", 0L),
                    sectorName = obj.optString("sectorName", "Sector Alpha"),
                    playerName = obj.optString("playerName", getActivePlayerName()),
                    wavesCleared = obj.optInt("wavesCleared", 0),
                    enemiesKilled = obj.optInt("enemiesKilled", 0),
                    totalDamage = obj.optDouble("totalDamage", 0.0).toFloat(),
                    scrapEarned = obj.optInt("scrapEarned", 0),
                    ascendedTowersCount = obj.optInt("ascendedTowersCount", 0),
                    mvpTower = obj.optString("mvpTower", "Vulcan Minigun"),
                    isVictory = obj.optBoolean("isVictory", false),
                    timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                    isDevTuned = obj.optBoolean("isDevTuned", false)
                )
                // Omit any runs with score of 0 or zero combat activity
                if (rec.score > 0) {
                    list.add(rec)
                }
            }
        } catch (_: Exception) {}

        return list.filter { it.score > 0 }.sortedWith(
            compareByDescending<RunRecord> { it.score }
                .thenByDescending { it.wavesCleared }
                .thenByDescending { it.enemiesKilled }
        ).take(30)
    }

    private fun saveLeaderboardList(list: List<RunRecord>) {
        val arr = JSONArray()
        for (item in list.filter { it.score > 0 }.take(30)) {
            val obj = JSONObject()
            obj.put("id", item.id)
            obj.put("seed", item.seed)
            obj.put("sectorName", item.sectorName)
            obj.put("playerName", item.playerName)
            obj.put("wavesCleared", item.wavesCleared)
            obj.put("enemiesKilled", item.enemiesKilled)
            obj.put("totalDamage", item.totalDamage.toDouble())
            obj.put("scrapEarned", item.scrapEarned)
            obj.put("ascendedTowersCount", item.ascendedTowersCount)
            obj.put("mvpTower", item.mvpTower)
            obj.put("isVictory", item.isVictory)
            obj.put("timestamp", item.timestamp)
            obj.put("isDevTuned", item.isDevTuned)
            arr.put(obj)
        }
        prefs.edit().putString("leaderboard_runs", arr.toString()).apply()
    }

    suspend fun recordRunAsync(record: RunRecord) = withContext(Dispatchers.IO) {
        recordRun(record)
    }

    fun recordRun(record: RunRecord) = synchronized(lock) {
        // If no wave was started or score is 0, do not save to leaderboard
        if (record.score <= 0 && record.wavesCleared <= 0 && record.enemiesKilled <= 0) {
            return
        }

        saveLastRun(record)

        val currentList = getLeaderboard().toMutableList()
        currentList.add(record)
        val topList = currentList.filter { it.score > 0 }.sortedWith(
            compareByDescending<RunRecord> { it.score }
                .thenByDescending { it.wavesCleared }
                .thenByDescending { it.enemiesKilled }
        ).take(30)

        saveLeaderboardList(topList)

        // Update Lifetime Telemetry
        val lt = getLifetimeTelemetry()
        val newLt = lt.copy(
            totalRuns = lt.totalRuns + 1,
            totalVictories = lt.totalVictories + (if (record.isVictory) 1 else 0),
            totalHostilesKilled = lt.totalHostilesKilled + record.enemiesKilled,
            totalScrapMined = lt.totalScrapMined + record.scrapEarned,
            totalDamageOutput = lt.totalDamageOutput + record.totalDamage,
            bestWaveEver = maxOf(lt.bestWaveEver, record.wavesCleared),
            mostUsedTower = record.mvpTower
        )
        saveLifetimeTelemetry(newLt)
    }

    fun getLifetimeTelemetry(): LifetimeTelemetry = synchronized(lock) {
        val totalDamage = if (prefs.contains("lt_total_damage_bits")) {
            java.lang.Double.longBitsToDouble(prefs.getLong("lt_total_damage_bits", 0L))
        } else {
            prefs.getFloat("lt_total_damage", 0f).toDouble()
        }
        LifetimeTelemetry(
            totalRuns = prefs.getInt("lt_total_runs", 0),
            totalVictories = prefs.getInt("lt_total_victories", 0),
            totalHostilesKilled = prefs.getInt("lt_total_kills", 0),
            totalScrapMined = prefs.getLong("lt_total_scrap", 0L),
            totalDamageOutput = totalDamage,
            bestWaveEver = prefs.getInt("lt_best_wave", 0),
            mostUsedTower = prefs.getString("lt_mvp_tower", "None") ?: "None"
        )
    }

    fun getLifetimeTelemetryForPlayer(playerName: String?): LifetimeTelemetry = synchronized(lock) {
        val targetName = playerName?.trim()?.ifBlank { null } ?: getActivePlayerName()
        val playerRuns = getLeaderboard().filter { it.playerName.equals(targetName, ignoreCase = true) }
        if (playerRuns.isEmpty()) {
            return LifetimeTelemetry(
                totalRuns = 0,
                totalVictories = 0,
                totalHostilesKilled = 0,
                totalScrapMined = 0L,
                totalDamageOutput = 0.0,
                bestWaveEver = 0,
                mostUsedTower = "None"
            )
        }

        val topTower = playerRuns.groupBy { it.mvpTower }.maxByOrNull { it.value.size }?.key ?: "Vulcan Minigun"

        return LifetimeTelemetry(
            totalRuns = playerRuns.size,
            totalVictories = playerRuns.count { it.isVictory },
            totalHostilesKilled = playerRuns.sumOf { it.enemiesKilled },
            totalScrapMined = playerRuns.sumOf { it.scrapEarned.toLong() },
            totalDamageOutput = playerRuns.sumOf { it.totalDamage.toDouble() },
            bestWaveEver = playerRuns.maxOfOrNull { it.wavesCleared } ?: 0,
            mostUsedTower = topTower
        )
    }

    private fun saveLifetimeTelemetry(lt: LifetimeTelemetry) {
        prefs.edit()
            .putInt("lt_total_runs", lt.totalRuns)
            .putInt("lt_total_victories", lt.totalVictories)
            .putInt("lt_total_kills", lt.totalHostilesKilled)
            .putLong("lt_total_scrap", lt.totalScrapMined)
            .putLong("lt_total_damage_bits", java.lang.Double.doubleToRawLongBits(lt.totalDamageOutput))
            .putFloat("lt_total_damage", lt.totalDamageOutput.toFloat())
            .putInt("lt_best_wave", lt.bestWaveEver)
            .putString("lt_mvp_tower", lt.mostUsedTower)
            .apply()
    }

    fun getBalanceConfig(): DevBalanceConfig {
        return DevBalanceConfig(
            startingScrap = prefs.getInt("cfg_starting_scrap", 220),
            globalIncomeMultiplier = prefs.getFloat("cfg_global_income", 1.0f),
            kineticDamageMultiplier = prefs.getFloat("cfg_kinetic_dmg", 1.0f),
            elementalDamageMultiplier = prefs.getFloat("cfg_elem_dmg", 1.0f),
            heavyDamageMultiplier = prefs.getFloat("cfg_heavy_dmg", 1.0f),
            waveBonusMultiplier = prefs.getFloat("cfg_wave_bonus", 1.0f),
            enemyHpMultiplier = prefs.getFloat("cfg_enemy_hp", 1.0f),
            buildCostMultiplier = prefs.getFloat("cfg_build_cost", 1.0f),
            easyTargetOpenSlots = prefs.getInt("cfg_easy_open_slots", 15),
            mediumTargetOpenSlots = prefs.getInt("cfg_med_open_slots", 15),
            hardTargetOpenSlots = prefs.getInt("cfg_hard_open_slots", 15),
            minLaneOverlap = prefs.getInt("cfg_min_lane_overlap", 4),
            maxLaneOverlap = prefs.getInt("cfg_max_lane_overlap", 12),
            mapColumns = prefs.getInt("cfg_map_columns", 10),
            mapRows = prefs.getInt("cfg_map_rows", 20),
            masterVolume = prefs.getFloat("cfg_master_vol", 0.7f),
            musicVolume = prefs.getFloat("cfg_music_vol", 0.45f),
            sfxVolume = prefs.getFloat("cfg_sfx_vol", 0.8f),
            upgradeVolume = prefs.getFloat("cfg_upgrade_vol", 0.85f),
            isMuted = prefs.getBoolean("cfg_audio_muted", false),
            musicTrackName = prefs.getString("cfg_music_track", "ROLLING_PLAYLIST") ?: "ROLLING_PLAYLIST",
            kineticSfxSlot = prefs.getInt("cfg_kinetic_sfx_slot", 1),
            elementalSfxSlot = prefs.getInt("cfg_elemental_sfx_slot", 1),
            siegeSfxSlot = prefs.getInt("cfg_siege_sfx_slot", 1),
            stellarSfxSlot = prefs.getInt("cfg_stellar_sfx_slot", 1),
            turretSkinSlot = prefs.getInt("cfg_turret_skin_slot", 1),
            mapThemeSlot = prefs.getInt("cfg_map_theme_slot", 1),
            kineticFireId = prefs.getString("cfg_kinetic_fire_id", null)
                ?: legacySlotToFireId("kinetic", prefs.getInt("cfg_kinetic_sfx_slot", 1)),
            elementalFireId = prefs.getString("cfg_elemental_fire_id", null)
                ?: legacySlotToFireId("elemental", prefs.getInt("cfg_elemental_sfx_slot", 1)),
            siegeFireId = prefs.getString("cfg_siege_fire_id", null)
                ?: legacySlotToFireId("siege", prefs.getInt("cfg_siege_sfx_slot", 1)),
            stellarFireId = prefs.getString("cfg_stellar_fire_id", null)
                ?: legacySlotToFireId("stellar", prefs.getInt("cfg_stellar_sfx_slot", 1)),
            turretSkinId = prefs.getString("cfg_turret_skin_id", null)
                ?: prefs.getInt("cfg_turret_skin_slot", 1).let { if (it in 2..5) "b60_skin_$it" else "default_skin_1" },
            mapThemeId = prefs.getString("cfg_map_theme_id", null)
                ?: prefs.getInt("cfg_map_theme_slot", 1).let { if (it in 2..5) "b60_theme_$it" else "default_theme_1" },
            bgmTrackId = prefs.getString("cfg_bgm_track_id", null)
                ?: legacyMusicNameToId(prefs.getString("cfg_music_track", "ROLLING_PLAYLIST") ?: "ROLLING_PLAYLIST"),
            sfxPackId = prefs.getString("cfg_sfx_pack_id", "default_sfx_pack_cyber_synth")
                ?: "default_sfx_pack_cyber_synth"
        )
    }

    private fun legacySlotToFireId(tag: String, slot: Int): String =
        if (slot in 2..5) "b60_fire_${tag}_$slot" else "default_fire_$tag"

    private fun legacyMusicNameToId(trackName: String): String = when (trackName) {
        "NONE" -> ""
        "ROLLING_PLAYLIST" -> "default_bgm_rolling_playlist"
        "CYBER_AMBIENT" -> "default_bgm_cyber_ambient"
        else -> "b60_bgm_${trackName.lowercase()}"
    }

    fun saveBalanceConfig(config: DevBalanceConfig) {
        prefs.edit()
            .putInt("cfg_starting_scrap", config.startingScrap)
            .putFloat("cfg_global_income", config.globalIncomeMultiplier)
            .putFloat("cfg_kinetic_dmg", config.kineticDamageMultiplier)
            .putFloat("cfg_elem_dmg", config.elementalDamageMultiplier)
            .putFloat("cfg_heavy_dmg", config.heavyDamageMultiplier)
            .putFloat("cfg_wave_bonus", config.waveBonusMultiplier)
            .putFloat("cfg_enemy_hp", config.enemyHpMultiplier)
            .putFloat("cfg_build_cost", config.buildCostMultiplier)
            .putInt("cfg_easy_open_slots", config.easyTargetOpenSlots)
            .putInt("cfg_med_open_slots", config.mediumTargetOpenSlots)
            .putInt("cfg_hard_open_slots", config.hardTargetOpenSlots)
            .putInt("cfg_min_lane_overlap", config.minLaneOverlap)
            .putInt("cfg_max_lane_overlap", config.maxLaneOverlap)
            .putInt("cfg_map_columns", config.mapColumns)
            .putInt("cfg_map_rows", config.mapRows)
            .putFloat("cfg_master_vol", config.masterVolume)
            .putFloat("cfg_music_vol", config.musicVolume)
            .putFloat("cfg_sfx_vol", config.sfxVolume)
            .putFloat("cfg_upgrade_vol", config.upgradeVolume)
            .putBoolean("cfg_audio_muted", config.isMuted)
            .putString("cfg_music_track", config.musicTrackName)
            .putInt("cfg_kinetic_sfx_slot", config.kineticSfxSlot)
            .putInt("cfg_elemental_sfx_slot", config.elementalSfxSlot)
            .putInt("cfg_siege_sfx_slot", config.siegeSfxSlot)
            .putInt("cfg_stellar_sfx_slot", config.stellarSfxSlot)
            .putInt("cfg_turret_skin_slot", config.turretSkinSlot)
            .putInt("cfg_map_theme_slot", config.mapThemeSlot)
            .putString("cfg_kinetic_fire_id", config.kineticFireId)
            .putString("cfg_elemental_fire_id", config.elementalFireId)
            .putString("cfg_siege_fire_id", config.siegeFireId)
            .putString("cfg_stellar_fire_id", config.stellarFireId)
            .putString("cfg_turret_skin_id", config.turretSkinId)
            .putString("cfg_map_theme_id", config.mapThemeId)
            .putString("cfg_bgm_track_id", config.bgmTrackId)
            .putString("cfg_sfx_pack_id", config.sfxPackId)
            .apply()
    }

    private fun readPersistedTowerPerformanceMap(): MutableMap<String, TowerPerformanceRecord> {
        val jsonStr = prefs.getString("tower_performance_records", "[]") ?: "[]"
        val map = mutableMapOf<String, TowerPerformanceRecord>()
        try {
            val arr = JSONArray(jsonStr)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val name = obj.optString("towerName", "")
                val clsStr = obj.optString("towerClass", "KINETIC_BALLISTIC")
                val cls = try { TowerClass.valueOf(clsStr) } catch (_: Exception) { TowerClass.KINETIC_BALLISTIC }
                if (name.isNotBlank()) {
                    map[name] = TowerPerformanceRecord(
                        towerName = name,
                        towerClass = cls,
                        totalDamage = obj.optDouble("totalDamage", 0.0),
                        totalKills = obj.optLong("totalKills", 0L),
                        totalBuilt = obj.optInt("totalBuilt", 0),
                        totalCostInvested = obj.optLong("totalCostInvested", 0L)
                    )
                }
            }
        } catch (_: Exception) {}
        return map
    }

    fun getTowerPerformance(): List<TowerPerformanceRecord> = synchronized(lock) {
        val map = readPersistedTowerPerformanceMap()
        val hasReset = prefs.getBoolean("has_reset_telemetry", false)
        if (map.isEmpty() && !hasReset) {
            return listOf(
                TowerPerformanceRecord("Hellfire Mortar", TowerClass.HEAVY_SIEGE, 42000.0, 215L, 2, 740L),
                TowerPerformanceRecord("Titan Railgun", TowerClass.HEAVY_SIEGE, 38500.0, 89L, 2, 860L),
                TowerPerformanceRecord("Tesla Stormcoil", TowerClass.ELEMENTAL_VOID, 31200.0, 184L, 3, 680L),
                TowerPerformanceRecord("Vulcan Minigun", TowerClass.KINETIC_BALLISTIC, 24500.0, 142L, 4, 480L),
                TowerPerformanceRecord("Twin Flak Cannon", TowerClass.KINETIC_BALLISTIC, 18900.0, 98L, 3, 510L),
                TowerPerformanceRecord("Frost Spire", TowerClass.ELEMENTAL_VOID, 14200.0, 62L, 2, 420L)
            )
        }

        return map.values.sortedByDescending { it.totalDamage }
    }

    fun resetTowerPerformance() = synchronized(lock) {
        prefs.edit()
            .putString("tower_performance_records", "[]")
            .putBoolean("has_reset_telemetry", true)
            .apply()
    }

    suspend fun recordTowerSessionAsync(towers: List<TowerInstance>) = withContext(Dispatchers.IO) {
        recordTowerSession(towers)
    }

    fun recordTowerSession(towers: List<TowerInstance>) = synchronized(lock) {
        if (towers.isEmpty()) return
        val existing = readPersistedTowerPerformanceMap()
        for (t in towers) {
            val name = t.archetype?.displayName ?: t.type.displayName
            val prev = existing[name] ?: TowerPerformanceRecord(
                towerName = name,
                towerClass = t.towerClass
            )
            val invested = (t.archetype?.baseCost ?: t.type.baseCost) + if (t.isTier3) 250 else 0
            existing[name] = prev.copy(
                totalDamage = prev.totalDamage + t.totalDamage.toDouble(),
                totalKills = prev.totalKills + t.kills.toLong(),
                totalBuilt = prev.totalBuilt + 1,
                totalCostInvested = prev.totalCostInvested + invested
            )
        }

        val arr = JSONArray()
        for (item in existing.values) {
            val obj = JSONObject()
            obj.put("towerName", item.towerName)
            obj.put("towerClass", item.towerClass.name)
            obj.put("totalDamage", item.totalDamage)
            obj.put("totalKills", item.totalKills)
            obj.put("totalBuilt", item.totalBuilt)
            obj.put("totalCostInvested", item.totalCostInvested)
            arr.put(obj)
        }
        prefs.edit().putString("tower_performance_records", arr.toString()).apply()
    }

    fun logWaveStart(waveIndex: Int) {
        val bundle = Bundle().apply {
            putString(FirebaseAnalytics.Param.LEVEL_NAME, "Wave_$waveIndex")
            putLong("wave_number", waveIndex.toLong())
        }
        analytics?.logEvent(FirebaseAnalytics.Event.LEVEL_START, bundle)
    }

    fun logWaveComplete(waveIndex: Int, scrapEarned: Int) {
        val bundle = Bundle().apply {
            putString(FirebaseAnalytics.Param.LEVEL_NAME, "Wave_$waveIndex")
            putLong("wave_number", waveIndex.toLong())
            putLong("scrap_earned", scrapEarned.toLong())
        }
        analytics?.logEvent("level_complete", bundle)
    }

    fun logLevelFail(waveFailed: Int, towersBuilt: Int, currencyRemaining: Int, mvpTowerClass: String) {
        val bundle = Bundle().apply {
            putString(FirebaseAnalytics.Param.LEVEL_NAME, "Wave_$waveFailed")
            putLong("wave_failed", waveFailed.toLong())
            putLong("towers_built", towersBuilt.toLong())
            putLong("currency_remaining", currencyRemaining.toLong())
            putString("mvp_tower_class", mvpTowerClass)
        }
        analytics?.logEvent("level_fail", bundle)
    }

    fun logTutorialStart() {
        analytics?.logEvent(FirebaseAnalytics.Event.TUTORIAL_BEGIN, null)
        analytics?.logEvent("tutorial_start", null)
    }

    fun logTutorialComplete() {
        analytics?.logEvent(FirebaseAnalytics.Event.TUTORIAL_COMPLETE, null)
    }
}
