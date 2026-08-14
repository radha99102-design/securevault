package com.ankitsaini.securevault.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo
import androidx.room.TypeConverters
import com.ankitsaini.securevault.data.converter.EventTypeConverter

@Entity(tableName = "security_events")
@TypeConverters(EventTypeConverter::class)
data class SecurityEvent(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "event_id")
    val eventId: Long = 0,
    
    @ColumnInfo(name = "package_name")
    val packageName: String,
    
    @ColumnInfo(name = "event_type")
    val eventType: EventType,
    
    @ColumnInfo(name = "event_timestamp")
    val eventTimestamp: Long = System.currentTimeMillis(),
    
    @ColumnInfo(name = "event_details")
    val eventDetails: String? = null,
    
    @ColumnInfo(name = "photo_path")
    val photoPath: String? = null,
    
    @ColumnInfo(name = "was_successful")
    val wasSuccessful: Boolean = false
)

enum class EventType {
    APP_LAUNCH_ATTEMPT,
    LOCK_SCREEN_SHOWN,
    UNLOCK_SUCCESSFUL,
    UNLOCK_FAILED,
    INTRUDER_PHOTO_CAPTURED,
    LOCK_TYPE_CHANGED,
    APP_PROTECTION_ENABLED,
    APP_PROTECTION_DISABLED,
    NOTIFICATION_MASKED,
    BIOMETRIC_AUTH_SUCCESS,
    BIOMETRIC_AUTH_FAILED,
    PATTERN_ATTEMPT_FAILED,
    PIN_ATTEMPT_FAILED
}
