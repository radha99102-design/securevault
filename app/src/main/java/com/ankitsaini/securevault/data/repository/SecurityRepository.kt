package com.ankitsaini.securevault.data.repository

import com.ankitsaini.securevault.data.dao.*
import com.ankitsaini.securevault.data.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SecurityRepository @Inject constructor(
    private val protectedAppDao: ProtectedAppDao,
    private val securityEventDao: SecurityEventDao,
    private val failedAttemptDao: FailedAttemptDao,
    private val appPolicyDao: AppPolicyDao,
    private val settingsDao: SettingsDao
) {
    
    // Protected Apps
    fun getAllProtectedApps(): Flow<List<ProtectedApp>> = 
        protectedAppDao.getAllProtectedApps()
    
    fun getActiveProtectedApps(): Flow<List<ProtectedApp>> = 
        protectedAppDao.getActiveProtectedApps()
    
    suspend fun getProtectedApp(packageName: String): ProtectedApp? = 
        protectedAppDao.getProtectedAppByPackage(packageName)
    
    fun observeProtectedApp(packageName: String): Flow<ProtectedApp?> = 
        protectedAppDao.observeProtectedAppByPackage(packageName)
    
    suspend fun addProtectedApp(app: ProtectedApp) = 
        protectedAppDao.insertProtectedApp(app)
    
    suspend fun updateProtectedApp(app: ProtectedApp) = 
        protectedAppDao.updateProtectedApp(app)
    
    suspend fun updateLockType(packageName: String, lockType: LockType) = 
        protectedAppDao.updateLockType(packageName, lockType)
    
    suspend fun toggleProtection(packageName: String, isProtected: Boolean) = 
        protectedAppDao.updateProtectionStatus(packageName, isProtected)
    
    suspend fun removeProtectedApp(packageName: String) = 
        protectedAppDao.deleteByPackageName(packageName)
    
    // Security Events
    fun getSecurityEvents(): Flow<List<SecurityEvent>> = 
        securityEventDao.getAllEvents()
    
    fun getEventsForPackage(packageName: String): Flow<List<SecurityEvent>> = 
        securityEventDao.getEventsForPackage(packageName)
    
    suspend fun logEvent(event: SecurityEvent): Long = 
        securityEventDao.insertEvent(event)
    
    suspend fun logFailedUnlock(packageName: String, method: String): Long {
        val eventId = securityEventDao.insertEvent(
            SecurityEvent(
                packageName = packageName,
                eventType = EventType.UNLOCK_FAILED,
                eventDetails = "Failed $method attempt",
                wasSuccessful = false
            )
        )
        
        failedAttemptDao.insertFailedAttempt(
            FailedAttemptRecord(
                packageName = packageName,
                attemptMethod = method
            )
        )
        
        return eventId
    }
    
    suspend fun logSuccessfulUnlock(packageName: String, method: String) {
        securityEventDao.insertEvent(
            SecurityEvent(
                packageName = packageName,
                eventType = EventType.UNLOCK_SUCCESSFUL,
                eventDetails = "Successful $method unlock",
                wasSuccessful = true
            )
        )
        protectedAppDao.updateLastAccessed(packageName)
    }
    
    // Failed Attempts
    fun getFailedAttempts(): Flow<List<FailedAttemptRecord>> = 
        failedAttemptDao.getAllFailedAttempts()
    
    fun getFailedAttemptsForPackage(packageName: String): Flow<List<FailedAttemptRecord>> = 
        failedAttemptDao.getFailedAttemptsForPackage(packageName)
    
    fun getFailedAttemptCount(packageName: String, sinceTime: Long): Flow<Int> = 
        failedAttemptDao.getFailedAttemptCountForPackage(packageName, sinceTime)
    
    // Policies
    fun getAllPolicies(): Flow<List<AppPolicy>> = 
        appPolicyDao.getAllPolicies()
    
    suspend fun getPolicy(policyId: String): AppPolicy? = 
        appPolicyDao.getPolicyById(policyId)
    
    suspend fun savePolicy(policy: AppPolicy) = 
        appPolicyDao.insertPolicy(policy)
    
    // Settings
    suspend fun getSetting(key: String): AppSettings? = 
        settingsDao.getSetting(key)
    
    fun getAllSettings(): Flow<List<AppSettings>> = 
        settingsDao.getAllSettings()
    
    suspend fun updateSetting(key: String, value: String) = 
        settingsDao.updateSetting(key, value)
    
    // Combined Data
    fun getDashboardData(): Flow<DashboardData> {
        return combine(
            protectedAppDao.getAllProtectedApps(),
            securityEventDao.getAllEvents(),
            failedAttemptDao.getAllFailedAttempts()
        ) { apps, events, attempts ->
            DashboardData(
                protectedApps = apps,
                recentEvents = events.take(10),
                failedAttempts = attempts,
                totalProtectedApps = apps.count { it.isProtected },
                totalFailedAttempts = attempts.size,
                totalSecurityEvents = events.size
            )
        }
    }
}

data class DashboardData(
    val protectedApps: List<ProtectedApp>,
    val recentEvents: List<SecurityEvent>,
    val failedAttempts: List<FailedAttemptRecord>,
    val totalProtectedApps: Int,
    val totalFailedAttempts: Int,
    val totalSecurityEvents: Int
)
