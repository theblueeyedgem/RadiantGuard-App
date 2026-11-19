package com.example.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import com.example.data.NoteAttachment
import com.example.data.NoteItem
import com.example.security.CryptoManager
import com.example.ui.NoteViewModel
import com.example.ui.theme.NoteColors
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddEditNoteScreen(
    noteId: Int,
    viewModel: NoteViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val notes by viewModel.notes.collectAsState()
    val existingNote = remember(notes, noteId) {
        if (noteId != -1) notes.firstOrNull { it.id == noteId } else null
    }

    var title by remember { mutableStateOf(existingNote?.title ?: "") }
    var content by remember { mutableStateOf(existingNote?.content ?: "") }
    var colorHex by remember { mutableStateOf(existingNote?.colorHex ?: "#FFFFFF") }
    var backgroundStyle by remember { mutableStateOf(existingNote?.backgroundStyle ?: "solid") }
    var isPinned by remember { mutableStateOf(existingNote?.isPinned ?: false) }
    var isLocked by remember { mutableStateOf(existingNote?.isLocked ?: false) }
    var lockPasswordHash by remember { mutableStateOf(existingNote?.lockPasswordHash ?: "") }
    var showLockDialog by remember { mutableStateOf(false) }
    var attachments by remember { mutableStateOf<List<NoteAttachment>>(existingNote?.getAttachments() ?: emptyList()) }
    var previewImageAttachment by remember { mutableStateOf<NoteAttachment?>(null) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(existingNote) {
        if (existingNote != null) {
            title = existingNote.title
            content = existingNote.content
            colorHex = existingNote.colorHex
            backgroundStyle = existingNote.backgroundStyle
            isPinned = existingNote.isPinned
            isLocked = existingNote.isLocked
            lockPasswordHash = existingNote.lockPasswordHash
            attachments = existingNote.getAttachments()
        }
    }

    val userMessage by viewModel.userMessage.collectAsState()
    LaunchedEffect(userMessage) {
        userMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearMessage()
        }
    }

    // Photo picker (Zero permissions required, modern Android API)
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris: List<Uri> ->
        coroutineScope.launch {
            if (uris.isNotEmpty()) {
                val newAttachments = attachments.toMutableList()
                uris.forEach { uri ->
                    val att = viewModel.attachUri(uri)
                    if (att != null) {
                        newAttachments.add(att)
                    }
                }
                attachments = newAttachments
                Toast.makeText(context, "Added ${uris.size} photo(s) to note", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // File picker for non-image files (documents, pdfs, audio, etc.)
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris: List<Uri> ->
        coroutineScope.launch {
            if (uris.isNotEmpty()) {
                val newAttachments = attachments.toMutableList()
                uris.forEach { uri ->
                    val att = viewModel.attachUri(uri)
                    if (att != null) {
                        newAttachments.add(att)
                    }
                }
                attachments = newAttachments
                Toast.makeText(context, "Added ${uris.size} file(s) to note", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // 12 offered solid colours: 6 light pastels + 6 dark-friendly (see NoteColors).
    val colorOptions = NoteColors.SOLID_COLORS
    val gradientOptions = NoteColors.GRADIENT_STYLES
    // Effective background colour for the top bar / Scaffold (first gradient hex when gradient).
    val effectiveBgHex = NoteColors.gradientHexes(backgroundStyle).firstOrNull() ?: colorHex
    // Deterministic readable text colour for this background (both themes).
    val editorTextColor = NoteColors.contentColorForStyle(backgroundStyle, colorHex)
    val editorMutedColor = editorTextColor.copy(alpha = 0.6f)

    fun parseColor(hex: String): Color {
        return try {
            Color(android.graphics.Color.parseColor(hex))
        } catch (e: Exception) {
            Color.White
        }
    }

    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (noteId == -1) "New Note" else "Edit Note", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showLockDialog = true }) {
                        Icon(
                            imageVector = if (isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                            contentDescription = if (isLocked) "Note locked — change password" else "Lock note with password",
                            tint = if (isLocked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                        )
                    }
                    IconButton(onClick = { isPinned = !isPinned }) {
                        Icon(
                            imageVector = if (isPinned) Icons.Default.PushPin else Icons.Default.OutlinedFlag,
                            contentDescription = "Pin Note",
                            tint = if (isPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                        )
                    }
                    if (existingNote != null) {
                        IconButton(onClick = { showDeleteConfirm = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete Note", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                    IconButton(
                        onClick = {
                            viewModel.saveNote(
                                id = if (noteId == -1) 0 else noteId,
                                title = title,
                                content = content,
                                colorHex = colorHex,
                                backgroundStyle = backgroundStyle,
                                isPinned = isPinned,
                                isLocked = isLocked,
                                lockPasswordHash = lockPasswordHash,
                                attachments = attachments
                            )
                            onNavigateBack()
                        }
                    ) {
                        Icon(Icons.Default.Check, contentDescription = "Save Note", tint = MaterialTheme.colorScheme.primary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = parseColor(effectiveBgHex).copy(alpha = 0.95f)
                )
            )
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 4.dp,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Add image button
                        FilledTonalIconButton(
                            onClick = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            }
                        ) {
                            Icon(Icons.Default.AddPhotoAlternate, contentDescription = "Add Image")
                        }

                        // Add file button
                        FilledTonalIconButton(
                            onClick = {
                                filePickerLauncher.launch(arrayOf("*/*"))
                            }
                        ) {
                            Icon(Icons.Default.AttachFile, contentDescription = "Add File")
                        }
                    }

                    Text(
                        "Backgrounds below",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        containerColor = parseColor(effectiveBgHex)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .then(if (NoteColors.isGradient(backgroundStyle)) Modifier.background(NoteColors.gradientBrush(backgroundStyle)!!) else Modifier)
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .verticalScroll(scrollState)
        ) {
            // Note Title
            TextField(
                value = title,
                onValueChange = { title = it },
                placeholder = { Text("Title", style = MaterialTheme.typography.headlineMedium, color = editorMutedColor) },
                textStyle = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold, color = editorTextColor),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedTextColor = editorTextColor,
                    unfocusedTextColor = editorTextColor,
                    cursorColor = editorTextColor
                ),
                modifier = Modifier.fillMaxWidth()
            )

            val dateFormat = SimpleDateFormat("MMMM dd, yyyy  hh:mm a", Locale.getDefault())
            Text(
                text = dateFormat.format(Date(existingNote?.updatedAt ?: System.currentTimeMillis())),
                style = MaterialTheme.typography.labelSmall,
                color = editorMutedColor,
                modifier = Modifier.padding(start = 16.dp, bottom = 12.dp)
            )

            // Backgrounds: solid colours + gradients (second row of swatches)
            Text(
                text = "BACKGROUNDS",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
            )
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                colorOptions.forEach { hex ->
                    val c = parseColor(hex)
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(c)
                            .border(
                                width = if (backgroundStyle == "solid" && colorHex == hex) 2.dp else 1.dp,
                                color = if (backgroundStyle == "solid" && colorHex == hex) MaterialTheme.colorScheme.primary else Color.LightGray,
                                shape = CircleShape
                            )
                            .clickable { colorHex = hex; backgroundStyle = "solid" }
                    )
                }
                gradientOptions.forEach { style ->
                    Box(
                        modifier = Modifier
                            .size(width = 52.dp, height = 30.dp)
                            .clip(RoundedCornerShape(15.dp))
                            .background(NoteColors.gradientBrush(style)!!)
                            .border(
                                width = if (backgroundStyle == style) 2.dp else 1.dp,
                                color = if (backgroundStyle == style) MaterialTheme.colorScheme.primary else Color.LightGray,
                                shape = RoundedCornerShape(15.dp)
                            )
                            .clickable { backgroundStyle = style }
                    )
                }
            }

            // Attachments Preview Section
            if (attachments.isNotEmpty()) {
                Text(
                    text = "ATTACHMENTS (${attachments.size})",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
                )

                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(attachments, key = { it.id }) { attachment ->
                        AttachmentCard(
                            attachment = attachment,
                            onPreviewImage = { previewImageAttachment = attachment },
                            onExportToGallery = { viewModel.exportToGallery(attachment) },
                            onExportToDownloads = { viewModel.exportToDownloads(attachment) },
                            onRemove = {
                                attachments = attachments.filter { it.id != attachment.id }
                            }
                        )
                    }
                }
            }

            // Note Content (Scrolls naturally without illegal weight modifier)
            TextField(
                value = content,
                onValueChange = { content = it },
                placeholder = { Text("Note content...", style = MaterialTheme.typography.bodyLarge, color = editorMutedColor) },
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = editorTextColor),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedTextColor = editorTextColor,
                    unfocusedTextColor = editorTextColor,
                    cursorColor = editorTextColor
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 260.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    // Per-note lock password dialog
    if (showLockDialog) {
        var lockPassword by remember { mutableStateOf("") }
        var lockError by remember { mutableStateOf<String?>(null) }
        AlertDialog(
            onDismissRequest = { showLockDialog = false },
            title = { Text(if (isLocked) "Note Locked" else "Lock Note") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        if (isLocked) "This note is locked. Enter a new password to change it, or remove the lock."
                        else "Set a password for this note (at least 4 characters). Its content stays hidden until the password is entered.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedTextField(
                        value = lockPassword,
                        onValueChange = { lockPassword = it; lockError = null },
                        label = { Text("Note password") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (lockError != null) Text(lockError!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (lockPassword.length < 4) {
                        lockError = "Password must be at least 4 characters"
                    } else {
                        lockPasswordHash = CryptoManager.sha256Hex(lockPassword)
                        isLocked = true
                        showLockDialog = false
                    }
                }) { Text(if (isLocked) "Change Password" else "Lock Note") }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (isLocked) {
                        TextButton(onClick = { isLocked = false; lockPasswordHash = ""; showLockDialog = false }) {
                            Text("Remove Lock")
                        }
                    }
                    TextButton(onClick = { showLockDialog = false }) { Text("Cancel") }
                }
            }
        )
    }

    // Image Preview Dialog
    if (previewImageAttachment != null) {
        val att = previewImageAttachment!!
        Dialog(
            onDismissRequest = { previewImageAttachment = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .wrapContentHeight(),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = att.fileName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { previewImageAttachment = null }) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (File(att.localFilePath).exists()) {
                        AsyncImage(
                            model = File(att.localFilePath),
                            contentDescription = att.fileName,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 380.dp)
                                .clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Fit
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            viewModel.exportToGallery(att)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Save to Gallery", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // Delete Confirmation Dialog
    if (showDeleteConfirm && existingNote != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Note") },
            text = { Text("Are you sure you want to permanently delete \"${existingNote.title}\"?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteNote(existingNote)
                        showDeleteConfirm = false
                        onNavigateBack()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun AttachmentCard(
    attachment: NoteAttachment,
    onPreviewImage: () -> Unit,
    onExportToGallery: () -> Unit,
    onExportToDownloads: () -> Unit,
    onRemove: () -> Unit
) {
    Surface(
        modifier = Modifier.width(180.dp),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shadowElevation = 2.dp
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .clickable {
                        if (attachment.isImage) {
                            onPreviewImage()
                        }
                    }
            ) {
                if (attachment.isImage && File(attachment.localFilePath).exists()) {
                    AsyncImage(
                        model = File(attachment.localFilePath),
                        contentDescription = attachment.fileName,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.Description,
                                contentDescription = null,
                                modifier = Modifier.size(36.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = attachment.fileName.substringAfterLast('.', "FILE").uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Remove button
                IconButton(
                    onClick = onRemove,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .size(26.dp)
                        .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Remove", tint = Color.White, modifier = Modifier.size(16.dp))
                }
            }

            Column(modifier = Modifier.padding(10.dp)) {
                Text(
                    text = attachment.fileName,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${(attachment.fileSizeBytes / 1024).coerceAtLeast(1)} KB",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Direct Gallery or File Download Button
                if (attachment.isImage) {
                    Button(
                        onClick = onExportToGallery,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(32.dp),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Save to Gallery", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                } else {
                    OutlinedButton(
                        onClick = onExportToDownloads,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(32.dp),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Save to Phone", fontSize = 11.sp)
                    }
                }
            }
        }
    }
}
