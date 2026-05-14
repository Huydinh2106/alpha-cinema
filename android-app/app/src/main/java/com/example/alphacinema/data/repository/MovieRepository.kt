package com.example.alphacinema.data.repository

import com.example.alphacinema.data.api.RetrofitClient
import com.example.alphacinema.data.local.SettingsManager
import com.example.alphacinema.data.model.CategoryInfo
import com.example.alphacinema.data.model.FirestoreMovie
import com.example.alphacinema.data.model.MovieItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MovieRepository {
    private val firestoreRepository = FirestoreRepository()
    
    // We keep this to allow fallback or access to api if needed, but for listings we use firestore
    private val api = RetrofitClient.instance

    suspend fun getLatestMovies(page: Int = 1): List<MovieItem> {
        return withContext(Dispatchers.IO) {
            val isKidsMode = SettingsManager.getInstance().isKidsModeEnabled.value
            val movies = firestoreRepository.getMovies(isKidsMode, 20)
            movies.map { it.toMovieItem() }
        }
    }

    suspend fun searchMoviesFromApi(keyword: String, page: Int = 1): List<MovieItem> {
        return withContext(Dispatchers.IO) {
            try {
                val response = api.searchMovies(keyword = keyword, page = page)
                response.items ?: response.data?.items ?: emptyList()
            } catch (e: Exception) {
                android.util.Log.e("MovieRepository", "searchMoviesFromApi failed", e)
                emptyList()
            }
        }
    }

    suspend fun getMoviesByType(type: String, page: Int = 1, limit: Int = 10, excludeSlugs: Set<String> = emptySet()): List<MovieItem> {
        return withContext(Dispatchers.IO) {
            val isKidsMode = SettingsManager.getInstance().isKidsModeEnabled.value
            val movies = firestoreRepository.getMoviesByType(type, isKidsMode, limit, excludeSlugs)
            movies.map { it.toMovieItem() }
        }
    }

    suspend fun getMoviesByCategory(slug: String, page: Int = 1, limit: Int = 10, excludeSlugs: Set<String> = emptySet()): List<MovieItem> {
        return withContext(Dispatchers.IO) {
            val isKidsMode = SettingsManager.getInstance().isKidsModeEnabled.value
            val movies = firestoreRepository.getMoviesByCategory(slug, isKidsMode, limit, excludeSlugs)
            movies.map { it.toMovieItem() }
        }
    }

    suspend fun getMoviesByCountry(country: String, page: Int = 1, limit: Int = 10, excludeSlugs: Set<String> = emptySet()): List<MovieItem> {
        return withContext(Dispatchers.IO) {
            val isKidsMode = SettingsManager.getInstance().isKidsModeEnabled.value
            val movies = firestoreRepository.getMoviesByCountry(country, isKidsMode, limit, excludeSlugs)
            movies.map { it.toMovieItem() }
        }
    }

    suspend fun getMoviesByHomeCategory(categoryId: String, limit: Int = 20): List<MovieItem> {
        return withContext(Dispatchers.IO) {
            val isKidsMode = SettingsManager.getInstance().isKidsModeEnabled.value
            val movies = firestoreRepository.getCategoryMovies(categoryId, isKidsMode, limit)
            movies.map { it.toMovieItem() }
        }
    }
    
    private fun FirestoreMovie.toMovieItem() = MovieItem(
        _id = slug,
        name = title,
        slug = slug,
        origin_name = originName,
        poster_url = posterUrl,
        thumb_url = thumbUrl,
        year = year.toInt(),
        type = type,
        episode_current = "Full",
        quality = "FHD",
        lang = "Vietsub",
        ageRating = ageRating,
        tmdb = com.example.alphacinema.data.model.TmdbInfo(
            type = null,
            id = null,
            season = null,
            vote_average = if (tmdbVoteAverage > 0.0) tmdbVoteAverage else null,
            vote_count = null
        ),
        category = categories.map { CategoryInfo(name = it, slug = "") },
        country = emptyList()
    )
}
