package com.ankitsaini.securevault.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ankitsaini.securevault.camera.CameraManager
import com.ankitsaini.securevault.data.model.SecurityEvent
import com.ankitsaini.securevault.data.model.EventType
import com.ankitsaini.securevault.data.repository.SecurityRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class IntruderPhotosViewModel @Inject constructor(
    private val securityRepository: SecurityRepository,
    private val cameraManager: CameraManager
) : ViewModel() {
    
    private val _intruderPhotos = MutableStateFlow<List<SecurityEvent>>(emptyList())
    val intruderPhotos: StateFlow<List<SecurityEvent>> = _intruderPhotos.asStateFlow()
    
    private val _selectedPhoto = MutableStateFlow<SecurityEvent?>(null)
    val selectedPhoto: StateFlow<SecurityEvent?> = _selectedPhoto.asStateFlow()
    
    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _totalPhotos = MutableStateFlow(0)
    val totalPhotos: StateFlow<Int> = _totalPhotos.asStateFlow()
    
    init {
        loadIntruderPhotos()
    }
    
    private fun loadIntruderPhotos() {
        viewModelScope.launch {
            _isLoading.value = true
            
            securityRepository.getSecurityEvents().collect { events ->
                val photoEvents = events.filter { 
                    it.eventType == EventType.INTRUDER_PHOTO_CAPTURED &&
                    it.photoPath != null &&
                    java.io.File(it.photoPath).exists()
                }
                
                _intruderPhotos.value = photoEvents
                _totalPhotos.value = photoEvents.size
                _isLoading.value = false
            }
        }
    }
    
    fun selectPhoto(event: SecurityEvent) {
        _selectedPhoto.value = event
    }
    
    fun clearSelection() {
        _selectedPhoto.value = null
    }
    
    fun deletePhoto(event: SecurityEvent) {
        viewModelScope.launch {
            event.photoPath?.let { path ->
                cameraManager.deletePhoto(path)
            }
            
            // Log deletion event
            securityRepository.logEvent(
                SecurityEvent(
                    packageName = event.packageName,
                    eventType = EventType.APP_PROTECTION_DISABLED,
                    eventDetails = "Intruder photo deleted",
                    wasSuccessful = true
                )
            )
            
            // Reload photos
            loadIntruderPhotos()
        }
    }
    
    fun deleteAllPhotos() {
        viewModelScope.launch {
            cameraManager.deleteAllPhotos()
            
            // Log deletion event
            securityRepository.logEvent(
                SecurityEvent(
                    packageName = "system",
                    eventType = EventType.APP_PROTECTION_DISABLED,
                    eventDetails = "All intruder photos deleted",
                    wasSuccessful = true
                )
            )
            
            // Clear list
            _intruderPhotos.value = emptyList()
            _totalPhotos.value = 0
        }
    }
    
    fun getPhotoCount(): Int {
        return _totalPhotos.value
    }
    
    fun getStorageSize(): Long {
        return cameraManager.getPhotoDirectorySize()
    }
    
    override fun onCleared() {
        super.onCleared()
        cameraManager.releaseCamera()
    }
}
