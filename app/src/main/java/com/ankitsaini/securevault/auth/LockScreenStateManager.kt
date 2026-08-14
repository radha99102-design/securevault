package com.ankitsaini.securevault.auth

import android.content.Context
import com.ankitsaini.securevault.data.model.LockType
import com.ankitsaini.securevault.data.repository.SecurityRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LockScreenStateManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val securityRepository: SecurityRepository,
    private val sessionManager: SessionManager
) {
    
    private val _showLockScreen = MutableStateFlow(false)
    val showLockScreen: StateFlow<Boolean> = _showLockScreen.asStateFlow()
    
    private val _lockedAppPackage = MutableStateFlow<String?>(null)
    val lockedAppPackage: StateFlow<String?> = _lockedAppPackage.asStateFlow()
    
    private val _lockScreenState = MutableStateFlow<LockScreenState>(LockScreenState.Hidden)
    val lockScreenState: StateFlow<LockScreenState> = _lockScreenState.asStateFlow()
    
    sealed class LockScreenState {
        object Hidden : LockScreenState()
        data class Showing(
            val packageName: String,
            val appName: String,
            val lockType: LockType,
            val attemptsRemaining: Int = 3
        ) : LockScreenState()
        data class LockedOut(
            val packageName: String,
            val retryAfterMs: Long
        ) : LockScreenState()
    }
    
    suspend fun showLockScreen(packageName: String) {
        val app = securityRepository.getProtectedApp(packageName) ?: return
        
        // Check if app is already unlocked
        if (sessionManager.isAppUnlocked(packageName)) {
            return
        }
        
        _lockedAppPackage.value = packageName
        _lockScreenState.value = LockScreenState.Showing(
            packageName = packageName,
            appName = app.appName,
            lockType = app.lockType
        )
        _showLockScreen.value = true
    }
    
    fun hideLockScreen() {
        _showLockScreen.value = false
        _lockedAppPackage.value = null
        _lockScreenState.value = LockScreenState.Hidden
    }
    
    fun onAuthenticationSuccess(packageName: String) {
        sessionManager.unlockApp(packageName)
        hideLockScreen()
    }
    
    fun onAuthenticationFailed(attemptsRemaining: Int) {
        val currentState = _lockScreenState.value
        if (currentState is LockScreenState.Showing) {
            _lockScreenState.value = currentState.copy(
                attemptsRemaining = attemptsRemaining
            )
        }
    }
    
    fun onLockedOut(packageName: String, retryAfterMs: Long) {
        _lockScreenState.value = LockScreenState.LockedOut(
            packageName = packageName,
            retryAfterMs = retryAfterMs
        )
    }
}
