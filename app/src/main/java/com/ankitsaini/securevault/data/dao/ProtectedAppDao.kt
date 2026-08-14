package com.ankitsaini.securevault.data.dao

import androidx.room.*
import com.ankitsaini.securevault.data.model.ProtectedApp
import com.ankitsaini.securevault.data.model.LockType
import kotlinx.coroutines.flow.Flow

@Dao
interface ProtectedAppDao {
    
    @Query("SELECT * FROM protected_apps ORDER BY app_name ASC")
    fun getAllProtectedApps(): Flow<List<ProtectedApp>>
    
    @Query("SELECT * FROM protected_apps WHERE package_name = :packageName LIMIT 1")
    suspend fun getProtectedAppByPackage(packageName: String): ProtectedApp?
    
    @Query("SELECT * FROM protected_apps WHERE package_name = :packageName LIMIT 1")
    fun observeProtectedAppByPackage(packageName: String): Flow<ProtectedApp?>
    
    @Query("SELECT * FROM protected_apps WHERE is_protected = 1 ORDER BY app_name ASC")
    fun getActiveProtectedApps(): Flow<List<ProtectedApp>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProtectedApp(protectedApp: ProtectedApp)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllProtectedApps(protectedApps: List<ProtectedApp>)
    
    @Update
    suspend fun updateProtectedApp(protectedApp: ProtectedApp)
    
    @Query("UPDATE protected_apps SET is_protected = :isProtected, updated_at = :timestamp WHERE package_name = :packageName")
    suspend fun updateProtectionStatus(packageName: String, isProtected: Boolean, timestamp: Long = System.currentTimeMillis())
    
    @Query("UPDATE protected_apps SET lock_type = :lockType, updated_at = :timestamp WHERE package_name = :packageName")
    suspend fun updateLockType(packageName: String, lockType: LockType, timestamp: Long = System.currentTimeMillis())
    
    @Query("UPDATE protected_apps SET pin_hash = :pinHash, updated_at = :timestamp WHERE package_name = :packageName")
    suspend fun updatePinHash(packageName: String, pinHash: String, timestamp: Long = System.currentTimeMillis())
    
    @Query("UPDATE protected_apps SET pattern_hash = :patternHash, updated_at = :timestamp WHERE package_name = :packageName")
    suspend fun updatePatternHash(packageName: String, patternHash: String, timestamp: Long = System.currentTimeMillis())
    
    @Query("UPDATE protected_apps SET use_biometric = :useBiometric, updated_at = :timestamp WHERE package_name = :packageName")
    suspend fun updateBiometricStatus(packageName: String, useBiometric: Boolean, timestamp: Long = System.currentTimeMillis())
    
    @Query("UPDATE protected_apps SET last_accessed_at = :timestamp WHERE package_name = :packageName")
    suspend fun updateLastAccessed(packageName: String, timestamp: Long = System.currentTimeMillis())
    
    @Delete
    suspend fun deleteProtectedApp(protectedApp: ProtectedApp)
    
    @Query("DELETE FROM protected_apps WHERE package_name = :packageName")
    suspend fun deleteByPackageName(packageName: String)
    
    @Query("DELETE FROM protected_apps")
    suspend fun deleteAllProtectedApps()
    
    @Query("SELECT COUNT(*) FROM protected_apps WHERE is_protected = 1")
    fun getProtectedAppCount(): Flow<Int>
}
