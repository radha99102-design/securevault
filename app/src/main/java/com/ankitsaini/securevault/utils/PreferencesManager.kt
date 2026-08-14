package com.ankitsaini.securevault.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PreferencesManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    companion object {
        private const val PREFS_NAME = "secure_vault_preferences"
        
        // Keys
        const val KEY_MASTER_PIN = "master_pin_hash"
        const val KEY_MASTER_PATTERN = "master_pattern_hash"
        const val KEY_BIOMETRIC_ENABLED = "biometric_enabled"
        const val KEY_AUTO_START = "auto_start_on_boot"
        const val KEY_INTRUDER_PHOTO = "intruder_photo_enabled"
        const val KEY_MAX_FAILED_ATTEMPTS = "max_failed_attempts"
        const val KEY_RELOCK_TIMEOUT = "relock_timeout_seconds"
        const val KEY_STEALTH_MODE = "stealth_mode"
        const val KEY_FAKE_CRASH = "fake_crash_on_failed"
        const val KEY_FIRST_LAUNCH = "first_launch"
        const val KEY_LAST_BACKUP = "last_backup_time"
    }
    
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()
    
    private val preferences: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        PREFS_NAME,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )
    
    fun putString(key: String, value: String) {
        preferences.edit().putString(key, value).apply()
    }
    
    fun getString(key: String, defaultValue: String? = null): String? {
        return preferences.getString(key, defaultValue)
    }
    
    fun putBoolean(key: String, value: Boolean) {
        preferences.edit().putBoolean(key, value).apply()
    }
    
    fun getBoolean(key: String, defaultValue: Boolean = false): Boolean {
        return preferences.getBoolean(key, defaultValue)
    }
    
    fun putInt(key: String, value: Int) {
        preferences.edit().putInt(key, value).apply()
    }
    
    fun getInt(key: String, defaultValue: Int = 0): Int {
        return preferences.getInt(key, defaultValue)
    }
    
    fun putLong(key: String, value: Long) {
        preferences.edit().putLong(key, value).apply()
    }
    
    fun getLong(key: String, defaultValue: Long = 0L): Long {
        return preferences.getLong(key, defaultValue)
    }
    
    fun putFloat(key: String, value: Float) {
        preferences.edit().putFloat(key, value).apply()
    }
    
    fun getFloat(key: String, defaultValue: Float = 0f): Float {
        return preferences.getFloat(key, defaultValue)
    }
    
    fun remove(key: String) {
        preferences.edit().remove(key).apply()
    }
    
    fun clear() {
        preferences.edit().clear().apply()
    }
    
    fun contains(key: String): Boolean {
        return preferences.contains(key)
    }
    
    fun getAll(): Map<String, *> {
        return preferences.all
    }
    
    fun isFirstLaunch(): Boolean {
        return getBoolean(KEY_FIRST_LAUNCH, true)
    }
    
    fun setFirstLaunchCompleted() {
        putBoolean(KEY_FIRST_LAUNCH, false)
    }
}
