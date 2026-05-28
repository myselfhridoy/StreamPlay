package com.myselfhridoy.streambdplayer.ui.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.NavType
import com.myselfhridoy.streambdplayer.data.local.PreferencesManager
import com.myselfhridoy.streambdplayer.ui.screens.browser.ChannelBrowserScreen
import com.myselfhridoy.streambdplayer.ui.screens.home.HomeScreen
import com.myselfhridoy.streambdplayer.ui.screens.onboarding.OnboardingScreen
import com.myselfhridoy.streambdplayer.ui.screens.player.PlayerScreen
import com.myselfhridoy.streambdplayer.utils.DeviceUtils

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val isTv = DeviceUtils.isTv(context)
    val preferencesManager = PreferencesManager(context)
    
    val hasSeenOnboarding by preferencesManager.hasSeenOnboardingFlow.collectAsState(initial = null)

    if (hasSeenOnboarding == null) {
        return // Loading state
    }

    val startDest = if (hasSeenOnboarding == true) "home" else "onboarding"

    NavHost(
        navController = navController,
        startDestination = startDest,
        modifier = Modifier.fillMaxSize()
    ) {
        composable("onboarding") {
            OnboardingScreen(navController = navController)
        }
        composable("home") {
            HomeScreen(navController = navController, isTv = isTv)
        }
        composable(
            route = "channelBrowser?url={url}&name={name}",
            arguments = listOf(
                navArgument("url") { type = NavType.StringType; defaultValue = "" },
                navArgument("name") { type = NavType.StringType; defaultValue = "Playlist" }
            )
        ) { backStackEntry ->
            val url = backStackEntry.arguments?.getString("url") ?: ""
            val name = backStackEntry.arguments?.getString("name") ?: "Playlist"
            ChannelBrowserScreen(
                navController = navController,
                playlistUrl = url,
                playlistName = name
            )
        }
        composable("player?url={url}") { backStackEntry ->
            PlayerScreen(navController = navController)
        }
    }
}
