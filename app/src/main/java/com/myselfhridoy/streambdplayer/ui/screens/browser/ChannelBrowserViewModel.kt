package com.myselfhridoy.streambdplayer.ui.screens.browser

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.myselfhridoy.streambdplayer.data.models.Channel
import com.myselfhridoy.streambdplayer.utils.M3UParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URL

class ChannelBrowserViewModel(application: Application) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow<BrowserUiState>(BrowserUiState.Loading)
    val uiState: StateFlow<BrowserUiState> = _uiState

    private val _categories = MutableStateFlow<List<String>>(listOf("All"))
    val categories: StateFlow<List<String>> = _categories

    private val _selectedCategory = MutableStateFlow("All")
    val selectedCategory: StateFlow<String> = _selectedCategory

    private var allChannels: List<Channel> = emptyList()

    fun loadPlaylist(url: String, isLocal: Boolean) {
        viewModelScope.launch {
            _uiState.value = BrowserUiState.Loading
            try {
                val content = withContext(Dispatchers.IO) {
                    if (isLocal) {
                        val uri = Uri.parse(url)
                        getApplication<Application>().contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } ?: ""
                    } else {
                        URL(url).readText()
                    }
                }
                val parsed = M3UParser.parseM3U(content)
                allChannels = parsed
                
                val uniqueCats = parsed.map { it.group }.distinct().sorted()
                _categories.value = listOf("All") + uniqueCats
                
                updateFilteredChannels()
            } catch (e: Exception) {
                _uiState.value = BrowserUiState.Error(e.message ?: "Failed to load playlist")
            }
        }
    }

    fun selectCategory(category: String) {
        _selectedCategory.value = category
        updateFilteredChannels()
    }

    private fun updateFilteredChannels() {
        val filtered = if (_selectedCategory.value == "All") {
            allChannels
        } else {
            allChannels.filter { it.group == _selectedCategory.value }
        }
        _uiState.value = BrowserUiState.Success(filtered)
    }
}

sealed class BrowserUiState {
    object Loading : BrowserUiState()
    data class Success(val channels: List<Channel>) : BrowserUiState()
    data class Error(val message: String) : BrowserUiState()
}
