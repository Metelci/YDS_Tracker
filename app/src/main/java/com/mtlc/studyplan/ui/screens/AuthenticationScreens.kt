package com.mtlc.studyplan.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.mtlc.studyplan.security.AuthenticationManager
import com.mtlc.studyplan.ui.theme.LocalSpacing
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Main authentication screen that determines which authentication method to show
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthenticationScreen(
    authManager: AuthenticationManager,
    onAuthenticationSuccess: () -> Unit,
    onAuthenticationFailed: (String) -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    var showBiometric by remember { mutableStateOf(false) }
    var showPin by remember { mutableStateOf(false) }
    var showPassword by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }

    // Check authentication method on initialization
    LaunchedEffect(Unit) {
        delay(500) // Brief loading for better UX

        when (authManager.getCurrentAuthMethod()) {
            AuthenticationManager.AuthMethod.BIOMETRIC -> {
                if (authManager.isBiometricEnabled() && authManager.isBiometricAvailable()) {
                    showBiometric = true
                } else {
                    showPin = true
                }
            }
            AuthenticationManager.AuthMethod.PIN -> showPin = true
            AuthenticationManager.AuthMethod.PASSWORD -> showPassword = true
            else -> showPin = true // Default to PIN
        }
        isLoading = false
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        when {
            isLoading -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircularProgressIndicator()
                    Text("Initializing security...", style = MaterialTheme.typography.bodyLarge)
                }
            }
            showBiometric -> {
                BiometricAuthenticationScreen(
                    authManager = authManager,
                    onSuccess = onAuthenticationSuccess,
                    onFallback = { showPin = true }
                )
            }
            showPin -> {
                PinAuthenticationScreen(
                    authManager = authManager,
                    onSuccess = onAuthenticationSuccess,
                    onSwitchToBiometric = {
                        if (authManager.isBiometricEnabled() && authManager.isBiometricAvailable()) {
                            showBiometric = true
                            showPin = false
                        }
                    }
                )
            }
            showPassword -> {
                PasswordAuthenticationScreen(
                    authManager = authManager,
                    onSuccess = onAuthenticationSuccess
                )
            }
        }
    }
}

