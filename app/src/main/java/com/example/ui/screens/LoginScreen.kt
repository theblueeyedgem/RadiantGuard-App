package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
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

/**
 * Glassmorphic welcome / lock screen (item 8).
 *
 * Deep brown/amber vertical gradient matching the app theme (with a darker
 * variant in dark mode), three slowly floating translucent orbs, and a glass
 * card (translucent surface, thin light border, 28dp corners) holding the
 * logo (scale-in entrance), app name, tagline, feature chips and the PIN
 * entry. Entrance uses AnimatedVisibility slide+fade; the orbs float on an
 * infiniteTransition.
 *
 * Behaviour fixes kept from item 1:
 *  - SETUP validation errors are local text now — no more login("length_err")
 *    / login("mismatch") hack that fed fake PINs into the unlock path.
 *  - Single unlock trigger: auto-unlock on the last digit. The keyboard Done
 *    action only dismisses the keyboard now; the Unlock button is guarded by
 *    AuthViewModel's isUnlocking re-entry guard, so it can never double-fire.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    viewModel: AuthViewModel,
    onAuthenticated: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val focusManager = LocalFocusManager.current
    val darkTheme = isSystemInDarkTheme()

    // Setup state
    var selectedPinLength by remember { mutableIntStateOf(4) }
    var setupPin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var setupPinVisible by remember { mutableStateOf(false) }
    var confirmPinVisible by remember { mutableStateOf(false) }
    var setupError by remember { mutableStateOf<String?>(null) }

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
            setupError = null
        }
    }

    // Entrance: logo scale-in + content slide/fade
    var entered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { entered = true }
    val logoScale by animateFloatAsState(
        targetValue = if (entered) 1f else 0.6f,
        animationSpec = tween(600, easing = FastOutSlowInEasing),
        label = "logoScale"
    )

    // Slow floating orbs
    val infinite = rememberInfiniteTransition(label = "orbFloat")
    val orb1 by infinite.animateFloat(0f, 26f, infiniteRepeatable(tween(5200), RepeatMode.Reverse), label = "orb1")
    val orb2 by infinite.animateFloat(0f, -20f, infiniteRepeatable(tween(6400), RepeatMode.Reverse), label = "orb2")
    val orb3 by infinite.animateFloat(0f, 16f, infiniteRepeatable(tween(7600), RepeatMode.Reverse), label = "orb3")

    val gradientColors = if (darkTheme) {
        listOf(Color(0xFF1A1009), Color(0xFF2A1A0E), Color(0xFF120B06))
    } else {
        listOf(Color(0xFF8E4C1E), Color(0xFFB4703A), Color(0xFFF3DFD6))
    }
    val glassBorder = Color.White.copy(alpha = 0.35f)
    val glassSurface = if (darkTheme) Color(0xFF2A1D12).copy(alpha = 0.55f) else Color.White.copy(alpha = 0.55f)
    val onGlass = if (darkTheme) Color(0xFFF5E9DD) else Color(0xFF201A17)
    val onGlassMuted = onGlass.copy(alpha = 0.72f)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(gradientColors))
    ) {
        // Translucent blurred-look orbs (radial gradient, slow float)
        Box(
            modifier = Modifier
                .offset(x = (-40).dp, y = (20 + orb1).dp)
                .size(220.dp)
                .background(
                    Brush.radialGradient(
                        listOf(Color(0xFFFFB68C).copy(alpha = 0.55f), Color.Transparent)
                    )
                )
        )
        Box(
            modifier = Modifier
                .offset(x = 220.dp, y = (300 + orb2).dp)
                .size(260.dp)
                .background(
                    Brush.radialGradient(
                        listOf(Color(0xFFFFDBCA).copy(alpha = 0.45f), Color.Transparent)
                    )
                )
        )
        Box(
            modifier = Modifier
                .offset(x = 60.dp, y = (620 + orb3).dp)
                .size(200.dp)
                .background(
                    Brush.radialGradient(
                        listOf(Color(0xFFD97706).copy(alpha = 0.4f), Color.Transparent)
                    )
                )
        )

        AnimatedVisibility(
            visible = entered,
            enter = fadeIn(tween(500)) + slideInVertically(tween(600)) { it / 3 },
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 40.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Logo with scale-in
                Image(
                    painter = painterResource(id = R.drawable.ic_radiant_guard_logo),
                    contentDescription = "Radiant Guard Logo",
                    modifier = Modifier
                        .size(96.dp)
                        .scale(logoScale)
                        .clip(RoundedCornerShape(24.dp))
                        .border(1.5.dp, glassBorder, RoundedCornerShape(24.dp))
                )

                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Radiant Guard",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Your passwords, subscriptions & notes — locked with one PIN.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.85f),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(14.dp))
                // Feature chips — small glass pills
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Vault" to Icons.Default.VpnKey, "Subscriptions" to Icons.Default.EventRepeat, "Notes" to Icons.Default.Description).forEach { (label, icon) ->
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = Color.White.copy(alpha = 0.18f),
                            border = BorderStroke(1.dp, glassBorder)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(label, color = Color.White, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Glass card holding the PIN flow
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(28.dp),
                    color = glassSurface,
                    border = BorderStroke(1.dp, glassBorder),
                    shadowElevation = 8.dp
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
                                    color = onGlass
                                )
                                Text(
                                    text = "Choose your preferred PIN length and set a secure numeric code.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = onGlassMuted,
                                    textAlign = TextAlign.Center
                                )

                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "SELECT PIN LENGTH",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (darkTheme) Color(0xFFFFB68C) else MaterialTheme.colorScheme.primary,
                                        letterSpacing = 1.sp
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        listOf(4, 6, 8).forEach { length ->
                                            FilterChip(
                                                selected = selectedPinLength == length,
                                                onClick = {
                                                    selectedPinLength = length
                                                    setupPin = ""
                                                    confirmPin = ""
                                                    setupError = null
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
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center,
                                        modifier = Modifier.padding(top = 4.dp)
                                    ) {
                                        Text("Custom: ", style = MaterialTheme.typography.bodySmall, color = onGlassMuted)
                                        IconButton(
                                            onClick = {
                                                if (selectedPinLength > 4) {
                                                    selectedPinLength--
                                                    setupPin = ""
                                                    confirmPin = ""
                                                    setupError = null
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
                                            color = onGlass,
                                            modifier = Modifier.padding(horizontal = 6.dp)
                                        )
                                        IconButton(
                                            onClick = {
                                                if (selectedPinLength < 10) {
                                                    selectedPinLength++
                                                    setupPin = ""
                                                    confirmPin = ""
                                                    setupError = null
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

                                PinDotsRow(count = selectedPinLength, enteredLength = setupPin.length)

                                OutlinedTextField(
                                    value = setupPin,
                                    onValueChange = { input ->
                                        setupPin = input.filter { it.isDigit() }.take(selectedPinLength)
                                        setupError = null
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

                                OutlinedTextField(
                                    value = confirmPin,
                                    onValueChange = { input ->
                                        confirmPin = input.filter { it.isDigit() }.take(selectedPinLength)
                                        setupError = null
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

                                AnimatedVisibility(visible = setupError != null || uiState.error != null) {
                                    Text(
                                        text = setupError ?: uiState.error ?: "",
                                        color = MaterialTheme.colorScheme.error,
                                        style = MaterialTheme.typography.bodySmall,
                                        textAlign = TextAlign.Center
                                    )
                                }

                                Button(
                                    onClick = {
                                        focusManager.clearFocus()
                                        if (setupPin.length != selectedPinLength) {
                                            setupError = "PIN must be exactly $selectedPinLength digits"
                                        } else if (setupPin != confirmPin) {
                                            setupError = "PINs do not match"
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
                                    color = onGlass
                                )
                                Text(
                                    text = "Enter your $requiredLength-digit PIN to decrypt your vault.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = onGlassMuted,
                                    textAlign = TextAlign.Center
                                )

                                PinDotsRow(count = requiredLength, enteredLength = unlockPin.length)

                                OutlinedTextField(
                                    value = unlockPin,
                                    onValueChange = { input ->
                                        val filtered = input.filter { it.isDigit() }.take(requiredLength)
                                        unlockPin = filtered
                                        viewModel.clearError()
                                        // Single unlock trigger: auto-unlock on the last digit.
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
                                    // Done only dismisses the keyboard — it no longer
                                    // re-fires unlock (that was the double-unlock bug).
                                    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
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

                                TextButton(onClick = { showResetDialog = true }) {
                                    Text(
                                        "Forgot PIN? Reset vault",
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

                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Zero-knowledge: only your PIN can decrypt the vault.",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )
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
