package com.example.alphacinema.data.api

import com.example.alphacinema.data.model.TmdbCreditsResponse
import com.example.alphacinema.data.model.TmdbDetailResponse
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface TmdbApiService {
    @GET("movie/{movie_id}/credits")
    suspend fun getMovieCredits(
        @Path("movie_id") movieId: String,
        @Query("api_key") apiKey: String = TmdbConfig.API_KEY,
        @Query("language") language: String = "vi-VN"
    ): TmdbCreditsResponse

    @GET("tv/{tv_id}/credits")
    suspend fun getTvCredits(
        @Path("tv_id") tvId: String,
        @Query("api_key") apiKey: String = TmdbConfig.API_KEY,
        @Query("language") language: String = "vi-VN"
    ): TmdbCreditsResponse

    @GET("movie/{movie_id}")
    suspend fun getMovieDetail(
        @Path("movie_id") movieId: String,
        @Query("api_key") apiKey: String = TmdbConfig.API_KEY,
        @Query("language") language: String = "vi-VN"
    ): TmdbDetailResponse

    @GET("tv/{tv_id}")
    suspend fun getTvDetail(
        @Path("tv_id") tvId: String,
        @Query("api_key") apiKey: String = TmdbConfig.API_KEY,
        @Query("language") language: String = "vi-VN"
    ): TmdbDetailResponse
}

object TmdbConfig {
    const val API_KEY = "bf682a9e013b0f81c338fd83d9ee7b71"
    const val IMAGE_BASE_URL = "https://image.tmdb.org/t/p/w185"
}
