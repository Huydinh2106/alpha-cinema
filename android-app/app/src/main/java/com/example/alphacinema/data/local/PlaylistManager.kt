package com.example.alphacinema.data.local

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

data class PlaylistMovie(
    val id: String,
    val title: String,
    val posterUrl: String
)

data class Playlist(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val movies: List<PlaylistMovie> = emptyList()
)

class PlaylistManager private constructor(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()

    private val _playlists = MutableStateFlow<List<Playlist>>(loadPlaylists())
    val playlists: StateFlow<List<Playlist>> = _playlists.asStateFlow()

    private fun loadPlaylists(): List<Playlist> {
        val json = prefs.getString(KEY_PLAYLISTS, null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<Playlist>>() {}.type
            gson.fromJson(json, type)
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun savePlaylists(playlists: List<Playlist>) {
        prefs.edit().putString(KEY_PLAYLISTS, gson.toJson(playlists)).apply()
        _playlists.value = playlists
    }

    fun createPlaylist(name: String, initialMovie: PlaylistMovie? = null) {
        val current = _playlists.value.toMutableList()
        val movies = if (initialMovie != null) listOf(initialMovie) else emptyList()
        current.add(Playlist(name = name, movies = movies))
        savePlaylists(current)
    }

    fun deletePlaylist(playlistId: String) {
        val current = _playlists.value.filter { it.id != playlistId }
        savePlaylists(current)
    }

    fun toggleMovieInPlaylist(playlistId: String, movie: PlaylistMovie) {
        val current = _playlists.value.map { playlist ->
            if (playlist.id == playlistId) {
                val movieExists = playlist.movies.any { it.id == movie.id }
                val newMovies = if (movieExists) {
                    playlist.movies.filter { it.id != movie.id }
                } else {
                    playlist.movies + movie
                }
                playlist.copy(movies = newMovies)
            } else {
                playlist
            }
        }
        savePlaylists(current)
    }

    fun removeMovieFromPlaylist(playlistId: String, movieId: String) {
        val current = _playlists.value.map { playlist ->
            if (playlist.id == playlistId) {
                playlist.copy(movies = playlist.movies.filter { it.id != movieId })
            } else {
                playlist
            }
        }
        savePlaylists(current)
    }

    fun isMovieInPlaylist(playlistId: String, movieId: String): Boolean {
        return _playlists.value.find { it.id == playlistId }?.movies?.any { it.id == movieId } ?: false
    }

    companion object {
        private const val PREFS_NAME = "alpha_cinema_playlists"
        private const val KEY_PLAYLISTS = "playlists_data"

        @Volatile
        private var INSTANCE: PlaylistManager? = null

        fun getInstance(context: Context): PlaylistManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: PlaylistManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
