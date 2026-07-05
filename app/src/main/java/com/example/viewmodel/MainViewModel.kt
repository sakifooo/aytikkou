package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ChannelEntity
import com.example.data.ChannelRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed class Screen {
    object Home : Screen()
    object LiveMatches : Screen()
    object Countries : Screen()
    object Favorites : Screen()
    object Search : Screen()
    object Settings : Screen()
    data class CountryDetail(val countryName: String) : Screen()
    data class Player(val channel: ChannelEntity) : Screen()
}

data class CountryItem(
    val name: String,
    val flag: String,
    val channelCount: Int
)

data class AppState(
    val currentScreen: Screen = Screen.Home,
    val backstack: List<Screen> = listOf(Screen.Home),
    val allChannels: List<ChannelEntity> = emptyList(),
    val favorites: List<ChannelEntity> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = true,
    val importMessage: String? = null,
    val selectedChannel: ChannelEntity? = null
)

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = ChannelRepository(application)
    
    private val _state = MutableStateFlow(AppState())
    val state: StateFlow<AppState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            repository.initializeIfNeeded()
            
            // Collect all channels
            launch {
                repository.allChannels.collect { channels ->
                    _state.update { it.copy(allChannels = channels, isLoading = false) }
                }
            }

            // Collect favorites
            launch {
                repository.favorites.collect { favs ->
                    _state.update { it.copy(favorites = favs) }
                }
            }
        }
    }

    fun navigateTo(screen: Screen) {
        _state.update { currentState ->
            val newBackstack = currentState.backstack.toMutableList()
            if (newBackstack.lastOrNull() != screen) {
                newBackstack.add(screen)
            }
            currentState.copy(
                currentScreen = screen,
                backstack = newBackstack
            )
        }
    }

    fun navigateBack(): Boolean {
        var handled = false
        _state.update { currentState ->
            val newBackstack = currentState.backstack.toMutableList()
            if (newBackstack.size > 1) {
                newBackstack.removeAt(newBackstack.size - 1)
                val prevScreen = newBackstack.last()
                handled = true
                currentState.copy(
                    currentScreen = prevScreen,
                    backstack = newBackstack
                )
            } else {
                currentState
            }
        }
        return handled
    }

    fun updateSearchQuery(query: String) {
        _state.update { it.copy(searchQuery = query) }
    }

    fun toggleFavorite(channel: ChannelEntity) {
        viewModelScope.launch {
            repository.toggleFavorite(channel.id, !channel.isFavorite)
        }
    }

    fun selectChannel(channel: ChannelEntity) {
        _state.update { it.copy(selectedChannel = channel) }
        navigateTo(Screen.Player(channel))
    }

    fun clearCache() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            repository.clearCache()
            showImportMessage("Cache cleared. Re-loaded default channels.")
        }
    }

    fun importM3U(content: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            val result = repository.importM3UContent(content)
            _state.update { it.copy(isLoading = false) }
            result.onSuccess { count ->
                showImportMessage("Successfully imported $count channels from M3U!")
            }
            result.onFailure { error ->
                showImportMessage("Failed to import M3U: ${error.message}")
            }
        }
    }

    fun importJSON(content: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            val result = repository.importJSONContent(content)
            _state.update { it.copy(isLoading = false) }
            result.onSuccess { count ->
                showImportMessage("Successfully imported $count channels from JSON!")
            }
            result.onFailure { error ->
                showImportMessage("Failed to import JSON: ${error.message}")
            }
        }
    }

    fun importFromUrl(url: String, format: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            val result = repository.importFromUrl(url, format)
            _state.update { it.copy(isLoading = false) }
            result.onSuccess { count ->
                showImportMessage("Successfully imported $count channels from link!")
            }
            result.onFailure { error ->
                showImportMessage("Failed loading URL: ${error.message}")
            }
        }
    }

    fun dismissImportMessage() {
        _state.update { it.copy(importMessage = null) }
    }

    private fun showImportMessage(msg: String) {
        _state.update { it.copy(importMessage = msg) }
    }

    // Helper functions for categories and grouping
    fun getCountries(): List<CountryItem> {
        val channels = _state.value.allChannels.filter { it.country != "Live Matches" }
        val counts = channels.groupBy { it.country.lowercase().trim() }
        
        val arabicCountries = listOf(
            "Morocco" to "🇲🇦",
            "Algeria" to "🇩🇿",
            "Tunisia" to "🇹🇳",
            "Libya" to "🇱🇾",
            "Egypt" to "🇪🇬",
            "Saudi Arabia" to "🇸🇦",
            "United Arab Emirates" to "🇦🇪",
            "Qatar" to "🇶🇦",
            "Kuwait" to "🇰🇼",
            "Bahrain" to "🇧🇭",
            "Oman" to "🇴🇲",
            "Jordan" to "🇯🇴",
            "Palestine" to "🇵🇸",
            "Lebanon" to "🇱🇧",
            "Syria" to "🇸🇾",
            "Iraq" to "🇮🇶",
            "Yemen" to "🇾🇪",
            "Sudan" to "🇸🇩",
            "Mauritania" to "🇲🇷"
        )

        return arabicCountries.map { (name, flag) ->
            val count = counts[name.lowercase().trim()]?.size ?: 0
            CountryItem(
                name = name,
                flag = flag,
                channelCount = count
            )
        }
    }

    fun getLiveMatches(): List<ChannelEntity> {
        // Find channels belonging to "Live Matches" or category "Sports" / starting with live tag
        return _state.value.allChannels.filter { 
            it.country.lowercase() == "live matches" || 
            it.category.lowercase() == "sports" ||
            it.name.startsWith("🔴")
        }
    }

    fun getCountryEmojiFlag(countryName: String): String {
        return when (countryName.lowercase().trim()) {
            "morocco", "ma", "maroc" -> "🇲🇦"
            "algeria", "dz" -> "🇩🇿"
            "tunisia", "tn" -> "🇹🇳"
            "libya", "ly" -> "🇱🇾"
            "egypt", "eg" -> "🇪🇬"
            "saudi arabia", "sa" -> "🇸🇦"
            "united arab Emirates", "ae", "uae" -> "🇦🇪"
            "qatar", "qa" -> "🇶🇦"
            "kuwait", "kw" -> "🇰🇼"
            "bahrain", "bh" -> "🇧🇭"
            "oman", "om" -> "🇴🇲"
            "jordan", "jo" -> "🇯🇴"
            "palestine", "ps" -> "🇵🇸"
            "lebanon", "lb" -> "🇱🇧"
            "syria", "sy" -> "🇸🇾"
            "iraq", "iq" -> "🇮🇶"
            "yemen", "ye" -> "🇾🇪"
            "sudan", "sd" -> "🇸🇩"
            "mauritania", "mr" -> "🇲🇷"
            "live matches" -> "🔴"
            else -> "🏳️"
        }
    }
}
