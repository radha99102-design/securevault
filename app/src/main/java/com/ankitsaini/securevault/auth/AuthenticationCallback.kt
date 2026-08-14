package com.ankitsaini.securevault.auth

import com.ankitsaini.securevault.data.model.LockType

interface AuthenticationCallback {
    fun onAuthenticationStarted(packageName: String, lockType: LockType)
    
    fun onAuthenticationSuccess(packageName: String, method: AuthenticationManager.AuthMethod)
    
    fun onAuthenticationFailed(
        packageName: String,
        method: AuthenticationManager.AuthMethod,
        attemptsRemaining: Int
    )
    
    fun onAuthenticationError(packageName: String, error: String)
    
    fun onAuthenticationLockedOut(packageName: String, retryAfterMs: Long)
    
    fun onAuthenticationCancelled(packageName: String)
}
