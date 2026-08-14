package com.ankitsaini.securevault.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ankitsaini.securevault.data.model.LockType
import com.ankitsaini.securevault.data.model.ProtectedApp
import com.ankitsaini.securevault.data.repository.AppInfoRepository
import com.ankitsaini.securevault.data.repository.SecurityRepository
import com.ankitsaini.securevault.ui.screens.FilterType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AppListViewModel @Inject constructor(
    private val appInfoRepository: AppInfoRepository,
    private val securityRepository: SecurityRepository
) : ViewModel() {
    
    private val _installedApps = MutableStateFlow<List<AppInfoRepository.InstalledAppInfo>>(emptyList())
    val installedApps: StateFlow<List<AppInfoRepository.InstalledAppInfo>> = _installedApps.asStateFlow()
    
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
    
    private val _filterType = MutableStateFlow(FilterType.ALL)
    val filterType: StateFlow<FilterType> = _filterType.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    init {
        loadInstalledApps()
        observeFilters()
    }
    
    private fun loadInstalledApps() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val apps = appInfoRepository.getInstalledApps()
                _installedApps.value = apps
            } catch (e: Exception) {
                // Handle error
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    private fun observeFilters() {
        viewModelScope.launch {
            combine(
                _installedApps,
                _searchQuery,
                _filterType
            ) { apps, query, filter ->
                var filteredApps = apps
                
                // Apply search filter
                if (query.isNotEmpty()) {
                    filteredApps = filteredApps.filter { app ->
                        app.appName.contains(query, ignoreCase = true) ||
                        app.packageName.contains(query, ignoreCase = true)
                    }
                }
                
                // Apply type filter
                filteredApps = when (filter) {
                    FilterType.ALL -> filteredApps
                    FilterType.PROTECTED -> filteredApps.filter { it.isProtected }
                    FilterType.UNPROTECTED -> filteredApps.filter { !it.isProtected }
                }
                
                filteredApps
            }.collect { filteredApps ->
                _installedApps.value = filteredApps
            }
        }
    }
    
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }
    
    fun clearSearch() {
        _searchQuery.value = ""
    }
    
    fun setFilter(filter: FilterType) {
        _filterType.value = filter
    }
    
    fun toggleProtection(packageName: String, isProtected: Boolean) {
        viewModelScope.launch {
            try {
                if (isProtected) {
                    // Add protection
                    val appInfo = appInfoRepository.getAppInfo(packageName)
                    if (appInfo != null) {
                        val protectedApp = ProtectedApp(
                            packageName = packageName,
                            appName = appInfo.appName,
                            isProtected = true,
                            lockType = LockType.PIN,
                            useBiometric = false,
                            notificationMasking = true,
                            maxFailedAttempts = 3
                        )
                        securityRepository.addProtectedApp(protectedApp)
                    }
                } else {
                    // Remove protection
                    securityRepository.removeProtectedApp(packageName)
                }
                
                // Reload apps to reflect changes
                loadInstalledApps()
            } catch (e: Exception) {
                // Handle error
            }
        }
    }
}
