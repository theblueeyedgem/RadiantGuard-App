package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.AuthViewModel
import com.example.ui.NoteViewModel
import com.example.ui.SubscriptionViewModel
import com.example.ui.VaultViewModel

enum class MainTab {
    VAULT,
    SUBSCRIPTIONS,
    NOTES
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    vaultViewModel: VaultViewModel,
    subscriptionViewModel: SubscriptionViewModel,
    noteViewModel: NoteViewModel,
    authViewModel: AuthViewModel,
    onLock: () -> Unit,
    onNavigateToAddPassword: () -> Unit,
    onNavigateToEditPassword: (Int) -> Unit,
    onNavigateToAddNote: () -> Unit,
    onNavigateToEditNote: (Int) -> Unit
) {
    var currentTab by remember { mutableStateOf(MainTab.VAULT) }
    var showChangePinDialog by remember { mutableStateOf(false) }
    val authUiState by authViewModel.uiState.collectAsState()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_radiant_guard_logo),
                            contentDescription = "Logo",
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Radiant Guard",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "PIN Protected (${authUiState.pinLength} Digits)",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                },
                actions = {
                    // Change PIN button
                    IconButton(
                        onClick = { showChangePinDialog = true },
                        modifier = Modifier
                            .padding(end = 4.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                    ) {
                        Icon(
                            Icons.Default.Pin,
                            contentDescription = "Change PIN & Length",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Lock button
                    IconButton(
                        onClick = onLock,
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .background(MaterialTheme.colorScheme.secondaryContainer, CircleShape)
                    ) {
                        Icon(
                            Icons.Default.Lock,
                            contentDescription = "Lock Vault",
                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp
            ) {
                NavigationBarItem(
                    selected = currentTab == MainTab.VAULT,
                    onClick = { currentTab = MainTab.VAULT },
                    icon = { Icon(Icons.Default.VpnKey, contentDescription = "Vault") },
                    label = { Text("Vault") }
                )
                NavigationBarItem(
                    selected = currentTab == MainTab.SUBSCRIPTIONS,
                    onClick = { currentTab = MainTab.SUBSCRIPTIONS },
                    icon = { Icon(Icons.Default.EventRepeat, contentDescription = "Subscriptions") },
                    label = { Text("Subscriptions") }
                )
                NavigationBarItem(
                    selected = currentTab == MainTab.NOTES,
                    onClick = { currentTab = MainTab.NOTES },
                    icon = { Icon(Icons.Default.Description, contentDescription = "Notes") },
                    label = { Text("Notes") }
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            AnimatedContent(
                targetState = currentTab,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "MainTabContent"
            ) { tab ->
                when (tab) {
                    MainTab.VAULT -> VaultListScreen(
                        viewModel = vaultViewModel,
                        onNavigateToAdd = onNavigateToAddPassword,
                        onNavigateToEdit = onNavigateToEditPassword
                    )
                    MainTab.SUBSCRIPTIONS -> SubscriptionListScreen(
                        viewModel = subscriptionViewModel
                    )
                    MainTab.NOTES -> NotesListScreen(
                        viewModel = noteViewModel,
                        onNavigateToAddNote = onNavigateToAddNote,
                        onNavigateToEditNote = onNavigateToEditNote
                    )
                }
            }
        }
    }

    if (showChangePinDialog) {
        ChangePinDialog(
            currentLength = authUiState.pinLength,
            onDismiss = { showChangePinDialog = false },
            onConfirmNewPin = { newPin, newLength ->
                authViewModel.changePin(newPin, newLength) { success ->
                    if (success) {
                        Toast.makeText(context, "Vault PIN updated to $newLength digits", Toast.LENGTH_SHORT).show()
                        showChangePinDialog = false
                    } else {
                        Toast.makeText(context, "Failed to update PIN", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
    }
}

@Composable
fun ChangePinDialog(
    currentLength: Int,
    onDismiss: () -> Unit,
    onConfirmNewPin: (String, Int) -> Unit
) {
    var selectedLength by remember { mutableIntStateOf(currentLength) }
    var newPin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var pinVisible by remember { mutableStateOf(false) }
    var errorText by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Change Vault PIN", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    "You decide your PIN length. Choose your desired length and enter your new numeric PIN.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    "CHOOSE PIN LENGTH",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 1.sp
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(4, 6, 8).forEach { length ->
                        FilterChip(
                            selected = selectedLength == length,
                            onClick = {
                                selectedLength = length
                                newPin = ""
                                confirmPin = ""
                                errorText = null
                            },
                            label = { Text("$length Digits") }
                        )
                    }
                }

                // Custom length stepper
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Length: ", style = MaterialTheme.typography.bodySmall)
                    IconButton(
                        onClick = {
                            if (selectedLength > 4) {
                                selectedLength--
                                newPin = ""
                                confirmPin = ""
                                errorText = null
                            }
                        },
                        enabled = selectedLength > 4,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = "Decrease length")
                    }
                    Text(
                        text = "$selectedLength Digits",
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                    IconButton(
                        onClick = {
                            if (selectedLength < 10) {
                                selectedLength++
                                newPin = ""
                                confirmPin = ""
                                errorText = null
                            }
                        },
                        enabled = selectedLength < 10,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Increase length")
                    }
                }

                // New PIN
                OutlinedTextField(
                    value = newPin,
                    onValueChange = { input ->
                        newPin = input.filter { it.isDigit() }.take(selectedLength)
                        errorText = null
                    },
                    label = { Text("New $selectedLength-Digit PIN") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    visualTransformation = if (pinVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { pinVisible = !pinVisible }) {
                            Icon(
                                imageVector = if (pinVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = if (pinVisible) "Hide PIN" else "Show PIN"
                            )
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                // Confirm PIN
                OutlinedTextField(
                    value = confirmPin,
                    onValueChange = { input ->
                        confirmPin = input.filter { it.isDigit() }.take(selectedLength)
                        errorText = null
                    },
                    label = { Text("Confirm $selectedLength-Digit PIN") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    visualTransformation = if (pinVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                if (errorText != null) {
                    Text(
                        text = errorText ?: "",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (newPin.length != selectedLength) {
                        errorText = "PIN must be exactly $selectedLength digits"
                    } else if (newPin != confirmPin) {
                        errorText = "PINs do not match"
                    } else {
                        onConfirmNewPin(newPin, selectedLength)
                    }
                },
                enabled = newPin.length == selectedLength && confirmPin.length == selectedLength
            ) {
                Text("Update PIN")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
