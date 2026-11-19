package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.SettingsStore
import com.example.data.VaultRepository
import com.example.security.CryptoManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class AuthScreenState {
    LOADING,
    SETUP_REQUIRED,
    LOGIN_REQUIRED,
    AUTHENTICATED
}

data class AuthUiState(
    val screenState: AuthScreenState = AuthScreenState.LOADING,
    val error: String? = null,
    val isUnlocking: Boolean = false,
    val isChangingPin: Boolean = false,
    val pinLength: Int = 4
)

class AuthViewModel(
    private val settingsStore: SettingsStore,
    private val vaultRepository: VaultRepository? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState(pinLength = settingsStore.getPinLength()))
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        checkAuthState()
    }

    fun checkAuthState() {
        val configuredLength = settingsStore.getPinLength()
        if (CryptoManager.isSessionActive()) {
            _uiState.update {
                it.copy(
                    screenState = AuthScreenState.AUTHENTICATED,
                    isUnlocking = false,
                    pinLength = configuredLength
                )
            }
        } else if (settingsStore.hasMasterPasswordTracker()) {
            _uiState.update {
                it.copy(
                    screenState = AuthScreenState.LOGIN_REQUIRED,
                    isUnlocking = false,
                    pinLength = configuredLength
                )
            }
        } else {
            _uiState.update {
                it.copy(
                    screenState = AuthScreenState.SETUP_REQUIRED,
                    isUnlocking = false,
                    pinLength = configuredLength
                )
            }
        }
    }

    fun setupPin(pin: String, chosenLength: Int) {
        if (pin.length != chosenLength) {
            _uiState.update { it.copy(error = "PIN must be exactly $chosenLength digits") }
            return
        }
        if (!pin.all { it.isDigit() }) {
            _uiState.update { it.copy(error = "PIN must only contain numbers") }
            return
        }

        _uiState.update { it.copy(isUnlocking = true, error = null) }

        viewModelScope.launch(Dispatchers.Default) {
            try {
                val salt = CryptoManager.generateSalt()
                val (aesKey, hash) = CryptoManager.deriveKeyAndHash(pin, salt, CryptoManager.FAST_ITERATION_COUNT)
                // One atomic write: salt + hash + iterations + PIN length together.
                settingsStore.saveMasterAuth(salt, hash, CryptoManager.FAST_ITERATION_COUNT, chosenLength)

                CryptoManager.initSession(aesKey)
                _uiState.update {
                    it.copy(
                        screenState = AuthScreenState.AUTHENTICATED,
                        isUnlocking = false,
                        pinLength = chosenLength,
                        error = null
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        screenState = AuthScreenState.SETUP_REQUIRED,
                        isUnlocking = false,
                        error = "PIN setup failed: ${e.message}"
                    )
                }
            }
        }
    }

    fun unlockWithPin(pin: String) {
        // Re-entry guard: the login screen auto-unlocks on the last digit AND has
        // an Unlock button — without this, both could fire and race each other.
        if (_uiState.value.isUnlocking) return
        val storedSalt = settingsStore.getMasterSalt()
        val storedHash = settingsStore.getMasterHash()
        val storedIterations = settingsStore.getMasterIterations()
        val storedLength = settingsStore.getPinLength()

        if (storedSalt == null || storedHash == null) {
            _uiState.update { it.copy(error = "Vault data missing. Please set up a new PIN.") }
            return
        }

        if (pin.length < storedLength) {
            _uiState.update { it.copy(error = "Please enter your full $storedLength-digit PIN") }
            return
        }

        _uiState.update { it.copy(isUnlocking = true, error = null) }

        viewModelScope.launch(Dispatchers.Default) {
            try {
                // First try fast iterations
                val (aesKey, newHash) = CryptoManager.deriveKeyAndHash(pin, storedSalt, storedIterations)
                if (newHash == storedHash) {
                    CryptoManager.initSession(aesKey)
                    _uiState.update { it.copy(screenState = AuthScreenState.AUTHENTICATED, isUnlocking = false, error = null) }
                    return@launch
                }

                // Fallback attempt for legacy migration
                if (storedIterations != CryptoManager.LEGACY_ITERATION_COUNT) {
                    val legacyResult = CryptoManager.deriveKeyAndHash(pin, storedSalt, CryptoManager.LEGACY_ITERATION_COUNT)
                    if (legacyResult.second == storedHash) {
                        val (fastKey, fastHash) = CryptoManager.deriveKeyAndHash(pin, storedSalt, CryptoManager.FAST_ITERATION_COUNT)
                        settingsStore.saveMasterAuth(storedSalt, fastHash, CryptoManager.FAST_ITERATION_COUNT)
                        CryptoManager.initSession(fastKey)
                        _uiState.update { it.copy(screenState = AuthScreenState.AUTHENTICATED, isUnlocking = false, error = null) }
                        return@launch
                    }
                }

                _uiState.update { it.copy(isUnlocking = false, error = "Incorrect PIN. Please try again.") }
            } catch (e: Exception) {
                _uiState.update { it.copy(isUnlocking = false, error = "Error verifying PIN") }
            }
        }
    }

    // Retained for backward compatibility
    fun login(password: String) {
        unlockWithPin(password)
    }

    // Retained for backward compatibility
    fun setupMasterPassword(password: String) {
        setupPin(password, password.length)
    }

    /**
     * Changes the app-lock PIN (and optionally its length).
     *
     * The old implementation generated a new salt/key and overwrote the stored
     * auth WITHOUT re-encrypting the vault, so every saved password became
     * "Decryption Error". This version:
     *  1. Verifies the CURRENT PIN first (stored salt/iterations, incl. legacy fallback).
     *  2. Re-encrypts every vault row from the old key to the new key. If any row
     *     fails to decrypt, it aborts and the old auth stays untouched — data is
     *     never wiped by a failed PIN change.
     *  3. Only then atomically saves the new salt/hash/length and starts the new session.
     * The callback is always delivered on the main thread with a real error message.
     */
    fun changePin(
        currentPin: String,
        newPin: String,
        chosenLength: Int,
        onComplete: (Boolean, String?) -> Unit
    ) {
        if (_uiState.value.isChangingPin) return
        if (newPin.length != chosenLength || !newPin.all { it.isDigit() }) {
            onComplete(false, "New PIN must be exactly $chosenLength digits")
            return
        }
        if (currentPin.isBlank()) {
            onComplete(false, "Please enter your current PIN")
            return
        }
        val storedSalt = settingsStore.getMasterSalt()
        val storedHash = settingsStore.getMasterHash()
        val storedIterations = settingsStore.getMasterIterations()
        if (storedSalt == null || storedHash == null) {
            onComplete(false, "No PIN is set yet — please set one up first")
            return
        }

        _uiState.update { it.copy(isChangingPin = true, error = null) }
        viewModelScope.launch(Dispatchers.Default) {
            try {
                // 1. Verify the current PIN against the stored hash.
                val (oldKey, oldHash) = CryptoManager.deriveKeyAndHash(currentPin, storedSalt, storedIterations)
                val verifiedOldKey = when {
                    oldHash == storedHash -> oldKey
                    storedIterations != CryptoManager.LEGACY_ITERATION_COUNT -> {
                        val legacy = CryptoManager.deriveKeyAndHash(currentPin, storedSalt, CryptoManager.LEGACY_ITERATION_COUNT)
                        if (legacy.second == storedHash) legacy.first else null
                    }
                    else -> null
                }
                if (verifiedOldKey == null) {
                    withContext(Dispatchers.Main) {
                        _uiState.update { it.copy(isChangingPin = false) }
                        onComplete(false, "Current PIN is incorrect")
                    }
                    return@launch
                }

                // 2. Derive the new key, then re-encrypt every vault row with it.
                val newSalt = CryptoManager.generateSalt()
                val (newKey, newHash) = CryptoManager.deriveKeyAndHash(newPin, newSalt, CryptoManager.FAST_ITERATION_COUNT)
                try {
                    vaultRepository?.reencryptAll(verifiedOldKey, newKey)
                } catch (e: Exception) {
                    // Abort: old auth and old data stay exactly as they were.
                    withContext(Dispatchers.Main) {
                        _uiState.update { it.copy(isChangingPin = false) }
                        onComplete(false, "Could not re-encrypt your vault — PIN was not changed. Please try again.")
                    }
                    return@launch
                }

                // 3. Only now persist the new auth, atomically (salt+hash+length).
                settingsStore.saveMasterAuth(newSalt, newHash, CryptoManager.FAST_ITERATION_COUNT, chosenLength)
                CryptoManager.initSession(newKey)
                withContext(Dispatchers.Main) {
                    _uiState.update { it.copy(pinLength = chosenLength, isChangingPin = false, error = null) }
                    onComplete(true, null)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _uiState.update { it.copy(isChangingPin = false) }
                    onComplete(false, "PIN change failed: ${e.message}")
                }
            }
        }
    }

    /** Old 3-arg overload, kept for compatibility: requires the current PIN, so it cannot succeed blindly. */
    @Deprecated("Use changePin(currentPin, newPin, chosenLength, onComplete) — the current PIN is required so the vault can be re-encrypted.")
    fun changePin(newPin: String, chosenLength: Int, onComplete: (Boolean) -> Unit) {
        changePin("", newPin, chosenLength) { success, _ -> onComplete(success) }
    }

    fun resetVault() {
        CryptoManager.clearSession()
        settingsStore.wipe()
        _uiState.update {
            it.copy(
                screenState = AuthScreenState.SETUP_REQUIRED,
                isUnlocking = false,
                isChangingPin = false,
                error = null,
                pinLength = 4
            )
        }
    }

    fun logout() {
        CryptoManager.clearSession()
        _uiState.update {
            it.copy(
                screenState = if (settingsStore.hasMasterPasswordTracker()) AuthScreenState.LOGIN_REQUIRED else AuthScreenState.SETUP_REQUIRED,
                isUnlocking = false,
                isChangingPin = false,
                pinLength = settingsStore.getPinLength(),
                error = null
            )
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
