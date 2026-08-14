package com.ankitsaini.securevault.services

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.ankitsaini.securevault.MainActivity
import com.ankitsaini.securevault.R
import com.ankitsaini.securevault.data.repository.SecurityRepository
import com.ankitsaini.securevault.auth.SessionManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import javax.inject.Inject

@AndroidEntryPoint
class AppMonitorService : Service() {
    
    @Inject
    lateinit var securityRepository: SecurityRepository
    
    @Inject
    lateinit var sessionManager: SessionManager
    
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var wakeLock: PowerManager.WakeLock? = null
    
    companion object {
        var isRunning = false
            private set
        
        private const val NOTIFICATION_CHANNEL_ID = "app_monitor_service"
        private const val NOTIFICATION_CHANNEL_NAME = "App Protection Service"
        private const val NOTIFICATION_ID = 1001
        
        fun startService(context: Context) {
            val intent = Intent(context, AppMonitorService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
        
        fun stopService(context: Context) {
            val intent = Intent(context, AppMonitorService::class.java)
            context.stopService(intent)
        }
    }
    
    override fun onCreate() {
        super.onCreate()
        isRunning = true
        acquireWakeLock()
        startForegroundWithNotification()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }
    
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
    
    private fun startForegroundWithNotification() {
        createNotificationChannel()
        
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            notificationIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        
        val notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Secure Vault")
            .setContentText("App protection is active")
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
        
        startForeground(NOTIFICATION_ID, notification)
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                NOTIFICATION_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows when app protection service is running"
                setShowBadge(false)
            }
            
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    private fun acquireWakeLock() {
        try {
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "SecureVault::AppMonitorWakeLock"
            ).apply {
                setReferenceCounted(false)
                acquire(30 * 60 * 1000L) // 30 minutes timeout
            }
        } catch (e: Exception) {
            // WakeLock acquisition failed
        }
    }
    
    private fun releaseWakeLock() {
        try {
            wakeLock?.let {
                if (it.isHeld) {
                    it.release()
                }
            }
            wakeLock = null
        } catch (e: Exception) {
            // WakeLock release failed
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        releaseWakeLock()
        serviceScope.cancel()
    }
    
    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        // Restart service if task is removed
        val restartIntent = Intent(applicationContext, AppMonitorService::class.java)
        restartIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startService(restartIntent)
    }
}
