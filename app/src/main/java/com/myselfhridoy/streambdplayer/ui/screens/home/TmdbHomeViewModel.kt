package com.myselfhridoy.streambdplayer.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myselfhridoy.streambdplayer.data.models.Category
import com.myselfhridoy.streambdplayer.data.models.MediaItem
import com.myselfhridoy.streambdplayer.data.remote.NetworkClient
import com.myselfhridoy.streambdplayer.data.remote.TmdbApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class TmdbUiState {
    object Loading : TmdbUiState()
    data class Success(val categories: List<Category>, val heroItems: List<MediaItem>) : TmdbUiState()
    data class Error(val message: String) : TmdbUiState()
}

class TmdbHomeViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<TmdbUiState>(TmdbUiState.Loading)
    val uiState: StateFlow<TmdbUiState> = _uiState.asStateFlow()

    private val predefinedCategories = listOf(
        Category(
            title = "Trending Bollywood",
            url = "discover/movie?with_original_language=hi&sort_by=popularity.desc&api_key=\${TmdbApi.API_KEY}",
            type = "movie"
        ),
        Category(
            title = "Blockbuster South Indian",
            url = "discover/movie?with_original_language=te|ta|ml|kn&sort_by=popularity.desc&api_key=\${TmdbApi.API_KEY}",
            type = "movie"
        ),
        Category(
            title = "Bangla Hits",
            url = "discover/movie?with_original_language=bn&sort_by=popularity.desc&api_key=\${TmdbApi.API_KEY}",
            type = "movie"
        ),
        Category(
            title = "Popular Series",
            url = "discover/tv?with_original_language=hi|bn|te|ta|en&sort_by=popularity.desc&api_key=\${TmdbApi.API_KEY}",
            type = "tv"
        )
    )

    init {
        fetchData()
    }

    fun fetchData() {
        viewModelScope.launch {
            _uiState.value = TmdbUiState.Loading
            try {
                val fetchedCategories = mutableListOf<Category>()
                val heroPool = mutableListOf<MediaItem>()

                for (cat in predefinedCategories) {
                    // We must replace the placeholder with the actual API key string since Retrofit @Url does not process placeholders
                    val actualUrl = cat.url.replace("\${TmdbApi.API_KEY}", TmdbApi.API_KEY)
                    val response = NetworkClient.tmdbApi.getCategoryItems(actualUrl)
                    val items = response.results.map { it.copy(mediaType = cat.type) }
                    
                    fetchedCategories.add(cat.copy(items = items))

                    // Add items with backdrop to hero pool (max 3 per category)
                    val validHeroes = items.filter { it.backdropPath != null }.take(3)
                    heroPool.addAll(validHeroes)
                }

                _uiState.value = TmdbUiState.Success(fetchedCategories, heroPool)
            } catch (e: Exception) {
                _uiState.value = TmdbUiState.Error(e.message ?: "Failed to fetch data")
            }
        }
    }
}
