package com.ankitsaini.securevault.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import com.ankitsaini.securevault.data.model.LockType
import com.ankitsaini.securevault.services.AppLockAccessibilityService
import com.ankitsaini.securevault.ui.screens.LockScreen
import com.ankitsaini.securevault.ui.theme.SecureVaultTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class LockScreenActivity : ComponentActivity() {
    
    private var packageName: String = ""
    private var appName: String = ""
    private var lockType: LockType = LockType.PIN
    
    companion object {
        private const val EXTRA_PACKAGE_NAME = "package_name"
        private const val EXTRA_APP_NAME = "app_name"
        private const val EXTRA_LOCK_TYPE = "lock_type"
        
        fun createIntent(
            context: Context,
            packageName: String,
            appName: String,
            lockType: LockType
        ): Intent {
            return Intent(context, LockScreenActivity::class.java).apply {
                putExtra(EXTRA_PACKAGE_NAME, packageName)
                putExtra(EXTRA_APP_NAME, appName)
                putExtra(EXTRA_LOCK_TYPE, lockType.name)
            }
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Set window flags for overlay
        window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED)
        window.addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)
        
        // Get extras
        packageName = intent.getStringExtra(EXTRA_PACKAGE_NAME) ?: ""
        appName = intent.getStringExtra(EXTRA_APP_NAME) ?: "App"
        lockType = LockType.valueOf(
            intent.getStringExtra(EXTRA_LOCK_TYPE) ?: LockType.PIN.name
        )
        
        setContent {
            SecureVaultTheme {
                LockScreen(
                    packageName = packageName,
                    appName = appName,
                    lockType = lockType,
                    onAuthenticated = {
                        handleAuthenticationSuccess()
                    },
                    onCancel = {
                        handleAuthenticationCancel()
                    }
                )
            }
        }
    }
    
    private fun handleAuthenticationSuccess() {
        // Notify accessibility service that lock screen is dismissed
        // The service will handle this via lifecycle
        setResult(Activity.RESULT_OK)
        finish()
    }
    
    private fun handleAuthenticationCancel() {
        setResult(Activity.RESULT_CANCELED)
        finish()
        
        // Go to home screen when cancelled
        val homeIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(homeIntent)
    }
    
    override fun onBackPressed() {
        super.onBackPressed()
        handleAuthenticationCancel()
    }
}
