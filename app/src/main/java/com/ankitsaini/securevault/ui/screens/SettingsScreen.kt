package com.ankitsaini.securevault.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ankitsaini.securevault.ui.viewmodel.SettingsViewModel
import com.ankitsaini.securevault.utils.Constants

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBackClick: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val settings by viewModel.settings.collectAsState()
    val permissions by viewModel.permissions.collectAsState()
    
    var showMasterPinDialog by remember { mutableStateOf(false) }
    var showBackupDialog by remember { mutableStateOf(false) }
    var showRestoreDialog by remember { mutableStateOf(false) }
    var showClearDataDialog by remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Settings")
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            // Security Section
            SettingsSection(title = "Security") {
                SettingsItem(
                    icon = Icons.Default.Lock,
                    title = "Master PIN",
                    subtitle = "Set or change master PIN",
                    onClick = { showMasterPinDialog = true }
                )
                
                SettingsItem(
                    icon = Icons.Default.Pattern,
                    title = "Master Pattern",
                    subtitle = "Set or change master pattern",
                    onClick = { viewModel.setupMasterPattern() }
                )
                
                SettingsToggleItem(
                    icon = Icons.Default.Fingerprint,
                    title = "Biometric Authentication",
                    subtitle = "Use fingerprint or face to unlock",
                    isChecked = settings.biometricEnabled,
                    onToggle = { viewModel.toggleBiometric() }
                )
            }
            
            // Protection Section
            SettingsSection(title = "Protection") {
                SettingsToggleItem(
                    icon = Icons.Default.Security,
                    title = "Auto-start on Boot",
                    subtitle = "Start protection when device boots",
                    isChecked = settings.autoStartOnBoot,
                    onToggle = { viewModel.toggleAutoStart() }
                )
                
                SettingsToggleItem(
                    icon = Icons.Default.PhotoCamera,
                    title = "Intruder Photo Capture",
                    subtitle = "Capture photo on failed attempts",
                    isChecked = settings.intruderPhotoEnabled,
                    onToggle = { viewModel.toggleIntruderPhoto() }
                )
                
                SettingsItem(
                    icon = Icons.Default.Timer,
                    title = "Max Failed Attempts",
                    subtitle = "${settings.maxFailedAttempts} attempts",
                    onClick = { viewModel.showFailedAttemptsDialog() }
                )
                
                SettingsItem(
                    icon = Icons.Default.Schedule,
                    title = "Relock Timeout",
                    subtitle = "${settings.relockTimeout} seconds",
                    onClick = { viewModel.showRelockTimeoutDialog() }
                )
            }
            
            // Permissions Section
            SettingsSection(title = "Permissions") {
                PermissionItem(
                    title = "Accessibility Service",
                    subtitle = "Detect protected app launches",
                    isGranted = permissions.accessibilityGranted,
                    onClick = { viewModel.openAccessibilitySettings() }
                )
                
                PermissionItem(
                    title = "Notification Access",
                    subtitle = "Mask notifications from protected apps",
                    isGranted = permissions.notificationGranted,
                    onClick = { viewModel.openNotificationSettings() }
                )
                
                PermissionItem(
                    title = "Overlay Permission",
                    subtitle = "Display lock screen over apps",
                    isGranted = permissions.overlayGranted,
                    onClick = { viewModel.openOverlaySettings() }
                )
                
                PermissionItem(
                    title = "Usage Access",
                    subtitle = "Monitor app usage statistics",
                    isGranted = permissions.usageStatsGranted,
                    onClick = { viewModel.openUsageAccessSettings() }
                )
                
                PermissionItem(
                    title = "Battery Optimization",
                    subtitle = "Run reliably in background",
                    isGranted = permissions.batteryOptimizationGranted,
                    onClick = { viewModel.requestIgnoreBatteryOptimizations() }
                )
            }
            
            // Advanced Section
            SettingsSection(title = "Advanced") {
                SettingsToggleItem(
                    icon = Icons.Default.VisibilityOff,
                    title = "Stealth Mode",
                    subtitle = "Hide app from launcher",
                    isChecked = settings.stealthMode,
                    onToggle = { viewModel.toggleStealthMode() }
                )
                
                SettingsToggleItem(
                    icon = Icons.Default.BugReport,
                    title = "Fake Crash",
                    subtitle = "Show fake crash on failed attempt",
                    isChecked = settings.fakeCrashEnabled,
                    onToggle = { viewModel.toggleFakeCrash() }
                )
                
                SettingsItem(
                    icon = Icons.Default.Backup,
                    title = "Backup Settings",
                    subtitle = "Export security configuration",
                    onClick = { showBackupDialog = true }
                )
                
                SettingsItem(
                    icon = Icons.Default.Restore,
                    title = "Restore Settings",
                    subtitle = "Import security configuration",
                    onClick = { showRestoreDialog = true }
                )
            }
            
            // Data Management Section
            SettingsSection(title = "Data Management") {
                SettingsItem(
                    icon = Icons.Default.Delete,
                    title = "Clear Security Log",
                    subtitle = "Delete all security events",
                    onClick = { viewModel.clearSecurityLog() }
                )
                
                SettingsItem(
                    icon = Icons.Default.DeleteSweep,
                    title = "Delete Intruder Photos",
                    subtitle = "Remove all captured photos",
                    onClick = { viewModel.deleteAllPhotos() }
                )
                
                SettingsItem(
                    icon = Icons.Default.Warning,
                    title = "Clear All Data",
                    subtitle = "Reset app to factory settings",
                    onClick = { showClearDataDialog = true },
                    isDestructive = true
                )
            }
            
            // About Section
            SettingsSection(title = "About") {
                SettingsItem(
                    icon = Icons.Default.Info,
                    title = "Version",
                    subtitle = viewModel.getAppVersion(),
                    onClick = null
                )
                
                SettingsItem(
                    icon = Icons.Default.Phone,
                    title = "Device",
                    subtitle = viewModel.getDeviceInfo(),
                    onClick = null
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
    
    // Dialogs
    if (showMasterPinDialog) {
        MasterPinDialog(
            onDismiss = { showMasterPinDialog = false },
            onConfirm = { pin ->
                viewModel.setMasterPin(pin)
                showMasterPinDialog = false
            }
        )
    }
    
    if (showBackupDialog) {
        BackupDialog(
            onDismiss = { showBackupDialog = false },
            onConfirm = {
                viewModel.createBackup()
                showBackupDialog = false
            }
        )
    }
    
    if (showRestoreDialog) {
        RestoreDialog(
            onDismiss = { showRestoreDialog = false },
            onConfirm = {
                viewModel.restoreBackup()
                showRestoreDialog = false
            }
        )
    }
    
    if (showClearDataDialog) {
        AlertDialog(
            onDismissRequest = { showClearDataDialog = false },
            title = { Text("Clear All Data") },
            text = { 
                Text("This will delete all settings, security logs, and intruder photos. This action cannot be undone.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearAllData()
                        showClearDataDialog = false
                    }
                ) {
                    Text("Clear All", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDataDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                content = content
            )
        }
    }
}

@Composable
fun SettingsItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String? = null,
    onClick: (() -> Unit)? = null,
    isDestructive: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = onClick != null) { onClick?.invoke() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
            )
            
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        if (onClick != null) {
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun SettingsToggleItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    isChecked: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Switch(
            checked = isChecked,
            onCheckedChange = { onToggle() }
        )
    }
}

