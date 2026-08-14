package com.ankitsaini.securevault.data.converter

import androidx.room.TypeConverter
import com.ankitsaini.securevault.data.model.EventType

class EventTypeConverter {
    @TypeConverter
    fun fromEventType(eventType: EventType): String {
        return eventType.name
    }
    
    @TypeConverter
    fun toEventType(value: String): EventType {
        return try {
            EventType.valueOf(value)
        } catch (e: IllegalArgumentException) {
            EventType.APP_LAUNCH_ATTEMPT
        }
    }
}
