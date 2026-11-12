package com.example.engine

import android.util.Log
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings

class RemoteConfigManager @JvmOverloads constructor(
    private val remoteConfig: FirebaseRemoteConfig? = runCatching { FirebaseRemoteConfig.getInstance() }.getOrNull()
) {

    val waveHpMultiplier: Float
        get() = (remoteConfig?.getDouble(KEY_WAVE_HP_MULTIPLIER) ?: 1.0).toFloat().coerceIn(0.2f, 5.0f)

    val scrapBountyMultiplier: Float
        get() = (remoteConfig?.getDouble(KEY_SCRAP_BOUNTY_MULTIPLIER) ?: 1.0).toFloat().coerceIn(0.2f, 5.0f)

    val bossHealthMultiplier: Float
        get() = (remoteConfig?.getDouble(KEY_BOSS_HEALTH_MULTIPLIER) ?: 1.0).toFloat().coerceIn(0.2f, 5.0f)

    val harmonicGridDamageBoost: Float
        get() = (remoteConfig?.getDouble(KEY_HARMONIC_GRID_DAMAGE_BOOST) ?: 0.20).toFloat().coerceIn(0.05f, 1.0f)

    fun initialize() {
        if (remoteConfig == null) {
            Log.d(TAG, "FirebaseRemoteConfig not available, using hardcoded balance defaults")
            return
        }

        try {
            val defaults = mapOf<String, Any>(
                KEY_WAVE_HP_MULTIPLIER to 1.0,
                KEY_SCRAP_BOUNTY_MULTIPLIER to 1.0,
                KEY_BOSS_HEALTH_MULTIPLIER to 1.0,
                KEY_HARMONIC_GRID_DAMAGE_BOOST to 0.20
            )
            remoteConfig.setDefaultsAsync(defaults)

            val configSettings = FirebaseRemoteConfigSettings.Builder()
                .setMinimumFetchIntervalInSeconds(3600)
                .build()
            remoteConfig.setConfigSettingsAsync(configSettings)

            remoteConfig.fetchAndActivate().addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "RemoteConfig fetch & activate successful. waveHpMul=$waveHpMultiplier, scrapMul=$scrapBountyMultiplier")
                } else {
                    Log.w(TAG, "RemoteConfig fetch failed: ${task.exception?.message}")
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "RemoteConfig initialization error: ${e.message}")
        }
    }

    companion object {
        private const val TAG = "RemoteConfigManager"
        const val KEY_WAVE_HP_MULTIPLIER = "wave_hp_multiplier"
        const val KEY_SCRAP_BOUNTY_MULTIPLIER = "scrap_bounty_multiplier"
        const val KEY_BOSS_HEALTH_MULTIPLIER = "boss_health_multiplier"
        const val KEY_HARMONIC_GRID_DAMAGE_BOOST = "harmonic_grid_damage_boost"
    }
}
