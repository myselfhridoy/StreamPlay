package com.myselfhridoy.streambdplayer.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.tv.material3.ExperimentalTvMaterial3Api

val PrimaryColor = Color(0xFF6200EE)
val BackgroundDark = Color(0xFF0D0D14)
val SurfaceDark = Color(0xFF1A1A24)
val TextPrimary = Color(0xFFFFFFFF)
val TextSecondary = Color(0xFF8A8AA3)

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryColor,
    background = BackgroundDark,
    surface = SurfaceDark,
    onPrimary = Color.White,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
)

@OptIn(ExperimentalTvMaterial3Api::class)
private val TvDarkColorScheme = androidx.tv.material3.darkColorScheme(
    primary = PrimaryColor,
    background = BackgroundDark,
    surface = SurfaceDark,
    onPrimary = Color.White,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
)

@Composable
fun StreamBDPlayerTheme(
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            WindowCompat.setDecorFitsSystemWindows(window, false)
        }
    }

    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun StreamBDPlayerTvTheme(
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            WindowCompat.setDecorFitsSystemWindows(window, false)
        }
    }

    androidx.tv.material3.MaterialTheme(
        colorScheme = TvDarkColorScheme,
        content = content
    )
}
