package com.example.data

import android.content.Context
import android.content.SharedPreferences

class SettingsStore(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("radiant_guard_settings", Context.MODE_PRIVATE)

    fun hasMasterPasswordTracker(): Boolean {
        return prefs.contains("master_salt") && prefs.contains("master_hash")
    }

    fun saveMasterAuth(saltBase64: String, hashBase64: String, iterations: Int = 15000) {
        prefs.edit()
            .putString("master_salt", saltBase64)
            .putString("master_hash", hashBase64)
            .putInt("master_iterations", iterations)
            .apply()
    }

    fun getMasterSalt(): String? = prefs.getString("master_salt", null)

    fun getMasterHash(): String? = prefs.getString("master_hash", null)

    fun getMasterIterations(): Int = prefs.getInt("master_iterations", 15000)

    fun savePinLength(length: Int) {
        prefs.edit().putInt("pin_length", length).apply()
    }

    fun getPinLength(): Int = prefs.getInt("pin_length", 4)
    
    fun wipe() {
        prefs.edit().clear().apply()
    }
}
