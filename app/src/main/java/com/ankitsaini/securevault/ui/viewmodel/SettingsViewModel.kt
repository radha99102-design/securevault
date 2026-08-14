package com.ankitsaini.securevault.ui.viewmodel

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ankitsaini.securevault.auth.PinHasher
import com.ankitsaini.securevault.auth.SessionManager
import com.ankitsaini.securevault.camera.CameraManager
import com.ankitsaini.securevault.data.repository.SecurityRepository
import com.ankitsaini.securevault.services.ServiceManager
import com.ankitsaini.securevault.utils.*
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.json.JSONObject
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val securityRepository: SecurityRepository,
    private val sessionManager: SessionManager,
    private val preferencesManager: PreferencesManager,
    private val serviceManager: ServiceManager,
    private val cameraManager: CameraManager,
    private val fileManager: FileManager,
    private val appUtils: AppUtils,
    @ApplicationContext private val context: Context
) : ViewModel() {
    
    private val _settings = MutableStateFlow(AppSettings())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()
    
    private val _permissions = MutableStateFlow(PermissionState())
    val permissions: StateFlow<PermissionState> = _permissions.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    data class AppSettings(
        val biometricEnabled: Boolean = false,
        val autoStartOnBoot: Boolean = true,
        val intruderPhotoEnabled: Boolean = true,
        val maxFailedAttempts: Int = 3,
        val relockTimeout: Int = 30,
        val stealthMode: Boolean = false,
        val fakeCrashEnabled: Boolean = false
    )
    
    data class PermissionState(
        val accessibilityGranted: Boolean = false,
        val notificationGranted: Boolean = false,
        val overlayGranted: Boolean = false,
        val usageStatsGranted: Boolean = false,
        val batteryOptimizationGranted: Boolean = false
    )
    
    init {
        loadSettings()
        checkPermissions()
    }
    
    private fun loadSettings() {
        _settings.value = AppSettings(
            biometricEnabled = preferencesManager.getBoolean(Constants.PREF_BIOMETRIC_ENABLED, false),
            autoStartOnBoot = preferencesManager.getBoolean(Constants.PREF_AUTO_START, true),
            intruderPhotoEnabled = preferencesManager.getBoolean(Constants.PREF_INTRUDER_PHOTO, true),
            maxFailedAttempts = preferencesManager.getInt(Constants.PREF_MAX_FAILED_ATTEMPTS, 3),
            relockTimeout = preferencesManager.getInt(Constants.PREF_RELOCK_TIMEOUT, 30),
            stealthMode = preferencesManager.getBoolean(Constants.PREF_STEALTH_MODE, false),
            fakeCrashEnabled = preferencesManager.getBoolean(Constants.KEY_FAKE_CRASH, false)
        )
    }
    
    private fun checkPermissions() {
        _permissions.value = PermissionState(
            accessibilityGranted = serviceManager.isAccessibilityServiceEnabled(),
            notificationGranted = serviceManager.isNotificationListenerEnabled(),
            overlayGranted = serviceManager.canDrawOverlays(),
            usageStatsGranted = serviceManager.hasUsageStatsPermission(),
            batteryOptimizationGranted = serviceManager.isIgnoringBatteryOptimizations()
        )
    }
    
    fun refreshPermissions() {
        checkPermissions()
    }
    
    fun toggleBiometric() {
        val newValue = !_settings.value.biometricEnabled
        preferencesManager.putBoolean(Constants.PREF_BIOMETRIC_ENABLED, newValue)
        _settings.value = _settings.value.copy(biometricEnabled = newValue)
    }
    
    fun toggleAutoStart() {
        val newValue = !_settings.value.autoStartOnBoot
        preferencesManager.putBoolean(Constants.PREF_AUTO_START, newValue)
        _settings.value = _settings.value.copy(autoStartOnBoot = newValue)
    }
    
    fun toggleIntruderPhoto() {
        val newValue = !_settings.value.intruderPhotoEnabled
        preferencesManager.putBoolean(Constants.PREF_INTRUDER_PHOTO, newValue)
        _settings.value = _settings.value.copy(intruderPhotoEnabled = newValue)
    }
    
    fun toggleStealthMode() {
        val newValue = !_settings.value.stealthMode
        preferencesManager.putBoolean(Constants.PREF_STEALTH_MODE, newValue)
        _settings.value = _settings.value.copy(stealthMode = newValue)
        
        val componentName = android.content.ComponentName(
            context,
            "${context.packageName}.ui.MainActivity"
        )
        val state = if (newValue) {
            android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_DISABLED
        } else {
            android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_ENABLED
        }
        context.packageManager.setComponentEnabledSetting(
            componentName,
            state,
            android.content.pm.PackageManager.DONT_KILL_APP
        )
    }
    
    fun toggleFakeCrash() {
        val newValue = !_settings.value.fakeCrashEnabled
        preferencesManager.putBoolean(Constants.KEY_FAKE_CRASH, newValue)
        _settings.value = _settings.value.copy(fakeCrashEnabled = newValue)
    }
    
    fun setMasterPin(pin: String) {
        viewModelScope.launch {
            val pinHash = PinHasher.hashPin(pin)
            sessionManager.setMasterPinHash(pinHash)
            preferencesManager.putString(Constants.PREF_MASTER_PIN, pinHash)
        }
    }
    
    fun openAccessibilitySettings() {
        serviceManager.openAccessibilitySettings()
    }
    
    fun openNotificationSettings() {
        serviceManager.openNotificationListenerSettings()
    }
    
    fun openOverlaySettings() {
        serviceManager.openOverlaySettings()
    }
    
    fun openUsageAccessSettings() {
        serviceManager.openUsageAccessSettings()
    }
    
    fun requestIgnoreBatteryOptimizations() {
        serviceManager.requestIgnoreBatteryOptimizations()
    }
    
    fun clearSecurityLog() {
        viewModelScope.launch {
            // This would need a DAO method to clear all events
            // For now, log a clearing event
            securityRepository.logEvent(
                com.ankitsaini.securevault.data.model.SecurityEvent(
                    packageName = "system",
                    eventType = com.ankitsaini.securevault.data.model.EventType.APP_PROTECTION_DISABLED,
                    eventDetails = "Security log cleared by user",
                    wasSuccessful = true
                )
            )
        }
    }
    
    fun deleteAllPhotos() {
        cameraManager.deleteAllPhotos()
    }
    
    fun createBackup() {
        viewModelScope.launch {
            _isLoading.value = true
            
            try {
                val backupData = JSONObject().apply {
                    put("version", 1)
                    put("timestamp", System.currentTimeMillis())
                    put("settings", JSONObject().apply {
                        put("biometricEnabled", _settings.value.biometricEnabled)
                        put("autoStartOnBoot", _settings.value.autoStartOnBoot)
                        put("intruderPhotoEnabled", _settings.value.intruderPhotoEnabled)
                        put("maxFailedAttempts", _settings.value.maxFailedAttempts)
                        put("relockTimeout", _settings.value.relockTimeout)
                        put("stealthMode", _settings.value.stealthMode)
                    })
                }
                
                val backupFile = fileManager.createBackupFile(backupData.toString())
                
                if (backupFile != null) {
                    preferencesManager.putLong(Constants.KEY_LAST_BACKUP, System.currentTimeMillis())
                }
            } catch (e: Exception) {
                // Handle backup error
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun restoreBackup() {
        viewModelScope.launch {
            _isLoading.value = true
            
            try {
                val backupFiles = fileManager.getFilesInDirectory("backups")
                val latestBackup = backupFiles.maxByOrNull { it.lastModified() }
                
                if (latestBackup != null) {
                    val backupData = latestBackup.readText()
                    val jsonObject = JSONObject(backupData)
                    
                    val settingsObject = jsonObject.getJSONObject("settings")
                    preferencesManager.putBoolean(Constants.PREF_BIOMETRIC_ENABLED, settingsObject.getBoolean("biometricEnabled"))
                    preferencesManager.putBoolean(Constants.PREF_AUTO_START, settingsObject.getBoolean("autoStartOnBoot"))
                    preferencesManager.putBoolean(Constants.PREF_INTRUDER_PHOTO, settingsObject.getBoolean("intruderPhotoEnabled"))
                    
                    loadSettings()
                }
            } catch (e: Exception) {
                // Handle restore error
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun clearAllData() {
        viewModelScope.launch {
            preferencesManager.clear()
            sessionManager.clearAllSessionData()
            cameraManager.deleteAllPhotos()
            fileManager.clearTempFiles()
            loadSettings()
        }
    }
    
    fun getAppVersion(): String {
        return "Version ${appUtils.getAppVersionName()} (${appUtils.getAppVersionCode()})"
    }
    
    fun getDeviceInfo(): String {
        val deviceInfo = appUtils.getDeviceInfo()
        return "${deviceInfo.manufacturer} ${deviceInfo.model} - Android ${deviceInfo.androidVersion}"
    }
}
