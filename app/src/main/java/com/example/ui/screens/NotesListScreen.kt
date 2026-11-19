package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import com.example.data.NoteItem
import com.example.ui.NoteViewModel
import com.example.ui.theme.NoteColors
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesListScreen(
    viewModel: NoteViewModel,
    onNavigateToAddNote: () -> Unit,
    onNavigateToEditNote: (Int) -> Unit
) {
    val notes by viewModel.notes.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val unlockedNoteIds by viewModel.unlockedNoteIds.collectAsStateWithLifecycle()
    var noteToUnlock by remember { mutableStateOf<NoteItem?>(null) }
    var noteToLock by remember { mutableStateOf<NoteItem?>(null) }

    fun openNote(note: NoteItem) {
        if (note.isLocked && !unlockedNoteIds.contains(note.id)) {
            noteToUnlock = note
        } else {
            onNavigateToEditNote(note.id)
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToAddNote,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "New Note")
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                // Search bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.searchQuery.value = it },
                    placeholder = { Text("Search phone notes & files...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.searchQuery.value = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear search")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (notes.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 56.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.Description,
                                contentDescription = null,
                                modifier = Modifier.size(54.dp),
                                tint = MaterialTheme.colorScheme.outline
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                if (searchQuery.isEmpty()) "No notes yet" else "No matching notes",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "Add notes with photos and files stored directly on your phone.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                val pinnedNotes = notes.filter { it.isPinned }
                val otherNotes = notes.filter { !it.isPinned }

                if (pinnedNotes.isNotEmpty()) {
                    item {
                        Text(
                            text = "PINNED",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                    items(pinnedNotes, key = { it.id }) { note ->
                        NoteCard(
                            note = note,
                            isUnlocked = unlockedNoteIds.contains(note.id),
                            onClick = { openNote(note) },
                            onTogglePin = { viewModel.togglePin(note) },
                            onLockNote = { noteToLock = note }
                        )
                    }
                }

                if (otherNotes.isNotEmpty()) {
                    if (pinnedNotes.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "OTHERS",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }
                    items(otherNotes, key = { it.id }) { note ->
                        NoteCard(
                            note = note,
                            isUnlocked = unlockedNoteIds.contains(note.id),
                            onClick = { openNote(note) },
                            onTogglePin = { viewModel.togglePin(note) },
                            onLockNote = { noteToLock = note }
                        )
                    }
                }
            }
        }
    }

    // Unlock a locked note (per-note password, session-only unlock)
    noteToUnlock?.let { note ->
        var password by remember(note.id) { mutableStateOf("") }
        var error by remember(note.id) { mutableStateOf<String?>(null) }
        AlertDialog(
            onDismissRequest = { noteToUnlock = null },
            title = { Text("Unlock Note") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("This note is locked. Enter its password to open it.", style = MaterialTheme.typography.bodySmall)
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it; error = null },
                        label = { Text("Note password") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (error != null) Text(error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (viewModel.tryUnlockNote(note, password)) {
                        noteToUnlock = null
                        onNavigateToEditNote(note.id)
                    } else {
                        error = "Wrong password"
                    }
                }) { Text("Unlock") }
            },
            dismissButton = { TextButton(onClick = { noteToUnlock = null }) { Text("Cancel") } }
        )
    }

    // Set a lock password on a note
    noteToLock?.let { note ->
        var password by remember(note.id) { mutableStateOf("") }
        var error by remember(note.id) { mutableStateOf<String?>(null) }
        AlertDialog(
            onDismissRequest = { noteToLock = null },
            title = { Text(if (note.isLocked) "Change Note Password" else "Lock Note") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Set a password for this note (at least 4 characters). The content stays hidden until it is entered.", style = MaterialTheme.typography.bodySmall)
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it; error = null },
                        label = { Text("Note password") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (error != null) Text(error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.lockNote(note, password) { ok ->
                        if (ok) noteToLock = null else error = "Password must be at least 4 characters"
                    }
                }) { Text("Lock Note") }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (note.isLocked) {
                        TextButton(onClick = { viewModel.removeNoteLock(note); noteToLock = null }) { Text("Remove Lock") }
                    }
                    TextButton(onClick = { noteToLock = null }) { Text("Cancel") }
                }
            }
        )
    }
}

