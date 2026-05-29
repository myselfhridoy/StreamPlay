package com.myselfhridoy.streambdplayer.data.remote

import com.myselfhridoy.streambdplayer.data.models.TmdbResponse
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.Url

interface TmdbApi {
    @GET
    suspend fun getCategoryItems(@Url url: String): TmdbResponse

    @GET("tv/{series_id}")
    suspend fun getTvDetails(
        @retrofit2.http.Path("series_id") seriesId: Int,
        @Query("api_key") apiKey: String = API_KEY
    ): com.myselfhridoy.streambdplayer.data.models.TvDetailsResponse

    @GET("tv/{series_id}/season/{season_number}")
    suspend fun getTvSeasonDetails(
        @retrofit2.http.Path("series_id") seriesId: Int,
        @retrofit2.http.Path("season_number") seasonNumber: Int,
        @Query("api_key") apiKey: String = API_KEY
    ): com.myselfhridoy.streambdplayer.data.models.TvSeasonResponse

    @GET("search/multi")
    suspend fun searchMulti(
        @Query("query") query: String,
        @Query("api_key") apiKey: String = API_KEY,
        @Query("page") page: Int = 1
    ): TmdbResponse

    companion object {
        const val BASE_URL = "https://api.themoviedb.org/3/"
        const val IMAGE_BASE_URL = "https://image.tmdb.org/t/p/w500"
        const val HERO_IMAGE_URL = "https://image.tmdb.org/t/p/original"
        const val API_KEY = "460327acf6e0235a391222cb530de9c8"
    }
}
