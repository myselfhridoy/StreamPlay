package com.myselfhridoy.streambdplayer.ui.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
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
import com.myselfhridoy.streambdplayer.ui.screens.history.HistoryScreen
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
        modifier = Modifier.fillMaxSize(),
        enterTransition = { fadeIn(animationSpec = tween(300)) },
        exitTransition = { fadeOut(animationSpec = tween(300)) },
        popEnterTransition = { fadeIn(animationSpec = tween(300)) },
        popExitTransition = { fadeOut(animationSpec = tween(300)) }
    ) {
        composable("onboarding") {
            OnboardingScreen(navController = navController)
        }
        composable("home") {
            HomeScreen(navController = navController, isTv = isTv)
        }
        composable(
            route = "channelBrowser?url={url}&name={name}&isLocal={isLocal}",
            arguments = listOf(
                navArgument("url") { type = NavType.StringType; defaultValue = "" },
                navArgument("name") { type = NavType.StringType; defaultValue = "Playlist" },
                navArgument("isLocal") { type = NavType.BoolType; defaultValue = false }
            )
        ) { backStackEntry ->
            val url = backStackEntry.arguments?.getString("url") ?: ""
            val name = backStackEntry.arguments?.getString("name") ?: "Playlist"
            val isLocal = backStackEntry.arguments?.getBoolean("isLocal") ?: false
            ChannelBrowserScreen(
                navController = navController,
                playlistUrl = url,
                playlistName = name,
                isLocal = isLocal
            )
        }
        composable(
            route = "tmdbDetails?item={item}",
            arguments = listOf(navArgument("item") { type = NavType.StringType; nullable = true })
        ) { backStackEntry ->
            val itemJson = backStackEntry.arguments?.getString("item") ?: ""
            com.myselfhridoy.streambdplayer.ui.screens.home.TmdbDetailsScreen(navController = navController, mediaItemJson = itemJson)
        }
        composable(
            route = "player?url={url}&title={title}&drmLicenseUrl={drmLicenseUrl}&drmSchemeUuid={drmSchemeUuid}&isVod={isVod}&headers={headers}&streamType={streamType}",
            arguments = listOf(
                navArgument("url") { type = NavType.StringType; nullable = true },
                navArgument("title") { type = NavType.StringType; nullable = true },
                navArgument("drmLicenseUrl") { type = NavType.StringType; nullable = true },
                navArgument("drmSchemeUuid") { type = NavType.StringType; nullable = true },
                navArgument("isVod") { type = NavType.BoolType; defaultValue = false },
                navArgument("headers") { type = NavType.StringType; nullable = true },
                navArgument("streamType") { type = NavType.StringType; nullable = true }
            )
        ) { backStackEntry ->
            val url = backStackEntry.arguments?.getString("url")
            val title = backStackEntry.arguments?.getString("title") ?: "Video Player"
            val drmLicenseUrl = backStackEntry.arguments?.getString("drmLicenseUrl")
            val drmSchemeUuid = backStackEntry.arguments?.getString("drmSchemeUuid")
            val isVod = backStackEntry.arguments?.getBoolean("isVod") ?: false
            val headersJson = backStackEntry.arguments?.getString("headers")
            val streamType = backStackEntry.arguments?.getString("streamType")
            PlayerScreen(
                navController = navController,
                mediaUrl = url,
                title = title,
                drmLicenseUrl = drmLicenseUrl,
                drmSchemeUuid = drmSchemeUuid,
                isVod = isVod,
                headersJson = headersJson,
                streamType = streamType
            )
        }
        composable(
            route = "selectServer?tmdbId={tmdbId}&type={type}&title={title}&season={season}&episode={episode}",
            arguments = listOf(
                navArgument("tmdbId") { type = NavType.StringType },
                navArgument("type") { type = NavType.StringType },
                navArgument("title") { type = NavType.StringType },
                navArgument("season") { type = NavType.StringType; nullable = true },
                navArgument("episode") { type = NavType.StringType; nullable = true }
            )
        ) { backStackEntry ->
            val tmdbId = backStackEntry.arguments?.getString("tmdbId") ?: ""
            val type = backStackEntry.arguments?.getString("type") ?: ""
            val title = backStackEntry.arguments?.getString("title") ?: "Select Server"
            val season = backStackEntry.arguments?.getString("season")
            val episode = backStackEntry.arguments?.getString("episode")
            com.myselfhridoy.streambdplayer.ui.screens.home.ServerSelectionScreen(
                navController = navController,
                tmdbId = tmdbId,
                type = type,
                title = title,
                season = season,
                episode = episode
            )
        }
        composable("search") {
            com.myselfhridoy.streambdplayer.ui.screens.home.SearchScreen(navController = navController)
        }
        composable("history") {
            HistoryScreen(navController = navController)
        }
    }
}
