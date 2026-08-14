package com.ankitsaini.securevault.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ankitsaini.securevault.data.model.LockType
import com.ankitsaini.securevault.data.model.ProtectedApp
import com.ankitsaini.securevault.ui.viewmodel.AppDetailsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppDetailsScreen(
    packageName: String,
    onBackClick: () -> Unit,
    viewModel: AppDetailsViewModel = hiltViewModel()
) {
    val appDetails by viewModel.appDetails.collectAsState()
    val securityEvents by viewModel.securityEvents.collectAsState()
    val isProtected by viewModel.isProtected.collectAsState()
    
    LaunchedEffect(packageName) {
        viewModel.loadAppDetails(packageName)
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = appDetails?.appName ?: "App Details",
                        fontWeight = FontWeight.Bold
                    )
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
            if (appDetails != null) {
                // App Info Section
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(60.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = appDetails!!.appName.firstOrNull()?.toString() ?: "?",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        Spacer(modifier = Modifier.width(16.dp))
                        
                        Column {
                            Text(
                                text = appDetails!!.appName,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = appDetails!!.packageName,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                
                // Protection Toggle
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Protection",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = if (isProtected) "App is protected" else "App is not protected",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = isProtected,
                            onCheckedChange = { viewModel.toggleProtection() }
                        )
                    }
                }
                
                if (isProtected) {
                    // Lock Type Selection
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Text(
                                text = "Lock Type",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Medium
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            LockTypeSelector(
                                currentLockType = appDetails!!.lockType,
                                onLockTypeSelected = { lockType ->
                                    viewModel.updateLockType(lockType)
                                }
                            )
                        }
                    }
                    
                    // Security Settings
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Text(
                                text = "Security Settings",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Medium
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            SettingsToggle(
                                title = "Notification Masking",
                                subtitle = "Hide notification content when locked",
                                isChecked = appDetails!!.notificationMasking,
                                onToggle = { viewModel.toggleNotificationMasking() }
                            )
                            
                            SettingsToggle(
                                title = "Intruder Photo",
                                subtitle = "Capture photo on failed attempts",
                                isChecked = appDetails!!.intruderPhotoEnabled,
                                onToggle = { viewModel.toggleIntruderPhoto() }
                            )
                            
                            SettingsToggle(
                                title = "Relock on Screen Off",
                                subtitle = "Lock app when screen turns off",
                                isChecked = appDetails!!.relockOnScreenOff,
                                onToggle = { viewModel.toggleRelockOnScreenOff() }
                            )
                        }
                    }
                }
                
                // Recent Events
                if (securityEvents.isNotEmpty()) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Text(
                                text = "Recent Security Events",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Medium
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            securityEvents.take(5).forEach { event ->
                                SecurityEventItem(event = event)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LockTypeSelector(
    currentLockType: LockType,
    onLockTypeSelected: (LockType) -> Unit
) {
    Column {
        LockTypeOption(
            lockType = LockType.PIN,
            title = "PIN",
            subtitle = "Use a 4-6 digit PIN",
            isSelected = currentLockType == LockType.PIN,
            onClick = { onLockTypeSelected(LockType.PIN) }
        )
        
        LockTypeOption(
            lockType = LockType.PATTERN,
            title = "Pattern",
            subtitle = "Draw a pattern to unlock",
            isSelected = currentLockType == LockType.PATTERN,
            onClick = { onLockTypeSelected(LockType.PATTERN) }
        )
        
        LockTypeOption(
            lockType = LockType.BIOMETRIC,
            title = "Biometric",
            subtitle = "Use fingerprint or face",
            isSelected = currentLockType == LockType.BIOMETRIC,
            onClick = { onLockTypeSelected(LockType.BIOMETRIC) }
        )
    }
}

@Composable
fun LockTypeOption(
    lockType: LockType,
    title: String,
    subtitle: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = isSelected,
            onClick = onClick
        )
        
        Spacer(modifier = Modifier.width(8.dp))
        
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun SettingsToggle(
    title: String,
    subtitle: String,
    isChecked: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
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
