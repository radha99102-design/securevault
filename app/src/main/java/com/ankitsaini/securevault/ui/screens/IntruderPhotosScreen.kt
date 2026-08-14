package com.ankitsaini.securevault.ui.screens

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ankitsaini.securevault.data.model.SecurityEvent
import com.ankitsaini.securevault.ui.viewmodel.IntruderPhotosViewModel
import com.ankitsaini.securevault.utils.DateUtils
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IntruderPhotosScreen(
    onBackClick: () -> Unit,
    viewModel: IntruderPhotosViewModel = hiltViewModel()
) {
    val photos by viewModel.intruderPhotos.collectAsState()
    val selectedPhoto by viewModel.selectedPhoto.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    
    var showDeleteDialog by remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Intruder Photos")
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (photos.isNotEmpty()) {
                        IconButton(onClick = { viewModel.deleteAllPhotos() }) {
                            Icon(
                                Icons.Default.DeleteSweep,
                                contentDescription = "Delete All"
                            )
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                
                photos.isEmpty() -> {
                    EmptyPhotosView(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                
                else -> {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        contentPadding = PaddingValues(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(
                            items = photos,
                            key = { it.photoPath ?: it.eventId.toString() }
                        ) { event ->
                            IntruderPhotoCard(
                                event = event,
                                onClick = {
                                    viewModel.selectPhoto(event)
                                    showDeleteDialog = true
                                }
                            )
                        }
                    }
                }
            }
        }
    }
    
    // Photo Detail Dialog
    if (showDeleteDialog && selectedPhoto != null) {
        PhotoDetailDialog(
            event = selectedPhoto!!,
            onDismiss = {
                showDeleteDialog = false
                viewModel.clearSelection()
            },
            onDelete = {
                viewModel.deletePhoto(selectedPhoto!!)
                showDeleteDialog = false
                viewModel.clearSelection()
            }
        )
    }
}

@Composable
fun IntruderPhotoCard(
    event: SecurityEvent,
    onClick: () -> Unit
) {
    val photoPath = event.photoPath
    val bitmap = remember(photoPath) {
        photoPath?.let { path ->
            try {
                BitmapFactory.decodeFile(path)?.asImageBitmap()
            } catch (e: Exception) {
                null
            }
        }
    }
    
    Box(
        modifier = Modifier
            .aspectRatio(0.75f)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick)
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = "Intruder photo",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.BrokenImage,
                    contentDescription = "Photo unavailable",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        // Timestamp overlay
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
                )
                .padding(4.dp)
                .align(Alignment.BottomCenter)
        ) {
            Text(
                text = DateUtils.formatTime(event.eventTimestamp),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun EmptyPhotosView(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.PhotoCamera,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "No intruder photos captured",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Photos will appear here when someone fails to unlock a protected app",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun PhotoDetailDialog(
    event: SecurityEvent,
    onDismiss: () -> Unit,
    onDelete: () -> Unit
) {
    val photoPath = event.photoPath
    val bitmap = remember(photoPath) {
        photoPath?.let { path ->
            try {
                BitmapFactory.decodeFile(path)?.asImageBitmap()
            } catch (e: Exception) {
                null
            }
        }
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Intruder Photo",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap,
                        contentDescription = "Intruder photo",
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(0.75f)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Fit
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "App: ${event.packageName}",
                    style = MaterialTheme.typography.bodyMedium
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = "Time: ${DateUtils.formatDateTime(event.eventTimestamp)}",
                    style = MaterialTheme.typography.bodyMedium
                )
                
                if (event.eventDetails != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Details: ${event.eventDetails}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDelete) {
                Text(
                    text = "Delete",
                    color = MaterialTheme.colorScheme.error
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}
