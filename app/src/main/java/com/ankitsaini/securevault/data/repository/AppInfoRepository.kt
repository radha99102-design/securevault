package com.ankitsaini.securevault.data.repository

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import com.ankitsaini.securevault.data.model.ProtectedApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppInfoRepository @Inject constructor(
    private val context: Context,
    private val securityRepository: SecurityRepository
) {
    
    data class InstalledAppInfo(
        val packageName: String,
        val appName: String,
        val icon: Drawable?,
        val isSystemApp: Boolean,
        val isProtected: Boolean = false
    )
    
    suspend fun getInstalledApps(): List<InstalledAppInfo> = withContext(Dispatchers.IO) {
        val packageManager = context.packageManager
        val installedApps = mutableListOf<InstalledAppInfo>()
        
        try {
            val packages = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
            val protectedApps = securityRepository.getAllProtectedApps()
            
            // Collect all protected app package names for quick lookup
            val protectedPackages = mutableSetOf<String>()
            protectedApps.collect { apps ->
                apps.forEach { app ->
                    if (app.isProtected) {
                        protectedPackages.add(app.packageName)
                    }
                }
            }
            
            for (appInfo in packages) {
                // Skip our own app
                if (appInfo.packageName == context.packageName) {
                    continue
                }
                
                val appName = try {
                    packageManager.getApplicationLabel(appInfo).toString()
                } catch (e: Exception) {
                    appInfo.packageName
                }
                
                val icon = try {
                    packageManager.getApplicationIcon(appInfo.packageName)
                } catch (e: Exception) {
                    null
                }
                
                installedApps.add(
                    InstalledAppInfo(
                        packageName = appInfo.packageName,
                        appName = appName,
                        icon = icon,
                        isSystemApp = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0,
                        isProtected = protectedPackages.contains(appInfo.packageName)
                    )
                )
            }
            
            installedApps.sortedBy { it.appName.lowercase() }
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    suspend fun getAppInfo(packageName: String): InstalledAppInfo? = withContext(Dispatchers.IO) {
        try {
            val packageManager = context.packageManager
            val appInfo = packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA)
            
            InstalledAppInfo(
                packageName = packageName,
                appName = packageManager.getApplicationLabel(appInfo).toString(),
                icon = packageManager.getApplicationIcon(packageName),
                isSystemApp = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
            )
        } catch (e: Exception) {
            null
        }
    }
    
    fun observeInstalledApps(): Flow<List<InstalledAppInfo>> = flow {
        emit(getInstalledApps())
    }.flowOn(Dispatchers.IO)
}
