package com.ankitsaini.securevault.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.ankitsaini.securevault.data.model.SecurityEvent
import com.ankitsaini.securevault.data.model.EventType
import com.ankitsaini.securevault.data.repository.SecurityRepository
import com.ankitsaini.securevault.auth.SessionManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import javax.inject.Inject

@AndroidEntryPoint
class NotificationFilterService : NotificationListenerService() {
    
    @Inject
    lateinit var securityRepository: SecurityRepository
    
    @Inject
    lateinit var sessionManager: SessionManager
    
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
    companion object {
        var isServiceRunning = false
            private set
        
        private const val NOTIFICATION_CHANNEL_ID = "secure_vault_notifications"
        private const val NOTIFICATION_CHANNEL_NAME = "Secure Vault Notifications"
    }
    
    override fun onListenerConnected() {
        super.onListenerConnected()
        isServiceRunning = true
        createNotificationChannel()
    }
    
    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (sbn == null) return
        
        val packageName = sbn.packageName
        
        serviceScope.launch {
            try {
                val protectedApp = securityRepository.getProtectedApp(packageName)
                
                if (protectedApp?.isProtected == true && 
                    protectedApp.notificationMasking &&
                    !sessionManager.isAppUnlocked(packageName)) {
                    
                    // Cancel original notification
                    cancelNotification(sbn.key)
                    
                    // Create masked notification
                    createMaskedNotification(
                        packageName = packageName,
                        appName = protectedApp.appName,
                        originalNotification = sbn.notification
                    )
                    
                    // Log the masking event
                    securityRepository.logEvent(
                        SecurityEvent(
                            packageName = packageName,
                            eventType = EventType.NOTIFICATION_MASKED,
                            eventDetails = "Notification content masked for protected app",
                            wasSuccessful = true
                        )
                    )
                }
            } catch (e: Exception) {
                // Handle error silently
            }
        }
    }
    
    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
        // Handle notification removal if needed
    }
    
    private fun createMaskedNotification(
        packageName: String,
        appName: String,
        originalNotification: Notification
    ) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        val maskedNotification = Notification.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setContentTitle(appName)
            .setContentText("Content hidden - Unlock app to view notification")
            .setStyle(Notification.BigTextStyle()
                .bigText("This notification is from a protected app. Unlock the app to view its content."))
            .setAutoCancel(true)
            .setOngoing(false)
            .build()
        
        try {
            notificationManager.notify(
                packageName.hashCode(),
                maskedNotification
            )
        } catch (e: Exception) {
            // Handle notification error
        }
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                NOTIFICATION_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications for masked app content"
                enableVibration(false)
                setShowBadge(false)
            }
            
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        isServiceRunning = false
        serviceScope.cancel()
    }
}