/**
 * First-time setup screen for new users
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FirstTimeSetupScreen(
    authManager: AuthenticationManager,
    onSetupComplete: () -> Unit
) {
    var currentStep by remember { mutableStateOf(0) }
    var pin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var selectedMethod by remember { mutableStateOf(AuthenticationManager.AuthMethod.PIN) }
    var showError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    val steps = listOf(
        "Choose Authentication Method",
        "Set Your PIN",
        "Confirm PIN",
        "Enable Biometric (Optional)"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "Security Setup",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            "Step ${currentStep + 1} of ${steps.size}: ${steps[currentStep]}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(32.dp))

        when (currentStep) {
            0 -> {
                // Choose authentication method
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Choose your preferred authentication method:")

                    AuthenticationMethodCard(
                        title = "PIN Code",
                        description = "4-6 digit PIN for quick access",
                        icon = Icons.Default.Lock,
                        selected = selectedMethod == AuthenticationManager.AuthMethod.PIN,
                        onClick = { selectedMethod = AuthenticationManager.AuthMethod.PIN }
                    )

                    AuthenticationMethodCard(
                        title = "Password",
                        description = "Strong password with letters, numbers, and symbols",
                        icon = Icons.Default.Password,
                        selected = selectedMethod == AuthenticationManager.AuthMethod.PASSWORD,
                        onClick = { selectedMethod = AuthenticationManager.AuthMethod.PASSWORD }
                    )

                    if (authManager.isBiometricAvailable()) {
                        AuthenticationMethodCard(
                            title = "Biometric",
                            description = "Fingerprint or face recognition",
                            icon = Icons.Outlined.Fingerprint,
                            selected = selectedMethod == AuthenticationManager.AuthMethod.BIOMETRIC,
                            onClick = { selectedMethod = AuthenticationManager.AuthMethod.BIOMETRIC }
                        )
                    }
                }
            }
            1 -> {
                // Set PIN/Password
                when (selectedMethod) {
                    AuthenticationManager.AuthMethod.PIN -> {
                        PinSetupField(
                            value = pin,
                            onValueChange = { pin = it },
                            label = "Enter PIN (4-6 digits)"
                        )
                    }
                    AuthenticationManager.AuthMethod.PASSWORD -> {
                        PasswordSetupField(
                            value = pin,
                            onValueChange = { pin = it },
                            label = "Enter Password"
                        )
                    }
                    else -> {
                        PinSetupField(
                            value = pin,
                            onValueChange = { pin = it },
                            label = "Enter PIN (4-6 digits)"
                        )
                    }
                }
            }
            2 -> {
                // Confirm PIN/Password
                when (selectedMethod) {
                    AuthenticationManager.AuthMethod.PIN -> {
                        PinSetupField(
                            value = confirmPin,
                            onValueChange = { confirmPin = it },
                            label = "Confirm PIN"
                        )
                    }
                    AuthenticationManager.AuthMethod.PASSWORD -> {
                        PasswordSetupField(
                            value = confirmPin,
                            onValueChange = { confirmPin = it },
                            label = "Confirm Password"
                        )
                    }
                    else -> {
                        PinSetupField(
                            value = confirmPin,
                            onValueChange = { confirmPin = it },
                            label = "Confirm PIN"
                        )
                    }
                }
            }
            3 -> {
                // Biometric setup (if available)
                if (authManager.isBiometricAvailable()) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text("Enable biometric authentication for faster access?")
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(onClick = {
                                // Enable biometric
                                authManager.enableBiometric()
                                onSetupComplete()
                            }) {
                                Text("Enable Biometric")
                            }
                            OutlinedButton(onClick = onSetupComplete) {
                                Text("Skip")
                            }
                        }
                    }
                } else {
                    // Skip biometric step
                    onSetupComplete()
                }
            }
        }

        if (showError) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                errorMessage,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            if (currentStep > 0) {
                OutlinedButton(
                    onClick = { currentStep-- },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Back")
                }
            }

            Button(
                onClick = {
                    when (currentStep) {
                        0 -> {
                            if (selectedMethod != AuthenticationManager.AuthMethod.BIOMETRIC) {
                                currentStep++
                            } else {
                                // Skip PIN setup for biometric-only
                                onSetupComplete()
                            }
                        }
                        1 -> {
                            if (pin.length in 4..6 || selectedMethod == AuthenticationManager.AuthMethod.PASSWORD) {
                                currentStep++
                                showError = false
                            } else {
                                showError = true
                                errorMessage = "PIN must be 4-6 digits"
                            }
                        }
                        2 -> {
                            if (pin == confirmPin) {
                                // Save the authentication method and PIN
                                when (selectedMethod) {
                                    AuthenticationManager.AuthMethod.PIN -> {
                                        if (authManager.setPin(pin)) {
                                            currentStep++
                                        } else {
                                            showError = true
                                            errorMessage = "Failed to set PIN"
                                        }
                                    }
                                    AuthenticationManager.AuthMethod.PASSWORD -> {
                                        if (authManager.setPassword(pin)) {
                                            currentStep++
                                        } else {
                                            showError = true
                                            errorMessage = "Failed to set password"
                                        }
                                    }
                                    else -> currentStep++
                                }
                            } else {
                                showError = true
                                errorMessage = "PINs do not match"
                            }
                        }
                        3 -> onSetupComplete()
                    }
                },
                modifier = Modifier.weight(1f),
                enabled = when (currentStep) {
                    0 -> selectedMethod != AuthenticationManager.AuthMethod.BIOMETRIC
                    1 -> pin.isNotBlank() && (pin.length in 4..6 || selectedMethod == AuthenticationManager.AuthMethod.PASSWORD)
                    2 -> confirmPin.isNotBlank()
                    else -> true
                }
            ) {
                Text(
                    when (currentStep) {
                        steps.lastIndex -> "Complete Setup"
                        else -> "Next"
                    }
                )
            }
        }
    }
}

/**
 * PIN authentication screen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PinAuthenticationScreen(
    authManager: AuthenticationManager,
    onSuccess: () -> Unit,
    onSwitchToBiometric: () -> Unit = {}
) {
    var pin by remember { mutableStateOf("") }
    var attempts by remember { mutableStateOf(0) }
    var showError by remember { mutableStateOf(false) }
    var isLocked by remember { mutableStateOf(false) }
    var lockoutTime by remember { mutableLongStateOf(0L) }

    val maxAttempts = 5
    val lockoutDuration = 30 * 60 * 1000L // 30 minutes

    LaunchedEffect(attempts) {
        if (attempts >= maxAttempts) {
            isLocked = true
            lockoutTime = System.currentTimeMillis() + lockoutDuration
        }
    }

    // Check lockout status
    LaunchedEffect(Unit) {
        while (isLocked) {
            val currentTime = System.currentTimeMillis()
            if (currentTime >= lockoutTime) {
                isLocked = false
                attempts = 0
                showError = false
            }
            delay(1000) // Check every second
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "Enter PIN",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(32.dp))

        if (isLocked) {
            val remainingMinutes = ((lockoutTime - System.currentTimeMillis()) / 1000 / 60).toInt()
            Text(
                "Too many failed attempts. Try again in $remainingMinutes minutes.",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        } else {
            PinInputField(
                value = pin,
                onValueChange = { pin = it },
                enabled = !isLocked
            )

            if (showError) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "Invalid PIN. ${maxAttempts - attempts} attempts remaining.",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    if (authManager.authenticateWithPin(pin)) {
                        onSuccess()
                    } else {
                        attempts++
                        showError = true
                        pin = ""
                    }
                },
                enabled = pin.length in 4..6 && !isLocked,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Authenticate")
            }

            if (authManager.isBiometricEnabled() && authManager.isBiometricAvailable()) {
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedButton(
                    onClick = onSwitchToBiometric,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Outlined.Fingerprint, contentDescription = "Biometric")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Use Biometric")
                }
            }
        }
    }
}

/**
 * Password authentication screen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PasswordAuthenticationScreen(
    authManager: AuthenticationManager,
    onSuccess: () -> Unit
) {
    var password by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }
    var attempts by remember { mutableStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "Enter Password",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            isError = showError,
            supportingText = if (showError) {
                { Text("Invalid password. Please try again.") }
            } else null,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                if (authManager.authenticateWithPassword(password)) {
                    onSuccess()
                } else {
                    attempts++
                    showError = true
                    password = ""
                }
            },
            enabled = password.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Authenticate")
        }
    }
}

/**
 * Biometric authentication screen
 */
