package com.myselfhridoy.streambdplayer.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.myselfhridoy.streambdplayer.data.models.MediaItem
import com.myselfhridoy.streambdplayer.data.remote.TmdbApi
import com.myselfhridoy.streambdplayer.ui.theme.BackgroundDark
import com.myselfhridoy.streambdplayer.utils.AddonManager
import com.myselfhridoy.streambdplayer.utils.StreamSource
import com.google.gson.Gson
import kotlinx.coroutines.launch
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TmdbDetailsScreen(navController: NavController, mediaItemJson: String) {
    val context = LocalContext.current
    val item = try {
        Gson().fromJson(mediaItemJson, MediaItem::class.java)
    } catch (e: Exception) {
        null
    }

    var tvDetails by androidx.compose.runtime.remember { mutableStateOf<com.myselfhridoy.streambdplayer.data.models.TvDetailsResponse?>(null) }
    var selectedSeason by androidx.compose.runtime.remember { mutableStateOf<com.myselfhridoy.streambdplayer.data.models.Season?>(null) }
    var seasonEpisodes by androidx.compose.runtime.remember { mutableStateOf<List<com.myselfhridoy.streambdplayer.data.models.Episode>>(emptyList()) }
    var selectedEpisode by androidx.compose.runtime.remember { mutableStateOf<com.myselfhridoy.streambdplayer.data.models.Episode?>(null) }

    androidx.compose.runtime.LaunchedEffect(item?.id) {
        if (item != null && item.name != null) {
            try {
                tvDetails = com.myselfhridoy.streambdplayer.data.remote.NetworkClient.tmdbApi.getTvDetails(item.id)
                if (tvDetails?.seasons?.isNotEmpty() == true) {
                    selectedSeason = tvDetails?.seasons?.firstOrNull { it.seasonNumber > 0 } ?: tvDetails?.seasons?.first()
                }
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    androidx.compose.runtime.LaunchedEffect(selectedSeason) {
        if (item != null && selectedSeason != null) {
            try {
                val response = com.myselfhridoy.streambdplayer.data.remote.NetworkClient.tmdbApi.getTvSeasonDetails(item.id, selectedSeason!!.seasonNumber)
                seasonEpisodes = response.episodes
                if (seasonEpisodes.isNotEmpty()) selectedEpisode = seasonEpisodes.first()
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    if (item == null) {
        Box(modifier = Modifier.fillMaxSize().background(BackgroundDark), contentAlignment = Alignment.Center) {
            Text("Error loading details", color = Color.White)
            Button(onClick = { navController.popBackStack() }, modifier = Modifier.padding(top = 16.dp)) {
                Text("Go Back")
            }
        }
        return
    }

    Scaffold(
        containerColor = BackgroundDark,
        topBar = {
            TopAppBar(
                title = { Text("") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundDark)
                .verticalScroll(rememberScrollState())
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
            ) {
                AsyncImage(
                    model = "${TmdbApi.HERO_IMAGE_URL}${item.backdropPath ?: item.posterPath}",
                    contentDescription = item.title ?: item.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, BackgroundDark),
                                startY = 100f
                            )
                        )
                )
            }

            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = item.title ?: item.name ?: "Unknown",
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Star, contentDescription = "Rating", tint = Color(0xFFF59E0B), modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = String.format("%.1f", item.voteAverage),
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
                
                Button(
                    onClick = {
                        val dummyUrl = android.net.Uri.encode("https://storage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4")
                        val dummyTitle = android.net.Uri.encode(item.title ?: item.name ?: "Trailer")
                        navController.navigate("player?url=$dummyUrl&title=$dummyTitle")
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray, contentColor = Color.White),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = "Trailer")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Play Trailer", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (tvDetails != null) {
                    // Season Selector
                    Text("Seasons", color = Color.White, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        androidx.compose.foundation.lazy.items(tvDetails!!.seasons.filter { it.seasonNumber > 0 }) { season ->
                            val isSelected = selectedSeason?.seasonNumber == season.seasonNumber
                            Button(
                                onClick = { selectedSeason = season },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSelected) Color(0xFFE50914) else Color(0xFF2A2A3A),
                                    contentColor = Color.White
                                )
                            ) {
                                Text("S${season.seasonNumber}")
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Episode Selector
                    if (seasonEpisodes.isNotEmpty()) {
                        Text("Episodes", color = Color.White, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            androidx.compose.foundation.lazy.items(seasonEpisodes) { episode ->
                                val isSelected = selectedEpisode?.episodeNumber == episode.episodeNumber
                                Button(
                                    onClick = { selectedEpisode = episode },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isSelected) Color(0xFFE50914) else Color(0xFF2A2A3A),
                                        contentColor = Color.White
                                    )
                                ) {
                                    Text("E${episode.episodeNumber}")
                                }
                            }
                        }
                        
                        selectedEpisode?.let { ep ->
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(ep.name, color = Color.White, fontWeight = FontWeight.Bold)
                            Text(ep.overview, color = Color.Gray, fontSize = 12.sp, maxLines = 2)
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }

                Button(
                    onClick = {
                        val type = if (item.name != null) "tv" else "movie"
                        var encodedTitle = android.net.Uri.encode(item.title ?: item.name ?: "Unknown")
                        var route = "selectServer?tmdbId=${item.id}&type=$type&title=$encodedTitle"
                        
                        if (type == "tv" && selectedSeason != null && selectedEpisode != null) {
                            encodedTitle = android.net.Uri.encode("${item.name} S${selectedSeason!!.seasonNumber} E${selectedEpisode!!.episodeNumber}")
                            route += "&season=${selectedSeason!!.seasonNumber}&episode=${selectedEpisode!!.episodeNumber}"
                        }
                        
                        navController.navigate(route)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE50914), contentColor = Color.White),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = "Play")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Play Now", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Overview",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = item.overview,
                    color = Color.LightGray,
                    fontSize = 16.sp,
                    lineHeight = 24.sp
                )
                Spacer(modifier = Modifier.height(100.dp))
            }
        }
        // Removed ModalBottomSheet
    }
}
