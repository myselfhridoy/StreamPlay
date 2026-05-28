package com.myselfhridoy.streambdplayer.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavController
import com.myselfhridoy.streambdplayer.ui.theme.BackgroundDark
import com.myselfhridoy.streambdplayer.ui.theme.SurfaceDark

data class NavItem(val title: String, val icon: ImageVector)

val navItems = listOf(
    NavItem("Home", Icons.Default.Home),
    NavItem("Channels", Icons.Default.List),
    NavItem("Search", Icons.Default.Search),
    NavItem("Settings", Icons.Default.Settings)
)

@Composable
fun MobileHomeScreen(navController: NavController) {
    var selectedItem by remember { mutableIntStateOf(0) }

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = SurfaceDark
            ) {
                navItems.forEachIndexed { index, item ->
                    NavigationBarItem(
                        icon = { Icon(item.icon, contentDescription = item.title) },
                        label = { Text(item.title) },
                        selected = selectedItem == index,
                        onClick = { selectedItem = index }
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .background(BackgroundDark)
        ) {
            // Content based on selectedItem
            // 0 -> Channels list
            // 1 -> Playlists
            // 2 -> Search
            // 3 -> Settings
            Text(text = "Mobile View: \${navItems[selectedItem].title}", color = androidx.compose.ui.graphics.Color.White)
        }
    }
}
