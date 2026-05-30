const fs = require('fs');

function updateAddonManager() {
    const file = "app/src/main/java/com/myselfhridoy/streambdplayer/utils/AddonManager.kt";
    let content = fs.readFileSync(file, 'utf8');
    
    const targetOldStreamSource = `data class StreamSource(
    val url: String,
    val quality: String,
    val provider: String,
    val type: String? = null,
    val headers: Map<String, String>? = null,
    val subtitles: List<SubtitleSource>? = null
)`;
    const newStreamSource = `data class StreamSource(
    val url: String?,
    val quality: String?,
    val provider: String?,
    val type: String? = null,
    val headers: Map<String, String>? = null,
    val subtitles: List<SubtitleSource>? = null
)`;

    const targetOldMapped = `                            val mapped = parsedSources.map { s ->
                                s.copy(provider = if (s.provider.isBlank()) addon.name else s.provider)
                            }`;
    const newMapped = `                            val mapped = parsedSources.filter { !it.url.isNullOrBlank() }.map { s ->
                                s.copy(provider = if (s.provider.isNullOrBlank()) addon.name else s.provider)
                            }`;

    content = content.replace(targetOldStreamSource, newStreamSource);
    content = content.replace(targetOldMapped, newMapped);
    
    fs.writeFileSync(file, content);
    console.log("AddonManager updated");
}

function updateServerSelection() {
    const file = "app/src/main/java/com/myselfhridoy/streambdplayer/ui/screens/home/ServerSelectionScreen.kt";
    let content = fs.readFileSync(file, 'utf8');

    const targetOldUrlEncode = `                                val encodedUrl = android.net.Uri.encode(source.url)
                                val encodedTitle = android.net.Uri.encode(title)
                                val headersJson = android.net.Uri.encode(Gson().toJson(source.headers ?: emptyMap<String, String>()))
                                val streamType = android.net.Uri.encode(source.type ?: "unknown")
                                navController.navigate("player?url=$encodedUrl&title=$encodedTitle&isVod=true&headers=$headersJson&streamType=$streamType")`;
    const newUrlEncode = `                                val safeUrl = source.url ?: return@Button
                                val encodedUrl = android.net.Uri.encode(safeUrl)
                                val encodedTitle = android.net.Uri.encode(title)
                                val headersJson = android.net.Uri.encode(Gson().toJson(source.headers ?: emptyMap<String, String>()))
                                val streamType = android.net.Uri.encode(source.type ?: "unknown")
                                navController.navigate("player?url=$encodedUrl&title=$encodedTitle&isVod=true&headers=$headersJson&streamType=$streamType")`;

    const targetOldTexts = `                                    Text(source.provider, fontWeight = FontWeight.Bold, fontSize = 16.sp, maxLines = 1)
                                    Text(source.type?.uppercase() ?: "STREAM", fontSize = 12.sp, color = Color.Gray)
                                }
                                Box(
                                    modifier = Modifier
                                        .background(Color(0xFFE50914), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(source.quality, fontSize = 12.sp, fontWeight = FontWeight.Bold)`;
    const newTexts = `                                    Text(source.provider ?: "Unknown", fontWeight = FontWeight.Bold, fontSize = 16.sp, maxLines = 1)
                                    Text(source.type?.uppercase() ?: "STREAM", fontSize = 12.sp, color = Color.Gray)
                                }
                                Box(
                                    modifier = Modifier
                                        .background(Color(0xFFE50914), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(source.quality ?: "Auto", fontSize = 12.sp, fontWeight = FontWeight.Bold)`;

    content = content.replace(targetOldUrlEncode, newUrlEncode);
    content = content.replace(targetOldTexts, newTexts);

    fs.writeFileSync(file, content);
    console.log("ServerSelectionScreen updated");
}

updateAddonManager();
updateServerSelection();
