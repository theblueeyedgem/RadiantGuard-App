package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material3.*
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.DecryptedVaultItem
import com.example.security.TOTPGenerator
import com.example.ui.VaultViewModel
import kotlinx.coroutines.delay

@Composable
fun VaultListScreen(
    viewModel: VaultViewModel,
    onNavigateToAdd: () -> Unit,
    onNavigateToEdit: (Int) -> Unit
) {
    val items by viewModel.vaultItems.collectAsStateWithLifecycle()
    val unlockedIds by viewModel.unlockedIds.collectAsStateWithLifecycle()
    var currentTime by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val context = LocalContext.current
    var itemToLock by remember { mutableStateOf<DecryptedVaultItem?>(null) }
    var itemToUnlock by remember { mutableStateOf<DecryptedVaultItem?>(null) }
    var pendingEditAfterUnlock by remember { mutableStateOf(false) }

    // Timer to update TOTP every second
    LaunchedEffect(Unit) {
        while (true) {
            currentTime = System.currentTimeMillis()
            delay(1000L)
        }
    }

    fun copyToClipboard(label: String, text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "$label copied", Toast.LENGTH_SHORT).show()
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToAdd,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Password")
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        if (items.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "Your vault is empty",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Add a password or 2FA account to get started.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp, vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "STORED CREDENTIALS",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 1.sp
                        )
                        Text(
                            "${items.size} Items",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                items(items, key = { it.id }) { item ->
                    val unlocked = viewModel.isUnlocked(item)
                    VaultItemCard(
                        item = item,
                        currentTime = currentTime,
                        isUnlocked = unlocked,
                        onClick = {
                            if (item.isLocked && !unlocked) {
                                pendingEditAfterUnlock = true
                                itemToUnlock = item
                            } else {
                                onNavigateToEdit(item.id)
                            }
                        },
                        onTogglePin = { viewModel.togglePin(item) },
                        onLockItem = { itemToLock = item },
                        onRemoveLock = { viewModel.removeLock(item) },
                        onCopyUsername = { copyToClipboard("Username", item.username) },
                        onCopyPassword = {
                            if (item.isLocked && !unlocked) {
                                Toast.makeText(context, "Unlock this item first", Toast.LENGTH_SHORT).show()
                            } else {
                                copyToClipboard("Password", item.password)
                            }
                        },
                        onCopyTotp = { code -> copyToClipboard("2FA Code", code) }
                    )
                }
            }
        }
    }

    // Set / change a per-item lock password
    itemToLock?.let { item ->
        var password by remember(item.id) { mutableStateOf("") }
        var error by remember(item.id) { mutableStateOf<String?>(null) }
        AlertDialog(
            onDismissRequest = { itemToLock = null },
            title = { Text("Lock with Password") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Set a password for \"${item.title}\" (at least 4 characters). Its password and 2FA code stay hidden until it is entered.", style = MaterialTheme.typography.bodySmall)
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it; error = null },
                        label = { Text("Item password") },
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
                    viewModel.lockItem(item, password) { ok ->
                        if (ok) itemToLock = null else error = "Password must be at least 4 characters"
                    }
                }) { Text("Lock Item") }
            },
            dismissButton = { TextButton(onClick = { itemToLock = null }) { Text("Cancel") } }
        )
    }

    // Unlock a locked item (session-only)
    itemToUnlock?.let { item ->
        var password by remember(item.id) { mutableStateOf("") }
        var error by remember(item.id) { mutableStateOf<String?>(null) }
        AlertDialog(
            onDismissRequest = { itemToUnlock = null; pendingEditAfterUnlock = false },
            title = { Text("Unlock Item") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Enter the password for \"${item.title}\" to reveal it.", style = MaterialTheme.typography.bodySmall)
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it; error = null },
                        label = { Text("Item password") },
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
                    if (viewModel.tryUnlock(item, password)) {
                        itemToUnlock = null
                        if (pendingEditAfterUnlock) {
                            pendingEditAfterUnlock = false
                            onNavigateToEdit(item.id)
                        }
                    } else {
                        error = "Wrong password"
                    }
                }) { Text("Unlock") }
            },
            dismissButton = { TextButton(onClick = { itemToUnlock = null; pendingEditAfterUnlock = false }) { Text("Cancel") } }
        )
    }
}

