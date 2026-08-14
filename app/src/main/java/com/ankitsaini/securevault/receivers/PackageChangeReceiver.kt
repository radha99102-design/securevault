package com.ankitsaini.securevault.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.ankitsaini.securevault.data.model.SecurityEvent
import com.ankitsaini.securevault.data.model.EventType
import com.ankitsaini.securevault.data.repository.SecurityRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import javax.inject.Inject

@AndroidEntryPoint
class PackageChangeReceiver : BroadcastReceiver() {
    
    @Inject
    lateinit var securityRepository: SecurityRepository
    
    private val receiverScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
    override fun onReceive(context: Context, intent: Intent) {
        val packageName = intent.data?.schemeSpecificPart ?: return
        
        when (intent.action) {
            Intent.ACTION_PACKAGE_REMOVED -> {
                handlePackageRemoved(packageName)
            }
            Intent.ACTION_PACKAGE_REPLACED -> {
                handlePackageReplaced(packageName)
            }
        }
    }
    
    private fun handlePackageRemoved(packageName: String) {
        receiverScope.launch {
            try {
                // Remove protection for uninstalled app
                securityRepository.removeProtectedApp(packageName)
                
                // Log the event
                securityRepository.logEvent(
                    SecurityEvent(
                        packageName = packageName,
                        eventType = EventType.APP_PROTECTION_DISABLED,
                        eventDetails = "Protected app was uninstalled",
                        wasSuccessful = true
                    )
                )
            } catch (e: Exception) {
                // Handle error
            }
        }
    }
    
    private fun handlePackageReplaced(packageName: String) {
        receiverScope.launch {
            try {
                // Check if app is still protected
                val protectedApp = securityRepository.getProtectedApp(packageName)
                
                if (protectedApp?.isProtected == true) {
                    // Log the event
                    securityRepository.logEvent(
                        SecurityEvent(
                            packageName = packageName,
                            eventType = EventType.APP_PROTECTION_ENABLED,
                            eventDetails = "Protected app was updated",
                            wasSuccessful = true
                        )
                    )
                }
            } catch (e: Exception) {
                // Handle error
            }
        }
    }
}
