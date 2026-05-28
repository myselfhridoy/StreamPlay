package com.myselfhridoy.streambdplayer.data.models

data class Channel(
    val name: String,
    val logo: String = "",
    val group: String = "Uncategorized",
    val url: String,
    val userAgent: String? = null,
    val cookie: String? = null,
    val httpReferer: String? = null,
    val origin: String? = null,
    val isLiveEvent: Boolean = false,
    val tokenUrl: String? = null,
    val tokenMatch: String? = null,
    val tokenReplace: String? = null,
    val tokenId: Int? = null,
    val drm: DrmConfig? = null
)

data class DrmConfig(
    val type: String? = null, // e.g., "widevine", "clearkey"
    val licenseServer: String? = null,
    val rawKeyPair: String? = null
)
