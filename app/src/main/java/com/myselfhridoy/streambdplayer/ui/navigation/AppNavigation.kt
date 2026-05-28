package com.myselfhridoy.streambdplayer.ui.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.myselfhridoy.streambdplayer.ui.screens.home.HomeScreen
import com.myselfhridoy.streambdplayer.ui.screens.player.PlayerScreen
import com.myselfhridoy.streambdplayer.utils.DeviceUtils

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val isTv = DeviceUtils.isTv(context)

    NavHost(
        navController = navController,
        startDestination = "home",
        modifier = Modifier.fillMaxSize()
    ) {
        composable("home") {
            HomeScreen(navController = navController, isTv = isTv)
        }
        composable("player") {
            PlayerScreen(navController = navController)
        }
    }
}
