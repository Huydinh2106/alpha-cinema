package com.example.alphacinema.data.repository

import com.example.alphacinema.data.api.RetrofitClient
import com.example.alphacinema.data.model.MovieItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MovieRepository {
    private val api = RetrofitClient.instance

    suspend fun getLatestMovies(page: Int = 1): List<MovieItem> {
        return withContext(Dispatchers.IO) {
            val response = api.getLatestMovies(page = page)
            // The API returns status and items. We just return items
            response.items ?: emptyList()
        }
    }

    suspend fun getMoviesByType(type: String, page: Int = 1, limit: Int = 10): List<MovieItem> {
        return withContext(Dispatchers.IO) {
            val response = api.getMoviesByType(type = type, page = page, limit = limit)
            // PhimAPI for 'v1/api/danh-sach/...' nests items inside 'data'
            response.data?.items ?: response.items ?: emptyList() 
        }
    }

    suspend fun getMoviesByCategory(slug: String, page: Int = 1, limit: Int = 10): List<MovieItem> {
        return withContext(Dispatchers.IO) {
            val response = api.getMoviesByCategory(slug = slug, page = page, limit = limit)
            response.data?.items ?: response.items ?: emptyList()
        }
    }
}