@Composable
fun NoteCard(
    note: NoteItem,
    onClick: () -> Unit,
    onTogglePin: () -> Unit,
    isUnlocked: Boolean = true,
    onLockNote: (NoteItem) -> Unit = {}
) {
    val attachments = remember(note.attachmentsJson) { note.getAttachments() }
    val firstImage = remember(attachments) { attachments.firstOrNull { it.isImage && File(it.localFilePath).exists() } }
    val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    var showMenu by remember { mutableStateOf(false) }

    // Contrast fix: text colour is computed from the card's own background
    // luminance (near-black on light cards, near-white on dark cards), so the
    // title/content are readable in BOTH light and dark themes.
    val contentColor = NoteColors.contentColorForStyle(note.backgroundStyle, note.colorHex)
    val mutedColor = contentColor.copy(alpha = 0.72f)
    val gradientBrush = NoteColors.gradientBrush(note.backgroundStyle)
    val isLockedHidden = note.isLocked && !isUnlocked

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = if (gradientBrush == null) NoteColors.parseColor(note.colorHex) else Color.Transparent,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shadowElevation = 1.dp
    ) {
        Box(
            modifier = if (gradientBrush != null) Modifier.background(gradientBrush) else Modifier
        ) {
            Column {
                // If image is attached, show preview banner at the top of the card
                if (firstImage != null && !isLockedHidden) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp)
                    ) {
                        AsyncImage(
                            model = File(firstImage.localFilePath),
                            contentDescription = "Note preview",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                }

                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (note.isLocked) {
                            Icon(
                                Icons.Default.Lock,
                                contentDescription = "Locked note",
                                tint = contentColor,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                        Text(
                            text = note.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = contentColor,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )

                        IconButton(
                            onClick = onTogglePin,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = if (note.isPinned) Icons.Default.PushPin else Icons.Default.OutlinedFlag,
                                contentDescription = "Toggle Pin",
                                tint = if (note.isPinned) MaterialTheme.colorScheme.primary else mutedColor,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Box {
                            IconButton(onClick = { showMenu = true }, modifier = Modifier.size(28.dp)) {
                                Icon(Icons.Default.MoreVert, contentDescription = "Note options", tint = mutedColor, modifier = Modifier.size(18.dp))
                            }
                            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                                DropdownMenuItem(
                                    text = { Text(if (note.isPinned) "Unpin" else "Pin") },
                                    onClick = { showMenu = false; onTogglePin() },
                                    leadingIcon = { Icon(Icons.Default.PushPin, contentDescription = null) }
                                )
                                DropdownMenuItem(
                                    text = { Text(if (note.isLocked) "Change / Remove Lock" else "Lock with Password") },
                                    onClick = { showMenu = false; onLockNote(note) },
                                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) }
                                )
                            }
                        }
                    }

                    if (isLockedHidden) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Locked — tap and enter the note password to view.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = mutedColor,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    } else if (note.content.isNotBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = note.content,
                            style = MaterialTheme.typography.bodyMedium,
                            color = mutedColor,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = dateFormat.format(Date(note.updatedAt)),
                            style = MaterialTheme.typography.labelSmall,
                            color = mutedColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        if (attachments.isNotEmpty() && !isLockedHidden) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = contentColor.copy(alpha = 0.12f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (firstImage != null) Icons.Default.Image else Icons.Default.AttachFile,
                                        contentDescription = null,
                                        modifier = Modifier.size(12.dp),
                                        tint = mutedColor
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "${attachments.size}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = mutedColor
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
