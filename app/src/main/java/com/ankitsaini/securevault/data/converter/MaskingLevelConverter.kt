package com.ankitsaini.securevault.data.converter

import androidx.room.TypeConverter
import com.ankitsaini.securevault.data.model.MaskingLevel

class MaskingLevelConverter {
    @TypeConverter
    fun fromMaskingLevel(level: MaskingLevel): String {
        return level.name
    }
    
    @TypeConverter
    fun toMaskingLevel(value: String): MaskingLevel {
        return try {
            MaskingLevel.valueOf(value)
        } catch (e: IllegalArgumentException) {
            MaskingLevel.FULL
        }
    }
}
