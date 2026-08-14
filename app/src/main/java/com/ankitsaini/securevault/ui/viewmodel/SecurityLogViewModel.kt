package com.ankitsaini.securevault.ui.viewmodel

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ankitsaini.securevault.data.model.SecurityEvent
import com.ankitsaini.securevault.data.model.EventType
import com.ankitsaini.securevault.data.repository.SecurityRepository
import com.ankitsaini.securevault.utils.FileManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import javax.inject.Inject

@HiltViewModel
class SecurityLogViewModel @Inject constructor(
    private val securityRepository: SecurityRepository,
    private val fileManager: FileManager,
    @ApplicationContext private val context: Context
) : ViewModel() {
    
    private val _allEvents = MutableStateFlow<List<SecurityEvent>>(emptyList())
    val allEvents: StateFlow<List<SecurityEvent>> = _allEvents.asStateFlow()
    
    private val _filteredEvents = MutableStateFlow<List<SecurityEvent>>(emptyList())
    val filteredEvents: StateFlow<List<SecurityEvent>> = _filteredEvents.asStateFlow()
    
    private val _filterType = MutableStateFlow<EventType?>(null)
    val filterType: StateFlow<EventType?> = _filterType.asStateFlow()
    
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
    
    private val _selectedDate = MutableStateFlow<Long?>(null)
    val selectedDate: StateFlow<Long?> = _selectedDate.asStateFlow()
    
    private val _stats = MutableStateFlow(SecurityStats())
    val stats: StateFlow<SecurityStats> = _stats.asStateFlow()
    
    data class SecurityStats(
        val totalEvents: Int = 0,
        val failedAttempts: Int = 0,
        val intruderPhotos: Int = 0,
        val successfulUnlocks: Int = 0,
        val appLaunches: Int = 0
    )
    
    init {
        observeEvents()
    }
    
    private fun observeEvents() {
        viewModelScope.launch {
            securityRepository.getSecurityEvents().collect { events ->
                _allEvents.value = events
                updateStats(events)
                applyFilters()
            }
        }
    }
    
    private fun updateStats(events: List<SecurityEvent>) {
        _stats.value = SecurityStats(
            totalEvents = events.size,
            failedAttempts = events.count { 
                it.eventType == EventType.UNLOCK_FAILED ||
                it.eventType == EventType.PIN_ATTEMPT_FAILED ||
                it.eventType == EventType.PATTERN_ATTEMPT_FAILED ||
                it.eventType == EventType.BIOMETRIC_AUTH_FAILED
            },
            intruderPhotos = events.count { 
                it.eventType == EventType.INTRUDER_PHOTO_CAPTURED 
            },
            successfulUnlocks = events.count { 
                it.eventType == EventType.UNLOCK_SUCCESSFUL ||
                it.eventType == EventType.BIOMETRIC_AUTH_SUCCESS
            },
            appLaunches = events.count { 
                it.eventType == EventType.APP_LAUNCH_ATTEMPT 
            }
        )
    }
    
    private fun applyFilters() {
        val events = _allEvents.value
        val filter = _filterType.value
        val query = _searchQuery.value
        val date = _selectedDate.value
        
        _filteredEvents.value = events.filter { event ->
            // Apply type filter
            if (filter != null && event.eventType != filter) {
                return@filter false
            }
            
            // Apply search filter
            if (query.isNotEmpty()) {
                val searchableText = "${event.eventType.name} ${event.eventDetails ?: ""} ${event.packageName}"
                if (!searchableText.contains(query, ignoreCase = true)) {
                    return@filter false
                }
            }
            
            // Apply date filter
            if (date != null) {
                val eventDate = java.util.Calendar.getInstance().apply {
                    timeInMillis = event.eventTimestamp
                }
                val selectedDateCalendar = java.util.Calendar.getInstance().apply {
                    timeInMillis = date
                }
                
                if (eventDate.get(java.util.Calendar.YEAR) != selectedDateCalendar.get(java.util.Calendar.YEAR) ||
                    eventDate.get(java.util.Calendar.DAY_OF_YEAR) != selectedDateCalendar.get(java.util.Calendar.DAY_OF_YEAR)) {
                    return@filter false
                }
            }
            
            true
        }
    }
    
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        applyFilters()
    }
    
    fun clearSearch() {
        _searchQuery.value = ""
        applyFilters()
    }
    
    fun setFilter(filter: EventType?) {
        _filterType.value = filter
        applyFilters()
    }
    
    fun setDate(date: Long?) {
        _selectedDate.value = date
        applyFilters()
    }
    
    fun clearLog() {
        viewModelScope.launch {
            securityRepository.logEvent(
                SecurityEvent(
                    packageName = "system",
                    eventType = EventType.APP_PROTECTION_DISABLED,
                    eventDetails = "Security log cleared by user",
                    wasSuccessful = true
                )
            )
            
            // Clear all events
            _allEvents.value = emptyList()
            _filteredEvents.value = emptyList()
            updateStats(emptyList())
        }
    }
    
    fun exportLog() {
        viewModelScope.launch {
            try {
                val events = _filteredEvents.value
                val jsonArray = JSONArray()
                
                events.forEach { event ->
                    val jsonObject = JSONObject().apply {
                        put("event_id", event.eventId)
                        put("package_name", event.packageName)
                        put("event_type", event.eventType.name)
                        put("timestamp", event.eventTimestamp)
                        put("details", event.eventDetails ?: "")
                        put("photo_path", event.photoPath ?: "")
                        put("was_successful", event.wasSuccessful)
                    }
                    jsonArray.put(jsonObject)
                }
                
                val jsonString = jsonArray.toString(2)
                val file = File(context.cacheDir, "security_log_export.json")
                file.writeText(jsonString)
                
                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )
                
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/json"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                
                context.startActivity(Intent.createChooser(shareIntent, "Export Security Log"))
            } catch (e: Exception) {
                // Handle export error
            }
        }
    }
}
