package com.example.engine.storage

import android.content.Context
import java.util.UUID

object PlayerIdentity {
  private const val PREFS_NAME = "rogue_td_player_identity"
  private const val KEY_DEVICE_UUID_V2 = "player_device_uuid_v2"
  private const val KEY_PLAYER_CALLSIGN = "player_callsign"

  fun getOrCreateDeviceUuid(context: Context): String {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    var uuid = prefs.getString(KEY_DEVICE_UUID_V2, null)
    if (uuid.isNullOrBlank()) {
      uuid = UUID.randomUUID().toString()
      prefs.edit().putString(KEY_DEVICE_UUID_V2, uuid).apply()
    }
    return uuid
  }

  fun getPlayerCallsign(context: Context): String {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val saved = prefs.getString(KEY_PLAYER_CALLSIGN, null)
    if (!saved.isNullOrBlank()) {
      return saved
    }
    val suffix = getOrCreateDeviceUuid(context).replace("-", "").take(4).uppercase()
    return "Commander-$suffix"
  }

  fun setPlayerCallsign(context: Context, callsign: String) {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    prefs.edit().putString(KEY_PLAYER_CALLSIGN, callsign.trim()).apply()
  }
}
