package com.ankitsaini.securevault.di

import android.content.Context
import com.ankitsaini.securevault.data.database.SecureVaultDatabase
import com.ankitsaini.securevault.data.dao.*
import com.ankitsaini.securevault.data.repository.AppInfoRepository
import com.ankitsaini.securevault.data.repository.SecurityRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    
    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context
    ): SecureVaultDatabase {
        return SecureVaultDatabase.getDatabase(
            context,
            CoroutineScope(SupervisorJob())
        )
    }
    
    @Provides
    fun provideProtectedAppDao(database: SecureVaultDatabase): ProtectedAppDao {
        return database.protectedAppDao()
    }
    
    @Provides
    fun provideSecurityEventDao(database: SecureVaultDatabase): SecurityEventDao {
        return database.securityEventDao()
    }
    
    @Provides
    fun provideFailedAttemptDao(database: SecureVaultDatabase): FailedAttemptDao {
        return database.failedAttemptDao()
    }
    
    @Provides
    fun provideAppPolicyDao(database: SecureVaultDatabase): AppPolicyDao {
        return database.appPolicyDao()
    }
    
    @Provides
    fun provideSettingsDao(database: SecureVaultDatabase): SettingsDao {
        return database.settingsDao()
    }
    
    @Provides
    @Singleton
    fun provideSecurityRepository(
        protectedAppDao: ProtectedAppDao,
        securityEventDao: SecurityEventDao,
        failedAttemptDao: FailedAttemptDao,
        appPolicyDao: AppPolicyDao,
        settingsDao: SettingsDao
    ): SecurityRepository {
        return SecurityRepository(
            protectedAppDao,
            securityEventDao,
            failedAttemptDao,
            appPolicyDao,
            settingsDao
        )
    }
    
    @Provides
    @Singleton
    fun provideAppInfoRepository(
        @ApplicationContext context: Context,
        securityRepository: SecurityRepository
    ): AppInfoRepository {
        return AppInfoRepository(context, securityRepository)
    }
}
