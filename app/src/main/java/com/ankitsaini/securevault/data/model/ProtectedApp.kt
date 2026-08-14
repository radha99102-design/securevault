package com.ankitsaini.securevault.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo
import androidx.room.TypeConverters
import com.ankitsaini.securevault.data.converter.LockTypeConverter
import com.ankitsaini.securevault.data.model.LockType
import java.util.Date

@Entity(tableName = "protected_apps")
@TypeConverters(LockTypeConverter::class)
data class ProtectedApp(
    @PrimaryKey
    @ColumnInfo(name = "package_name")
    val packageName: String,
    
    @ColumnInfo(name = "app_name")
    val appName: String,
    
    @ColumnInfo(name = "is_protected")
    val isProtected: Boolean = true,
    
    @ColumnInfo(name = "lock_type")
    val lockType: LockType = LockType.PIN,
    
    @ColumnInfo(name = "pin_hash")
    val pinHash: String? = null,
    
    @ColumnInfo(name = "pattern_hash")
    val patternHash: String? = null,
    
    @ColumnInfo(name = "use_biometric")
    val useBiometric: Boolean = false,
    
    @ColumnInfo(name = "lock_delay_minutes")
    val lockDelayMinutes: Int = 0,
    
    @ColumnInfo(name = "notification_masking")
    val notificationMasking: Boolean = true,
    
    @ColumnInfo(name = "max_failed_attempts")
    val maxFailedAttempts: Int = 3,
    
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
    
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis(),
    
    @ColumnInfo(name = "last_accessed_at")
    val lastAccessedAt: Long? = null,
    
    @ColumnInfo(name = "app_icon")
    val appIcon: ByteArray? = null,
    
    @ColumnInfo(name = "is_system_app")
    val isSystemApp: Boolean = false,
    
    @ColumnInfo(name = "relock_on_screen_off")
    val relockOnScreenOff: Boolean = true,
    
    @ColumnInfo(name = "intruder_photo_enabled")
    val intruderPhotoEnabled: Boolean = true
)

enum class LockType {
    PIN,
    PATTERN,
    BIOMETRIC,
    NONE
}

data class ProtectedAppWithDetails(
    val protectedApp: ProtectedApp,
    val securityEvents: List<SecurityEvent> = emptyList(),
    val failedAttemptCount: Int = 0
)
