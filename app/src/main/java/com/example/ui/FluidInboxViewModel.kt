package com.example.ui

import android.content.Context
import android.provider.Settings
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.LiquidNotificationMediator
import com.example.data.NotificationDatabase
import com.example.data.NotificationEntity
import com.example.data.NotificationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class FluidInboxViewModel(private val repository: NotificationRepository) : ViewModel() {

    private val _appLanguage = MutableStateFlow("bn") // "bn" or "en"
    val appLanguage: StateFlow<String> = _appLanguage.asStateFlow()

    fun toggleLanguage() {
        _appLanguage.value = if (_appLanguage.value == "bn") "en" else "bn"
    }

    private val _selectedFilter = MutableStateFlow("ALL") // "ALL", "CALLS", "MESSAGES"
    val selectedFilter: StateFlow<String> = _selectedFilter.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isPermissionGranted = MutableStateFlow(false)
    val isPermissionGranted: StateFlow<Boolean> = _isPermissionGranted.asStateFlow()

    // Holds the item that is currently in the Liquid Dynamic Island display
    private val _activeIslandNotification = MutableStateFlow<NotificationEntity?>(null)
    val activeIslandNotification: StateFlow<NotificationEntity?> = _activeIslandNotification.asStateFlow()

    private val _isIslandExpanded = MutableStateFlow(false)
    val isIslandExpanded: StateFlow<Boolean> = _isIslandExpanded.asStateFlow()

    // Unified notifications list with filter & search applied (ONLY UNDER 24 HOURS OLD)
    val uiState: StateFlow<List<NotificationEntity>> = combine(
        repository.allNotifications,
        _selectedFilter,
        _searchQuery
    ) { rawList, filter, query ->
        val oneDayAgo = System.currentTimeMillis() - (24 * 60 * 60 * 1000L)
        rawList.filter { item ->
            val isRecent = item.timestamp >= oneDayAgo
            val matchFilter = when (filter) {
                "CALLS" -> item.type == "CALL"
                "MESSAGES" -> item.type == "MESSAGE"
                else -> true
            }
            val matchQuery = if (query.isBlank()) true else {
                item.title.contains(query, ignoreCase = true) ||
                        item.message.contains(query, ignoreCase = true) ||
                        item.appName.contains(query, ignoreCase = true)
            }
            isRecent && matchFilter && matchQuery
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // History notifications (OLDER THAN 24 HOURS, UP TO 30 DAYS OLD)
    val historyState: StateFlow<List<NotificationEntity>> = combine(
        repository.allNotifications,
        _searchQuery
    ) { rawList, query ->
        val currentTime = System.currentTimeMillis()
        val oneDayAgo = currentTime - (24 * 60 * 60 * 1000L)
        val thirtyDaysAgo = currentTime - (30 * 24 * 60 * 60 * 1000L)
        rawList.filter { item ->
            val isHistorical = item.timestamp in thirtyDaysAgo until oneDayAgo
            val matchQuery = if (query.isBlank()) true else {
                item.title.contains(query, ignoreCase = true) ||
                        item.message.contains(query, ignoreCase = true) ||
                        item.appName.contains(query, ignoreCase = true)
            }
            isHistorical && matchQuery
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    init {
        // Automatically delete notifications older than 30 days to keep database fresh
        viewModelScope.launch {
            val thirtyDaysAgo = System.currentTimeMillis() - (30 * 24 * 60 * 60 * 1000L)
            repository.deleteOldNotifications(thirtyDaysAgo)
        }

        // Collect real-time broadcasts from system background interceptor
        viewModelScope.launch {
            LiquidNotificationMediator.incomingNotifications.collect { freshNotify ->
                // Feed this into the Dynamic Island UI popup immediately!
                _activeIslandNotification.value = freshNotify
                _isIslandExpanded.value = false // start collapsed, user can tap to expand
            }
        }
    }

    fun setFilter(filter: String) {
        _selectedFilter.value = filter
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun clearActiveIsland() {
        _activeIslandNotification.value = null
        _isIslandExpanded.value = false
    }

    fun setIslandExpanded(expanded: Boolean) {
        _isIslandExpanded.value = expanded
    }

    fun markAsRead(id: Long) = viewModelScope.launch {
        repository.markAsRead(id)
    }

    fun markAllAsRead() = viewModelScope.launch {
        repository.markAllAsRead()
    }

    fun deleteNotification(id: Long) = viewModelScope.launch {
        repository.deleteById(id)
        if (_activeIslandNotification.value?.id == id) {
            clearActiveIsland()
        }
    }

    fun clearAllNotifications() = viewModelScope.launch {
        repository.clearAll()
        clearActiveIsland()
    }

    fun checkPermission(context: Context) {
        val enabledListeners = Settings.Secure.getString(
            context.contentResolver,
            "enabled_notification_listeners"
        )
        val isEnabled = enabledListeners?.contains(context.packageName) == true
        _isPermissionGranted.value = isEnabled
    }

    // High fidelity simulate alerts trigger
    fun simulateNotification(
        title: String,
        message: String,
        platform: String,
        type: String,
        appName: String
    ) {
        viewModelScope.launch {
            val simulated = NotificationEntity(
                title = title,
                message = message,
                appName = appName,
                platform = platform,
                type = type,
                timestamp = System.currentTimeMillis()
            )
            val id = repository.insert(simulated)
            _activeIslandNotification.value = simulated.copy(id = id)
            _isIslandExpanded.value = false
        }
    }
}

class FluidInboxViewModelFactory(private val repository: NotificationRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(FluidInboxViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return FluidInboxViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
