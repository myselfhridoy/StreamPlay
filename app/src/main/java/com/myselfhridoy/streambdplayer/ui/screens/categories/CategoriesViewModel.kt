package com.myselfhridoy.streambdplayer.ui.screens.categories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myselfhridoy.streambdplayer.data.remote.IptvPlaylist
import com.myselfhridoy.streambdplayer.data.remote.NetworkClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class CategoriesUiState {
    object Loading : CategoriesUiState()
    data class Success(val categories: List<CategoryItem>) : CategoriesUiState()
    data class Error(val message: String) : CategoriesUiState()
}

data class CategoryItem(
    val id: String,
    val name: String,
    val url: String,
    val logo: String
)

class CategoriesViewModel : ViewModel() {
    private val _uiState = MutableStateFlow<CategoriesUiState>(CategoriesUiState.Loading)
    val uiState: StateFlow<CategoriesUiState> = _uiState.asStateFlow()

    init {
        fetchData()
    }

    fun fetchData() {
        viewModelScope.launch {
            _uiState.value = CategoriesUiState.Loading
            try {
                val response = NetworkClient.iptvApi.getCategories()
                val parsedData = mutableListOf<CategoryItem>()

                if (response.isNotEmpty() && response[0].playlists.isNotEmpty()) {
                    val playlistsObj = response[0].playlists
                    playlistsObj.forEach { (key, value) ->
                        parsedData.add(
                            CategoryItem(
                                id = key,
                                name = key,
                                url = value.url,
                                logo = value.logo
                            )
                        )
                    }
                }
                _uiState.value = CategoriesUiState.Success(parsedData)
            } catch (e: Exception) {
                _uiState.value = CategoriesUiState.Error(e.message ?: "Failed to load categories")
            }
        }
    }
}
