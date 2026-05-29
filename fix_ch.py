import re

file_path = "d:/Projects/StreamPlay/app/src/main/java/com/myselfhridoy/streambdplayer/ui/screens/player/PlayerScreen.kt"
with open(file_path, "r", encoding="utf-8") as f:
    content = f.read()

if "import com.myselfhridoy.streambdplayer.utils.TokenParser" not in content:
    content = content.replace("import com.myselfhridoy.streambdplayer.utils.PlaylistManager", "import com.myselfhridoy.streambdplayer.utils.PlaylistManager\nimport com.myselfhridoy.streambdplayer.utils.TokenParser\nimport com.google.gson.Gson\nimport kotlinx.coroutines.launch")

old_ch_down = """                        TVButton(
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
                        )"""

new_ch_down = """                        val scope = rememberCoroutineScope()
                        TVButton(
                            icon = Icons.Default.KeyboardArrowDown,
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

                                        val finalUrl = if (!prevChannel.tokenUrl.isNullOrEmpty()) {
                                            TokenParser.resolveTokenForUrl(context, prevChannel.url, prevChannel.tokenUrl!!, prevChannel.tokenType ?: "catchup") ?: prevChannel.url
                                        } else {
                                            prevChannel.url
                                        }

                                        val encodedTitle = android.net.Uri.encode(prevChannel.name)
                                        val encodedUrl = android.net.Uri.encode(finalUrl)
                                        val route = "player?url=$encodedUrl&title=$encodedTitle&headers=$headersJson"
                                        navController.navigate(route) {
                                            popUpTo("player") { inclusive = true }
                                        }
                                    }
                                } else {
                                    Toast.makeText(context, "No previous channel", Toast.LENGTH_SHORT).show()
                                }
                            },
                            size = 48.dp
                        )"""

old_ch_up = """                        TVButton(
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
                        )"""

new_ch_up = """                        TVButton(
                            icon = Icons.Default.KeyboardArrowUp,
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

                                        val finalUrl = if (!nextChannel.tokenUrl.isNullOrEmpty()) {
                                            TokenParser.resolveTokenForUrl(context, nextChannel.url, nextChannel.tokenUrl!!, nextChannel.tokenType ?: "catchup") ?: nextChannel.url
                                        } else {
                                            nextChannel.url
                                        }

                                        val encodedTitle = android.net.Uri.encode(nextChannel.name)
                                        val encodedUrl = android.net.Uri.encode(finalUrl)
                                        val route = "player?url=$encodedUrl&title=$encodedTitle&headers=$headersJson"
                                        navController.navigate(route) {
                                            popUpTo("player") { inclusive = true }
                                        }
                                    }
                                } else {
                                    Toast.makeText(context, "No next channel", Toast.LENGTH_SHORT).show()
                                }
                            },
                            size = 48.dp
                        )"""

content = content.replace(old_ch_down, new_ch_down)
content = content.replace(old_ch_up, new_ch_up)

with open(file_path, "w", encoding="utf-8") as f:
    f.write(content)

print("Updated CH buttons to pass headers and resolve tokens.")
