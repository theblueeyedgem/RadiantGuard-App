package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.AuthScreenState
import com.example.ui.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    viewModel: AuthViewModel,
    onAuthenticated: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val focusManager = LocalFocusManager.current

    // Setup state
    var selectedPinLength by remember { mutableIntStateOf(4) }
    var setupPin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var setupPinVisible by remember { mutableStateOf(false) }
    var confirmPinVisible by remember { mutableStateOf(false) }

    // Unlock state
    var unlockPin by remember { mutableStateOf("") }
    var unlockPinVisible by remember { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf(false) }

    // Sync selectedPinLength with stored length when entering screen
    LaunchedEffect(uiState.pinLength) {
        if (uiState.pinLength in 4..10) {
            selectedPinLength = uiState.pinLength
        }
    }

    // When screen transitions to authenticated, trigger callback
    LaunchedEffect(uiState.screenState) {
        if (uiState.screenState == AuthScreenState.AUTHENTICATED) {
            onAuthenticated()
        } else {
            setupPin = ""
            confirmPin = ""
            unlockPin = ""
        }
    }

    val scrollState = rememberScrollState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Radiant Guard Logo
            Image(
                painter = painterResource(id = R.drawable.ic_radiant_guard_logo),
                contentDescription = "Radiant Guard Logo",
                modifier = Modifier
                    .size(90.dp)
                    .clip(RoundedCornerShape(22.dp))
            )

            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Radiant Guard",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(6.dp))
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
            ) {
                Text(
                    text = "ZERO-KNOWLEDGE PIN VAULT",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                shadowElevation = 2.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    when (uiState.screenState) {
                        AuthScreenState.SETUP_REQUIRED -> {
                            Text(
                                text = "Create Vault PIN",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Choose your preferred PIN length and set a secure numeric code.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )

                            // Option for PIN length decided entirely by user
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "SELECT PIN LENGTH",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    letterSpacing = 1.sp
                                )
                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val lengths = listOf(4, 6, 8)
                                    lengths.forEach { length ->
                                        FilterChip(
                                            selected = selectedPinLength == length,
                                            onClick = {
                                                selectedPinLength = length
                                                setupPin = ""
                                                confirmPin = ""
                                                viewModel.clearError()
                                            },
                                            label = {
                                                Text(
                                                    "$length Digits",
                                                    fontWeight = if (selectedPinLength == length) FontWeight.Bold else FontWeight.Normal
                                                )
                                            },
                                            modifier = Modifier.padding(horizontal = 4.dp)
                                        )
                                    }
                                }

                                // Stepper to choose custom length (4 to 10 digits)
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center,
                                    modifier = Modifier.padding(top = 4.dp)
                                ) {
                                    Text(
                                        "Custom: ",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    IconButton(
                                        onClick = {
                                            if (selectedPinLength > 4) {
                                                selectedPinLength--
                                                setupPin = ""
                                                confirmPin = ""
                                                viewModel.clearError()
                                            }
                                        },
                                        enabled = selectedPinLength > 4,
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.Remove, contentDescription = "Decrease length")
                                    }
                                    Text(
                                        text = "$selectedPinLength digits",
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.padding(horizontal = 6.dp)
                                    )
                                    IconButton(
                                        onClick = {
                                            if (selectedPinLength < 10) {
                                                selectedPinLength++
                                                setupPin = ""
                                                confirmPin = ""
                                                viewModel.clearError()
                                            }
                                        },
                                        enabled = selectedPinLength < 10,
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = "Increase length")
                                    }
                                }
                            }

                            // Visual PIN Dots Indicator
                            PinDotsRow(
                                count = selectedPinLength,
                                enteredLength = setupPin.length
                            )

                            // Enter PIN field (Strictly numeric keyboard)
                            OutlinedTextField(
                                value = setupPin,
                                onValueChange = { input ->
                                    val filtered = input.filter { it.isDigit() }.take(selectedPinLength)
                                    setupPin = filtered
                                    viewModel.clearError()
                                },
                                label = { Text("Enter $selectedPinLength-Digit PIN") },
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.NumberPassword,
                                    imeAction = ImeAction.Next
                                ),
                                visualTransformation = if (setupPinVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                trailingIcon = {
                                    IconButton(onClick = { setupPinVisible = !setupPinVisible }) {
                                        Icon(
                                            imageVector = if (setupPinVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                            contentDescription = if (setupPinVisible) "Hide PIN" else "Show PIN"
                                        )
                                    }
                                },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp)
                            )

                            // Confirm PIN field (Strictly numeric keyboard)
                            OutlinedTextField(
                                value = confirmPin,
                                onValueChange = { input ->
                                    val filtered = input.filter { it.isDigit() }.take(selectedPinLength)
                                    confirmPin = filtered
                                    viewModel.clearError()
                                },
                                label = { Text("Confirm $selectedPinLength-Digit PIN") },
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.NumberPassword,
                                    imeAction = ImeAction.Done
                                ),
                                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                                visualTransformation = if (confirmPinVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                trailingIcon = {
                                    IconButton(onClick = { confirmPinVisible = !confirmPinVisible }) {
                                        Icon(
                                            imageVector = if (confirmPinVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                            contentDescription = if (confirmPinVisible) "Hide PIN" else "Show PIN"
                                        )
                                    }
                                },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp)
                            )

                            AnimatedVisibility(visible = uiState.error != null) {
                                Text(
                                    text = uiState.error ?: "",
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall,
                                    textAlign = TextAlign.Center
                                )
                            }

                            Button(
                                onClick = {
                                    focusManager.clearFocus()
                                    if (setupPin.length != selectedPinLength) {
                                        viewModel.login("length_err")
                                    } else if (setupPin != confirmPin) {
                                        viewModel.login("mismatch")
                                    } else {
                                        viewModel.setupPin(setupPin, selectedPinLength)
                                    }
                                },
                                enabled = !uiState.isUnlocking && setupPin.length == selectedPinLength && confirmPin.length == selectedPinLength,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                if (uiState.isUnlocking) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                } else {
                                    Text("Set PIN & Create Vault", fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        AuthScreenState.LOGIN_REQUIRED -> {
                            val requiredLength = uiState.pinLength

                            Text(
                                text = "Unlock Vault",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Enter your $requiredLength-digit PIN to decrypt your vault.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )

                            // Visual PIN Dots
                            PinDotsRow(
                                count = requiredLength,
                                enteredLength = unlockPin.length
                            )

                            // Numeric-only PIN Field
                            OutlinedTextField(
                                value = unlockPin,
                                onValueChange = { input ->
                                    val filtered = input.filter { it.isDigit() }.take(requiredLength)
                                    unlockPin = filtered
                                    viewModel.clearError()

                                    // Automatic unlock when the user finishes entering all digits of their PIN
                                    if (filtered.length == requiredLength && !uiState.isUnlocking) {
                                        focusManager.clearFocus()
                                        viewModel.unlockWithPin(filtered)
                                    }
                                },
                                label = { Text("$requiredLength-Digit PIN") },
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.NumberPassword,
                                    imeAction = ImeAction.Done
                                ),
                                keyboardActions = KeyboardActions(
                                    onDone = {
                                        focusManager.clearFocus()
                                        if (unlockPin.length == requiredLength) {
                                            viewModel.unlockWithPin(unlockPin)
                                        }
                                    }
                                ),
                                visualTransformation = if (unlockPinVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                trailingIcon = {
                                    IconButton(onClick = { unlockPinVisible = !unlockPinVisible }) {
                                        Icon(
                                            imageVector = if (unlockPinVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                            contentDescription = if (unlockPinVisible) "Hide PIN" else "Show PIN"
                                        )
                                    }
                                },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp)
                            )

                            AnimatedVisibility(visible = uiState.error != null) {
                                Text(
                                    text = uiState.error ?: "",
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall,
                                    textAlign = TextAlign.Center
                                )
                            }

                            Button(
                                onClick = {
                                    focusManager.clearFocus()
                                    viewModel.unlockWithPin(unlockPin)
                                },
                                enabled = !uiState.isUnlocking && unlockPin.length == requiredLength,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                if (uiState.isUnlocking) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Unlocking...")
                                } else {
                                    Text("Unlock Vault", fontWeight = FontWeight.Bold)
                                }
                            }

                            // Option to reset vault PIN if forgotten
                            TextButton(
                                onClick = { showResetDialog = true }
                            ) {
                                Text(
                                    "Forgot PIN / Reset Vault",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }

                        else -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Reset Vault PIN") },
            text = {
                Text("Because Radiant Guard is zero-knowledge encrypted, resetting your vault clears the stored master keys so you can set a new PIN. Do you want to proceed?")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showResetDialog = false
                        viewModel.resetVault()
                    }
                ) {
                    Text("Reset Vault", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun PinDotsRow(
    count: Int,
    enteredLength: Int
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 8.dp)
    ) {
        for (i in 0 until count) {
            val isFilled = i < enteredLength
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(
                        if (isFilled) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceVariant
                    )
                    .border(
                        1.5.dp,
                        if (isFilled) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outlineVariant,
                        CircleShape
                    )
            )
        }
    }
}
