package com.ankitsaini.securevault.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo

@Entity(tableName = "app_policies")
data class AppPolicy(
    @PrimaryKey
    @ColumnInfo(name = "policy_id")
    val policyId: String,
    
    @ColumnInfo(name = "policy_name")
    val policyName: String,
    
    @ColumnInfo(name = "default_lock_type")
    val defaultLockType: LockType = LockType.PIN,
    
    @ColumnInfo(name = "require_biometric_fallback")
    val requireBiometricFallback: Boolean = false,
    
    @ColumnInfo(name = "relock_timeout_seconds")
    val relockTimeoutSeconds: Int = 30,
    
    @ColumnInfo(name = "max_failed_attempts")
    val maxFailedAttempts: Int = 3,
    
    @ColumnInfo(name = "intruder_photo_enabled")
    val intruderPhotoEnabled: Boolean = true,
    
    @ColumnInfo(name = "notification_masking_level")
    val notificationMaskingLevel: MaskingLevel = MaskingLevel.FULL,
    
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
    
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)

enum class MaskingLevel {
    NONE,
    PARTIAL,
    FULL
}

data class PolicyWithApps(
    val policy: AppPolicy,
    val apps: List<ProtectedApp> = emptyList()
)
