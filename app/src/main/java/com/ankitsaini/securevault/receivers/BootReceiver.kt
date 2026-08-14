package com.ankitsaini.securevault.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.ankitsaini.securevault.services.AppMonitorService
import com.ankitsaini.securevault.data.repository.SecurityRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import javax.inject.Inject

@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {
    
    @Inject
    lateinit var securityRepository: SecurityRepository
    
    private val receiverScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            "android.intent.action.QUICKBOOT_POWERON" -> {
                handleBootCompleted(context)
            }
        }
    }
    
    private fun handleBootCompleted(context: Context) {
        receiverScope.launch {
            try {
                // Check if auto-start is enabled
                val autoStartSetting = securityRepository.getSetting("auto_start_on_boot")
                val autoStart = autoStartSetting?.settingsValue?.toBoolean() ?: true
                
                if (autoStart) {
                    // Start the monitoring service
                    AppMonitorService.startService(context)
                }
            } catch (e: Exception) {
                // Handle error
            }
        }
    }
}
