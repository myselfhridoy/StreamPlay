package com.myselfhridoy.streambdplayer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.myselfhridoy.streambdplayer.ui.navigation.AppNavigation
import com.myselfhridoy.streambdplayer.ui.theme.StreamBDPlayerTheme
import com.myselfhridoy.streambdplayer.ui.theme.StreamBDPlayerTvTheme
import com.myselfhridoy.streambdplayer.utils.DeviceUtils

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val isTv = DeviceUtils.isTv(this)
            
            if (isTv) {
                StreamBDPlayerTvTheme {
                    androidx.tv.material3.Surface(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        AppNavigation()
                    }
                }
            } else {
                StreamBDPlayerTheme {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = androidx.compose.material3.MaterialTheme.colorScheme.background
                    ) {
                        AppNavigation()
                    }
                }
            }
        }
    }
}
