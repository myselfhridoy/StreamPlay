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
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

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
                        val client = OkHttpClient.Builder()
                            .connectTimeout(15, TimeUnit.SECONDS)
                            .readTimeout(15, TimeUnit.SECONDS)
                            .build()
                        val req = Request.Builder()
                            .url(url)
                            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                            .build()
                        val res = client.newCall(req).execute()
                        res.body?.string() ?: throw Exception("Empty response from URL")
                    }
                }
                val parsed = M3UParser.parsePlaylist(content)
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
