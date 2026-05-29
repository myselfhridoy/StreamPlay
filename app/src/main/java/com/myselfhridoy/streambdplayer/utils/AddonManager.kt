package com.myselfhridoy.streambdplayer.utils

import android.annotation.SuppressLint
import android.content.Context
import android.webkit.JavascriptInterface
import android.webkit.WebView
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume

data class SubtitleSource(
    val language: String,
    val url: String,
    val label: String? = null
)

data class StreamSource(
    val url: String,
    val quality: String,
    val provider: String,
    val type: String? = null,
    val headers: Map<String, String>? = null,
    val subtitles: List<SubtitleSource>? = null
)

data class Addon(
    val id: String,
    val name: String,
    val url: String,
    val enabled: Boolean
)

object AddonManager {
    private const val DEFAULT_REGISTRY_URL = "https://streambd-iptv.netlify.app/addons/StreamBD_IPTV_Addon.json"
    private const val ADDONS_PREF_NAME = "prysm_addons_prefs"
    private const val ADDONS_KEY = "addons_list"

    private var cachedAddons: List<Addon>? = null
    private val addonScriptsCache = mutableMapOf<String, String>()
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()
    private val gson = Gson()

    suspend fun getAddons(context: Context, forceSync: Boolean = false): List<Addon> = withContext(Dispatchers.IO) {
        if (!forceSync && cachedAddons != null) {
            return@withContext cachedAddons!!
        }

        val prefs = context.getSharedPreferences(ADDONS_PREF_NAME, Context.MODE_PRIVATE)
        val savedAddonsJson = prefs.getString(ADDONS_KEY, null)
        
        var addons = mutableListOf<Addon>()
        if (savedAddonsJson != null) {
            val type = object : TypeToken<List<Addon>>() {}.type
            addons = gson.fromJson(savedAddonsJson, type)
        }

        if (forceSync || addons.isEmpty()) {
            try {
                val req = Request.Builder().url(DEFAULT_REGISTRY_URL).build()
                val res = client.newCall(req).execute()
                if (res.isSuccessful) {
                    val body = res.body?.string()
                    if (body != null) {
                        val type = object : TypeToken<List<Addon>>() {}.type
                        val registryAddons: List<Addon> = gson.fromJson(body, type)
                        
                        var changed = false
                        for (rAddon in registryAddons) {
                            val existingIndex = addons.indexOfFirst { it.id == rAddon.id }
                            if (existingIndex >= 0) {
                                if (addons[existingIndex].url != rAddon.url || addons[existingIndex].name != rAddon.name) {
                                    addons[existingIndex] = rAddon
                                    changed = true
                                }
                            } else {
                                addons.add(rAddon)
                                changed = true
                            }
                        }
                        
                        if (changed) {
                            prefs.edit().putString(ADDONS_KEY, gson.toJson(addons)).apply()
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        cachedAddons = addons
        return@withContext addons
    }

    @SuppressLint("SetJavaScriptEnabled", "JavascriptInterface")
    suspend fun resolveFromAddons(
        context: Context,
        type: String, // "movie" or "tv"
        tmdbId: Int,
        season: Int? = null,
        episode: Int? = null
    ): List<StreamSource> = withContext(Dispatchers.IO) {
        val addons = getAddons(context)
        val activeAddons = addons.filter { it.enabled }
        
        val allSources = mutableListOf<StreamSource>()
        
        // Load crypto-js from assets
        val cryptoJsCode = try {
            context.assets.open("crypto-js.min.js").bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }

        for (addon in activeAddons) {
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
                    val sourcesJson = executeAddonInWebView(context, cryptoJsCode, jsCode, type, tmdbId, season, episode)
                    if (sourcesJson.isNotEmpty() && sourcesJson != "null" && sourcesJson != "undefined") {
                        val sourceType = object : TypeToken<List<StreamSource>>() {}.type
                        val parsedSources: List<StreamSource> = gson.fromJson(sourcesJson, sourceType)
                        val mapped = parsedSources.map { s ->
                            s.copy(provider = if (s.provider.isBlank()) addon.name else s.provider)
                        }
                        allSources.addAll(mapped)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        
        return@withContext allSources
    }

    private suspend fun executeAddonInWebView(
        context: Context,
        cryptoJsCode: String,
        addonCode: String,
        type: String,
        tmdbId: Int,
        season: Int?,
        episode: Int?
    ): String = withContext(Dispatchers.Main) {
        suspendCancellableCoroutine { continuation ->
            val webView = WebView(context)
            webView.settings.javaScriptEnabled = true
            webView.settings.domStorageEnabled = true

            webView.settings.allowUniversalAccessFromFileURLs = true
            webView.settings.allowFileAccessFromFileURLs = true

            val interfaceName = "AndroidBridge"
            
            webView.addJavascriptInterface(object : Any() {
                @JavascriptInterface
                fun onResult(result: String) {
                    if (continuation.isActive) {
                        continuation.resume(result)
                    }
                }

                @JavascriptInterface
                fun onError(error: String) {
                    if (continuation.isActive) {
                        continuation.resume("[]")
                    }
                }
            }, interfaceName)

            val seasonArg = season?.toString() ?: "null"
            val episodeArg = episode?.toString() ?: "null"

            // Inject the cryptoJS and addonCode into an HTML file loaded from file:// to bypass CORS
            val html = """
                <html>
                <head>
                    <script>
                        $cryptoJsCode
                    </script>
                    <script>
                        try {
                            var addonModule = $addonCode;
                            var parserFunc = addonModule;
                            
                            if (typeof parserFunc === 'function') {
                                parserFunc = parserFunc(CryptoJS);
                            }
                            
                            if (typeof parserFunc === 'function') {
                                parserFunc("$type", "$tmdbId", $seasonArg, $episodeArg)
                                    .then(function(sources) {
                                        AndroidBridge.onResult(JSON.stringify(sources));
                                    })
                                    .catch(function(err) {
                                        AndroidBridge.onError(err.toString());
                                    });
                            } else {
                                AndroidBridge.onError("Parser is not a function");
                            }
                        } catch(e) {
                            AndroidBridge.onError(e.toString());
                        }
                    </script>
                </head>
                <body></body>
                </html>
            """.trimIndent()

            webView.loadDataWithBaseURL("file:///android_asset/dummy.html", html, "text/html", "UTF-8", null)

            continuation.invokeOnCancellation {
                webView.destroy()
            }
        }
    }
}
