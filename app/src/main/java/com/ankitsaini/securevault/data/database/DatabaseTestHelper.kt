package com.ankitsaini.securevault.data.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ankitsaini.securevault.data.dao.*
import com.ankitsaini.securevault.data.model.*
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class DatabaseTestHelper {
    
    private lateinit var database: SecureVaultDatabase
    private lateinit var protectedAppDao: ProtectedAppDao
    private lateinit var securityEventDao: SecurityEventDao
    private lateinit var failedAttemptDao: FailedAttemptDao
    
    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(
            context,
            SecureVaultDatabase::class.java
        ).allowMainThreadQueries().build()
        
        protectedAppDao = database.protectedAppDao()
        securityEventDao = database.securityEventDao()
        failedAttemptDao = database.failedAttemptDao()
    }
    
    @After
    @Throws(IOException::class)
    fun closeDb() {
        database.close()
    }
    
    fun testInsertAndRetrieveProtectedApp() = runBlocking {
        val app = ProtectedApp(
            packageName = "com.example.testapp",
            appName = "Test App",
            isProtected = true,
            lockType = LockType.PIN,
            pinHash = "hashed_pin_123"
        )
        
        protectedAppDao.insertProtectedApp(app)
        
        val retrievedApp = protectedAppDao.getProtectedAppByPackage("com.example.testapp")
        assert(retrievedApp != null)
        assert(retrievedApp?.appName == "Test App")
        assert(retrievedApp?.lockType == LockType.PIN)
    }
    
    fun testSecurityEventLogging() = runBlocking {
        val event = SecurityEvent(
            packageName = "com.example.testapp",
            eventType = EventType.UNLOCK_FAILED,
            eventDetails = "Test failed attempt"
        )
        
        val eventId = securityEventDao.insertEvent(event)
        assert(eventId > 0)
        
        val failedAttempt = FailedAttemptRecord(
            packageName = "com.example.testapp",
            attemptMethod = "PIN"
        )
        
        val attemptId = failedAttemptDao.insertFailedAttempt(failedAttempt)
        assert(attemptId > 0)
    }
}
