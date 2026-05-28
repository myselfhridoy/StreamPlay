package com.myselfhridoy.streambdplayer.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorites")
data class Favorite(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val url: String,
    val logo: String,
    val groupName: String,
    val timestamp: Long = System.currentTimeMillis()
)
