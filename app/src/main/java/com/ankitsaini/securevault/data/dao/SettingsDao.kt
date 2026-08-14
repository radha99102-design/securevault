package com.ankitsaini.securevault.data.dao

import androidx.room.*
import com.ankitsaini.securevault.data.model.AppSettings
import kotlinx.coroutines.flow.Flow

@Dao
interface SettingsDao {
    
    @Query("SELECT * FROM app_settings WHERE settings_key = :key LIMIT 1")
    suspend fun getSetting(key: String): AppSettings?
    
    @Query("SELECT * FROM app_settings")
    fun getAllSettings(): Flow<List<AppSettings>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSetting(setting: AppSettings)
    
    @Query("UPDATE app_settings SET settings_value = :value, updated_at = :timestamp WHERE settings_key = :key")
    suspend fun updateSetting(key: String, value: String, timestamp: Long = System.currentTimeMillis())
    
    @Query("DELETE FROM app_settings WHERE settings_key = :key")
    suspend fun deleteSetting(key: String)
    
    @Query("DELETE FROM app_settings")
    suspend fun deleteAllSettings()
}
