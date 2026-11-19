package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.DecryptedVaultItem
import com.example.data.VaultRepository
import com.example.security.CryptoManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class VaultViewModel(private val repository: VaultRepository) : ViewModel() {

    val vaultItems: StateFlow<List<DecryptedVaultItem>> = repository.allDecryptedItems
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    /** Ids of locked vault items the user has unlocked for this session only. */
    private val _unlockedIds = MutableStateFlow<Set<Int>>(emptySet())
    val unlockedIds: StateFlow<Set<Int>> = _unlockedIds.asStateFlow()

    fun isUnlocked(item: DecryptedVaultItem): Boolean =
        !item.isLocked || _unlockedIds.value.contains(item.id)

    fun saveItem(item: DecryptedVaultItem) {
        viewModelScope.launch {
            repository.insertOrUpdate(item)
        }
    }

    fun deleteItem(id: Int) {
        viewModelScope.launch {
            repository.deleteById(id)
            _unlockedIds.update { it - id }
        }
    }

    fun togglePin(item: DecryptedVaultItem) {
        viewModelScope.launch { repository.togglePin(item.id) }
    }

    /** Sets a per-item lock password (stored only as a SHA-256 hash). */
    fun lockItem(item: DecryptedVaultItem, password: String, onDone: (Boolean) -> Unit = {}) {
        if (password.length < 4) {
            onDone(false)
            return
        }
        viewModelScope.launch {
            repository.setItemLock(item.id, CryptoManager.sha256Hex(password))
            _unlockedIds.update { it - item.id }
            onDone(true)
        }
    }

    fun removeLock(item: DecryptedVaultItem) {
        viewModelScope.launch {
            repository.removeItemLock(item.id)
            _unlockedIds.update { it - item.id }
        }
    }

    /** Returns true when [password] matches the item's lock password; unlocks it for this session. */
    fun tryUnlock(item: DecryptedVaultItem, password: String): Boolean {
        if (item.lockPasswordHash.isEmpty()) return false
        val ok = CryptoManager.sha256Hex(password) == item.lockPasswordHash
        if (ok) _unlockedIds.update { it + item.id }
        return ok
    }

    fun generatePassword(): String {
        return CryptoManager.generateSecureRandomPassword(16)
    }
}
