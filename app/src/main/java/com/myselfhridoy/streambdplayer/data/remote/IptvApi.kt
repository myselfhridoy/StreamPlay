package com.myselfhridoy.streambdplayer.data.remote

import retrofit2.http.GET

data class IptvPlaylist(
    val url: String,
    val logo: String
)

data class IptvCategoryRoot(
    val playlists: Map<String, IptvPlaylist>
)

interface IptvApi {
    @GET("playlists/IPTV.json")
    suspend fun getCategories(): List<IptvCategoryRoot>

    companion object {
        const val BASE_URL = "https://streambd-iptv.netlify.app/"
    }
}
