import re

file_path = "d:/Projects/StreamPlay/app/src/main/java/com/myselfhridoy/streambdplayer/ui/screens/player/PlayerScreen.kt"
with open(file_path, "r", encoding="utf-8") as f:
    content = f.read()

# Add imports
imports = """import androidx.compose.ui.graphics.Brush
import com.myselfhridoy.streambdplayer.utils.PlaylistManager"""

if "import androidx.compose.ui.graphics.Brush" not in content:
    content = content.replace("import androidx.compose.ui.graphics.Color", "import androidx.compose.ui.graphics.Color\n" + imports)

# Replace the Custom Overlay Controls section
start_marker = "// Custom Overlay Controls"
end_marker = "        }\n    }\n}\n\n@Composable\nfun TVButton("

# Define the new overlay
new_overlay = """// Custom Overlay Controls
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
                        text = title,
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    
                    if (!isVod) {
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
                    }
                }

                // Center Controls
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
                    }
                }
"""

start_idx = content.find(start_marker)
end_idx = content.find(end_marker)

if start_idx != -1 and end_idx != -1:
    new_content = content[:start_idx] + new_overlay + content[end_idx:]
    with open(file_path, "w", encoding="utf-8") as f:
        f.write(new_content)
    print("Successfully replaced.")
else:
    print("Could not find markers.", start_idx, end_idx)
