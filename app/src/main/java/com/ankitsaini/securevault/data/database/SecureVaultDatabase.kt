package com.ankitsaini.securevault.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.ankitsaini.securevault.data.converter.EventTypeConverter
import com.ankitsaini.securevault.data.converter.LockTypeConverter
import com.ankitsaini.securevault.data.converter.MaskingLevelConverter
import com.ankitsaini.securevault.data.dao.*
import com.ankitsaini.securevault.data.model.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        ProtectedApp::class,
        SecurityEvent::class,
        FailedAttemptRecord::class,
        AppPolicy::class,
        AppSettings::class
    ],
    version = 1,
    exportSchema = false // Changed to false for simplicity
)
@TypeConverters(
    LockTypeConverter::class,
    EventTypeConverter::class,
    MaskingLevelConverter::class
)
abstract class SecureVaultDatabase : RoomDatabase() {
    
    abstract fun protectedAppDao(): ProtectedAppDao
    abstract fun securityEventDao(): SecurityEventDao
    abstract fun failedAttemptDao(): FailedAttemptDao
    abstract fun appPolicyDao(): AppPolicyDao
    abstract fun settingsDao(): SettingsDao
    
    companion object {
        @Volatile
        private var INSTANCE: SecureVaultDatabase? = null
        
        fun getDatabase(context: Context, scope: CoroutineScope): SecureVaultDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SecureVaultDatabase::class.java,
                    "secure_vault_database"
                )
                .addCallback(DatabaseCallback(scope))
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
        
        private class DatabaseCallback(
            private val scope: CoroutineScope
        ) : Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        seedDefaultPolicies(database)
                    }
                }
            }
            
            private suspend fun seedDefaultPolicies(database: SecureVaultDatabase) {
                val defaultPolicy = AppPolicy(
                    policyId = "default_policy",
                    policyName = "Default Security Policy",
                    defaultLockType = LockType.PIN,
                    requireBiometricFallback = true,
                    relockTimeoutSeconds = 30,
                    maxFailedAttempts = 3,
                    intruderPhotoEnabled = true,
                    notificationMaskingLevel = MaskingLevel.FULL
                )
                database.appPolicyDao().insertPolicy(defaultPolicy)
                
                val strictPolicy = AppPolicy(
                    policyId = "strict_policy",
                    policyName = "Strict Security Policy",
                    defaultLockType = LockType.BIOMETRIC,
                    requireBiometricFallback = true,
                    relockTimeoutSeconds = 15,
                    maxFailedAttempts = 2,
                    intruderPhotoEnabled = true,
                    notificationMaskingLevel = MaskingLevel.FULL
                )
                database.appPolicyDao().insertPolicy(strictPolicy)
                
                // Seed default settings
                database.settingsDao().insertSetting(
                    AppSettings(
                        settingsKey = "global_security_enabled",
                        settingsValue = "true",
                        settingsType = "boolean"
                    )
                )
                
                database.settingsDao().insertSetting(
                    AppSettings(
                        settingsKey = "intruder_photo_enabled",
                        settingsValue = "true",
                        settingsType = "boolean"
                    )
                )
                
                database.settingsDao().insertSetting(
                    AppSettings(
                        settingsKey = "max_failed_attempts",
                        settingsValue = "3",
                        settingsType = "int"
                    )
                )
                
                database.settingsDao().insertSetting(
                    AppSettings(
                        settingsKey = "relock_timeout_seconds",
                        settingsValue = "30",
                        settingsType = "int"
                    )
                )
            }
        }
    }
}
