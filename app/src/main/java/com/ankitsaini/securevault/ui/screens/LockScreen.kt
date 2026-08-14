package com.ankitsaini.securevault.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ankitsaini.securevault.data.model.LockType
import com.ankitsaini.securevault.auth.AuthViewModel

@Composable
fun LockScreen(
    packageName: String,
    appName: String,
    lockType: LockType,
    onAuthenticated: () -> Unit,
    onCancel: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    LaunchedEffect(packageName, lockType) {
        viewModel.setupAuthentication(packageName, lockType)
    }
    
    // Auto-authenticate if lock type is NONE
    LaunchedEffect(lockType) {
        if (lockType == LockType.NONE) {
            onAuthenticated()
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            // App Icon
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = appName.firstOrNull()?.toString() ?: "?",
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // App Name
            Text(
                text = appName,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "This app is protected",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Error Message
            if (uiState.errorMessage != null) {
                Text(
                    text = uiState.errorMessage!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            // Lock UI based on type
            when (lockType) {
                LockType.PIN -> PinInput(
                    onPinEntered = { pin ->
                        viewModel.authenticateWithPin(pin)
                    },
                    isLoading = uiState.isLoading,
                    isLockedOut = uiState.isLockedOut
                )
                
                LockType.PATTERN -> PatternInput(
                    onPatternEntered = { pattern ->
                        viewModel.authenticateWithPattern(pattern)
                    },
                    isLoading = uiState.isLoading,
                    isLockedOut = uiState.isLockedOut
                )
                
                LockType.BIOMETRIC -> BiometricPromptUI(
                    isAvailable = uiState.biometricAvailable,
                    onAuthenticate = {
                        // Will be handled by parent activity
                    }
                )
                
                LockType.NONE -> {
                    // No lock needed - auto authenticate
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Cancel Button
            TextButton(onClick = onCancel) {
                Text("Cancel")
            }
            
            // Locked Out Message
            if (uiState.isLockedOut) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Too many failed attempts. Please try again later.",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun PinInput(
    onPinEntered: (String) -> Unit,
    isLoading: Boolean,
    isLockedOut: Boolean
) {
    var pin by remember { mutableStateOf("") }
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // PIN Dots
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            repeat(4) { index ->
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(
                            if (index < pin.length) 
                                MaterialTheme.colorScheme.primary 
                            else 
                                MaterialTheme.colorScheme.surfaceVariant
                        )
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Number Pad
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            for (row in 0..2) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    for (col in 1..3) {
                        val number = row * 3 + col
                        NumberButton(
                            number = number.toString(),
                            onClick = {
                                if (pin.length < 4 && !isLockedOut && !isLoading) {
                                    pin += number
                                    if (pin.length == 4) {
                                        onPinEntered(pin)
                                        pin = ""
                                    }
                                }
                            }
                        )
                    }
                }
            }
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Spacer(modifier = Modifier.size(72.dp))
                
                NumberButton(
                    number = "0",
                    onClick = {
                        if (pin.length < 4 && !isLockedOut && !isLoading) {
                            pin += "0"
                            if (pin.length == 4) {
                                onPinEntered(pin)
                                pin = ""
                            }
                        }
                    }
                )
                
                IconButton(
                    onClick = {
                        if (pin.isNotEmpty() && !isLockedOut && !isLoading) {
                            pin = pin.dropLast(1)
                        }
                    },
                    modifier = Modifier.size(72.dp),
                    enabled = !isLockedOut && !isLoading
                ) {
                    Icon(
                        Icons.Default.Backspace,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
fun NumberButton(
    number: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(72.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = number,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun PatternInput(
    onPatternEntered: (List<Int>) -> Unit,
    isLoading: Boolean,
    isLockedOut: Boolean
) {
    var selectedPattern by remember { mutableStateOf(listOf<Int>()) }
    val patternSize = 3
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Draw your pattern",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Pattern Grid
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            for (row in 0 until patternSize) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    for (col in 0 until patternSize) {
                        val index = row * patternSize + col
                        PatternDot(
                            isSelected = selectedPattern.contains(index),
                            onClick = {
                                if (!isLockedOut && !isLoading) {
                                    if (!selectedPattern.contains(index)) {
                                        selectedPattern = selectedPattern + index
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Button(
                onClick = {
                    if (selectedPattern.isNotEmpty() && !isLockedOut && !isLoading) {
                        onPatternEntered(selectedPattern)
                    }
                },
                enabled = selectedPattern.isNotEmpty() && !isLockedOut && !isLoading
            ) {
                Text("Verify Pattern")
            }
            
            OutlinedButton(
                onClick = { selectedPattern = emptyList() },
                enabled = selectedPattern.isNotEmpty() && !isLockedOut && !isLoading
            ) {
                Text("Clear")
            }
        }
    }
}

@Composable
fun PatternDot(
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(
                if (isSelected) 
                    MaterialTheme.colorScheme.primary 
                else 
                    MaterialTheme.colorScheme.surfaceVariant
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (isSelected) {
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onPrimary)
            )
        }
    }
}

@Composable
fun BiometricPromptUI(
    isAvailable: Boolean,
    onAuthenticate: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Fingerprint,
            contentDescription = "Biometric Authentication",
            modifier = Modifier.size(64.dp),
            tint = if (isAvailable) 
                MaterialTheme.colorScheme.primary 
            else 
                MaterialTheme.colorScheme.error
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = if (isAvailable) 
                "Use fingerprint or face to unlock" 
            else 
                "Biometric authentication not available",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
        
        if (isAvailable) {
            Spacer(modifier = Modifier.height(24.dp))
            
            Button(onClick = onAuthenticate) {
                Text("Authenticate")
            }
        }
    }
}
