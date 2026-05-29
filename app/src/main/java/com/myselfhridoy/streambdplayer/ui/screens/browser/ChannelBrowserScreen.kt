package com.myselfhridoy.streambdplayer.ui.screens.browser

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.myselfhridoy.streambdplayer.data.models.Channel
import com.myselfhridoy.streambdplayer.ui.theme.BackgroundDark
import com.myselfhridoy.streambdplayer.ui.theme.SurfaceDark
import com.myselfhridoy.streambdplayer.utils.TokenParser
import com.myselfhridoy.streambdplayer.utils.PlaylistManager
import com.google.gson.Gson
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChannelBrowserScreen(
    navController: NavController,
    playlistUrl: String,
    playlistName: String,
    isLocal: Boolean = false,
    viewModel: ChannelBrowserViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    
    val coroutineScope = rememberCoroutineScope()
    var isResolving by remember { mutableStateOf(false) }

    LaunchedEffect(playlistUrl) {
        viewModel.loadPlaylist(playlistUrl, isLocal)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(playlistName, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundDark)
            )
        },
        containerColor = BackgroundDark
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            
            // Categories Bar
            if (categories.size > 1) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(categories) { category ->
                        CategoryChip(
                            text = category,
                            isSelected = category == selectedCategory,
                            onClick = { viewModel.selectCategory(category) }
                        )
                    }
                }
            }

            // Main Content
            when (val state = uiState) {
                is BrowserUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color(0xFFE50914))
                    }
                }
                is BrowserUiState.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(text = state.message, color = Color.Red)
                    }
                }
                is BrowserUiState.Success -> {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 100.dp),
                        contentPadding = PaddingValues(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(state.channels) { channel ->
                            ChannelItem(channel = channel, onClick = {
                                if (isResolving) return@ChannelItem
                                
                                PlaylistManager.currentPlaylist = state.channels
                                PlaylistManager.currentIndex = state.channels.indexOf(channel)
                                
                                val encodedTitle = android.net.Uri.encode(channel.name)
                                
                                val baseHeaders = mutableMapOf<String, String>()
                                channel.userAgent?.let { baseHeaders["User-Agent"] = it }
                                channel.httpReferer?.let { baseHeaders["Referer"] = it }
                                channel.origin?.let { baseHeaders["Origin"] = it }
                                channel.cookie?.let { baseHeaders["Cookie"] = it }

                                if (!channel.tokenUrl.isNullOrEmpty()) {
                                    isResolving = true
                                    coroutineScope.launch {
                                        val resolved = TokenParser.resolveTokenForUrl(
                                            baseUrl = channel.url,
                                            tokenUrl = channel.tokenUrl,
                                            tokenId = channel.tokenId?.toString(),
                                            headers = baseHeaders,
                                            tokenMatch = channel.tokenMatch,
                                            tokenReplace = channel.tokenReplace
                                        )
                                        isResolving = false
                                        
                                        val finalUrl = android.net.Uri.encode(resolved?.url ?: channel.url)
                                        val headersJson = android.net.Uri.encode(Gson().toJson(resolved?.headers ?: baseHeaders))
                                        
                                        val drmType = resolved?.drm?.type ?: channel.drm?.type
                                        val drmLicense = resolved?.drm?.licenseServer ?: channel.drm?.licenseServer
                                        val rawKeyPair = resolved?.drm?.rawKeyPair ?: channel.drm?.rawKeyPair
                                        var route = "player?url=$finalUrl&title=$encodedTitle&headers=$headersJson"
                                        
                                        if (drmType == "widevine" && !drmLicense.isNullOrEmpty()) {
                                            val drmUrlEnc = android.net.Uri.encode(drmLicense)
                                            route += "&drmLicenseUrl=$drmUrlEnc&drmSchemeUuid=edef8ba9-79d6-4ace-a3c8-27dcd51d21ed"
                                        } else if (drmType == "clearkey") {
                                            val license = rawKeyPair ?: drmLicense
                                            if (!license.isNullOrEmpty()) {
                                                val drmUrlEnc = android.net.Uri.encode(license)
                                                route += "&drmLicenseUrl=$drmUrlEnc&drmSchemeUuid=e2719d58-a985-b3c9-781a-b030af78d30e"
                                            }
                                        }
                                        
                                        navController.navigate(route)
                                    }
                                } else {
                                    val finalUrl = android.net.Uri.encode(channel.url)
                                    val headersJson = android.net.Uri.encode(Gson().toJson(baseHeaders))
                                    var route = "player?url=$finalUrl&title=$encodedTitle&headers=$headersJson"
                                    
                                    if (channel.drm?.type == "widevine" && !channel.drm.licenseServer.isNullOrEmpty()) {
                                        val drmUrlEnc = android.net.Uri.encode(channel.drm.licenseServer)
                                        route += "&drmLicenseUrl=$drmUrlEnc&drmSchemeUuid=edef8ba9-79d6-4ace-a3c8-27dcd51d21ed"
                                    } else if (channel.drm?.type == "clearkey") {
                                        val license = channel.drm.rawKeyPair ?: channel.drm.licenseServer
                                        if (!license.isNullOrEmpty()) {
                                            val drmUrlEnc = android.net.Uri.encode(license)
                                            route += "&drmLicenseUrl=$drmUrlEnc&drmSchemeUuid=e2719d58-a985-b3c9-781a-b030af78d30e"
                                        }
                                    }
                                    
                                    navController.navigate(route)
                                }
                            })
                        }
                    }
                }
            }
        }
        
        if (isResolving) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.7f))
                    .clickable(enabled = false) {},
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = Color(0xFFE50914))
                    Text("Resolving Stream...", color = Color.White, modifier = Modifier.padding(top = 16.dp))
                }
            }
        }
    }
}

@Composable
fun CategoryChip(text: String, isSelected: Boolean, onClick: () -> Unit) {
    Surface(
        color = if (isSelected) Color.White else SurfaceDark,
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.clickable { onClick() }
    ) {
        Text(
            text = text,
            color = if (isSelected) Color.Black else Color.White,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }
}

@Composable
fun ChannelItem(channel: Channel, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(16.dp))
                .background(SurfaceDark),
            contentAlignment = Alignment.Center
        ) {
            if (channel.logo.isNotEmpty()) {
                AsyncImage(
                    model = channel.logo,
                    contentDescription = channel.name,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.size(60.dp)
                )
            } else {
                Text(
                    text = channel.name.take(2).uppercase(),
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        Text(
            text = channel.name,
            color = Color.White,
            fontSize = 12.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}
