package com.ankitsaini.securevault.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ankitsaini.securevault.data.repository.DashboardData
import com.ankitsaini.securevault.data.repository.SecurityRepository
import com.ankitsaini.securevault.services.AppMonitorService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val securityRepository: SecurityRepository
) : ViewModel() {
    
    private val _dashboardData = MutableStateFlow<DashboardData>(
        DashboardData(
            protectedApps = emptyList(),
            recentEvents = emptyList(),
            failedAttempts = emptyList(),
            totalProtectedApps = 0,
            totalFailedAttempts = 0,
            totalSecurityEvents = 0
        )
    )
    val dashboardData: StateFlow<DashboardData> = _dashboardData.asStateFlow()
    
    private val _isServiceRunning = MutableStateFlow(false)
    val isServiceRunning: StateFlow<Boolean> = _isServiceRunning.asStateFlow()
    
    init {
        observeDashboardData()
        checkServiceStatus()
    }
    
    private fun observeDashboardData() {
        viewModelScope.launch {
            securityRepository.getDashboardData().collect { data ->
                _dashboardData.value = data
            }
        }
    }
    
    private fun checkServiceStatus() {
        viewModelScope.launch {
            // Check if the service is running
            _isServiceRunning.value = AppMonitorService.isRunning
        }
    }
    
    fun toggleService() {
        // Toggle the protection service
        _isServiceRunning.value = !_isServiceRunning.value
        // Here you would start/stop the actual service
    }
}
