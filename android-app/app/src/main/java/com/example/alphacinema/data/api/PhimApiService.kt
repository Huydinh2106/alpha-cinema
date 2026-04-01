package com.example.alphacinema.data.api

import com.example.alphacinema.data.model.CategoryInfo
import com.example.alphacinema.data.model.MovieDetailResponse
import com.example.alphacinema.data.model.MovieListResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface PhimApiService {
    @GET("danh-sach/phim-moi-cap-nhat")
    suspend fun getLatestMovies(
        @Query("page") page: Int = 1
    ): MovieListResponse

    @GET("v1/api/danh-sach/{type}")
    suspend fun getMoviesByType(
        @retrofit2.http.Path("type") type: String,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 10,
        @Query("sort_field") sortField: String = "modified.time",
        @Query("sort_type") sortType: String = "desc"
    ): MovieListResponse

    @GET("v1/api/the-loai/{slug}")
    suspend fun getMoviesByCategory(
        @retrofit2.http.Path("slug") slug: String,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 10,
        @Query("sort_field") sortField: String = "modified.time",
        @Query("sort_type") sortType: String = "desc"
    ): MovieListResponse

    @GET("phim/{slug}")
    suspend fun getMovieDetail(
        @retrofit2.http.Path("slug") slug: String
    ): MovieDetailResponse
}
