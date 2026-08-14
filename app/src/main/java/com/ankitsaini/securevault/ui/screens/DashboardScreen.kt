package com.ankitsaini.securevault.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ankitsaini.securevault.data.model.ProtectedApp
import com.ankitsaini.securevault.data.model.SecurityEvent
import com.ankitsaini.securevault.data.repository.DashboardData
import com.ankitsaini.securevault.ui.viewmodel.DashboardViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onNavigateToAppList: () -> Unit,
    onNavigateToSecurityLog: () -> Unit,
    onNavigateToSettings: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val dashboardData by viewModel.dashboardData.collectAsState()
    val isServiceRunning by viewModel.isServiceRunning.collectAsState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Secure Vault",
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Service Status Card
            item {
                ServiceStatusCard(
                    isRunning = isServiceRunning,
                    onToggleService = { viewModel.toggleService() }
                )
            }
            
            // Stats Overview
            item {
                StatsOverview(
                    dashboardData = dashboardData
                )
            }
            
            // Quick Actions
            item {
                QuickActionsRow(
                    onViewAllApps = onNavigateToAppList,
                    onViewSecurityLog = onNavigateToSecurityLog,
                    onOpenSettings = onNavigateToSettings
                )
            }
            
            // Recently Protected Apps
            if (dashboardData.protectedApps.isNotEmpty()) {
                item {
                    Text(
                        text = "Protected Apps",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                items(dashboardData.protectedApps.take(5)) { app ->
                    ProtectedAppCard(
                        app = app,
                        onClick = { /* Navigate to app details */ }
                    )
                }
            }
            
            // Recent Security Events
            if (dashboardData.recentEvents.isNotEmpty()) {
                item {
                    Text(
                        text = "Recent Activity",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                items(dashboardData.recentEvents.take(5)) { event ->
                    SecurityEventItem(event = event)
                }
            }
        }
    }
}

@Composable
fun ServiceStatusCard(
    isRunning: Boolean,
    onToggleService: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isRunning) 
                MaterialTheme.colorScheme.primaryContainer 
            else 
                MaterialTheme.colorScheme.errorContainer
        )
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
                    text = if (isRunning) "Protection Active" else "Protection Inactive",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (isRunning) 
                        "Your apps are being protected" 
                    else 
                        "Enable protection to secure your apps",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Switch(
                checked = isRunning,
                onCheckedChange = { onToggleService() }
            )
        }
    }
}

@Composable
fun StatsOverview(dashboardData: DashboardData) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatItem(
                value = dashboardData.totalProtectedApps.toString(),
                label = "Protected",
                icon = Icons.Default.Lock
            )
            StatItem(
                value = dashboardData.totalSecurityEvents.toString(),
                label = "Events",
                icon = Icons.Default.Security
            )
            StatItem(
                value = dashboardData.totalFailedAttempts.toString(),
                label = "Failed",
                icon = Icons.Default.Warning
            )
        }
    }
}

@Composable
fun StatItem(
    value: String,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun QuickActionsRow(
    onViewAllApps: () -> Unit,
    onViewSecurityLog: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        QuickActionButton(
            text = "Apps",
            icon = Icons.Default.Apps,
            onClick = onViewAllApps
        )
        QuickActionButton(
            text = "Log",
            icon = Icons.Default.History,
            onClick = onViewSecurityLog
        )
        QuickActionButton(
            text = "Settings",
            icon = Icons.Default.Settings,
            onClick = onOpenSettings
        )
    }
}

@Composable
fun QuickActionButton(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = text,
            modifier = Modifier.size(32.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium
        )
    }
}

@Composable
fun ProtectedAppCard(
    app: ProtectedApp,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = app.appName.firstOrNull()?.toString() ?: "?",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = app.appName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = app.lockType.name,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = "Protected",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun SecurityEventItem(event: SecurityEvent) {
    val dateFormat = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = when (event.eventType) {
                com.ankitsaini.securevault.data.model.EventType.UNLOCK_FAILED -> Icons.Default.Warning
                com.ankitsaini.securevault.data.model.EventType.UNLOCK_SUCCESSFUL -> Icons.Default.CheckCircle
                com.ankitsaini.securevault.data.model.EventType.INTRUDER_PHOTO_CAPTURED -> Icons.Default.PhotoCamera
                else -> Icons.Default.Info
            },
            contentDescription = null,
            tint = when (event.eventType) {
                com.ankitsaini.securevault.data.model.EventType.UNLOCK_FAILED -> MaterialTheme.colorScheme.error
                com.ankitsaini.securevault.data.model.EventType.UNLOCK_SUCCESSFUL -> Color(0xFF4CAF50)
                else -> MaterialTheme.colorScheme.primary
            }
        )
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Column {
            Text(
                text = event.eventType.name.replace("_", " "),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = dateFormat.format(Date(event.eventTimestamp)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
