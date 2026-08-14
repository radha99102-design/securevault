package com.ankitsaini.securevault.data.dao

import androidx.room.*
import com.ankitsaini.securevault.data.model.FailedAttemptRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface FailedAttemptDao {
    
    @Query("SELECT * FROM failed_attempts ORDER BY attempt_timestamp DESC")
    fun getAllFailedAttempts(): Flow<List<FailedAttemptRecord>>
    
    @Query("SELECT * FROM failed_attempts WHERE package_name = :packageName ORDER BY attempt_timestamp DESC LIMIT :limit")
    fun getFailedAttemptsForPackage(packageName: String, limit: Int = 50): Flow<List<FailedAttemptRecord>>
    
    @Query("SELECT * FROM failed_attempts WHERE attempt_timestamp >= :sinceTime ORDER BY attempt_timestamp DESC")
    fun getRecentFailedAttempts(sinceTime: Long): Flow<List<FailedAttemptRecord>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFailedAttempt(record: FailedAttemptRecord): Long
    
    @Query("SELECT COUNT(*) FROM failed_attempts WHERE package_name = :packageName AND attempt_timestamp >= :sinceTime")
    fun getFailedAttemptCountForPackage(packageName: String, sinceTime: Long): Flow<Int>
    
    @Query("DELETE FROM failed_attempts WHERE attempt_timestamp < :beforeTime")
    suspend fun deleteAttemptsOlderThan(beforeTime: Long)
    
    @Query("DELETE FROM failed_attempts WHERE package_name = :packageName")
    suspend fun deleteAttemptsForPackage(packageName: String)
    
    @Query("DELETE FROM failed_attempts")
    suspend fun deleteAllAttempts()
}
