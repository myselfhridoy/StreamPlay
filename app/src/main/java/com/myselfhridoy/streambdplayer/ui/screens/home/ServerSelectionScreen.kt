package com.myselfhridoy.streambdplayer.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.myselfhridoy.streambdplayer.ui.theme.BackgroundDark
import com.myselfhridoy.streambdplayer.utils.AddonManager
import com.myselfhridoy.streambdplayer.utils.StreamSource
import com.google.gson.Gson
import kotlinx.coroutines.launch
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServerSelectionScreen(
    navController: NavController,
    tmdbId: String,
    type: String,
    title: String,
    season: String? = null,
    episode: String? = null,
    viewModel: ServerSelectionViewModel = viewModel()
) {
    val context = LocalContext.current
    val sourcesLoading by viewModel.isLoading.collectAsState()
    val streamSources by viewModel.streamSources.collectAsState()

    LaunchedEffect(tmdbId, type, season, episode) {
        viewModel.fetchSources(context, tmdbId, type, season, episode)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Select Server: $title", color = Color.White, fontSize = 18.sp, maxLines = 1) },
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
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            if (sourcesLoading && streamSources.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = Color(0xFFE50914))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Fetching servers from addons...", color = Color.White)
                    }
                }
            } else if (!sourcesLoading && streamSources.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No sources found. Make sure you have addons enabled.", color = Color.Gray)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(streamSources) { source ->
                        Button(
                            onClick = {
                                val encodedUrl = android.net.Uri.encode(source.url)
                                val encodedTitle = android.net.Uri.encode(title)
                                val headersJson = android.net.Uri.encode(Gson().toJson(source.headers ?: emptyMap<String, String>()))
                                val streamType = android.net.Uri.encode(source.type ?: "unknown")
                                navController.navigate("player?url=$encodedUrl&title=$encodedTitle&isVod=true&headers=$headersJson&streamType=$streamType")
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2A2A3A), contentColor = Color.White),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(64.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(source.provider, fontWeight = FontWeight.Bold, fontSize = 16.sp, maxLines = 1)
                                    Text(source.type?.uppercase() ?: "STREAM", fontSize = 12.sp, color = Color.Gray)
                                }
                                Box(
                                    modifier = Modifier
                                        .background(Color(0xFFE50914), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(source.quality, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                    
                    if (sourcesLoading) {
                        item {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    CircularProgressIndicator(color = Color(0xFFE50914), modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Fetching more...", color = Color.Gray, fontSize = 14.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
