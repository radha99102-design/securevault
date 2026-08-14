package com.ankitsaini.securevault.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.ankitsaini.securevault.auth.SessionManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class ScreenOffReceiver : BroadcastReceiver() {
    
    @Inject
    lateinit var sessionManager: SessionManager
    
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_SCREEN_OFF -> {
                handleScreenOff()
            }
        }
    }
    
    private fun handleScreenOff() {
        // Relock all apps when screen turns off
        sessionManager.relockAllApps()
    }
}
