package com.myselfhridoy.streambdplayer.data.remote

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object NetworkClient {
    
    val tmdbApi: TmdbApi by lazy {
        Retrofit.Builder()
            .baseUrl(TmdbApi.BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(TmdbApi::class.java)
    }

    val iptvApi: IptvApi by lazy {
        Retrofit.Builder()
            .baseUrl(IptvApi.BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(IptvApi::class.java)
    }
}
