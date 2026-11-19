package com.example.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsStore(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("radiant_guard_settings", Context.MODE_PRIVATE)

    /** Current theme mode: "system", "light" or "dark". Observable so MainActivity can re-theme live. */
    private val _themeMode = MutableStateFlow(prefs.getString("theme_mode", "system") ?: "system")
    val themeModeFlow: StateFlow<String> = _themeMode.asStateFlow()

    fun getThemeMode(): String = _themeMode.value

    fun setThemeMode(mode: String) {
        val safe = when (mode) {
            "light", "dark" -> mode
            else -> "system"
        }
        prefs.edit().putString("theme_mode", safe).apply()
        _themeMode.value = safe
    }

    fun hasMasterPasswordTracker(): Boolean {
        return prefs.contains("master_salt") && prefs.contains("master_hash")
    }

    /**
     * Single atomic write of the whole master-auth record (salt + hash + iterations
     * + PIN length) in one edit().commit(). Previously salt/hash and PIN length were
     * two separate .apply() writes, so a crash between them could leave a
     * mismatched salt/hash/length pair and a permanently un-unlockable vault.
     */
    fun saveMasterAuth(saltBase64: String, hashBase64: String, iterations: Int = 15000, pinLength: Int = -1) {
        val editor = prefs.edit()
            .putString("master_salt", saltBase64)
            .putString("master_hash", hashBase64)
            .putInt("master_iterations", iterations)
        if (pinLength > 0) editor.putInt("pin_length", pinLength)
        editor.commit()
    }

    /** Backwards-compatible overload: delegates to the atomic 4-arg write. */
    fun saveMasterAuth(saltBase64: String, hashBase64: String, iterations: Int) {
        saveMasterAuth(saltBase64, hashBase64, iterations, -1)
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
        _themeMode.value = "system"
    }
}
