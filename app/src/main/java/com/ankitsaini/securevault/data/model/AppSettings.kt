package com.ankitsaini.securevault.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo

@Entity(tableName = "app_settings")
data class AppSettings(
    @PrimaryKey
    @ColumnInfo(name = "settings_key")
    val settingsKey: String,
    
    @ColumnInfo(name = "settings_value")
    val settingsValue: String,
    
    @ColumnInfo(name = "settings_type")
    val settingsType: String,
    
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)

data class GlobalSettings(
    val masterPinHash: String? = null,
    val masterPatternHash: String? = null,
    val biometricEnabled: Boolean = false,
    val autoStartOnBoot: Boolean = true,
    val showNotifications: Boolean = true,
    val intruderPhotoEnabled: Boolean = true,
    val maxFailedAttempts: Int = 3,
    val relockTimeoutSeconds: Int = 30,
    val stealthMode: Boolean = false,
    val fakeCrashOnFailedAttempt: Boolean = false
)
