package com.example.engine

import android.content.Context
import android.content.SharedPreferences
import com.example.model.LifetimeTelemetry
import com.example.model.RunRecord
import org.json.JSONArray
import org.json.JSONObject

class TelemetryManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("rogue_td_telemetry", Context.MODE_PRIVATE)

    fun getLeaderboard(): List<RunRecord> {
        val jsonStr = prefs.getString("leaderboard_runs", "[]") ?: "[]"
        val list = mutableListOf<RunRecord>()
        try {
            val arr = JSONArray(jsonStr)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    RunRecord(
                        id = obj.optString("id", ""),
                        seed = obj.optLong("seed", 0L),
                        sectorName = obj.optString("sectorName", "Sector Alpha"),
                        wavesCleared = obj.optInt("wavesCleared", 0),
                        enemiesKilled = obj.optInt("enemiesKilled", 0),
                        totalDamage = obj.optDouble("totalDamage", 0.0).toFloat(),
                        scrapEarned = obj.optInt("scrapEarned", 0),
                        ascendedTowersCount = obj.optInt("ascendedTowersCount", 0),
                        mvpTower = obj.optString("mvpTower", "Vulcan Minigun"),
                        isVictory = obj.optBoolean("isVictory", false),
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                    )
                )
            }
        } catch (_: Exception) {}

        // If empty, add a couple of cool baseline competitor records so leaderboard isn't blank on launch
        if (list.isEmpty()) {
            list.add(
                RunRecord(
                    id = "base_1",
                    seed = 849204L,
                    sectorName = "Sector Zero: Cyber Core",
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

        return list.sortedWith(
            compareByDescending<RunRecord> { it.wavesCleared }
                .thenByDescending { it.enemiesKilled }
                .thenByDescending { it.totalDamage }
        ).take(20)
    }

    fun recordRun(record: RunRecord) {
        val currentList = getLeaderboard().toMutableList()
        currentList.add(record)
        val topList = currentList.sortedWith(
            compareByDescending<RunRecord> { it.wavesCleared }
                .thenByDescending { it.enemiesKilled }
                .thenByDescending { it.totalDamage }
        ).take(25)

        val arr = JSONArray()
        for (item in topList) {
            val obj = JSONObject()
            obj.put("id", item.id)
            obj.put("seed", item.seed)
            obj.put("sectorName", item.sectorName)
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
}
