package com.ankitsaini.securevault.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ankitsaini.securevault.data.model.LockType
import com.ankitsaini.securevault.data.repository.SecurityRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authenticationManager: AuthenticationManager,
    private val sessionManager: SessionManager,
    private val securityRepository: SecurityRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()
    
    private val _authState = MutableStateFlow<AuthenticationManager.AuthState>(AuthenticationManager.AuthState.Idle)
    val authState: StateFlow<AuthenticationManager.AuthState> = _authState.asStateFlow()
    
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
    
    init {
        _uiState.value = AuthUiState(
            biometricAvailable = authenticationManager.isBiometricAvailable()
        )
        
        viewModelScope.launch {
            authenticationManager.authState.collect { state ->
                when (state) {
                    is AuthenticationManager.AuthState.Success -> {
                        _uiState.value = _uiState.value.copy(isLoading = false)
                        _authState.value = state
                    }
                    is AuthenticationManager.AuthState.Error -> {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            errorMessage = state.message
                        )
                        _authState.value = state
                    }
                    is AuthenticationManager.AuthState.Failed -> {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            attemptsRemaining = state.attemptsRemaining,
                            errorMessage = "Authentication failed"
                        )
                        _authState.value = state
                    }
                    is AuthenticationManager.AuthState.LockedOut -> {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            isLockedOut = true
                        )
                        _authState.value = state
                    }
                    else -> {
                        _authState.value = state
                    }
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
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            val app = securityRepository.getProtectedApp(packageName)
            val storedHash = app?.pinHash ?: sessionManager.getMasterPinHash()
            
            if (storedHash != null) {
                val result = authenticationManager.verifyPin(
                    packageName = packageName,
                    enteredPin = pin,
                    storedPinHash = storedHash
                )
                
                handleAuthResult(result, packageName)
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "No PIN set for this app"
                )
            }
        }
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
                sessionManager.authenticateSession()
                _uiState.value = _uiState.value.copy(isLoading = false)
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