@Composable
fun PermissionItem(
    title: String,
    subtitle: String,
    isGranted: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (isGranted) Icons.Default.CheckCircle else Icons.Default.Warning,
            contentDescription = if (isGranted) "Granted" else "Not Granted",
            tint = if (isGranted) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error,
            modifier = Modifier.size(24.dp)
        )
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun MasterPinDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var pin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Set Master PIN") },
        text = {
            Column {
                OutlinedTextField(
                    value = pin,
                    onValueChange = { 
                        if (it.length <= 6 && it.all { char -> char.isDigit() }) {
                            pin = it
                            errorMessage = null
                        }
                    },
                    label = { Text("PIN (4-6 digits)") },
                    visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.NumberPassword
                    ),
                    singleLine = true
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                OutlinedTextField(
                    value = confirmPin,
                    onValueChange = { 
                        if (it.length <= 6 && it.all { char -> char.isDigit() }) {
                            confirmPin = it
                            errorMessage = null
                        }
                    },
                    label = { Text("Confirm PIN") },
                    visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.NumberPassword
                    ),
                    singleLine = true
                )
                
                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    when {
                        pin.length < 4 -> errorMessage = "PIN must be at least 4 digits"
                        pin != confirmPin -> errorMessage = "PINs do not match"
                        else -> onConfirm(pin)
                    }
                }
            ) {
                Text("Set PIN")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun BackupDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Backup") },
        text = { 
            Text("This will create a backup of your security configuration, including protected apps and settings. The backup will be stored securely in the app's private storage.")
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Create Backup")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun RestoreDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Restore Backup") },
        text = { 
            Text("This will restore your security configuration from the backup. Current settings will be overwritten.")
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Restore")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
