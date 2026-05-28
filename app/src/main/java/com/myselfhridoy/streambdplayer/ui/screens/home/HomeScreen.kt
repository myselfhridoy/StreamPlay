package com.myselfhridoy.streambdplayer.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigation.NavController
import com.myselfhridoy.streambdplayer.ui.theme.BackgroundDark

@Composable
fun HomeScreen(navController: NavController, isTv: Boolean) {
    if (isTv) {
        TvHomeScreen(navController = navController)
    } else {
        MobileHomeScreen(navController = navController)
    }
}
