package com.example.engine

import android.content.Context
import android.content.SharedPreferences
import com.example.engine.storage.PlayerIdentity
import com.example.model.DevBalanceConfig
import com.example.model.LifetimeTelemetry
import com.example.model.RunRecord
import com.example.model.TowerClass
import com.example.model.TowerInstance
import com.example.model.TowerPerformanceRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class TelemetryManager(private val context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("rogue_td_telemetry", Context.MODE_PRIVATE)

    fun getActivePlayerName(): String {
        return PlayerIdentity.getPlayerCallsign(context)
    }

    fun setActivePlayerName(name: String) {
        PlayerIdentity.setPlayerCallsign(context, name)
    }

    fun getLastRun(): RunRecord? {
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

    fun saveLastRun(record: RunRecord) {
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

    fun updateLastRunPlayerName(newName: String) {
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

    fun getLeaderboard(): List<RunRecord> {
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

    fun recordRun(record: RunRecord) {
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

    fun getLifetimeTelemetry(): LifetimeTelemetry {
        return LifetimeTelemetry(
            totalRuns = prefs.getInt("lt_total_runs", 0),
            totalVictories = prefs.getInt("lt_total_victories", 0),
            totalHostilesKilled = prefs.getInt("lt_total_kills", 0),
            totalScrapMined = prefs.getLong("lt_total_scrap", 0L),
            totalDamageOutput = prefs.getFloat("lt_total_damage", 0f).toDouble(),
            bestWaveEver = prefs.getInt("lt_best_wave", 0),
            mostUsedTower = prefs.getString("lt_mvp_tower", "None") ?: "None"
        )
    }

    fun getLifetimeTelemetryForPlayer(playerName: String?): LifetimeTelemetry {
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
            buildCostMultiplier = prefs.getFloat("cfg_build_cost", 1.0f)
        )
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
            .apply()
    }

    fun getTowerPerformance(): List<TowerPerformanceRecord> {
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

        val hasReset = prefs.getBoolean("has_reset_telemetry", false)
        if (map.isEmpty() && !hasReset) {
            map["Vulcan Minigun"] = TowerPerformanceRecord("Vulcan Minigun", TowerClass.KINETIC_BALLISTIC, 24500.0, 142L, 4, 480L)
            map["Twin Flak Cannon"] = TowerPerformanceRecord("Twin Flak Cannon", TowerClass.KINETIC_BALLISTIC, 18900.0, 98L, 3, 510L)
            map["Frost Spire"] = TowerPerformanceRecord("Frost Spire", TowerClass.ELEMENTAL_VOID, 14200.0, 62L, 2, 420L)
            map["Tesla Stormcoil"] = TowerPerformanceRecord("Tesla Stormcoil", TowerClass.ELEMENTAL_VOID, 31200.0, 184L, 3, 680L)
            map["Hellfire Mortar"] = TowerPerformanceRecord("Hellfire Mortar", TowerClass.HEAVY_SIEGE, 42000.0, 215L, 2, 740L)
            map["Titan Railgun"] = TowerPerformanceRecord("Titan Railgun", TowerClass.HEAVY_SIEGE, 38500.0, 89L, 2, 860L)
        }

        return map.values.sortedByDescending { it.totalDamage }
    }

    fun resetTowerPerformance() {
        prefs.edit()
            .putString("tower_performance_records", "[]")
            .putBoolean("has_reset_telemetry", true)
            .apply()
    }

    suspend fun recordTowerSessionAsync(towers: List<TowerInstance>) = withContext(Dispatchers.IO) {
        recordTowerSession(towers)
    }

    fun recordTowerSession(towers: List<TowerInstance>) {
        if (towers.isEmpty()) return
        val existing = getTowerPerformance().associateBy { it.towerName }.toMutableMap()
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
}
