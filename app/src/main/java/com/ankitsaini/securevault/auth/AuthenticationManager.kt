package com.ankitsaini.securevault.auth

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.ankitsaini.securevault.data.model.LockType
import com.ankitsaini.securevault.data.repository.SecurityRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthenticationManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val securityRepository: SecurityRepository
) {
    
    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()
    
    private val _failedAttempts = MutableStateFlow<Int>(0)
    val failedAttempts: StateFlow<Int> = _failedAttempts.asStateFlow()
    
    companion object {
        private const val KEYSTORE = "AndroidKeyStore"
        private const val KEY_ALIAS = "SecureVaultBiometricKey"
        private const val MAX_FAILED_ATTEMPTS = 3
    }
    
    sealed class AuthState {
        object Idle : AuthState()
        object Authenticating : AuthState()
        data class Success(val packageName: String) : AuthState()
        data class Error(val message: String, val errorCode: Int? = null) : AuthState()
        data class Failed(val attemptsRemaining: Int) : AuthState()
        object LockedOut : AuthState()
    }
    
    sealed class AuthResult {
        data class Success(val packageName: String) : AuthResult()
        data class Failure(val message: String, val errorCode: Int? = null) : AuthResult()
        data class LockedOut(val retryAfterMillis: Long) : AuthResult()
        object Cancelled : AuthResult()
    }
    
    enum class AuthMethod {
        PIN,
        PATTERN,
        BIOMETRIC
    }
    
    fun isBiometricAvailable(): Boolean {
        val biometricManager = BiometricManager.from(context)
        return when (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)) {
            BiometricManager.BIOMETRIC_SUCCESS -> true
            else -> false
        }
    }
    
    fun getBiometricError(): String? {
        val biometricManager = BiometricManager.from(context)
        return when (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)) {
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> 
                "No biometric credentials enrolled"
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> 
                "Device doesn't support biometric authentication"
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> 
                "Biometric hardware unavailable"
            BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED -> 
                "Security update required"
            BiometricManager.BIOMETRIC_ERROR_UNSUPPORTED -> 
                "Biometric authentication unsupported"
            BiometricManager.BIOMETRIC_STATUS_UNKNOWN -> 
                "Unknown biometric status"
            else -> null
        }
    }
    
    suspend fun authenticateWithBiometric(
        activity: FragmentActivity,
        packageName: String,
        onResult: (AuthResult) -> Unit
    ) {
        _authState.value = AuthState.Authenticating
        
        val executor = ContextCompat.getMainExecutor(context)
        val biometricPrompt = BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    _authState.value = AuthState.Success(packageName)
                    _failedAttempts.value = 0
                    
                    // Log successful authentication
                    kotlinx.coroutines.MainScope().launch {
                        securityRepository.logSuccessfulUnlock(packageName, "BIOMETRIC")
                        securityRepository.logEvent(
                            com.ankitsaini.securevault.data.model.SecurityEvent(
                                packageName = packageName,
                                eventType = com.ankitsaini.securevault.data.model.EventType.BIOMETRIC_AUTH_SUCCESS,
                                eventDetails = "Biometric authentication successful",
                                wasSuccessful = true
                            )
                        )
                    }
                    
                    onResult(AuthResult.Success(packageName))
                }
                
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    _authState.value = AuthState.Error(errString.toString(), errorCode)
                    
                    // Log failed biometric attempt
                    kotlinx.coroutines.MainScope().launch {
                        securityRepository.logEvent(
                            com.ankitsaini.securevault.data.model.SecurityEvent(
                                packageName = packageName,
                                eventType = com.ankitsaini.securevault.data.model.EventType.BIOMETRIC_AUTH_FAILED,
                                eventDetails = "Biometric error: $errString",
                                wasSuccessful = false
                            )
                        )
                    }
                    
                    onResult(AuthResult.Failure(errString.toString(), errorCode))
                }
                
                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    _failedAttempts.value += 1
                    
                    val attemptsRemaining = MAX_FAILED_ATTEMPTS - _failedAttempts.value
                    if (attemptsRemaining <= 0) {
                        _authState.value = AuthState.LockedOut
                        onResult(AuthResult.LockedOut(30000)) // 30 second lockout
                    } else {
                        _authState.value = AuthState.Failed(attemptsRemaining)
                        onResult(AuthResult.Failure("Biometric authentication failed", null))
                    }
                }
            }
        )
        
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Unlock ${getAppName(packageName)}")
            .setSubtitle("Verify your identity to continue")
            .setDescription("Use your fingerprint or face to unlock this app")
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
            .setNegativeButtonText("Cancel")
            .build()
        
        biometricPrompt.authenticate(promptInfo)
    }
    
    suspend fun verifyPin(
        packageName: String,
        enteredPin: String,
        storedPinHash: String
    ): AuthResult {
        _authState.value = AuthState.Authenticating
        
        return try {
            val enteredPinHash = PinHasher.hashPin(enteredPin)
            
            if (enteredPinHash == storedPinHash) {
                _authState.value = AuthState.Success(packageName)
                _failedAttempts.value = 0
                
                // Log successful authentication
                securityRepository.logSuccessfulUnlock(packageName, "PIN")
                
                AuthResult.Success(packageName)
            } else {
                _failedAttempts.value += 1
                val attemptsRemaining = MAX_FAILED_ATTEMPTS - _failedAttempts.value
                
                // Log failed attempt
                securityRepository.logFailedUnlock(packageName, "PIN")
                
                if (attemptsRemaining <= 0) {
                    _authState.value = AuthState.LockedOut
                    AuthResult.LockedOut(30000)
                } else {
                    _authState.value = AuthState.Failed(attemptsRemaining)
                    AuthResult.Failure("Invalid PIN. $attemptsRemaining attempts remaining", null)
                }
            }
        } catch (e: Exception) {
            _authState.value = AuthState.Error("PIN verification failed", null)
            AuthResult.Failure("PIN verification error: ${e.message}", null)
        }
    }
    
    suspend fun verifyPattern(
        packageName: String,
        enteredPattern: List<Int>,
        storedPatternHash: String
    ): AuthResult {
        _authState.value = AuthState.Authenticating
        
        return try {
            val enteredPatternHash = PatternHasher.hashPattern(enteredPattern)
            
            if (enteredPatternHash == storedPatternHash) {
                _authState.value = AuthState.Success(packageName)
                _failedAttempts.value = 0
                
                // Log successful authentication
                securityRepository.logSuccessfulUnlock(packageName, "PATTERN")
                
                AuthResult.Success(packageName)
            } else {
                _failedAttempts.value += 1
                val attemptsRemaining = MAX_FAILED_ATTEMPTS - _failedAttempts.value
                
                // Log failed attempt
                securityRepository.logFailedUnlock(packageName, "PATTERN")
                securityRepository.logEvent(
                    com.ankitsaini.securevault.data.model.SecurityEvent(
                        packageName = packageName,
                        eventType = com.ankitsaini.securevault.data.model.EventType.PATTERN_ATTEMPT_FAILED,
                        eventDetails = "Pattern verification failed",
                        wasSuccessful = false
                    )
                )
                
                if (attemptsRemaining <= 0) {
                    _authState.value = AuthState.LockedOut
                    AuthResult.LockedOut(30000)
                } else {
                    _authState.value = AuthState.Failed(attemptsRemaining)
                    AuthResult.Failure("Invalid pattern. $attemptsRemaining attempts remaining", null)
                }
            }
        } catch (e: Exception) {
            _authState.value = AuthState.Error("Pattern verification failed", null)
            AuthResult.Failure("Pattern verification error: ${e.message}", null)
        }
    }
    
    fun resetFailedAttempts() {
        _failedAttempts.value = 0
        _authState.value = AuthState.Idle
    }
    
    private suspend fun getAppName(packageName: String): String {
        return try {
            securityRepository.getProtectedApp(packageName)?.appName ?: "App"
        } catch (e: Exception) {
            "App"
        }
    }
}
