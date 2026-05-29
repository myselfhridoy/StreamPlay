package com.myselfhridoy.streambdplayer.utils

import java.util.regex.Pattern

data class DrmInfo(
    val type: String,
    val rawKeyPair: String? = null,
    val licenseServer: String? = null
)

data class JsDecodeResult(
    val url: String,
    val drm: DrmInfo? = null
)

object JSDecoder {

    fun decode(text: String): JsDecodeResult? {
        try {
            var decrypted = ""

            // Check if it's the XOR encrypted payload
            val xorPattern = Pattern.compile("function\\s+xorDecrypt\\(data,\\s*key\\)", Pattern.CASE_INSENSITIVE)
            val encryptedPattern = Pattern.compile("(?:let|const|var)\\s+encrypted\\s*=\\s*\"(.*?)\";", Pattern.CASE_INSENSITIVE)
            val keyPattern = Pattern.compile("(?:let|const|var)\\s+decryptionKey\\s*=\\s*\"(.*?)\";", Pattern.CASE_INSENSITIVE)

            val xorMatcher = xorPattern.matcher(text)
            val encryptedMatcher = encryptedPattern.matcher(text)
            val keyMatcher = keyPattern.matcher(text)

            if (xorMatcher.find() && encryptedMatcher.find() && keyMatcher.find()) {
                val encrypted = encryptedMatcher.group(1)
                val key = keyMatcher.group(1)

                if (encrypted != null && key != null) {
                    val decoded = Base64Utils.decode(encrypted)
                    val sb = StringBuilder()
                    for (i in decoded.indices) {
                        val charCode = decoded[i].code xor key[i % key.length].code
                        sb.append(charCode.toChar())
                    }
                    decrypted = sb.toString()
                }
            } else {
                // The eval packer requires a JS engine to unpack.
                // Note: Porting `new Function(...)` requires QuickJS/Rhino which is omitted here to avoid bloating.
                // We fallback to regex search if we can't execute JS.
            }

            if (decrypted.isNotEmpty()) {
                val urlPattern = Pattern.compile("(?:const|let|var)\\s+(?:mpdUrl|url)\\s*=\\s*'(.*?)';", Pattern.CASE_INSENSITIVE)
                val kidPattern = Pattern.compile("(?:const|let)\\s+kid\\s*=\\s*'(.*?)';", Pattern.CASE_INSENSITIVE)
                val keyStrPattern = Pattern.compile("(?:const|let)\\s+key\\s*=\\s*'(.*?)';", Pattern.CASE_INSENSITIVE)

                val urlMatcher = urlPattern.matcher(decrypted)
                val kidMatcher = kidPattern.matcher(decrypted)
                val keyStrMatcher = keyStrPattern.matcher(decrypted)

                var finalUrl = ""
                if (urlMatcher.find()) {
                    finalUrl = urlMatcher.group(1) ?: ""
                } else {
                    val genericUrlPattern = Pattern.compile("https?://[^\\s'\"]+")
                    val genericMatcher = genericUrlPattern.matcher(decrypted)
                    if (genericMatcher.find()) {
                        finalUrl = genericMatcher.group(0) ?: ""
                    }
                }

                if (finalUrl.isNotEmpty()) {
                    var drm: DrmInfo? = null

                    val widevinePattern = Pattern.compile("(?:widevine|licenseUrl|licenseServer)[a-zA-Z0-9_]*\\s*[:=]\\s*['\"](https?://[^'\"]+)['\"]", Pattern.CASE_INSENSITIVE)
                    val widevineAlphaPattern = Pattern.compile("['\"]com\\.widevine\\.alpha['\"]\\s*:\\s*['\"](https?://[^'\"]+)['\"]", Pattern.CASE_INSENSITIVE)
                    val playreadyPattern = Pattern.compile("['\"]com\\.microsoft\\.playready['\"]\\s*:\\s*['\"](https?://[^'\"]+)['\"]", Pattern.CASE_INSENSITIVE)
                    
                    val w1 = widevinePattern.matcher(decrypted)
                    val w2 = widevineAlphaPattern.matcher(decrypted)
                    val p1 = playreadyPattern.matcher(decrypted)

                    if (w1.find()) {
                        drm = DrmInfo(type = "widevine", licenseServer = w1.group(1))
                    } else if (w2.find()) {
                        drm = DrmInfo(type = "widevine", licenseServer = w2.group(1))
                    } else if (p1.find()) {
                        drm = DrmInfo(type = "playready", licenseServer = p1.group(1))
                    } else if (kidMatcher.find() && keyStrMatcher.find()) {
                        drm = DrmInfo(type = "clearkey", rawKeyPair = "${kidMatcher.group(1)}:${keyStrMatcher.group(1)}")
                    } else {
                        val rawPairPattern = Pattern.compile("([a-fA-F0-9]{32}):([a-fA-F0-9]{32})")
                        val rawPairMatcher = rawPairPattern.matcher(decrypted)
                        if (rawPairMatcher.find()) {
                            drm = DrmInfo(type = "clearkey", rawKeyPair = "${rawPairMatcher.group(1)}:${rawPairMatcher.group(2)}")
                        }
                    }

                    return JsDecodeResult(url = finalUrl, drm = drm)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }
}
