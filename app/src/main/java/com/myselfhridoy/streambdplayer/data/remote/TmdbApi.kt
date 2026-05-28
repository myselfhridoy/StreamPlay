package com.myselfhridoy.streambdplayer.data.remote

import com.myselfhridoy.streambdplayer.data.models.TmdbResponse
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.Url

interface TmdbApi {
    @GET
    suspend fun getCategoryItems(@Url url: String): TmdbResponse

    companion object {
        const val BASE_URL = "https://api.themoviedb.org/3/"
        const val IMAGE_BASE_URL = "https://image.tmdb.org/t/p/w500"
        const val HERO_IMAGE_URL = "https://image.tmdb.org/t/p/original"
        const val API_KEY = "460327acf6e0235a391222cb530de9c8"
    }
}
