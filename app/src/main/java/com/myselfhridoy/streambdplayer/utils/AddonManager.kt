package com.myselfhridoy.streambdplayer.utils

import android.annotation.SuppressLint
import android.content.Context
import android.webkit.JavascriptInterface
import android.webkit.WebView
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withTimeoutOrNull
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
    val enabled: Boolean,
    val functionName: String? = null
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
                            executeAddonInWebView(context, cryptoJsCode, jsCode, addon.name, addon.url, addon.functionName, type, tmdbId, season, episode)
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

    private suspend fun executeAddonInWebView(
        context: Context,
        cryptoJsCode: String,
        addonCode: String,
        addonName: String,
        addonUrl: String,
        functionName: String?,
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
                        webView.post { webView.destroy() }
                    }
                }

                @JavascriptInterface
                fun onError(error: String) {
                    if (continuation.isActive) {
                        continuation.resume("[]")
                        webView.post { webView.destroy() }
                    }
                }

                @JavascriptInterface
                fun log(msg: String) {
                    android.util.Log.d("AddonManagerJS", msg)
                }

                @JavascriptInterface
                fun error(msg: String) {
                    android.util.Log.e("AddonManagerJS", msg)
                }

                @JavascriptInterface
                fun doFetch(reqId: String, url: String, optionsJson: String?) {
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            val requestBuilder = okhttp3.Request.Builder().url(url)
                            if (optionsJson != null && optionsJson != "null" && optionsJson != "undefined") {
                                val type = object : com.google.gson.reflect.TypeToken<Map<String, Any>>() {}.type
                                val options: Map<String, Any>? = gson.fromJson(optionsJson, type)
                                if (options != null) {
                                    val method = (options["method"] as? String)?.uppercase() ?: "GET"
                                    var contentTypeStr = "text/plain"
                                    val headersMap = options["headers"] as? Map<String, Any>
                                    headersMap?.forEach { (k, v) ->
                                        val valStr = v.toString()
                                        if (k.equals("content-type", ignoreCase = true)) {
                                            contentTypeStr = valStr
                                        }
                                        requestBuilder.addHeader(k, valStr)
                                    }
                                    
                                    val bodyStr = options["body"]?.toString()
                                    if (method == "POST" || method == "PUT" || method == "PATCH") {
                                        val mediaType = contentTypeStr.toMediaTypeOrNull()
                                        val reqBody = (bodyStr ?: "").toRequestBody(mediaType)
                                        requestBuilder.method(method, reqBody)
                                    } else {
                                        requestBuilder.method(method, null)
                                    }
                                }
                            }
                            
                            val response = client.newCall(requestBuilder.build()).execute()
                            
                            val inputStream = response.body?.byteStream()
                            val buffer = java.io.ByteArrayOutputStream()
                            val data = ByteArray(8192)
                            var totalRead = 0
                            if (inputStream != null) {
                                var read = inputStream.read(data)
                                while (read != -1 && totalRead < 5 * 1024 * 1024) {
                                    buffer.write(data, 0, read)
                                    totalRead += read
                                    read = inputStream.read(data)
                                }
                                inputStream.close()
                            }
                            val bodyBytes = buffer.toByteArray()
                            
                            val status = response.code
                            
                            val responseHeadersMap = mutableMapOf<String, String>()
                            response.headers.names().forEach { name ->
                                responseHeadersMap[name] = response.headers.values(name).joinToString(", ")
                            }
                            val headersJsonStr = gson.toJson(responseHeadersMap)
                            
                            withContext(Dispatchers.Main) {
                                val base64Body = android.util.Base64.encodeToString(bodyBytes, android.util.Base64.NO_WRAP)
                                val safeHeadersJson = android.util.Base64.encodeToString(headersJsonStr.toByteArray(), android.util.Base64.NO_WRAP)
                                webView.evaluateJavascript("javascript:window.onAndroidFetchResponse('" + reqId + "', " + status + ", '" + safeHeadersJson + "', '" + base64Body + "');", null)
                            }
                        } catch (e: Exception) {
                            withContext(Dispatchers.Main) {
                                webView.evaluateJavascript("javascript:window.onAndroidFetchError('" + reqId + "', '" + (e.message?.replace("'", "\\'") ?: "Error") + "');", null)
                            }
                        }
                    }
                }
            }, interfaceName)

            val seasonArg = season?.toString() ?: "null"
            val episodeArg = episode?.toString() ?: "null"

            val addonCodeBase64 = android.util.Base64.encodeToString(addonCode.toByteArray(), android.util.Base64.NO_WRAP)

            val html = """
                <html>
                <head>
                    <script>
                        $cryptoJsCode
                    </script>
                    <script>
                        window.console = {
                            log: function() { AndroidBridge.log(Array.prototype.slice.call(arguments).join(' ')); },
                            error: function() { AndroidBridge.error(Array.prototype.slice.call(arguments).join(' ')); },
                            warn: function() { AndroidBridge.log(Array.prototype.slice.call(arguments).join(' ')); }
                        };
                        window.originalFetch = window.fetch;
                        window.fetchPromises = {};
                        window.onAndroidFetchResponse = function(reqId, status, headersBase64, base64Body) {
                            var p = window.fetchPromises[reqId];
                            if (p) {
                                var bodyText;
                                try {
                                    bodyText = decodeURIComponent(escape(window.atob(base64Body)));
                                } catch (e) {
                                    bodyText = window.atob(base64Body);
                                }
                                var headersJson = decodeURIComponent(escape(window.atob(headersBase64)));
                                var headersObj = JSON.parse(headersJson);
                                var res = {
                                    status: status,
                                    ok: status >= 200 && status < 300,
                                    headers: {
                                        get: function(name) {
                                            var key = Object.keys(headersObj).find(function(k) { return k.toLowerCase() === name.toLowerCase(); });
                                            return key ? headersObj[key] : null;
                                        }
                                    },
                                    text: function() { return Promise.resolve(bodyText); },
                                    json: function() { return Promise.resolve(JSON.parse(bodyText)); }
                                };
                                p.resolve(res);
                                delete window.fetchPromises[reqId];
                            }
                        };
                        window.onAndroidFetchError = function(reqId, error) {
                            var p = window.fetchPromises[reqId];
                            if (p) {
                                p.reject(new Error(error));
                                delete window.fetchPromises[reqId];
                            }
                        };
                        window.fetch = function(url, options) {
                            return new Promise(function(resolve, reject) {
                                var reqId = Math.random().toString(36).substring(7);
                                window.fetchPromises[reqId] = { resolve: resolve, reject: reject };
                                var optionsJson = options ? JSON.stringify(options) : "null";
                                AndroidBridge.doFetch(reqId, url, optionsJson);
                            });
                        };

                        try {
                            var decodedCode = decodeURIComponent(escape(window.atob("$addonCodeBase64")));
                            var parserFunc;
                            try {
                                parserFunc = new Function("CryptoJS", decodedCode)(CryptoJS);
                            } catch(e) {}
                            
                            if (typeof parserFunc !== 'function') {
                                eval(decodedCode);
                                ${if (!functionName.isNullOrBlank()) "parserFunc = $functionName;" else ""}
                                if (typeof parserFunc !== 'function') {
                                    if (typeof resolveStream === 'function') parserFunc = resolveStream;
                                    else if (typeof resolve === 'function') parserFunc = resolve;
                                }
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
