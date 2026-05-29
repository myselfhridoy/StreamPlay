import re

file_path = "d:/Projects/StreamPlay/app/src/main/java/com/myselfhridoy/streambdplayer/ui/screens/player/PlayerScreen.kt"
with open(file_path, "r", encoding="utf-8") as f:
    content = f.read()

# Add missing imports for DisposableEffect and borders
if "import androidx.compose.runtime.DisposableEffect" not in content:
    content = content.replace("import androidx.compose.runtime.*", "import androidx.compose.runtime.*\nimport androidx.compose.runtime.DisposableEffect")
if "import androidx.compose.foundation.border" not in content:
    content = content.replace("import androidx.compose.foundation.background", "import androidx.compose.foundation.background\nimport androidx.compose.foundation.border")
if "import androidx.compose.animation.core.*" not in content:
    content = content.replace("import androidx.compose.runtime.*", "import androidx.compose.runtime.*\nimport androidx.compose.animation.core.*")
if "import androidx.media3.common.C" not in content:
    content = content.replace("import androidx.media3.common.Player", "import androidx.media3.common.Player\nimport androidx.media3.common.C")

# Replace the DisposableEffect part
if "DisposableEffect(Unit)" not in content:
    content = content.replace("val context = LocalContext.current", """val context = LocalContext.current
    val activity = context as? Activity

    DisposableEffect(Unit) {
        onDispose {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }""")

# 1. Replace Live Badge
old_badge = """                    if (!isVod) {
                        Surface(
                            color = Color(0x44FF0000),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "🔴 LIVE",
                                color = Color.Red,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }"""

new_badge = """                    if (!isVod) {
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
                    }"""
content = content.replace(old_badge, new_badge)

# 2. Center Controls: Add CH+/-
old_center = """                // Center Controls
                Row(
                    modifier = Modifier.align(Alignment.Center),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    if (isVod) {
                        TVButton(
                            icon = Icons.Default.Replay10,
                            onClick = {
                                val newPos = (exoPlayer.currentPosition - 10000).coerceAtLeast(0)
                                exoPlayer.seekTo(newPos)
                                currentPosition = newPos
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
                    
                    if (isVod) {
                        TVButton(
                            icon = Icons.Default.Forward10,
                            onClick = {
                                val newPos = (exoPlayer.currentPosition + 10000).coerceAtMost(duration)
                                exoPlayer.seekTo(newPos)
                                currentPosition = newPos
                            },
                            size = 48.dp
                        )
                    }
                }"""

new_center = """                // Center Controls
                Row(
                    modifier = Modifier.align(Alignment.Center),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    if (isVod) {
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
                            icon = Icons.Default.KeyboardArrowDown,
                            onClick = {
                                val prevChannel = PlaylistManager.playPrevious()
                                if (prevChannel != null) {
                                    val encodedTitle = android.net.Uri.encode(prevChannel.name)
                                    val finalUrl = android.net.Uri.encode(prevChannel.url)
                                    val route = "player?url=$finalUrl&title=$encodedTitle"
                                    navController.navigate(route) {
                                        popUpTo("player") { inclusive = true }
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
                    
                    if (isVod) {
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
                            icon = Icons.Default.KeyboardArrowUp,
                            onClick = {
                                val nextChannel = PlaylistManager.playNext()
                                if (nextChannel != null) {
                                    val encodedTitle = android.net.Uri.encode(nextChannel.name)
                                    val finalUrl = android.net.Uri.encode(nextChannel.url)
                                    val route = "player?url=$finalUrl&title=$encodedTitle"
                                    navController.navigate(route) {
                                        popUpTo("player") { inclusive = true }
                                    }
                                } else {
                                    Toast.makeText(context, "No next channel", Toast.LENGTH_SHORT).show()
                                }
                            },
                            size = 48.dp
                        )
                    }
                }"""
content = content.replace(old_center, new_center)

# 3. Bottom Controls Row: Remove old CH+/-, add Quality
old_bottom_row = """                    // Bottom Controls Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.End
                    ) {
                        if (!isVod) {
                            IconButton(onClick = { 
                                val prevChannel = PlaylistManager.playPrevious()
                                if (prevChannel != null) {
                                    val encodedTitle = android.net.Uri.encode(prevChannel.name)
                                    val finalUrl = android.net.Uri.encode(prevChannel.url) // Need to handle token logic later if needed
                                    val route = "player?url=$finalUrl&title=$encodedTitle"
                                    navController.navigate(route) {
                                        popUpTo("player") { inclusive = true }
                                    }
                                } else {
                                    Toast.makeText(context, "No previous channel", Toast.LENGTH_SHORT).show()
                                }
                            }) {
                                Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Channel Down", tint = Color.White)
                            }
                            IconButton(onClick = { 
                                val nextChannel = PlaylistManager.playNext()
                                if (nextChannel != null) {
                                    val encodedTitle = android.net.Uri.encode(nextChannel.name)
                                    val finalUrl = android.net.Uri.encode(nextChannel.url)
                                    val route = "player?url=$finalUrl&title=$encodedTitle"
                                    navController.navigate(route) {
                                        popUpTo("player") { inclusive = true }
                                    }
                                } else {
                                    Toast.makeText(context, "No next channel", Toast.LENGTH_SHORT).show()
                                }
                            }) {
                                Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Channel Up", tint = Color.White)
                            }
                            Spacer(modifier = Modifier.weight(1f))
                        }

                        var isMuted by remember { mutableStateOf(exoPlayer.volume == 0f) }
                        IconButton(onClick = {
                            isMuted = !isMuted
                            exoPlayer.volume = if (isMuted) 0f else 1f
                        }) {
                            Icon(if (isMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp, contentDescription = "Volume", tint = Color.White)
                        }

                        if (isVod) {
                            IconButton(onClick = {
                                TrackSelectionDialogBuilder(
                                    context,
                                    "Select Subtitles",
                                    exoPlayer,
                                    androidx.media3.common.C.TRACK_TYPE_TEXT
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
                            Icon(Icons.Default.PictureInPictureAlt, contentDescription = "PiP", tint = Color.White)
                        }
                    }"""

new_bottom_row = """                    // Bottom Controls Row
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
                            Icon(Icons.Default.PictureInPictureAlt, contentDescription = "PiP", tint = Color.White)
                        }
                    }"""

content = content.replace(old_bottom_row, new_bottom_row)

with open(file_path, "w", encoding="utf-8") as f:
    f.write(content)

print("Replaced PlayerScreen successfully.")
