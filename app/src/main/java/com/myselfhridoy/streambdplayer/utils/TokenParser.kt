package com.myselfhridoy.streambdplayer.utils

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URL
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

data class ResolvedToken(
    val url: String,
    val headers: Map<String, String>? = null,
    val drm: DrmInfo? = null
)

object TokenParser {

    private val client = OkHttpClient.Builder()
        .connectTimeout(4, TimeUnit.SECONDS)
        .readTimeout(4, TimeUnit.SECONDS)
        .build()

    private const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"

    suspend fun resolveTokenForUrl(
        context: Context,
        baseUrl: String,
        tokenUrl: String?,
        tokenId: String? = null,
        headers: Map<String, String>? = null,
        tokenMatch: String? = null,
        tokenReplace: String? = null
    ): ResolvedToken? = withContext(Dispatchers.IO) {
        if (tokenUrl.isNullOrEmpty()) return@withContext null

        val lowerTokenUrl = tokenUrl.lowercase()

        // 0. streamPlay WebViewSniffer
        if (lowerTokenUrl == "streamplay") {
            val isVidsrc = baseUrl.contains("vsembed") || baseUrl.contains("vidsrc")
            val userAgent = if (isVidsrc) {
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
            } else null
            return@withContext WebViewSniffer.sniff(context, baseUrl, headers, userAgent)
        }

        // 1. InfinityFree
        if (lowerTokenUrl == "if") {
            val bypass = InfinityFreeBypass.resolve(baseUrl, headers)
            if (bypass?.content?.contains("#EXTM3U") == true) {
                // Parse mini-M3U (similar to JS logic)
                var streamUrl = ""
                val streamHeaders = bypass.headers?.toMutableMap() ?: mutableMapOf()
                var drm: DrmInfo? = null
                
                val lines = bypass.content.split("\n").map { it.trim() }
                for (line in lines) {
                    if (line.isEmpty() || line.startsWith("#EXTM3U")) continue
                    if (line.startsWith("#KODIPROP:inputstream.adaptive.license_type=")) {
                        val type = line.split("=").getOrNull(1)?.lowercase()?.trim()
                        if (type?.contains("widevine") == true) drm = DrmInfo(type = "widevine")
                        else if (type?.contains("playready") == true) drm = DrmInfo(type = "playready")
                        else if (type?.contains("clearkey") == true) drm = DrmInfo(type = "clearkey")
                        continue
                    }
                    if (line.startsWith("#KODIPROP:inputstream.adaptive.license_key=")) {
                        val keyVal = line.substringAfter("=").trim()
                        if (keyVal.matches(Regex("^[0-9a-f]{32}:[0-9a-f]{32}$", RegexOption.IGNORE_CASE))) {
                            drm = DrmInfo(type = "clearkey", rawKeyPair = keyVal)
                        } else {
                            drm = drm?.copy(licenseServer = keyVal) ?: DrmInfo(type = "unknown", licenseServer = keyVal)
                        }
                        continue
                    }
                    if (line.startsWith("#EXTVLCOPT:")) {
                        val payload = line.substring("#EXTVLCOPT:".length).trim()
                        val sep = payload.indexOf("=")
                        if (sep != -1) {
                            val k = payload.substring(0, sep).trim()
                            val v = payload.substring(sep + 1).trim()
                            streamHeaders[k] = v
                        }
                        continue
                    }
                    if (!line.startsWith("#")) {
                        streamUrl = line
                        continue
                    }
                }
                if (streamUrl.isNotEmpty()) {
                    if (!streamUrl.startsWith("http")) return@withContext ResolvedToken(url = bypass.url, headers = bypass.headers)
                    return@withContext ResolvedToken(url = streamUrl, headers = streamHeaders, drm = drm)
                }
            } else if (bypass?.content != null) {
                // Try jsdecode
                val decoded = JSDecoder.decode(bypass.content)
                if (decoded != null) {
                    var finalUrl = decoded.url
                    if (tokenMatch != null && tokenReplace != null) finalUrl = finalUrl.replace(tokenMatch, tokenReplace)
                    return@withContext ResolvedToken(url = finalUrl, headers = getStreamHeaders(baseUrl, bypass.headers), drm = decoded.drm)
                }
                // Try crichd
                val crichdRegex = Pattern.compile("return\\s*\\(\\s*\\[(.*?)\\]\\.join\\(", Pattern.CASE_INSENSITIVE)
                val m = crichdRegex.matcher(bypass.content)
                if (m.find()) {
                    val arr = m.group(1)
                    val chars = Regex("[\"']([^\"']*)[\"']").findAll(arr ?: "").map { it.groupValues[1] }.joinToString("").replace("\\/", "/")
                    var finalUrl = chars
                    if (tokenMatch != null && tokenReplace != null) finalUrl = finalUrl.replace(tokenMatch, tokenReplace)
                    return@withContext ResolvedToken(url = finalUrl, headers = getStreamHeaders(baseUrl, bypass.headers))
                }
            }
            return@withContext bypass?.let { ResolvedToken(url = it.url, headers = it.headers) }
        }

        // 2. JSDecode
        if (lowerTokenUrl == "jsdecode") {
            try {
                val req = Request.Builder().url(baseUrl).apply {
                    addHeader("User-Agent", USER_AGENT)
                    headers?.forEach { (k, v) -> addHeader(k, v) }
                }.build()
                val res = client.newCall(req).execute()
                if (res.isSuccessful) {
                    val html = res.body?.string() ?: ""
                    val decoded = JSDecoder.decode(html)
                    if (decoded != null) {
                        var finalUrl = decoded.url
                        if (tokenMatch != null && tokenReplace != null) finalUrl = finalUrl.replace(tokenMatch, tokenReplace)
                        return@withContext ResolvedToken(url = finalUrl, headers = getStreamHeaders(baseUrl, headers), drm = decoded.drm)
                    }
                }
            } catch (e: Exception) { e.printStackTrace() }
            return@withContext null
        }

        // 3. crichd
        if (lowerTokenUrl == "crichd") {
            try {
                val req = Request.Builder().url(baseUrl).apply {
                    addHeader("User-Agent", USER_AGENT)
                    headers?.forEach { (k, v) -> addHeader(k, v) }
                }.build()
                val res = client.newCall(req).execute()
                if (res.isSuccessful) {
                    val html = res.body?.string() ?: ""
                    val m = Pattern.compile("return\\s*\\(\\s*\\[(.*?)\\]\\.join\\(", Pattern.CASE_INSENSITIVE).matcher(html)
                    if (m.find()) {
                        val arr = m.group(1)
                        val chars = Regex("[\"']([^\"']*)[\"']").findAll(arr ?: "").map { it.groupValues[1] }.joinToString("").replace("\\/", "/")
                        var finalUrl = chars
                        if (tokenMatch != null && tokenReplace != null) finalUrl = finalUrl.replace(tokenMatch, tokenReplace)
                        return@withContext ResolvedToken(url = finalUrl, headers = getStreamHeaders(baseUrl, headers))
                    }
                }
            } catch (e: Exception) { e.printStackTrace() }
            return@withContext null
        }

        // 4. fetchiframe
        if (lowerTokenUrl == "fetchiframe") {
            try {
                val req = Request.Builder().url(baseUrl).apply {
                    addHeader("User-Agent", USER_AGENT)
                    headers?.forEach { (k, v) -> addHeader(k, v) }
                }.build()
                val res = client.newCall(req).execute()
                if (res.isSuccessful) {
                    val html = res.body?.string() ?: ""
                    val iframeRegex = Regex("<iframe[^>]+src=[\"']([^\"']+)[\"']")
                    val match = iframeRegex.find(html)
                    if (match != null) {
                        var src = match.groupValues[1]
                        if (tokenMatch != null && tokenReplace != null) {
                            src = src.replace(tokenMatch, tokenReplace)
                        }
                        return@withContext ResolvedToken(url = src, headers = getStreamHeaders(baseUrl, headers))
                    } else {
                        // Fallback: search for m3u8 in html
                        val m3u8Regex = Regex("[\"']([^\"']+\\.m3u8[^\"]*)[\"']")
                        val m3u8Match = m3u8Regex.find(html)
                        if (m3u8Match != null) {
                            var src = m3u8Match.groupValues[1]
                            if (tokenMatch != null && tokenReplace != null) {
                                src = src.replace(tokenMatch, tokenReplace)
                            }
                            return@withContext ResolvedToken(url = src, headers = getStreamHeaders(baseUrl, headers))
                        }
                    }
                }
            } catch (e: Exception) { e.printStackTrace() }
            return@withContext null
        }

        // 5. /3 fallback
        if (lowerTokenUrl == "/3") {
            try {
                val req = Request.Builder().url(baseUrl).apply {
                    addHeader("User-Agent", USER_AGENT)
                    headers?.forEach { (k, v) -> addHeader(k, v) }
                }.build()
                val res = client.newCall(req).execute()
                if (res.isSuccessful) {
                    val html = res.body?.string() ?: ""
                    val streams = extractAllUrls(html).filter { isStreamUrl(it) }
                    if (streams.isNotEmpty()) {
                        return@withContext ResolvedToken(url = streams.first(), headers = getStreamHeaders(baseUrl, headers))
                    }
                }
            } catch (e: Exception) { e.printStackTrace() }
            return@withContext null
        }

        // 6. handlerId logic (omitted complex extraction for brevity, basic fallback)
        val idMatch = Regex("^[\\\\/]?(\\d+)$").find(tokenUrl)
        if (idMatch != null) {
            // Simplified handler logic
            try {
                val req = Request.Builder().url(baseUrl).apply {
                    addHeader("User-Agent", USER_AGENT)
                    headers?.forEach { (k, v) -> addHeader(k, v) }
                }.build()
                val res = client.newCall(req).execute()
                if (res.isSuccessful) {
                    val body = res.body?.string() ?: ""
                    val streams = extractAllUrls(body).filter { isStreamUrl(it) }
                    if (streams.isNotEmpty()) {
                        val idx = (tokenId?.toIntOrNull() ?: 1) - 1
                        val streamUrl = streams.getOrElse(idx) { streams.first() }
                        return@withContext ResolvedToken(url = streamUrl, headers = getStreamHeaders(baseUrl, headers))
                    }
                }
            } catch (e: Exception) { e.printStackTrace() }
        }

        null
    }

