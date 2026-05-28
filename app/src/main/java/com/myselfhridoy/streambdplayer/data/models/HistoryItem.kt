package com.myselfhridoy.streambdplayer.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "history_items")
data class HistoryItem(
    @PrimaryKey val url: String,
    val name: String? = null,
    val logo: String? = null,
    val groupName: String? = null,
    val cookie: String? = null,
    val referer: String? = null,
    val origin: String? = null,
    val userAgent: String? = null,
    val drmUrl: String? = null,
    val drmScheme: String? = null,
    val streamFormat: String? = null,
    val tokenUrl: String? = null,
    val tokenMatch: String? = null,
    val tokenReplace: String? = null,
    val tokenId: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)
