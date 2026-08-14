package com.ankitsaini.securevault.auth

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    companion object {
        private const val PREFS_NAME = "secure_vault_session"
        private const val KEY_MASTER_AUTH = "master_authenticated"
        private const val KEY_LAST_AUTH_TIME = "last_auth_time"
        private const val KEY_LOCKED_UNTIL = "locked_until"
        private const val KEY_MASTER_PIN_HASH = "master_pin_hash"
        private const val KEY_MASTER_PATTERN_HASH = "master_pattern_hash"
        private const val SESSION_TIMEOUT_MS = 30 * 60 * 1000L // 30 minutes
    }
    
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()
    
    private val sharedPreferences: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        PREFS_NAME,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )
    
    private val _isAuthenticated = MutableStateFlow(false)
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()
    
    private val _unlockedApps = MutableStateFlow<Set<String>>(emptySet())
    val unlockedApps: StateFlow<Set<String>> = _unlockedApps.asStateFlow()
    
    init {
        checkAuthenticationState()
    }
    
    fun setMasterPinHash(pinHash: String) {
        sharedPreferences.edit().putString(KEY_MASTER_PIN_HASH, pinHash).apply()
    }
    
    fun getMasterPinHash(): String? {
        return sharedPreferences.getString(KEY_MASTER_PIN_HASH, null)
    }
    
    fun setMasterPatternHash(patternHash: String) {
        sharedPreferences.edit().putString(KEY_MASTER_PATTERN_HASH, patternHash).apply()
    }
    
    fun getMasterPatternHash(): String? {
        return sharedPreferences.getString(KEY_MASTER_PATTERN_HASH, null)
    }
    
    fun authenticateSession() {
        val currentTime = System.currentTimeMillis()
        sharedPreferences.edit().apply {
            putBoolean(KEY_MASTER_AUTH, true)
            putLong(KEY_LAST_AUTH_TIME, currentTime)
        }.apply()
        _isAuthenticated.value = true
    }
    
    fun isSessionValid(): Boolean {
        val lastAuthTime = sharedPreferences.getLong(KEY_LAST_AUTH_TIME, 0)
        val lockedUntil = sharedPreferences.getLong(KEY_LOCKED_UNTIL, 0)
        val currentTime = System.currentTimeMillis()
        
        return sharedPreferences.getBoolean(KEY_MASTER_AUTH, false) &&
               currentTime - lastAuthTime < SESSION_TIMEOUT_MS &&
               currentTime > lockedUntil
    }
    
    fun invalidateSession() {
        sharedPreferences.edit().apply {
            putBoolean(KEY_MASTER_AUTH, false)
        }.apply()
        _isAuthenticated.value = false
        _unlockedApps.value = emptySet()
    }
    
    fun lockForDuration(durationMs: Long) {
        val lockedUntil = System.currentTimeMillis() + durationMs
        sharedPreferences.edit().putLong(KEY_LOCKED_UNTIL, lockedUntil).apply()
        invalidateSession()
    }
    
    fun unlockApp(packageName: String) {
        val currentUnlocked = _unlockedApps.value.toMutableSet()
        currentUnlocked.add(packageName)
        _unlockedApps.value = currentUnlocked
    }
    
    fun isAppUnlocked(packageName: String): Boolean {
        return _unlockedApps.value.contains(packageName)
    }
    
    fun relockApp(packageName: String) {
        val currentUnlocked = _unlockedApps.value.toMutableSet()
        currentUnlocked.remove(packageName)
        _unlockedApps.value = currentUnlocked
    }
    
    fun relockAllApps() {
        _unlockedApps.value = emptySet()
    }
    
    private fun checkAuthenticationState() {
        _isAuthenticated.value = isSessionValid()
    }
    
    fun clearAllSessionData() {
        sharedPreferences.edit().clear().apply()
        _isAuthenticated.value = false
        _unlockedApps.value = emptySet()
    }
}
