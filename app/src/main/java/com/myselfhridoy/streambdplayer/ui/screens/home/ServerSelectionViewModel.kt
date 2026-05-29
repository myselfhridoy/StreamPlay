package com.myselfhridoy.streambdplayer.ui.screens.home

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myselfhridoy.streambdplayer.utils.AddonManager
import com.myselfhridoy.streambdplayer.utils.StreamSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ServerSelectionViewModel : ViewModel() {
    private val _streamSources = MutableStateFlow<List<StreamSource>>(emptyList())
    val streamSources: StateFlow<List<StreamSource>> = _streamSources

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    private var hasFetched = false

    fun fetchSources(
        context: Context,
        tmdbId: String,
        type: String,
        season: String?,
        episode: String?
    ) {
        if (hasFetched) return

        val tmdbIdInt = tmdbId.toIntOrNull() ?: return
        
        viewModelScope.launch {
            _isLoading.value = true
            _streamSources.value = AddonManager.resolveFromAddons(
                context, 
                type, 
                tmdbIdInt, 
                season?.toIntOrNull(), 
                episode?.toIntOrNull()
            )
            _isLoading.value = false
            hasFetched = true
        }
    }
}
