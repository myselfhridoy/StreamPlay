import re

file_path = "d:/Projects/StreamPlay/app/src/main/java/com/myselfhridoy/streambdplayer/utils/AddonManager.kt"
with open(file_path, "r", encoding="utf-8") as f:
    content = f.read()

# Add new imports
if "import kotlinx.coroutines.async" not in content:
    content = content.replace("import kotlinx.coroutines.Dispatchers", "import kotlinx.coroutines.Dispatchers\nimport kotlinx.coroutines.async\nimport kotlinx.coroutines.awaitAll\nimport kotlinx.coroutines.withTimeoutOrNull")

start_marker = "    @SuppressLint(\"SetJavaScriptEnabled\", \"JavascriptInterface\")\n    suspend fun resolveFromAddons("
end_marker = "    private suspend fun executeAddonInWebView("

new_func = """    @SuppressLint("SetJavaScriptEnabled", "JavascriptInterface")
    suspend fun resolveFromAddons(
        context: Context,
        type: String, // "movie" or "tv"
        tmdbId: Int,
        season: Int? = null,
        episode: Int? = null,
        onSourceFound: (List<StreamSource>) -> Unit
    ) = withContext(Dispatchers.IO) {
        val addons = getAddons(context)
        val activeAddons = addons.filter { it.enabled }
        
        // Load crypto-js from assets
        val cryptoJsCode = try {
            context.assets.open("crypto-js.min.js").bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }

        val deferredResults = activeAddons.map { addon ->
            async {
                try {
                    var jsCode = addonScriptsCache[addon.id]
                    if (jsCode == null) {
                        val req = Request.Builder().url(addon.url).build()
                        val res = client.newCall(req).execute()
                        if (res.isSuccessful) {
                            jsCode = res.body?.string()
                            if (jsCode != null) {
                                addonScriptsCache[addon.id] = jsCode
                            }
                        }
                    }

                    if (jsCode != null) {
                        // 8 seconds timeout for each addon to prevent hanging
                        val sourcesJson = withTimeoutOrNull(8000L) {
                            executeAddonInWebView(context, cryptoJsCode, jsCode, type, tmdbId, season, episode)
                        }
                        
                        if (sourcesJson != null && sourcesJson.isNotEmpty() && sourcesJson != "null" && sourcesJson != "undefined") {
                            val sourceType = object : TypeToken<List<StreamSource>>() {}.type
                            val parsedSources: List<StreamSource> = gson.fromJson(sourcesJson, sourceType)
                            val mapped = parsedSources.map { s ->
                                s.copy(provider = if (s.provider.isBlank()) addon.name else s.provider)
                            }
                            if (mapped.isNotEmpty()) {
                                withContext(Dispatchers.Main) {
                                    onSourceFound(mapped)
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        
        // Wait for all addons to finish (or timeout)
        deferredResults.awaitAll()
    }

"""

start_idx = content.find(start_marker)
end_idx = content.find(end_marker)

if start_idx != -1 and end_idx != -1:
    new_content = content[:start_idx] + new_func + content[end_idx:]
    with open(file_path, "w", encoding="utf-8") as f:
        f.write(new_content)
    print("Successfully replaced AddonManager.")
else:
    print("Could not find markers.", start_idx, end_idx)
