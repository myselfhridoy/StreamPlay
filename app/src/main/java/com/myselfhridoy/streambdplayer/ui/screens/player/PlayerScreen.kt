package com.myselfhridoy.streambdplayer.ui.screens.player

import android.app.Activity
import android.app.PictureInPictureParams
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Rational
import android.content.pm.ActivityInfo
import android.view.KeyEvent
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.animation.core.*
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.zIndex
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import com.myselfhridoy.streambdplayer.utils.PlaylistManager
import com.myselfhridoy.streambdplayer.utils.TokenParser
import com.myselfhridoy.streambdplayer.utils.WebViewSniffer
import kotlinx.coroutines.launch
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import androidx.media3.ui.TrackSelectionDialogBuilder
import androidx.navigation.NavController
import kotlinx.coroutines.delay
import java.util.UUID
import android.widget.Toast
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

@OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(
    navController: NavController,
    mediaUrl: String? = null,
    title: String = "Video Player",
    drmLicenseUrl: String? = null,
    drmSchemeUuid: String? = null,
    isVod: Boolean = false,
    headersJson: String? = null,
    streamType: String? = null
) {
    val context = LocalContext.current
    
    var currentMediaUrl by remember { mutableStateOf(mediaUrl) }
    var currentTitle by remember { mutableStateOf(title) }
    var currentDrmLicenseUrl by remember { mutableStateOf(drmLicenseUrl) }
    var currentDrmSchemeUuid by remember { mutableStateOf(drmSchemeUuid) }
    var currentIsVod by remember { mutableStateOf(isVod) }
    var currentHeadersJson by remember { mutableStateOf(headersJson) }
    var currentStreamType by remember { mutableStateOf(streamType) }

    val parsedHeaders = remember(currentHeadersJson) {
        try {
            if (!currentHeadersJson.isNullOrEmpty()) {
                val type = object : TypeToken<Map<String, String>>() {}.type
                Gson().fromJson<Map<String, String>>(currentHeadersJson, type)
            } else emptyMap()
        } catch (e: Exception) {
            emptyMap()
        }
    }

    // Handle Fullscreen and Landscape Orientation
    DisposableEffect(Unit) {
        val activity = context as? Activity
        activity?.let {
            it.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            val window = it.window
            WindowCompat.setDecorFitsSystemWindows(window, false)
            WindowInsetsControllerCompat(window, window.decorView).apply {
                hide(WindowInsetsCompat.Type.systemBars())
                systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        }
        
        onDispose {
            activity?.let {
                it.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                val window = it.window
                WindowCompat.setDecorFitsSystemWindows(window, true)
                WindowInsetsControllerCompat(window, window.decorView).apply {
                    show(WindowInsetsCompat.Type.systemBars())
                }
            }
        }
    }

    // ExoPlayer Setup
    val exoPlayer = remember {
        ExoPlayer.Builder(context)
            .build().apply {
                playWhenReady = true
            }
    }

    var isPlaying by remember { mutableStateOf(exoPlayer.isPlaying) }
    var showControls by remember { mutableStateOf(true) }
    var currentPosition by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }
    var playbackSpeed by remember { mutableFloatStateOf(1.0f) }
    var showSpeedMenu by remember { mutableStateOf(false) }
    
    val activity = context as? Activity
    var isLandscape by remember { mutableStateOf(activity?.requestedOrientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) }
    var resizeMode by remember { mutableIntStateOf(AspectRatioFrameLayout.RESIZE_MODE_FIT) }

    var isSniffing by remember { mutableStateOf(false) }

    LaunchedEffect(currentMediaUrl, currentDrmLicenseUrl, currentDrmSchemeUuid) {
        if (!currentMediaUrl.isNullOrEmpty()) {
            var finalUri = currentMediaUrl
            var finalHeaders = parsedHeaders
            
            if (currentIsVod && currentStreamType == "streamPlay") {
                isSniffing = true
                val resolved = WebViewSniffer.sniff(context, currentMediaUrl!!, finalHeaders)
                if (resolved != null) {
                    finalUri = resolved.url
                    finalHeaders = resolved.headers?.ifEmpty { finalHeaders } ?: finalHeaders
                }
                isSniffing = false
            }

            val httpDataSourceFactory = DefaultHttpDataSource.Factory().apply {
                setDefaultRequestProperties(finalHeaders)
                val ua = finalHeaders["User-Agent"] ?: finalHeaders["user-agent"]
                if (ua != null) {
                    setUserAgent(ua)
                }
            }
            val mediaSourceFactory = DefaultMediaSourceFactory(context).setDataSourceFactory(httpDataSourceFactory)
            val mediaItemBuilder = MediaItem.Builder().setUri(finalUri)
            
            if (!drmLicenseUrl.isNullOrEmpty() && !drmSchemeUuid.isNullOrEmpty()) {
                try {
                    val drmUuid = UUID.fromString(drmSchemeUuid)
                    
                    if (drmUuid == androidx.media3.common.C.CLEARKEY_UUID && drmLicenseUrl.contains(":")) {
                        // Raw key pair kid_hex:key_hex
                        val parts = drmLicenseUrl.split(":")
                        if (parts.size == 2) {
                            val kidHex = parts[0]
                            val keyHex = parts[1]
                            
                            val kidBytes = kidHex.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
                            val keyBytes = keyHex.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
                            
                            val kidBase64 = android.util.Base64.encodeToString(kidBytes, android.util.Base64.URL_SAFE or android.util.Base64.NO_PADDING or android.util.Base64.NO_WRAP)
                            val keyBase64 = android.util.Base64.encodeToString(keyBytes, android.util.Base64.URL_SAFE or android.util.Base64.NO_PADDING or android.util.Base64.NO_WRAP)
                            
                            val clearKeyJson = """{"keys":[{"kty":"oct","k":"$keyBase64","kid":"$kidBase64"}],"type":"temporary"}"""
                            
                            mediaSourceFactory.setDrmSessionManagerProvider {
                                val drmCallback = androidx.media3.exoplayer.drm.LocalMediaDrmCallback(clearKeyJson.toByteArray())
                                androidx.media3.exoplayer.drm.DefaultDrmSessionManager.Builder()
                                    .setUuidAndExoMediaDrmProvider(drmUuid, androidx.media3.exoplayer.drm.FrameworkMediaDrm.DEFAULT_PROVIDER)
                                    .build(drmCallback)
                            }
                            
                            // Tell ExoPlayer this item is DRM protected
                            mediaItemBuilder.setDrmConfiguration(
                                MediaItem.DrmConfiguration.Builder(drmUuid).build()
                            )
                        }
                    } else {
                        mediaItemBuilder.setDrmConfiguration(
                            MediaItem.DrmConfiguration.Builder(drmUuid)
                                .setLicenseUri(drmLicenseUrl)
                                .build()
                        )
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Invalid DRM Configuration", Toast.LENGTH_SHORT).show()
                }
            }
            val mediaSource = mediaSourceFactory.createMediaSource(mediaItemBuilder.build())
            exoPlayer.setMediaSource(mediaSource)
            exoPlayer.prepare()
        }
    }

    DisposableEffect(Unit) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    duration = exoPlayer.duration.coerceAtLeast(0L)
                    val isLiveStream = exoPlayer.isCurrentMediaItemLive || 
                                       exoPlayer.isCurrentMediaItemDynamic || 
                                       exoPlayer.duration == C.TIME_UNSET
                    if (isLiveStream) {
                        currentIsVod = false
                    } else if (exoPlayer.duration > 0 && exoPlayer.duration != C.TIME_UNSET) {
                        currentIsVod = true
                    }
                }
            }
            override fun onPlayerError(error: PlaybackException) {
                if (error.errorCode == PlaybackException.ERROR_CODE_DRM_LICENSE_ACQUISITION_FAILED ||
                    error.errorCode == PlaybackException.ERROR_CODE_DRM_PROVISIONING_FAILED) {
                    Toast.makeText(context, "DRM Authentication Failed", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(context, "Playback Error: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    LaunchedEffect(isPlaying, showControls) {
        while (isPlaying && showControls) {
            currentPosition = exoPlayer.currentPosition
            duration = exoPlayer.duration.coerceAtLeast(0L)
            delay(1000)
        }
    }

    // Auto-hide controls
    LaunchedEffect(showControls, isPlaying) {
        if (showControls && isPlaying) {
            delay(4000)
            showControls = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable { showControls = !showControls }
    ) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = false // We use custom compose controls
                    this.resizeMode = resizeMode
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            },
            update = { view ->
                view.resizeMode = resizeMode
            },
            modifier = Modifier.fillMaxSize()
        )

        // Custom Overlay Controls
        if (showControls) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xAA000000),
                                Color.Transparent,
                                Color.Transparent,
                                Color(0xDD000000)
                            )
                        )
                    )
            ) {
                // Top Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .align(Alignment.TopCenter),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = currentTitle,
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    
                    if (!currentIsVod) {
                        val infiniteTransition = rememberInfiniteTransition()
                        val alpha by infiniteTransition.animateFloat(
                            initialValue = 0.3f,
                            targetValue = 1f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(1000, easing = LinearEasing),
                                repeatMode = RepeatMode.Reverse
                            )
                        )
                        Box(
                            modifier = Modifier
                                .background(Color(0x33FF0000), RoundedCornerShape(16.dp))
                                .border(1.dp, Color(0x88FF0000), RoundedCornerShape(16.dp))
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .background(Color.Red.copy(alpha = alpha), CircleShape)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("LIVE", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }

                // Center Controls
                val scope = rememberCoroutineScope()
                Row(
                    modifier = Modifier.align(Alignment.Center),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    if (currentIsVod) {
                        TVButton(
                            icon = Icons.Default.Replay10,
                            onClick = {
                                val newPos = (exoPlayer.currentPosition - 10000).coerceAtLeast(0)
                                exoPlayer.seekTo(newPos)
                                currentPosition = newPos
                            },
                            size = 48.dp
                        )
                    } else {
                        TVButton(
                            icon = Icons.Default.SkipPrevious,
                            onClick = {
                                val prevChannel = PlaylistManager.playPrevious()
                                if (prevChannel != null) {
                                    scope.launch {
                                        val baseHeaders = mutableMapOf<String, String>()
                                        prevChannel.userAgent?.let { baseHeaders["User-Agent"] = it }
                                        prevChannel.httpReferer?.let { baseHeaders["Referer"] = it }
                                        prevChannel.origin?.let { baseHeaders["Origin"] = it }
                                        prevChannel.cookie?.let { baseHeaders["Cookie"] = it }
                                        val headersJson = android.net.Uri.encode(Gson().toJson(baseHeaders))

                                        val resolved = TokenParser.resolveTokenForUrl(
                                            context = context,
                                            baseUrl = prevChannel.url,
                                            tokenUrl = prevChannel.tokenUrl,
                                            tokenId = prevChannel.tokenId?.toString(),
                                            headers = baseHeaders,
                                            tokenMatch = prevChannel.tokenMatch,
                                            tokenReplace = prevChannel.tokenReplace
                                        )
                                        val finalUrl = resolved?.url ?: prevChannel.url

                                        val newDrmType = resolved?.drm?.type ?: prevChannel.drm?.type
                                        val newDrmLicense = resolved?.drm?.licenseServer ?: prevChannel.drm?.licenseServer
                                        val newRawKeyPair = resolved?.drm?.rawKeyPair ?: prevChannel.drm?.rawKeyPair
                                        
                                        var newDrmLicenseUrl: String? = null
                                        var newDrmSchemeUuid: String? = null
                                        if (newDrmType == "widevine" && !newDrmLicense.isNullOrEmpty()) {
                                            newDrmLicenseUrl = newDrmLicense
                                            newDrmSchemeUuid = "edef8ba9-79d6-4ace-a3c8-27dcd51d21ed"
                                        } else if (newDrmType == "clearkey") {
                                            val license = newRawKeyPair ?: newDrmLicense
                                            if (!license.isNullOrEmpty()) {
                                                newDrmLicenseUrl = license
                                                newDrmSchemeUuid = "e2719d58-a985-b3c9-781a-b030af78d30e"
                                            }
                                        }

                                        currentMediaUrl = finalUrl
                                        currentTitle = prevChannel.name
                                        currentDrmLicenseUrl = newDrmLicenseUrl
                                        currentDrmSchemeUuid = newDrmSchemeUuid
                                        currentIsVod = !prevChannel.isLiveEvent
                                        currentHeadersJson = Gson().toJson(baseHeaders)
                                        currentStreamType = prevChannel.tokenUrl
                                    }
                                } else {
                                    Toast.makeText(context, "No previous channel", Toast.LENGTH_SHORT).show()
                                }
                            },
                            size = 48.dp
                        )
                    }
                    
                    TVButton(
                        icon = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        onClick = {
                            if (isPlaying) exoPlayer.pause() else exoPlayer.play()
                        },
                        size = 56.dp,
                        isPrimary = true
                    )
                    
                    if (currentIsVod) {
                        TVButton(
                            icon = Icons.Default.Forward10,
                            onClick = {
                                val newPos = (exoPlayer.currentPosition + 10000).coerceAtMost(duration)
                                exoPlayer.seekTo(newPos)
                                currentPosition = newPos
                            },
                            size = 48.dp
                        )
                    } else {
                        TVButton(
                            icon = Icons.Default.SkipNext,
                            onClick = {
                                val nextChannel = PlaylistManager.playNext()
                                if (nextChannel != null) {
                                    scope.launch {
                                        val baseHeaders = mutableMapOf<String, String>()
                                        nextChannel.userAgent?.let { baseHeaders["User-Agent"] = it }
                                        nextChannel.httpReferer?.let { baseHeaders["Referer"] = it }
                                        nextChannel.origin?.let { baseHeaders["Origin"] = it }
                                        nextChannel.cookie?.let { baseHeaders["Cookie"] = it }
                                        val headersJson = android.net.Uri.encode(Gson().toJson(baseHeaders))

                                        val resolved = TokenParser.resolveTokenForUrl(
                                            context = context,
                                            baseUrl = nextChannel.url,
                                            tokenUrl = nextChannel.tokenUrl,
                                            tokenId = nextChannel.tokenId?.toString(),
                                            headers = baseHeaders,
                                            tokenMatch = nextChannel.tokenMatch,
                                            tokenReplace = nextChannel.tokenReplace
                                        )
                                        val finalUrl = resolved?.url ?: nextChannel.url

                                        val newDrmType = resolved?.drm?.type ?: nextChannel.drm?.type
                                        val newDrmLicense = resolved?.drm?.licenseServer ?: nextChannel.drm?.licenseServer
                                        val newRawKeyPair = resolved?.drm?.rawKeyPair ?: nextChannel.drm?.rawKeyPair
                                        
                                        var newDrmLicenseUrl: String? = null
                                        var newDrmSchemeUuid: String? = null
                                        if (newDrmType == "widevine" && !newDrmLicense.isNullOrEmpty()) {
                                            newDrmLicenseUrl = newDrmLicense
                                            newDrmSchemeUuid = "edef8ba9-79d6-4ace-a3c8-27dcd51d21ed"
                                        } else if (newDrmType == "clearkey") {
                                            val license = newRawKeyPair ?: newDrmLicense
                                            if (!license.isNullOrEmpty()) {
                                                newDrmLicenseUrl = license
                                                newDrmSchemeUuid = "e2719d58-a985-b3c9-781a-b030af78d30e"
                                            }
                                        }

                                        currentMediaUrl = finalUrl
                                        currentTitle = nextChannel.name
                                        currentDrmLicenseUrl = newDrmLicenseUrl
                                        currentDrmSchemeUuid = newDrmSchemeUuid
                                        currentIsVod = !nextChannel.isLiveEvent
                                        currentHeadersJson = Gson().toJson(baseHeaders)
                                        currentStreamType = nextChannel.tokenUrl
                                    }
                                } else {
                                    Toast.makeText(context, "No next channel", Toast.LENGTH_SHORT).show()
                                }
                            },
                            size = 48.dp
                        )
                    }
                }

                // Bottom Bar
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp, vertical = 16.dp)
                        .align(Alignment.BottomCenter)
                ) {
                    if (isVod) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = formatTime(currentPosition),
                                color = Color.White,
                                fontSize = 14.sp
                            )
                            Slider(
                                value = if (duration > 0) (currentPosition.toFloat() / duration.toFloat()) else 0f,
                                onValueChange = { value ->
                                    val newPos = (value * duration).toLong()
                                    exoPlayer.seekTo(newPos)
                                    currentPosition = newPos
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 16.dp),
                                colors = SliderDefaults.colors(
                                    thumbColor = Color(0xFFE50914),
                                    activeTrackColor = Color(0xFFE50914)
                                )
                            )
                            Text(
                                text = formatTime(duration),
                                color = Color.White,
                                fontSize = 14.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    // Bottom Controls Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.End
                    ) {
                        Spacer(modifier = Modifier.weight(1f))
                        
                        var isMuted by remember { mutableStateOf(exoPlayer.volume == 0f) }
                        IconButton(onClick = {
                            isMuted = !isMuted
                            exoPlayer.volume = if (isMuted) 0f else 1f
                        }) {
                            Icon(if (isMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp, contentDescription = "Volume", tint = Color.White)
                        }
                        
                        IconButton(onClick = {
                            TrackSelectionDialogBuilder(
                                context,
                                "Select Quality",
                                exoPlayer,
                                C.TRACK_TYPE_VIDEO
                            ).build().show()
                        }) {
                            Icon(Icons.Default.Settings, contentDescription = "Quality", tint = Color.White)
                        }

                        if (isVod) {
                            IconButton(onClick = {
                                TrackSelectionDialogBuilder(
                                    context,
                                    "Select Subtitles",
                                    exoPlayer,
                                    C.TRACK_TYPE_TEXT
                                ).build().show()
                            }) {
                                Icon(Icons.Default.Subtitles, contentDescription = "Subtitles", tint = Color.White)
                            }

                            Box {
                                IconButton(onClick = { showSpeedMenu = true }) {
                                    Icon(Icons.Default.Speed, contentDescription = "Speed", tint = Color.White)
                                }
                                DropdownMenu(
                                    expanded = showSpeedMenu,
                                    onDismissRequest = { showSpeedMenu = false }
                                ) {
                                    val speeds = listOf(0.75f, 1.0f, 1.25f, 1.5f, 2.0f)
                                    speeds.forEach { speed ->
                                        DropdownMenuItem(
                                            text = { Text("${speed}x") },
                                            onClick = {
                                                playbackSpeed = speed
                                                exoPlayer.playbackParameters = PlaybackParameters(speed)
                                                showSpeedMenu = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        IconButton(onClick = {
                            resizeMode = when (resizeMode) {
                                AspectRatioFrameLayout.RESIZE_MODE_FIT -> AspectRatioFrameLayout.RESIZE_MODE_FILL
                                AspectRatioFrameLayout.RESIZE_MODE_FILL -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                                else -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                            }
                        }) {
                            Icon(Icons.Default.AspectRatio, contentDescription = "Aspect Ratio", tint = Color.White)
                        }
                        
                        IconButton(onClick = {
                            if (isLandscape) {
                                activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                                isLandscape = false
                            } else {
                                activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                                isLandscape = true
                            }
                        }) {
                            Icon(Icons.Default.ScreenRotation, contentDescription = "Rotate", tint = Color.White)
                        }

                        IconButton(onClick = { 
                            showControls = false
                            enterPiP(context) 
                        }) {
                            Icon(
                                Icons.Default.PictureInPictureAlt, 
                                contentDescription = "PiP", 
                                tint = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TVButton(
    icon: ImageVector,
    onClick: () -> Unit,
    size: androidx.compose.ui.unit.Dp = 64.dp,
    isPrimary: Boolean = false
) {
    var isFocused by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        if (isPrimary) {
            try { focusRequester.requestFocus() } catch (e: Exception) {}
        }
    }

    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(if (isFocused) Color.White else Color(0x33FFFFFF))
            .focusRequester(focusRequester)
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
            .clickable { onClick() }
            .onKeyEvent {
                if (it.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_DPAD_CENTER || it.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_ENTER) {
                    if (it.nativeKeyEvent.action == KeyEvent.ACTION_UP) onClick()
                    true
                } else {
                    false
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isFocused) Color.Black else Color.White,
            modifier = Modifier.size(size / 2)
        )
    }
}

private fun formatTime(ms: Long): String {
    val totalSeconds = ms / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        String.format("%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%02d:%02d", minutes, seconds)
    }
}

private fun enterPiP(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val activity = context as? Activity
        val hasPip = activity?.packageManager?.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE) == true
        if (hasPip) {
            val params = PictureInPictureParams.Builder()
                .setAspectRatio(Rational(16, 9))
                .build()
            activity?.enterPictureInPictureMode(params)
        }
    }
}
