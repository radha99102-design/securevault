package com.ankitsaini.securevault.data.converter

import androidx.room.TypeConverter
import com.ankitsaini.securevault.data.model.LockType

class LockTypeConverter {
    @TypeConverter
    fun fromLockType(lockType: LockType): String {
        return lockType.name
    }
    
    @TypeConverter
    fun toLockType(value: String): LockType {
        return try {
            LockType.valueOf(value)
        } catch (e: IllegalArgumentException) {
            LockType.PIN
        }
    }
}
