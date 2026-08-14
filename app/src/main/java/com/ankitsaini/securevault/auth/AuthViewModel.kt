package com.ankitsaini.securevault.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ankitsaini.securevault.data.model.LockType
import com.ankitsaini.securevault.data.model.SecurityEvent
import com.ankitsaini.securevault.data.model.EventType
import com.ankitsaini.securevault.data.repository.SecurityRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authenticationManager: AuthenticationManager,
    private val sessionManager: SessionManager,
    private val securityRepository: SecurityRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Initial)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()
    
    private val _currentPackage = MutableStateFlow<String?>(null)
    val currentPackage: StateFlow<String?> = _currentPackage.asStateFlow()
    
    data class AuthUiState(
        val isLoading: Boolean = false,
        val lockType: LockType = LockType.PIN,
        val errorMessage: String? = null,
        val attemptsRemaining: Int = 3,
        val isLockedOut: Boolean = false,
        val biometricAvailable: Boolean = false
    )
    
    sealed class AuthEvents {
        data class ShowError(val message: String) : AuthEvents()
        data class AuthenticationSuccess(val packageName: String) : AuthEvents()
        object AuthenticationCancelled : AuthEvents()
        data class LockedOut(val retryAfterMs: Long) : AuthEvents()
    }
    
    init {
        _uiState.value = AuthUiState(
            biometricAvailable = authenticationManager.isBiometricAvailable()
        )
        
        // Observe authentication state
        viewModelScope.launch {
            authenticationManager.authState.collect { state ->
                when (state) {
                    is AuthenticationManager.AuthState.Success -> {
                        _uiState.value = _uiState.value.copy(isLoading = false)
                    }
                    is AuthenticationManager.AuthState.Error -> {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            errorMessage = state.message
                        )
                    }
                    is AuthenticationManager.AuthState.Failed -> {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            attemptsRemaining = state.attemptsRemaining,
                            errorMessage = "Authentication failed"
                        )
                    }
                    is AuthenticationManager.AuthState.LockedOut -> {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            isLockedOut = true
                        )
                    }
                    else -> {}
                }
            }
        }
    }
    
    fun setupAuthentication(packageName: String, lockType: LockType) {
        _currentPackage.value = packageName
        _uiState.value = _uiState.value.copy(
            lockType = lockType,
            errorMessage = null,
            isLoading = false
        )
    }
    
    fun authenticateWithPin(pin: String) {
        val packageName = _currentPackage.value ?: return
        val storedHash = when {
            packageName == "com.ankitsaini.securevault" -> 
                sessionManager.getMasterPinHash()
            else -> null
        }
        
        if (storedHash == null) {
            viewModelScope.launch {
                val app = securityRepository.getProtectedApp(packageName)
                if (app?.pinHash != null) {
                    performPinAuthentication(packageName, pin, app.pinHash)
                } else {
                    _uiState.value = _uiState.value.copy(
                        errorMessage = "No PIN set for this app"
                    )
                }
            }
        } else {
            viewModelScope.launch {
                performPinAuthentication(packageName, pin, storedHash)
            }
        }
    }
    
    private suspend fun performPinAuthentication(
        packageName: String,
        pin: String,
        storedHash: String
    ) {
        _uiState.value = _uiState.value.copy(isLoading = true)
        
        val result = authenticationManager.verifyPin(
            packageName = packageName,
            enteredPin = pin,
            storedPinHash = storedHash
        )
        
        handleAuthResult(result, packageName)
    }
    
    fun authenticateWithPattern(pattern: List<Int>) {
        val packageName = _currentPackage.value ?: return
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            val app = securityRepository.getProtectedApp(packageName)
            val storedHash = app?.patternHash ?: sessionManager.getMasterPatternHash()
            
            if (storedHash != null) {
                val result = authenticationManager.verifyPattern(
                    packageName = packageName,
                    enteredPattern = pattern,
                    storedPatternHash = storedHash
                )
                
                handleAuthResult(result, packageName)
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "No pattern set for this app"
                )
            }
        }
    }
    
    private fun handleAuthResult(
        result: AuthenticationManager.AuthResult,
        packageName: String
    ) {
        when (result) {
            is AuthenticationManager.AuthResult.Success -> {
                sessionManager.unlockApp(packageName)
                _uiState.value = _uiState.value.copy(isLoading = false)
                sessionManager.authenticateSession()
            }
            is AuthenticationManager.AuthResult.Failure -> {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = result.message
                )
            }
            is AuthenticationManager.AuthResult.LockedOut -> {
                sessionManager.lockForDuration(result.retryAfterMillis)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isLockedOut = true,
                    errorMessage = "Too many failed attempts. Try again later."
                )
            }
            is AuthenticationManager.AuthResult.Cancelled -> {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }
    
    fun resetState() {
        _uiState.value = AuthUiState(
            biometricAvailable = authenticationManager.isBiometricAvailable()
        )
        authenticationManager.resetFailedAttempts()
    }
    
    fun cancelAuthentication() {
        authenticationManager.resetFailedAttempts()
        _uiState.value = AuthUiState(
            biometricAvailable = authenticationManager.isBiometricAvailable()
        )
    }
}
