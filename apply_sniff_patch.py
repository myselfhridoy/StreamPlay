import os
import re

app_dir = "d:/Projects/StreamPlay/app/src/main/java/com/myselfhridoy/streambdplayer"

# 1. Update AppNavigation.kt
nav_file = os.path.join(app_dir, "ui/navigation/AppNavigation.kt")
with open(nav_file, "r", encoding="utf-8") as f:
    nav_code = f.read()

nav_old_route = 'route = "player?url={url}&title={title}&drmLicenseUrl={drmLicenseUrl}&drmSchemeUuid={drmSchemeUuid}&isVod={isVod}&headers={headers}"'
nav_new_route = 'route = "player?url={url}&title={title}&drmLicenseUrl={drmLicenseUrl}&drmSchemeUuid={drmSchemeUuid}&isVod={isVod}&headers={headers}&streamType={streamType}"'

nav_old_args = """                navArgument("isVod") { type = NavType.BoolType; defaultValue = false },
                navArgument("headers") { type = NavType.StringType; nullable = true }
            )"""
nav_new_args = """                navArgument("isVod") { type = NavType.BoolType; defaultValue = false },
                navArgument("headers") { type = NavType.StringType; nullable = true },
                navArgument("streamType") { type = NavType.StringType; nullable = true }
            )"""

nav_old_pass = """            val headersJson = backStackEntry.arguments?.getString("headers")
            PlayerScreen(
                navController = navController,
                mediaUrl = url,
                title = title,
                drmLicenseUrl = drmLicenseUrl,
                drmSchemeUuid = drmSchemeUuid,
                isVod = isVod,
                headersJson = headersJson
            )"""
nav_new_pass = """            val headersJson = backStackEntry.arguments?.getString("headers")
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
            )"""

nav_code = nav_code.replace(nav_old_route, nav_new_route)
nav_code = nav_code.replace(nav_old_args, nav_new_args)
nav_code = nav_code.replace(nav_old_pass, nav_new_pass)

with open(nav_file, "w", encoding="utf-8") as f:
    f.write(nav_code)


# 2. Update ServerSelectionScreen.kt
sel_file = os.path.join(app_dir, "ui/screens/home/ServerSelectionScreen.kt")
with open(sel_file, "r", encoding="utf-8") as f:
    sel_code = f.read()

sel_old_click = """                                val headersJson = android.net.Uri.encode(Gson().toJson(source.headers ?: emptyMap<String, String>()))
                                navController.navigate("player?url=$encodedUrl&title=$encodedTitle&isVod=true&headers=$headersJson")"""
sel_new_click = """                                val headersJson = android.net.Uri.encode(Gson().toJson(source.headers ?: emptyMap<String, String>()))
                                val streamType = android.net.Uri.encode(source.type ?: "unknown")
                                navController.navigate("player?url=$encodedUrl&title=$encodedTitle&isVod=true&headers=$headersJson&streamType=$streamType")"""

sel_code = sel_code.replace(sel_old_click, sel_new_click)

with open(sel_file, "w", encoding="utf-8") as f:
    f.write(sel_code)


# 3. Update PlayerScreen.kt
player_file = os.path.join(app_dir, "ui/screens/player/PlayerScreen.kt")
with open(player_file, "r", encoding="utf-8") as f:
    player_code = f.read()

# Add streamType param
player_sig_old = """    drmSchemeUuid: String? = null,
    isVod: Boolean = false,
    headersJson: String? = null
)"""
player_sig_new = """    drmSchemeUuid: String? = null,
    isVod: Boolean = false,
    headersJson: String? = null,
    streamType: String? = null
)"""
player_code = player_code.replace(player_sig_old, player_sig_new)

# Add WebViewSniffer import
player_code = player_code.replace("import com.myselfhridoy.streambdplayer.utils.TokenParser", "import com.myselfhridoy.streambdplayer.utils.TokenParser\nimport com.myselfhridoy.streambdplayer.utils.WebViewSniffer")

