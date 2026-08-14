package com.ankitsaini.securevault.utils

import android.content.Context
import com.ankitsaini.securevault.data.model.ProtectedApp
import com.ankitsaini.securevault.data.model.SecurityEvent
import com.ankitsaini.securevault.data.repository.SecurityRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackupManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val securityRepository: SecurityRepository,
    private val fileManager: FileManager,
    private val encryptionUtils: EncryptionUtils
) {
    
    data class BackupResult(
        val success: Boolean,
        val message: String,
        val backupFile: java.io.File? = null
    )
    
    suspend fun createFullBackup(): BackupResult {
        return try {
            // Gather all data
            val protectedApps = securityRepository.getAllProtectedApps().first()
            val securityEvents = securityRepository.getSecurityEvents().first()
            
            // Create backup JSON
            val backupJson = JSONObject().apply {
                put("version", 1)
                put("timestamp", System.currentTimeMillis())
                put("app_version", context.packageManager.getPackageInfo(context.packageName, 0).versionName)
                
                // Protected apps
                put("protected_apps", JSONArray().apply {
                    protectedApps.forEach { app ->
                        put(JSONObject().apply {
                            put("package_name", app.packageName)
                            put("app_name", app.appName)
                            put("is_protected", app.isProtected)
                            put("lock_type", app.lockType.name)
                            put("use_biometric", app.useBiometric)
                            put("notification_masking", app.notificationMasking)
                            put("max_failed_attempts", app.maxFailedAttempts)
                            put("intruder_photo_enabled", app.intruderPhotoEnabled)
                            put("relock_on_screen_off", app.relockOnScreenOff)
                        })
                    }
                })
                
                // Security events (last 100)
                put("security_events", JSONArray().apply {
                    securityEvents.take(100).forEach { event ->
                        put(JSONObject().apply {
                            put("package_name", event.packageName)
                            put("event_type", event.eventType.name)
                            put("timestamp", event.eventTimestamp)
                            put("details", event.eventDetails ?: "")
                            put("was_successful", event.wasSuccessful)
                        })
                    }
                })
            }
            
            // Encrypt backup data
            val encryptedData = encryptionUtils.encryptData(backupJson.toString())
            
            if (encryptedData != null) {
                // Save backup file
                val backupFile = fileManager.createBackupFile(encryptedData)
                
                if (backupFile != null) {
                    BackupResult(
                        success = true,
                        message = "Backup created successfully",
                        backupFile = java.io.File(backupFile)
                    )
                } else {
                    BackupResult(
                        success = false,
                        message = "Failed to save backup file"
                    )
                }
            } else {
                BackupResult(
                    success = false,
                    message = "Failed to encrypt backup data"
                )
            }
        } catch (e: Exception) {
            BackupResult(
                success = false,
                message = "Backup failed: ${e.message}"
            )
        }
    }
    
    suspend fun restoreBackup(backupFile: java.io.File): BackupResult {
        return try {
            // Read encrypted backup
            val encryptedData = backupFile.readText()
            
            // Decrypt backup data
            val decryptedData = encryptionUtils.decryptData(encryptedData)
            
            if (decryptedData != null) {
                val backupJson = JSONObject(decryptedData)
                
                // Restore protected apps
                val protectedAppsArray = backupJson.getJSONArray("protected_apps")
                for (i in 0 until protectedAppsArray.length()) {
                    val appJson = protectedAppsArray.getJSONObject(i)
                    
                    val protectedApp = ProtectedApp(
                        packageName = appJson.getString("package_name"),
                        appName = appJson.getString("app_name"),
                        isProtected = appJson.getBoolean("is_protected"),
                        lockType = com.ankitsaini.securevault.data.model.LockType.valueOf(
                            appJson.getString("lock_type")
                        ),
                        useBiometric = appJson.getBoolean("use_biometric"),
                        notificationMasking = appJson.getBoolean("notification_masking"),
                        maxFailedAttempts = appJson.getInt("max_failed_attempts"),
                        intruderPhotoEnabled = appJson.getBoolean("intruder_photo_enabled"),
                        relockOnScreenOff = appJson.getBoolean("relock_on_screen_off")
                    )
                    
                    securityRepository.addProtectedApp(protectedApp)
                }
                
                BackupResult(
                    success = true,
                    message = "Backup restored successfully"
                )
            } else {
                BackupResult(
                    success = false,
                    message = "Failed to decrypt backup data"
                )
            }
        } catch (e: Exception) {
            BackupResult(
                success = false,
                message = "Restore failed: ${e.message}"
            )
        }
    }
    
    fun getAvailableBackups(): List<java.io.File> {
        return fileManager.getFilesInDirectory("backups")
            .filter { it.extension == "json" }
            .sortedByDescending { it.lastModified() }
    }
}
