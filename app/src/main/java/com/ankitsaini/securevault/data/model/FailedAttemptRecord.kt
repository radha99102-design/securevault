package com.ankitsaini.securevault.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo

@Entity(tableName = "failed_attempts")
data class FailedAttemptRecord(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "attempt_id")
    val attemptId: Long = 0,
    
    @ColumnInfo(name = "package_name")
    val packageName: String,
    
    @ColumnInfo(name = "attempt_timestamp")
    val attemptTimestamp: Long = System.currentTimeMillis(),
    
    @ColumnInfo(name = "attempt_method")
    val attemptMethod: String,
    
    @ColumnInfo(name = "photo_path")
    val photoPath: String? = null,
    
    @ColumnInfo(name = "device_battery_level")
    val deviceBatteryLevel: Int? = null,
    
    @ColumnInfo(name = "device_location")
    val deviceLocation: String? = null
)
