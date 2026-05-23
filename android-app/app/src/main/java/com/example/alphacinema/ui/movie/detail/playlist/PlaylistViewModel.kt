package com.example.alphacinema.ui.movie.detail.playlist

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.alphacinema.data.local.Playlist
import com.example.alphacinema.data.local.PlaylistManager
import com.example.alphacinema.data.local.PlaylistMovie
import com.example.alphacinema.ui.movie.detail.MovieDetailUi
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class PlaylistViewModel(application: Application) : AndroidViewModel(application) {
    private val playlistManager = PlaylistManager.getInstance(application)
    val playlists: StateFlow<List<Playlist>> = playlistManager.playlists

    fun createPlaylistWithMovie(name: String, movie: MovieDetailUi) {
        viewModelScope.launch {
            val playlistMovie = PlaylistMovie(
                id = movie.id,
                title = movie.title,
                posterUrl = movie.posterUrl
            )
            playlistManager.createPlaylist(name, playlistMovie)
        }
    }

    fun toggleMovieInPlaylist(playlistId: String, movie: MovieDetailUi) {
        viewModelScope.launch {
            val playlistMovie = PlaylistMovie(
                id = movie.id,
                title = movie.title,
                posterUrl = movie.posterUrl
            )
            playlistManager.toggleMovieInPlaylist(playlistId, playlistMovie)
        }
    }

    fun removeMovieFromPlaylist(playlistId: String, movieId: String) {
        viewModelScope.launch {
            playlistManager.removeMovieFromPlaylist(playlistId, movieId)
        }
    }

    fun deletePlaylist(playlistId: String) {
        viewModelScope.launch {
            playlistManager.deletePlaylist(playlistId)
        }
    }
}
