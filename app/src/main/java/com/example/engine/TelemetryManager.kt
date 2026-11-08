package com.example.engine

import android.content.Context
import android.content.SharedPreferences
import com.example.model.LifetimeTelemetry
import com.example.model.RunRecord
import org.json.JSONArray
import org.json.JSONObject

class TelemetryManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("rogue_td_telemetry", Context.MODE_PRIVATE)

    fun getActivePlayerName(): String {
        return prefs.getString("active_player_name", "Operative") ?: "Operative"
    }

    fun setActivePlayerName(name: String) {
        val trimmed = name.trim().ifBlank { "Operative" }
        prefs.edit().putString("active_player_name", trimmed).apply()
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
                timestamp = obj.optLong("timestamp", System.currentTimeMillis())
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
        }
        prefs.edit().putString("last_run_record", obj.toString()).apply()
    }

    fun updateLastRunPlayerName(newName: String) {
        val trimmed = newName.trim().ifBlank { "Operative" }
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
                    playerName = obj.optString("playerName", "Viper-7"),
                    wavesCleared = obj.optInt("wavesCleared", 0),
                    enemiesKilled = obj.optInt("enemiesKilled", 0),
                    totalDamage = obj.optDouble("totalDamage", 0.0).toFloat(),
                    scrapEarned = obj.optInt("scrapEarned", 0),
                    ascendedTowersCount = obj.optInt("ascendedTowersCount", 0),
                    mvpTower = obj.optString("mvpTower", "Vulcan Minigun"),
                    isVictory = obj.optBoolean("isVictory", false),
                    timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                )
                // Omit any runs with score of 0 or zero combat activity
                if (rec.score > 0) {
                    list.add(rec)
                }
            }
        } catch (_: Exception) {}

        // If empty, add a couple of baseline competitor records
        if (list.isEmpty()) {
            list.add(
                RunRecord(
                    id = "base_1",
                    seed = 849204L,
                    sectorName = "Sector Zero: Cyber Convergence",
                    playerName = "Apex_Ghost",
                    wavesCleared = 14,
                    enemiesKilled = 248,
                    totalDamage = 48500f,
                    scrapEarned = 2650,
                    ascendedTowersCount = 2,
                    mvpTower = "Hellfire Mortar",
                    isVictory = false,
                    timestamp = System.currentTimeMillis() - 86400000L
                )
            )
            list.add(
                RunRecord(
                    id = "base_2",
                    seed = 192842L,
                    sectorName = "Glacial Split Chasm",
                    playerName = "Nova_Prime",
                    wavesCleared = 10,
                    enemiesKilled = 165,
                    totalDamage = 29400f,
                    scrapEarned = 1890,
                    ascendedTowersCount = 1,
                    mvpTower = "Tesla Stormcoil",
                    isVictory = false,
                    timestamp = System.currentTimeMillis() - 172800000L
                )
            )
        }

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
            arr.put(obj)
        }
        prefs.edit().putString("leaderboard_runs", arr.toString()).apply()
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
            totalRuns = prefs.getInt("lt_total_runs", 1),
            totalVictories = prefs.getInt("lt_total_victories", 0),
            totalHostilesKilled = prefs.getInt("lt_total_kills", 413),
            totalScrapMined = prefs.getLong("lt_total_scrap", 4540L),
            totalDamageOutput = prefs.getFloat("lt_total_damage", 77900f).toDouble(),
            bestWaveEver = prefs.getInt("lt_best_wave", 14),
            mostUsedTower = prefs.getString("lt_mvp_tower", "Siege Mortar") ?: "Siege Mortar"
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

    fun getBalanceConfig(): com.example.model.DevBalanceConfig {
        return com.example.model.DevBalanceConfig(
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

    fun saveBalanceConfig(config: com.example.model.DevBalanceConfig) {
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

    fun getTowerPerformance(): List<com.example.model.TowerPerformanceRecord> {
        val jsonStr = prefs.getString("tower_performance_records", "[]") ?: "[]"
        val map = mutableMapOf<String, com.example.model.TowerPerformanceRecord>()
        try {
            val arr = JSONArray(jsonStr)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val name = obj.optString("towerName", "")
                val clsStr = obj.optString("towerClass", "KINETIC_BALLISTIC")
                val cls = try { com.example.model.TowerClass.valueOf(clsStr) } catch (_: Exception) { com.example.model.TowerClass.KINETIC_BALLISTIC }
                if (name.isNotBlank()) {
                    map[name] = com.example.model.TowerPerformanceRecord(
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

        // Baseline defaults if completely empty
        if (map.isEmpty()) {
            map["Vulcan Minigun"] = com.example.model.TowerPerformanceRecord("Vulcan Minigun", com.example.model.TowerClass.KINETIC_BALLISTIC, 24500.0, 142L, 4, 480L)
            map["Twin Flak Cannon"] = com.example.model.TowerPerformanceRecord("Twin Flak Cannon", com.example.model.TowerClass.KINETIC_BALLISTIC, 18900.0, 98L, 3, 510L)
            map["Frost Spire"] = com.example.model.TowerPerformanceRecord("Frost Spire", com.example.model.TowerClass.ELEMENTAL_VOID, 14200.0, 62L, 2, 420L)
            map["Tesla Stormcoil"] = com.example.model.TowerPerformanceRecord("Tesla Stormcoil", com.example.model.TowerClass.ELEMENTAL_VOID, 31200.0, 184L, 3, 680L)
            map["Hellfire Mortar"] = com.example.model.TowerPerformanceRecord("Hellfire Mortar", com.example.model.TowerClass.HEAVY_SIEGE, 42000.0, 215L, 2, 740L)
            map["Titan Railgun"] = com.example.model.TowerPerformanceRecord("Titan Railgun", com.example.model.TowerClass.HEAVY_SIEGE, 38500.0, 89L, 2, 860L)
        }

        return map.values.sortedByDescending { it.totalDamage }
    }

    fun recordTowerSession(towers: List<com.example.model.TowerInstance>) {
        if (towers.isEmpty()) return
        val existing = getTowerPerformance().associateBy { it.towerName }.toMutableMap()
        for (t in towers) {
            val name = t.archetype?.displayName ?: t.type.displayName
            val prev = existing[name] ?: com.example.model.TowerPerformanceRecord(
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

