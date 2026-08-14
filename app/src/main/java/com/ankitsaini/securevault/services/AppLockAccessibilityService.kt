package com.ankitsaini.securevault.services

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import com.ankitsaini.securevault.data.model.LockType
import com.ankitsaini.securevault.data.model.SecurityEvent
import com.ankitsaini.securevault.data.model.EventType
import com.ankitsaini.securevault.data.repository.SecurityRepository
import com.ankitsaini.securevault.auth.SessionManager
import com.ankitsaini.securevault.ui.LockScreenActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import javax.inject.Inject

@AndroidEntryPoint
class AppLockAccessibilityService : AccessibilityService() {
    
    @Inject
    lateinit var securityRepository: SecurityRepository
    
    @Inject
    lateinit var sessionManager: SessionManager
    
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val handler = Handler(Looper.getMainLooper())
    
    private var currentForegroundPackage: String? = null
    private var isLockScreenShowing = false
    
    companion object {
        var isServiceRunning = false
            private set
        
        private const val LOCK_SCREEN_DELAY_MS = 100L
    }
    
    override fun onServiceConnected() {
        super.onServiceConnected()
        isServiceRunning = true
    }
    
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        
        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                handleWindowStateChanged(event)
            }
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> {
                handleWindowContentChanged(event)
            }
        }
    }
    
    private fun handleWindowStateChanged(event: AccessibilityEvent) {
        val packageName = event.packageName?.toString() ?: return
        val className = event.className?.toString() ?: return
        
        // Ignore our own app
        if (packageName == packageName) return
        
        // Ignore system UI and launcher
        if (isSystemPackage(packageName)) return
        
        // Check if this is a new app launch
        if (currentForegroundPackage != packageName) {
            currentForegroundPackage = packageName
            checkIfAppIsProtected(packageName)
        }
    }
    
    private fun handleWindowContentChanged(event: AccessibilityEvent) {
        // Additional checks for app transitions
        val packageName = event.packageName?.toString() ?: return
        
        if (currentForegroundPackage != packageName) {
            currentForegroundPackage = packageName
            checkIfAppIsProtected(packageName)
        }
    }
    
    private fun checkIfAppIsProtected(packageName: String) {
        // Don't check if lock screen is already showing
        if (isLockScreenShowing) return
        
        // Check if app is already unlocked
        if (sessionManager.isAppUnlocked(packageName)) return
        
        serviceScope.launch {
            try {
                val protectedApp = securityRepository.getProtectedApp(packageName)
                
                if (protectedApp?.isProtected == true) {
                    // Log the app launch attempt
                    securityRepository.logEvent(
                        SecurityEvent(
                            packageName = packageName,
                            eventType = EventType.APP_LAUNCH_ATTEMPT,
                            eventDetails = "Protected app launch detected",
                            wasSuccessful = true
                        )
                    )
                    
                    // Show lock screen after small delay to ensure app is in foreground
                    handler.postDelayed({
                        showLockScreen(packageName, protectedApp.appName, protectedApp.lockType)
                    }, LOCK_SCREEN_DELAY_MS)
                }
            } catch (e: Exception) {
                // Handle error silently
            }
        }
    }
    
    private fun showLockScreen(packageName: String, appName: String, lockType: LockType) {
        if (isLockScreenShowing) return
        
        isLockScreenShowing = true
        
        val intent = LockScreenActivity.createIntent(
            context = this,
            packageName = packageName,
            appName = appName,
            lockType = lockType
        )
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        intent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
        
        try {
            startActivity(intent)
        } catch (e: Exception) {
            isLockScreenShowing = false
        }
    }
    
    fun onLockScreenDismissed() {
        isLockScreenShowing = false
    }
    
    private fun isSystemPackage(packageName: String): Boolean {
        return packageName == "com.android.systemui" ||
               packageName == "com.android.launcher" ||
               packageName == "com.google.android.apps.nexuslauncher" ||
               packageName == "com.android.settings" ||
               packageName.startsWith("com.android.") ||
               packageName.startsWith("com.google.android.")
    }
    
    override fun onInterrupt() {
        // Handle service interruption
        isLockScreenShowing = false
    }
    
    override fun onDestroy() {
        super.onDestroy()
        isServiceRunning = false
        serviceScope.cancel()
        handler.removeCallbacksAndMessages(null)
    }
}