# Update token parser calls
player_code = player_code.replace("val resolved = TokenParser.resolveTokenForUrl(\n                                            baseUrl = prevChannel.url,", "val resolved = TokenParser.resolveTokenForUrl(\n                                            context = context,\n                                            baseUrl = prevChannel.url,")
player_code = player_code.replace("val resolved = TokenParser.resolveTokenForUrl(\n                                            baseUrl = nextChannel.url,", "val resolved = TokenParser.resolveTokenForUrl(\n                                            context = context,\n                                            baseUrl = nextChannel.url,")

# Add sniffing logic before setting URI
eff_old = """    LaunchedEffect(mediaUrl, drmLicenseUrl, drmSchemeUuid) {
        if (!mediaUrl.isNullOrEmpty()) {
            val mediaItemBuilder = MediaItem.Builder().setUri(mediaUrl)"""

eff_new = """    var isSniffing by remember { mutableStateOf(false) }

    LaunchedEffect(mediaUrl, drmLicenseUrl, drmSchemeUuid) {
        if (!mediaUrl.isNullOrEmpty()) {
            var finalUri = mediaUrl
            var finalHeaders = headers
            
            if (isVod && streamType == "streamPlay") {
                isSniffing = true
                val resolved = WebViewSniffer.sniff(context, mediaUrl, finalHeaders)
                if (resolved != null) {
                    finalUri = resolved.url
                    finalHeaders = resolved.headers?.ifEmpty { finalHeaders } ?: finalHeaders
                }
                isSniffing = false
            }

            val httpDataSourceFactory = DefaultHttpDataSource.Factory().apply {
                setDefaultRequestProperties(finalHeaders)
            }
            exoPlayer.setMediaSourceFactory(DefaultMediaSourceFactory(context).setDataSourceFactory(httpDataSourceFactory))

            val mediaItemBuilder = MediaItem.Builder().setUri(finalUri)"""

player_code = player_code.replace(eff_old, eff_new)

# Fix duplicate DefaultMediaSourceFactory calls (we inject it in LaunchedEffect now)
player_code = player_code.replace(""".setMediaSourceFactory(DefaultMediaSourceFactory(context).setDataSourceFactory(httpDataSourceFactory))
            .build().apply {""", """.build().apply {""")

# We need to move the headers definition ABOVE LaunchedEffect.
hdr_old = """    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> { exoPlayer.pause() }
                Lifecycle.Event.ON_RESUME -> { exoPlayer.play() }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val exoPlayer = remember {
        val headers = try {
            if (!headersJson.isNullOrEmpty()) {
                val type = object : TypeToken<Map<String, String>>() {}.type
                Gson().fromJson<Map<String, String>>(headersJson, type)
            } else emptyMap()
        } catch (e: Exception) {
            emptyMap()
        }

        val httpDataSourceFactory = DefaultHttpDataSource.Factory().apply {
            setDefaultRequestProperties(headers)
        }

        ExoPlayer.Builder(context)
            .build().apply {
                playWhenReady = true
            }
    }"""

hdr_new = """    val headers = remember(headersJson) {
        try {
            if (!headersJson.isNullOrEmpty()) {
                val type = object : TypeToken<Map<String, String>>() {}.type
                Gson().fromJson<Map<String, String>>(headersJson, type)
            } else emptyMap()
        } catch (e: Exception) {
            emptyMap()
        }
    }

    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            playWhenReady = true
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> { exoPlayer.pause() }
                Lifecycle.Event.ON_RESUME -> { exoPlayer.play() }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }"""

player_code = player_code.replace(hdr_old, hdr_new)


# Add sniffing UI overlay
ui_old = """    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {"""

ui_new = """    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (isSniffing) {
            Box(
                modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.7f)).zIndex(100f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = Color(0xFFE50914), strokeWidth = 3.dp)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Extracting Stream...", color = Color.White)
                }
            }
        }"""

player_code = player_code.replace(ui_old, ui_new)
player_code = player_code.replace("import androidx.compose.ui.Alignment", "import androidx.compose.ui.Alignment\nimport androidx.compose.ui.zIndex.zIndex")

with open(player_file, "w", encoding="utf-8") as f:
    f.write(player_code)

print("Updates applied to Navigation, ServerSelection, and PlayerScreen")
