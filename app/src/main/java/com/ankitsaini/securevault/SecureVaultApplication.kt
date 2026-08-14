package com.ankitsaini.securevault

import android.app.Application
import android.content.Context
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class SecureVaultApplication : Application() {
    
    companion object {
        private var instance: SecureVaultApplication? = null
        
        fun getInstance(): SecureVaultApplication {
            return instance ?: throw IllegalStateException("Application not initialized")
        }
        
        fun getContext(): Context {
            return instance ?: throw IllegalStateException("Application not initialized")
        }
    }
    
    override fun onCreate() {
        super.onCreate()
        instance = this
    }
}
