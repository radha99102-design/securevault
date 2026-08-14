package com.ankitsaini.securevault.data.dao

import androidx.room.*
import com.ankitsaini.securevault.data.model.SecurityEvent
import com.ankitsaini.securevault.data.model.EventType
import kotlinx.coroutines.flow.Flow

@Dao
interface SecurityEventDao {
    
    @Query("SELECT * FROM security_events ORDER BY event_timestamp DESC")
    fun getAllEvents(): Flow<List<SecurityEvent>>
    
    @Query("SELECT * FROM security_events WHERE package_name = :packageName ORDER BY event_timestamp DESC LIMIT :limit")
    fun getEventsForPackage(packageName: String, limit: Int = 50): Flow<List<SecurityEvent>>
    
    @Query("SELECT * FROM security_events WHERE event_type = :eventType ORDER BY event_timestamp DESC LIMIT :limit")
    fun getEventsByType(eventType: EventType, limit: Int = 50): Flow<List<SecurityEvent>>
    
    @Query("SELECT * FROM security_events WHERE event_timestamp >= :startTime AND event_timestamp <= :endTime ORDER BY event_timestamp DESC")
    fun getEventsInTimeRange(startTime: Long, endTime: Long): Flow<List<SecurityEvent>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: SecurityEvent): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllEvents(events: List<SecurityEvent>)
    
    @Query("DELETE FROM security_events WHERE event_timestamp < :beforeTime")
    suspend fun deleteEventsOlderThan(beforeTime: Long)
    
    @Query("DELETE FROM security_events WHERE package_name = :packageName")
    suspend fun deleteEventsForPackage(packageName: String)
    
    @Query("DELETE FROM security_events")
    suspend fun deleteAllEvents()
    
    @Query("SELECT COUNT(*) FROM security_events WHERE event_type IN (:eventTypes) AND event_timestamp >= :sinceTime")
    fun getFailedAttemptCount(eventTypes: List<EventType>, sinceTime: Long): Flow<Int>
}
