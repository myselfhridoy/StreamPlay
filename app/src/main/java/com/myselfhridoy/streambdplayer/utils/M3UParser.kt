package com.myselfhridoy.streambdplayer.utils

import com.myselfhridoy.streambdplayer.data.models.Channel
import com.myselfhridoy.streambdplayer.data.models.DrmConfig
import org.json.JSONObject

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object M3UParser {

    suspend fun preprocessM3U(content: String): String = withContext(Dispatchers.IO) {
        // Here we could implement the batch token resolution before parsing
        // Ported from preprocessTokenUrls in Expo
        // For simplicity, we just return the content as the actual resolution is typically done at playback time in the ViewModel
        content
    }
    
    fun parseM3U(content: String): List<Channel> {
        val channels = mutableListOf<Channel>()
        val lines = content.split("\n").map { it.trim() }
        
        var currentName = ""
        var currentLogo = ""
        var currentGroup = "Uncategorized"
        var currentUserAgent: String? = null
        var currentCookie: String? = null
        var currentHttpReferer: String? = null
        var currentOrigin: String? = null
        var currentTokenUrl: String? = null
        var currentTokenMatch: String? = null
        var currentTokenReplace: String? = null
        var currentTokenId: Int? = null
        var currentDrm: DrmConfig? = null

        fun resetCurrentChannel() {
            currentName = ""
            currentLogo = ""
            currentGroup = "Uncategorized"
            currentUserAgent = null
            currentCookie = null
            currentHttpReferer = null
            currentOrigin = null
            currentTokenUrl = null
            currentTokenMatch = null
            currentTokenReplace = null
            currentTokenId = null
            currentDrm = null
        }

        for (line in lines) {
            if (line.isEmpty()) continue

            if (line.startsWith("#EXTINF:")) {
                // Extract properties using basic string parsing or regex
                val logoMatch = Regex("""tvg-logo="([^"]+)"""").find(line)
                val groupMatch = Regex("""group-title="([^"]+)"""").find(line)
                
                currentLogo = logoMatch?.groupValues?.get(1) ?: ""
                currentGroup = groupMatch?.groupValues?.get(1) ?: "Uncategorized"
                
                val commaIndex = line.lastIndexOf(',')
                if (commaIndex != -1) {
                    currentName = line.substring(commaIndex + 1).trim()
                } else {
                    val nameMatch = Regex("""tvg-name="([^"]+)"""").find(line)
                    currentName = nameMatch?.groupValues?.get(1) ?: "Unknown Channel"
                }
            } else if (line.startsWith("#EXTVLCOPT:")) {
                val opt = line.substring(11).trim()
                val lowerOpt = opt.lowercase()
                when {
                    lowerOpt.startsWith("http-user-agent=") -> currentUserAgent = opt.substring(16).trim()
                    lowerOpt.startsWith("http-referrer=") || lowerOpt.startsWith("http-referer=") -> {
                        currentHttpReferer = opt.substring(opt.indexOf('=') + 1).trim()
                    }
                    lowerOpt.startsWith("http-origin=") -> {
                        currentOrigin = opt.substring(opt.indexOf('=') + 1).trim()
                    }
                }
            } else if (line.startsWith("#KODIPROP:inputstream.adaptive.license_type=")) {
                val type = line.substringAfter("=").trim().lowercase()
                val parsedType = when {
                    type.contains("widevine") -> "widevine"
                    type.contains("playready") -> "playready"
                    type.contains("clearkey") -> "clearkey"
                    else -> null
                }
                currentDrm = currentDrm?.copy(type = parsedType) ?: DrmConfig(type = parsedType)
            } else if (line.startsWith("#KODIPROP:inputstream.adaptive.license_key=")) {
                val key = line.substringAfter("=").trim()
                val isRawClearKeyPair = Regex("^[0-9a-fA-F]{32}:[0-9a-fA-F]{32}$").matches(key)
                currentDrm = if (isRawClearKeyPair) {
                    currentDrm?.copy(type = "clearkey", rawKeyPair = key) ?: DrmConfig(type = "clearkey", rawKeyPair = key)
                } else {
                    currentDrm?.copy(licenseServer = key) ?: DrmConfig(licenseServer = key)
                }
            } else if (line.startsWith("#EXTHTTP:")) {
                try {
                    val jsonStr = line.removePrefix("#EXTHTTP:").trim()
                    val jsonObj = JSONObject(jsonStr)
                    if (jsonObj.has("cookie")) {
                        currentCookie = jsonObj.getString("cookie")
                    }
                } catch (e: Exception) {
                    // Ignore parse error
                }
            } else if (line.startsWith("#EXTATTRFROMURL:")) {
                val attrUrl = line.removePrefix("#EXTATTRFROMURL:").trim()
                if (attrUrl.contains("|")) {
                    val parsed = parseUrlHeaders(attrUrl)
                    currentHttpReferer = parsed.headers["referer"] ?: currentHttpReferer
                    currentUserAgent = parsed.headers["user-agent"] ?: currentUserAgent
                    currentOrigin = parsed.headers["origin"] ?: currentOrigin
                    currentCookie = parsed.headers["cookie"] ?: currentCookie
                    
                    currentTokenUrl = parsed.custom["tokenurl"] ?: currentTokenUrl
                    currentTokenMatch = parsed.custom["tokenmatch"] ?: currentTokenMatch
                    currentTokenReplace = parsed.custom["tokenreplace"] ?: currentTokenReplace
                    currentTokenId = parsed.custom["tokenid"]?.toIntOrNull() ?: currentTokenId
                }
            } else if (!line.startsWith("#")) {
                // URL line
                val cleanUrl: String
                if (line.contains("|")) {
                    val parsed = parseUrlHeaders(line)
                    cleanUrl = parsed.cleanUrl
                    
                    currentHttpReferer = parsed.headers["referer"] ?: currentHttpReferer
                    currentUserAgent = parsed.headers["user-agent"] ?: currentUserAgent
                    currentOrigin = parsed.headers["origin"] ?: currentOrigin
                    currentCookie = parsed.headers["cookie"] ?: currentCookie
                    
                    currentTokenUrl = parsed.custom["tokenurl"] ?: currentTokenUrl
                    currentTokenMatch = parsed.custom["tokenmatch"] ?: currentTokenMatch
                    currentTokenReplace = parsed.custom["tokenreplace"] ?: currentTokenReplace
                    currentTokenId = parsed.custom["tokenid"]?.toIntOrNull() ?: currentTokenId
                } else {
                    cleanUrl = line.trim()
                }

                if (cleanUrl.isNotEmpty()) {
                            val isLive = cleanUrl.contains(".m3u8", ignoreCase = true) || 
                                         cleanUrl.contains(".ts", ignoreCase = true) || 
                                         cleanUrl.contains("live", ignoreCase = true)
                            channels.add(
                                Channel(
                                    name = currentName,
                                    logo = currentLogo,
                                    group = currentGroup,
                                    url = cleanUrl,
                                    userAgent = currentUserAgent,
                                    cookie = currentCookie,
                                    httpReferer = currentHttpReferer,
                                    origin = currentOrigin,
                                    tokenUrl = currentTokenUrl,
                                    tokenMatch = currentTokenMatch,
                                    tokenReplace = currentTokenReplace,
                                    tokenId = currentTokenId,
                                    drm = currentDrm,
                                    isLiveEvent = isLive
                                )
                            )
                }
                resetCurrentChannel()
            }
        }
        return channels
    }

    private data class ParsedUrl(
        val cleanUrl: String, 
        val headers: Map<String, String>, 
        val custom: Map<String, String>
    )

    private fun parseUrlHeaders(line: String): ParsedUrl {
        val parts = line.split("|")
        val cleanUrl = parts.firstOrNull()?.trim() ?: ""
        val headers = mutableMapOf<String, String>()
        val custom = mutableMapOf<String, String>()

        for (i in 1 until parts.size) {
            val headerPart = parts[i].trim()
            val equalIndex = headerPart.indexOf('=')
            if (equalIndex > -1) {
                val key = headerPart.substring(0, equalIndex).trim()
                val value = headerPart.substring(equalIndex + 1).trim()
                val lower = key.lowercase()

                if (lower == "tokenurl" || lower == "token-url" || lower == "tokenid" || lower == "tokenmatch" || lower == "tokenreplace") {
                    custom[key] = value
                    custom[lower] = value
                } else {
                    headers[key] = value
                    headers[lower] = value
                }
            }
        }
        return ParsedUrl(cleanUrl, headers, custom)
    }
}