    private fun getStreamHeaders(baseUrl: String, headers: Map<String, String>?): Map<String, String> {
        val out = headers?.toMutableMap() ?: mutableMapOf()
        try {
            val u = URL(baseUrl)
            out["Referer"] = "${u.protocol}://${u.host}/"
            out["Origin"] = "${u.protocol}://${u.host}"
        } catch (e: Exception) {}
        return out
    }

    private fun isStreamUrl(url: String): Boolean {
        val ext = Regex("\\.(m3u8|mpd|mp4|mkv|ts|webm|flv|avi|mov|mpeg|mpg|m4s|f4m|ism|sdp)(\\?|$)", RegexOption.IGNORE_CASE)
        val proto = Regex("^(rtmp|rtsp|udp|srt)://", RegexOption.IGNORE_CASE)
        return ext.containsMatchIn(url) || proto.containsMatchIn(url)
    }

    private fun extractAllUrls(payload: String): List<String> {
        val candidates = mutableListOf<String>()
        val decoded = payload.replace("\\/", "/")
            .replace("\\u002F", "/", ignoreCase = true)
            .replace("\\u003A", ":", ignoreCase = true)
            .replace("\\u0026", "&", ignoreCase = true)
        
        val httpRegex = Regex("https?://[\\w\\-._~:/?#\\[\\]@!$&'()*+,;=%]+")
        httpRegex.findAll(decoded).forEach { candidates.add(it.value) }
        
        val protoRegex = Regex("(rtmp|rtsp|udp|srt)://[\\w\\-._~:/?#\\[\\]@!$&'()*+,;=%]+")
        protoRegex.findAll(decoded).forEach { candidates.add(it.value) }
        
        return candidates.distinct()
    }
}
