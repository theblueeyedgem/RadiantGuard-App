package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.SettingsStore
import com.example.security.CryptoManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

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
    val pinLength: Int = 4
)

class AuthViewModel(private val settingsStore: SettingsStore) : ViewModel() {

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
                settingsStore.saveMasterAuth(salt, hash, CryptoManager.FAST_ITERATION_COUNT)
                settingsStore.savePinLength(chosenLength)
                
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

    fun changePin(newPin: String, chosenLength: Int, onComplete: (Boolean) -> Unit) {
        if (newPin.length != chosenLength || !newPin.all { it.isDigit() }) {
            onComplete(false)
            return
        }
        viewModelScope.launch(Dispatchers.Default) {
            try {
                val salt = CryptoManager.generateSalt()
                val (aesKey, hash) = CryptoManager.deriveKeyAndHash(newPin, salt, CryptoManager.FAST_ITERATION_COUNT)
                settingsStore.saveMasterAuth(salt, hash, CryptoManager.FAST_ITERATION_COUNT)
                settingsStore.savePinLength(chosenLength)
                CryptoManager.initSession(aesKey)
                _uiState.update { it.copy(pinLength = chosenLength) }
                onComplete(true)
            } catch (e: Exception) {
                onComplete(false)
            }
        }
    }

    fun resetVault() {
        CryptoManager.clearSession()
        settingsStore.wipe()
        _uiState.update {
            it.copy(
                screenState = AuthScreenState.SETUP_REQUIRED,
                isUnlocking = false,
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
                pinLength = settingsStore.getPinLength(),
                error = null
            )
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
