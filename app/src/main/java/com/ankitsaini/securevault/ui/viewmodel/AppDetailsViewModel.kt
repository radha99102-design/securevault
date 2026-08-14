package com.ankitsaini.securevault.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ankitsaini.securevault.data.model.LockType
import com.ankitsaini.securevault.data.model.ProtectedApp
import com.ankitsaini.securevault.data.model.SecurityEvent
import com.ankitsaini.securevault.data.repository.AppInfoRepository
import com.ankitsaini.securevault.data.repository.SecurityRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AppDetailsViewModel @Inject constructor(
    private val securityRepository: SecurityRepository,
    private val appInfoRepository: AppInfoRepository
) : ViewModel() {
    
    private val _appDetails = MutableStateFlow<ProtectedApp?>(null)
    val appDetails: StateFlow<ProtectedApp?> = _appDetails.asStateFlow()
    
    private val _securityEvents = MutableStateFlow<List<SecurityEvent>>(emptyList())
    val securityEvents: StateFlow<List<SecurityEvent>> = _securityEvents.asStateFlow()
    
    private val _isProtected = MutableStateFlow(false)
    val isProtected: StateFlow<Boolean> = _isProtected.asStateFlow()
    
    private var currentPackageName: String = ""
    
    fun loadAppDetails(packageName: String) {
        currentPackageName = packageName
        _isProtected.value = false
        
        viewModelScope.launch {
            // Load app details
            val app = securityRepository.getProtectedApp(packageName)
            _appDetails.value = app
            _isProtected.value = app?.isProtected ?: false
            
            // Load security events
            securityRepository.getEventsForPackage(packageName).collect { events ->
                _securityEvents.value = events
            }
        }
    }
    
    fun toggleProtection() {
        viewModelScope.launch {
            val currentApp = _appDetails.value
            
            if (currentApp?.isProtected == true) {
                // Remove protection
                securityRepository.removeProtectedApp(currentPackageName)
                _isProtected.value = false
                _appDetails.value = null
            } else {
                // Add protection
                val appInfo = appInfoRepository.getAppInfo(currentPackageName)
                if (appInfo != null) {
                    val protectedApp = ProtectedApp(
                        packageName = currentPackageName,
                        appName = appInfo.appName,
                        isProtected = true,
                        lockType = LockType.PIN,
                        notificationMasking = true,
                        maxFailedAttempts = 3,
                        intruderPhotoEnabled = true,
                        relockOnScreenOff = true
                    )
                    securityRepository.addProtectedApp(protectedApp)
                    _isProtected.value = true
                    _appDetails.value = protectedApp
                }
            }
        }
    }
    
    fun updateLockType(lockType: LockType) {
        viewModelScope.launch {
            securityRepository.updateLockType(currentPackageName, lockType)
            _appDetails.value = _appDetails.value?.copy(lockType = lockType)
        }
    }
    
    fun toggleNotificationMasking() {
        viewModelScope.launch {
            val currentApp = _appDetails.value ?: return@launch
            val updatedApp = currentApp.copy(
                notificationMasking = !currentApp.notificationMasking
            )
            securityRepository.updateProtectedApp(updatedApp)
            _appDetails.value = updatedApp
        }
    }
    
    fun toggleIntruderPhoto() {
        viewModelScope.launch {
            val currentApp = _appDetails.value ?: return@launch
            val updatedApp = currentApp.copy(
                intruderPhotoEnabled = !currentApp.intruderPhotoEnabled
            )
            securityRepository.updateProtectedApp(updatedApp)
            _appDetails.value = updatedApp
        }
    }
    
    fun toggleRelockOnScreenOff() {
        viewModelScope.launch {
            val currentApp = _appDetails.value ?: return@launch
            val updatedApp = currentApp.copy(
                relockOnScreenOff = !currentApp.relockOnScreenOff
            )
            securityRepository.updateProtectedApp(updatedApp)
            _appDetails.value = updatedApp
        }
    }
}
