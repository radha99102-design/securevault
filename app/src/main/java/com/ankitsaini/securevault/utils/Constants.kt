package com.ankitsaini.securevault.utils

object Constants {
    
    // App info
    const val APP_NAME = "Secure Vault"
    const val PACKAGE_NAME = "com.ankitsaini.securevault"
    
    // Notification channels
    const val CHANNEL_SERVICE = "service_channel"
    const val CHANNEL_ALERTS = "alerts_channel"
    const val CHANNEL_MASKED = "masked_notifications"
    
    // Notification IDs
    const val NOTIFICATION_SERVICE_ID = 1001
    const val NOTIFICATION_ALERT_ID = 1002
    const val NOTIFICATION_MASKED_BASE = 2000
    
    // Security settings
    const val DEFAULT_MAX_FAILED_ATTEMPTS = 3
    const val DEFAULT_RELOCK_TIMEOUT = 30 // seconds
    const val DEFAULT_LOCKOUT_DURATION = 30 // seconds
    const val MAX_PIN_LENGTH = 6
    const val MIN_PIN_LENGTH = 4
    const val PATTERN_GRID_SIZE = 3
    
    // File paths
    const val SECURITY_PHOTOS_DIR = "security_photos"
    const val BACKUP_DIR = "backups"
    const val LOGS_DIR = "logs"
    const val TEMP_DIR = "temp"
    
    // Request codes
    const val REQUEST_CODE_ACCESSIBILITY = 100
    const val REQUEST_CODE_NOTIFICATION = 101
    const val REQUEST_CODE_OVERLAY = 102
    const val REQUEST_CODE_USAGE_STATS = 103
    const val REQUEST_CODE_BIOMETRIC = 104
    const val REQUEST_CODE_CAMERA = 105
    const val REQUEST_CODE_BATTERY = 106
    
    // Preferences keys
    const val PREF_MASTER_PIN = "master_pin_hash"
    const val PREF_MASTER_PATTERN = "master_pattern_hash"
    const val PREF_BIOMETRIC_ENABLED = "biometric_enabled"
    const val PREF_AUTO_START = "auto_start_on_boot"
    const val PREF_INTRUDER_PHOTO = "intruder_photo_enabled"
    const val PREF_MAX_FAILED_ATTEMPTS = "max_failed_attempts"
    const val PREF_RELOCK_TIMEOUT = "relock_timeout"
    const val PREF_STEALTH_MODE = "stealth_mode"
    
    // Timeouts
    const val SESSION_TIMEOUT_MS = 30 * 60 * 1000L // 30 minutes
    const val LOCK_SCREEN_DELAY_MS = 100L
    const val PHOTO_CAPTURE_DELAY_MS = 500L
    
    // Limits
    const val MAX_SECURITY_EVENTS = 1000
    const val MAX_INTRUDER_PHOTOS = 50
    const val MAX_LOG_FILE_SIZE = 5 * 1024 * 1024 // 5MB
}
