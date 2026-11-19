package com.example.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.NoteAttachment
import com.example.data.NoteItem
import com.example.data.NoteRepository
import com.example.data.NoteStorageManager
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

    fun clearMessage() {
        _userMessage.value = null
    }

    fun saveNote(
        id: Int = 0,
        title: String,
        content: String,
        colorHex: String = "#FFFFFF",
        isPinned: Boolean = false,
        attachments: List<NoteAttachment> = emptyList()
    ) {
        viewModelScope.launch {
            val json = NoteItem.serializeAttachments(attachments)
            val note = NoteItem(
                id = id,
                title = title.ifBlank { "Untitled Note" },
                content = content,
                colorHex = colorHex,
                isPinned = isPinned,
                attachmentsJson = json,
                createdAt = if (id == 0) System.currentTimeMillis() else System.currentTimeMillis(),
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
            _userMessage.value = "Note deleted"
        }
    }

    fun togglePin(note: NoteItem) {
        viewModelScope.launch {
            val updated = note.copy(isPinned = !note.isPinned, updatedAt = System.currentTimeMillis())
            repository.update(updated)
        }
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
