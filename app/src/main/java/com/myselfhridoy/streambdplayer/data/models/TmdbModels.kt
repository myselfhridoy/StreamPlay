package com.myselfhridoy.streambdplayer.data.models

import com.google.gson.annotations.SerializedName

data class TmdbResponse(
    val page: Int,
    val results: List<MediaItem>,
    @SerializedName("total_pages") val totalPages: Int,
    @SerializedName("total_results") val totalResults: Int
)

data class MediaItem(
    val id: Int,
    val title: String?,
    val name: String?,
    @SerializedName("poster_path") val posterPath: String?,
    @SerializedName("backdrop_path") val backdropPath: String?,
    @SerializedName("vote_average") val voteAverage: Double,
    val overview: String,
    @SerializedName("media_type") var mediaType: String? = null
)

data class Category(
    val title: String,
    val url: String,
    val type: String,
    var items: List<MediaItem> = emptyList()
)
