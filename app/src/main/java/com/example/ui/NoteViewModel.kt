package com.example.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.NoteAttachment
import com.example.data.NoteItem
import com.example.data.NoteRepository
import com.example.data.NoteStorageManager
import com.example.security.CryptoManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class NoteViewModel(
    application: Application,
    private val repository: NoteRepository
) : AndroidViewModel(application) {

    val searchQuery = MutableStateFlow("")

    @OptIn(ExperimentalCoroutinesApi::class)
    val notes: StateFlow<List<NoteItem>> = searchQuery
        .flatMapLatest { query ->
            if (query.isBlank()) {
                repository.allNotes
            } else {
                repository.searchNotes(query.trim())
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage: StateFlow<String?> = _userMessage.asStateFlow()

    /** Ids of locked notes the user has unlocked for this session only. */
    private val _unlockedNoteIds = MutableStateFlow<Set<Int>>(emptySet())
    val unlockedNoteIds: StateFlow<Set<Int>> = _unlockedNoteIds.asStateFlow()

    fun isNoteUnlocked(note: NoteItem): Boolean =
        !note.isLocked || _unlockedNoteIds.value.contains(note.id)

    fun clearMessage() {
        _userMessage.value = null
    }

    fun saveNote(
        id: Int = 0,
        title: String,
        content: String,
        colorHex: String = "#FFFFFF",
        backgroundStyle: String = "solid",
        isPinned: Boolean = false,
        isLocked: Boolean = false,
        lockPasswordHash: String = "",
        attachments: List<NoteAttachment> = emptyList()
    ) {
        viewModelScope.launch {
            val json = NoteItem.serializeAttachments(attachments)
            // FIX: editing a note used to reset createdAt to "now" (both branches
            // used System.currentTimeMillis()). The original date is now preserved.
            val existing = if (id != 0) repository.getById(id) else null
            val note = NoteItem(
                id = id,
                title = title.ifBlank { "Untitled Note" },
                content = content,
                colorHex = colorHex,
                backgroundStyle = backgroundStyle,
                isPinned = isPinned,
                // Keep an existing lock unless the caller explicitly changed it.
                isLocked = isLocked || (existing?.isLocked ?: false),
                lockPasswordHash = lockPasswordHash.ifEmpty { existing?.lockPasswordHash ?: "" },
                attachmentsJson = json,
                createdAt = existing?.createdAt ?: System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            if (id == 0) {
                repository.insert(note)
            } else {
                repository.update(note)
            }
            _userMessage.value = "Note saved"
        }
    }

    fun deleteNote(note: NoteItem) {
        viewModelScope.launch {
            repository.delete(note)
            _unlockedNoteIds.update { it - note.id }
            _userMessage.value = "Note deleted"
        }
    }

    fun togglePin(note: NoteItem) {
        viewModelScope.launch {
            val updated = note.copy(isPinned = !note.isPinned, updatedAt = System.currentTimeMillis())
            repository.update(updated)
        }
    }

    /** Sets a per-note lock password (stored only as a SHA-256 hash). */
    fun lockNote(note: NoteItem, password: String, onDone: (Boolean) -> Unit = {}) {
        if (password.length < 4) {
            onDone(false)
            return
        }
        viewModelScope.launch {
            repository.update(note.copy(isLocked = true, lockPasswordHash = CryptoManager.sha256Hex(password), updatedAt = System.currentTimeMillis()))
            _unlockedNoteIds.update { it - note.id }
            onDone(true)
        }
    }

    fun removeNoteLock(note: NoteItem) {
        viewModelScope.launch {
            repository.update(note.copy(isLocked = false, lockPasswordHash = "", updatedAt = System.currentTimeMillis()))
            _unlockedNoteIds.update { it - note.id }
        }
    }

    /** Returns true when [password] matches the note's lock password; unlocks it for this session. */
    fun tryUnlockNote(note: NoteItem, password: String): Boolean {
        if (note.lockPasswordHash.isEmpty()) return false
        val ok = CryptoManager.sha256Hex(password) == note.lockPasswordHash
        if (ok) _unlockedNoteIds.update { it + note.id }
        return ok
    }

    suspend fun attachUri(uri: Uri): NoteAttachment? {
        return NoteStorageManager.saveUriToPhoneStorage(getApplication(), uri)
    }

    fun exportToGallery(attachment: NoteAttachment) {
        viewModelScope.launch {
            val success = NoteStorageManager.exportImageToGallery(getApplication(), attachment)
            _userMessage.value = if (success) {
                "Saved to phone Gallery (Pictures/RadiantGuard)"
            } else {
                "Failed to export to Gallery"
            }
        }
    }

    fun exportToDownloads(attachment: NoteAttachment) {
        viewModelScope.launch {
            val success = NoteStorageManager.exportFileToDownloads(getApplication(), attachment)
            _userMessage.value = if (success) {
                "Downloaded to phone storage (Downloads/RadiantGuard)"
            } else {
                "Failed to download file"
            }
        }
    }
}
