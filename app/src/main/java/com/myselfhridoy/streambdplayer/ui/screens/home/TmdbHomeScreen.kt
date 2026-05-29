package com.myselfhridoy.streambdplayer.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.myselfhridoy.streambdplayer.data.models.Category
import com.myselfhridoy.streambdplayer.data.models.MediaItem
import com.myselfhridoy.streambdplayer.data.remote.TmdbApi
import com.myselfhridoy.streambdplayer.ui.theme.BackgroundDark
import com.myselfhridoy.streambdplayer.ui.theme.PrimaryColor
import com.myselfhridoy.streambdplayer.ui.theme.SurfaceDark
import kotlinx.coroutines.delay
import com.google.gson.Gson

@Composable
fun TmdbHomeScreen(
    navController: NavController,
    viewModel: TmdbHomeViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Box(modifier = Modifier.fillMaxSize().background(BackgroundDark)) {
        when (val state = uiState) {
            is TmdbUiState.Loading -> {
                CircularProgressIndicator(
                    color = PrimaryColor,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            is TmdbUiState.Error -> {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = state.message, color = Color.Red, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { viewModel.fetchData() }, colors = ButtonDefaults.buttonColors(containerColor = PrimaryColor)) {
                        Text("Retry")
                    }
                }
            }
            is TmdbUiState.Success -> {
                TmdbHomeContent(
                    categories = state.categories,
                    heroItems = state.heroItems,
                    navController = navController
                )
            }
        }
    }
}

@Composable
fun TmdbHomeContent(
    categories: List<Category>,
    heroItems: List<MediaItem>,
    navController: NavController
) {
    var heroIndex by remember { mutableStateOf(0) }

    LaunchedEffect(heroItems) {
        if (heroItems.isNotEmpty()) {
            while (true) {
                delay(6000)
                heroIndex = (heroIndex + 1) % heroItems.size
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 100.dp) // Space for bottom nav
        ) {
            // Hero Section
        item {
            if (heroItems.isNotEmpty()) {
                val heroItem = heroItems[heroIndex]
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 12f)
                ) {
                    AsyncImage(
                        model = "${TmdbApi.HERO_IMAGE_URL}${heroItem.backdropPath ?: heroItem.posterPath}",
                        contentDescription = heroItem.title ?: heroItem.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    
                    // Gradient overlay
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color(0x88000000),
                                        BackgroundDark
                                    ),
                                    startY = 100f
                                )
                            )
                    )

                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(24.dp)
                    ) {
                        Text(
                            text = heroItem.title ?: heroItem.name ?: "Unknown",
                            color = Color.White,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Button(
                                onClick = {
                                    val itemJson = android.net.Uri.encode(Gson().toJson(heroItem))
                                    navController.navigate("tmdbDetails?item=$itemJson")
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = "Play")
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Play / Details", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // Categories
        items(categories) { category ->
            if (category.items.isNotEmpty()) {
                Column(modifier = Modifier.padding(vertical = 12.dp)) {
                    Text(
                        text = category.title,
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(category.items) { item ->
                            MediaCard(item = item, onClick = {
                                val itemJson = android.net.Uri.encode(Gson().toJson(item))
                                navController.navigate("tmdbDetails?item=$itemJson")
                            })
                        }
                    }
                }
            }
        }
        
        // Floating Search Button (Netflix Style)
        IconButton(
            onClick = { navController.navigate("search") },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 16.dp, end = 16.dp)
                .clip(androidx.compose.foundation.shape.CircleShape)
                .background(Color(0x88000000))
        ) {
            Icon(Icons.Default.Search, contentDescription = "Search", tint = Color.White)
        }
    }
}

@Composable
fun MediaCard(item: MediaItem, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .width(120.dp)
            .height(180.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(SurfaceDark)
            .clickable { onClick() }
    ) {
        AsyncImage(
            model = "${TmdbApi.IMAGE_BASE_URL}${item.posterPath}",
            contentDescription = item.title ?: item.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        
        // Rating Badge
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(6.dp)
                .background(Color(0x99000000), RoundedCornerShape(6.dp))
                .padding(horizontal = 4.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Star, contentDescription = "Rating", tint = Color(0xFFF59E0B), modifier = Modifier.size(12.dp))
            Spacer(modifier = Modifier.width(2.dp))
            Text(
                text = String.format("%.1f", item.voteAverage),
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