@Composable
fun VaultItemCard(
    item: DecryptedVaultItem,
    currentTime: Long,
    onClick: () -> Unit,
    onCopyUsername: () -> Unit,
    onCopyPassword: () -> Unit,
    onCopyTotp: (String) -> Unit,
    isUnlocked: Boolean = true,
    onTogglePin: () -> Unit = {},
    onLockItem: () -> Unit = {},
    onRemoveLock: () -> Unit = {}
) {
    var showMenu by remember { mutableStateOf(false) }
    val lockedHidden = item.isLocked && !isUnlocked
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shadowElevation = 1.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    val initial = item.title.firstOrNull()?.toString()?.uppercase() ?: "?"
                    Text(
                        text = initial,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                
                Spacer(modifier = Modifier.width(14.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (item.isPinned) {
                            Icon(Icons.Default.PushPin, contentDescription = "Pinned", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                        if (item.isLocked) {
                            Icon(Icons.Default.Lock, contentDescription = "Locked", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                        Text(
                            text = item.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (item.username.isNotEmpty()) {
                        Text(
                            text = item.username,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (item.isLocked && !isUnlocked) {
                        Text(
                            text = "••••••  Locked — tap to unlock",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Box {
                    IconButton(onClick = { showMenu = true }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Item options", modifier = Modifier.size(18.dp))
                    }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        DropdownMenuItem(
                            text = { Text(if (item.isPinned) "Unpin" else "Pin") },
                            onClick = { showMenu = false; onTogglePin() },
                            leadingIcon = { Icon(Icons.Default.PushPin, contentDescription = null) }
                        )
                        DropdownMenuItem(
                            text = { Text(if (item.isLocked) "Change Password" else "Lock with Password") },
                            onClick = { showMenu = false; onLockItem() },
                            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) }
                        )
                        if (item.isLocked) {
                            DropdownMenuItem(
                                text = { Text("Remove Lock") },
                                onClick = { showMenu = false; onRemoveLock() }
                            )
                        }
                    }
                }

                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "Edit details",
                    tint = MaterialTheme.colorScheme.outline
                )
            }

            // Quick actions row for copying credentials
            if (item.username.isNotEmpty() || item.password.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (item.username.isNotEmpty()) {
                        OutlinedButton(
                            onClick = onCopyUsername,
                            modifier = Modifier.height(32.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Copy User", fontSize = 11.sp)
                        }
                    }

                    if (item.password.isNotEmpty() && !lockedHidden) {
                        OutlinedButton(
                            onClick = onCopyPassword,
                            modifier = Modifier.height(32.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Key, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Copy Pass", fontSize = 11.sp)
                        }
                    }
                }
            }
            
            if (item.totpSecret.isNotEmpty() && !lockedHidden) {
                Spacer(modifier = Modifier.height(12.dp))
                
                val totpCode = TOTPGenerator.generateTOTP(item.totpSecret, currentTime)
                val formattedCode = if (totpCode.length == 6) "${totpCode.substring(0,3)} ${totpCode.substring(3)}" else totpCode

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                        .clickable { onCopyTotp(totpCode) }
                        .padding(12.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    "2FA CODE",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    letterSpacing = 1.sp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    "(tap to copy)",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            CircularProgressIndicator(
                                progress = { TOTPGenerator.getProgressPercentage(currentTime) },
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = formattedCode,
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                            )
                            Icon(
                                Icons.Default.ContentCopy,
                                contentDescription = "Copy 2FA code",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
