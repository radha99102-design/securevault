package com.ankitsaini.securevault.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ankitsaini.securevault.data.model.SecurityEvent
import com.ankitsaini.securevault.data.model.EventType
import com.ankitsaini.securevault.ui.viewmodel.SecurityLogViewModel
import com.ankitsaini.securevault.utils.DateUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecurityLogScreen(
    onBackClick: () -> Unit,
    viewModel: SecurityLogViewModel = hiltViewModel()
) {
    val events by viewModel.filteredEvents.collectAsState()
    val filterType by viewModel.filterType.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()
    
    var showFilterDialog by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Security Log")
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showFilterDialog = true }) {
                        Icon(Icons.Default.FilterList, contentDescription = "Filter")
                    }
                    IconButton(onClick = { viewModel.exportLog() }) {
                        Icon(Icons.Default.Share, contentDescription = "Export")
                    }
                    IconButton(onClick = { viewModel.clearLog() }) {
                        Icon(Icons.Default.Delete, contentDescription = "Clear Log")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.updateSearchQuery(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                placeholder = { Text("Search events...") },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = "Search")
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.clearSearch() }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true
            )
            
            // Stats Summary
            SecurityStatsBar(viewModel = viewModel)
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Events List
            if (events.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No security events found",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Events will appear here when apps are accessed",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    items(
                        items = events,
                        key = { it.eventId }
                    ) { event ->
                        SecurityEventCard(event = event)
                    }
                }
            }
        }
    }
    
    // Filter Dialog
    if (showFilterDialog) {
        FilterDialog(
            currentFilter = filterType,
            onFilterSelected = { type ->
                viewModel.setFilter(type)
                showFilterDialog = false
            },
            onDismiss = { showFilterDialog = false }
        )
    }
}

@Composable
fun SecurityStatsBar(viewModel: SecurityLogViewModel) {
    val stats by viewModel.stats.collectAsState()
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
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
                value = stats.totalEvents.toString(),
                label = "Total",
                color = MaterialTheme.colorScheme.primary
            )
            StatItem(
                value = stats.failedAttempts.toString(),
                label = "Failed",
                color = MaterialTheme.colorScheme.error
            )
            StatItem(
                value = stats.intruderPhotos.toString(),
                label = "Intruders",
                color = Color(0xFFFF9800)
            )
            StatItem(
                value = stats.successfulUnlocks.toString(),
                label = "Success",
                color = Color(0xFF4CAF50)
            )
        }
    }
}

@Composable
fun StatItem(
    value: String,
    label: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun SecurityEventCard(event: SecurityEvent) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = when (event.eventType) {
                EventType.UNLOCK_FAILED,
                EventType.BIOMETRIC_AUTH_FAILED,
                EventType.PIN_ATTEMPT_FAILED,
                EventType.PATTERN_ATTEMPT_FAILED -> 
                    MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                EventType.INTRUDER_PHOTO_CAPTURED -> 
                    Color(0xFFFF9800).copy(alpha = 0.2f)
                else -> 
                    MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Event Icon
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(getEventColor(event.eventType).copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = getEventIcon(event.eventType),
                    contentDescription = null,
                    tint = getEventColor(event.eventType),
                    modifier = Modifier.size(20.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Event Details
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = formatEventType(event.eventType),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )
                if (event.eventDetails != null) {
                    Text(
                        text = event.eventDetails,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = DateUtils.getRelativeTime(event.eventTimestamp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Photo indicator
            if (event.photoPath != null) {
                Icon(
                    imageVector = Icons.Default.PhotoCamera,
                    contentDescription = "Photo captured",
                    tint = Color(0xFFFF9800),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun FilterDialog(
    currentFilter: EventType?,
    onFilterSelected: (EventType?) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Filter Events") },
        text = {
            Column {
                FilterOption(
                    text = "All Events",
                    isSelected = currentFilter == null,
                    onClick = {
                        onFilterSelected(null)
                    }
                )
                
                FilterOption(
                    text = "Failed Attempts",
                    isSelected = currentFilter == EventType.UNLOCK_FAILED,
                    onClick = {
                        onFilterSelected(EventType.UNLOCK_FAILED)
                    }
                )
                
                FilterOption(
                    text = "Intruder Photos",
                    isSelected = currentFilter == EventType.INTRUDER_PHOTO_CAPTURED,
                    onClick = {
                        onFilterSelected(EventType.INTRUDER_PHOTO_CAPTURED)
                    }
                )
                
                FilterOption(
                    text = "Successful Unlocks",
                    isSelected = currentFilter == EventType.UNLOCK_SUCCESSFUL,
                    onClick = {
                        onFilterSelected(EventType.UNLOCK_SUCCESSFUL)
                    }
                )
                
                FilterOption(
                    text = "App Launches",
                    isSelected = currentFilter == EventType.APP_LAUNCH_ATTEMPT,
                    onClick = {
                        onFilterSelected(EventType.APP_LAUNCH_ATTEMPT)
                    }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
fun FilterOption(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = isSelected,
            onClick = onClick
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = text)
    }
}

private fun getEventIcon(eventType: EventType): androidx.compose.ui.graphics.vector.ImageVector {
    return when (eventType) {
        EventType.UNLOCK_SUCCESSFUL -> Icons.Default.CheckCircle
        EventType.UNLOCK_FAILED -> Icons.Default.Error
        EventType.INTRUDER_PHOTO_CAPTURED -> Icons.Default.PhotoCamera
        EventType.APP_LAUNCH_ATTEMPT -> Icons.Default.Launch
        EventType.LOCK_SCREEN_SHOWN -> Icons.Default.Lock
        EventType.BIOMETRIC_AUTH_SUCCESS -> Icons.Default.Fingerprint
        EventType.BIOMETRIC_AUTH_FAILED -> Icons.Default.Fingerprint
        EventType.PATTERN_ATTEMPT_FAILED -> Icons.Default.GridOn
        EventType.PIN_ATTEMPT_FAILED -> Icons.Default.Pin
        EventType.NOTIFICATION_MASKED -> Icons.Default.NotificationsOff
        else -> Icons.Default.Info
    }
}

private fun getEventColor(eventType: EventType): Color {
    return when (eventType) {
        EventType.UNLOCK_SUCCESSFUL,
        EventType.BIOMETRIC_AUTH_SUCCESS -> Color(0xFF4CAF50)
        EventType.UNLOCK_FAILED,
        EventType.BIOMETRIC_AUTH_FAILED,
        EventType.PATTERN_ATTEMPT_FAILED,
        EventType.PIN_ATTEMPT_FAILED -> MaterialTheme.colorScheme.error
        EventType.INTRUDER_PHOTO_CAPTURED -> Color(0xFFFF9800)
        EventType.APP_LAUNCH_ATTEMPT -> MaterialTheme.colorScheme.primary
        EventType.NOTIFICATION_MASKED -> Color(0xFF9C27B0)
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
}

private fun formatEventType(eventType: EventType): String {
    return eventType.name
        .lowercase()
        .split("_")
        .joinToString(" ") { word ->
            word.replaceFirstChar { it.uppercase() }
        }
}
