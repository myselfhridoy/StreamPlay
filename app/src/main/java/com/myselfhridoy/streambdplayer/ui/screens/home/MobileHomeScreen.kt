package com.myselfhridoy.streambdplayer.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
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
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.myselfhridoy.streambdplayer.ui.theme.BackgroundDark
import com.myselfhridoy.streambdplayer.ui.theme.SurfaceDark

import com.myselfhridoy.streambdplayer.ui.screens.network.NetworkScreen
import com.myselfhridoy.streambdplayer.ui.screens.favorites.FavoritesScreen
import com.myselfhridoy.streambdplayer.ui.screens.home.TmdbHomeScreen
import com.myselfhridoy.streambdplayer.ui.screens.categories.CategoriesScreen

data class NavItem(val title: String, val icon: ImageVector)

val navItems = listOf(
    NavItem("Home", Icons.Default.Home),
    NavItem("Categories", Icons.Default.Dashboard),
    NavItem("Network", Icons.Default.CloudDownload),
    NavItem("Favorites", Icons.Default.Star),
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
            when (selectedItem) {
                0 -> TmdbHomeScreen(navController)
                1 -> CategoriesScreen(navController)
                2 -> NetworkScreen(navController)
                3 -> FavoritesScreen(navController)
                4 -> Text(text = "Settings Coming Soon", color = androidx.compose.ui.graphics.Color.White, modifier = Modifier.padding(16.dp))
            }
        }
    }
}