@Composable
fun BiometricAuthenticationScreen(
    authManager: AuthenticationManager,
    onSuccess: () -> Unit,
    onFallback: () -> Unit
) {
    var showPrompt by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        showPrompt = true
    }

    if (showPrompt) {
        Dialog(onDismissRequest = onFallback) {
            Surface(
                shape = MaterialTheme.shapes.medium,
                tonalElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        Icons.Outlined.Fingerprint,
                        contentDescription = "Biometric",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(48.dp)
                    )

                    Text(
                        "Biometric Authentication",
                        style = MaterialTheme.typography.headlineSmall
                    )

                    Text(
                        "Use your fingerprint or face to continue",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedButton(onClick = onFallback) {
                        Text("Use PIN Instead")
                    }
                }
            }
        }
    }
}

// Helper composables
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AuthenticationMethodCard(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(icon, contentDescription = title)
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(description, style = MaterialTheme.typography.bodySmall)
            }
            RadioButton(selected = selected, onClick = onClick)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PinSetupField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String
) {
    OutlinedTextField(
        value = value,
        onValueChange = { if (it.length <= 6) onValueChange(it) },
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        visualTransformation = PasswordVisualTransformation(),
        modifier = Modifier.fillMaxWidth()
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PasswordSetupField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        visualTransformation = PasswordVisualTransformation(),
        modifier = Modifier.fillMaxWidth()
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PinInputField(
    value: String,
    onValueChange: (String) -> Unit,
    enabled: Boolean = true
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(6) { index ->
            OutlinedTextField(
                value = if (index < value.length) value[index].toString() else "",
                onValueChange = { newValue ->
                    if (newValue.length <= 1 && newValue.all { it.isDigit() }) {
                        val newPin = value.take(index) + newValue + value.drop(index + 1)
                        onValueChange(newPin)
                    }
                },
                modifier = Modifier
                    .size(48.dp)
                    .weight(1f, false),
                textStyle = MaterialTheme.typography.headlineMedium.copy(
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                ),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                visualTransformation = PasswordVisualTransformation(),
                enabled = enabled,
                singleLine = true
            )
        }
    }
}