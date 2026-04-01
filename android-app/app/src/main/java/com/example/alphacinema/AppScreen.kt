@file:OptIn(ExperimentalFoundationApi::class)

package com.example.alphacinema

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

enum class ScreenType {
    HOME,
    SEARCH,
    SUPPORT,
    ACCOUNT
}

sealed interface AppRoute {
    data class Main(val screen: ScreenType) : AppRoute
    data class MovieDetail(val movieId: String) : AppRoute
    data class Player(val movieId: String, val episodeId: String?) : AppRoute
}

@Composable
fun AppScreen(modifier: Modifier = Modifier) {
    var currentMainScreen by remember { mutableStateOf(ScreenType.HOME) }
    var currentRoute by remember { mutableStateOf<AppRoute>(AppRoute.Main(ScreenType.HOME)) }

    fun openMovieDetail(movieId: String) {
        val movie = MovieDetailFakeData.findMovie(movieId)
        if (movie != null) {
            currentRoute = AppRoute.MovieDetail(movieId = movie.id)
        }
    }

    fun openPlayerFromHome(movieUi: MovieUi) {
        val movie = MovieDetailFakeData.movies.firstOrNull {
            it.subtitle.equals(movieUi.subtitle, ignoreCase = true) ||
                it.title.equals(movieUi.title, ignoreCase = true)
        } ?: MovieDetailFakeData.findDefaultMovie()
        currentRoute = AppRoute.Player(
            movieId = movie.id,
            episodeId = movie.episodes.firstOrNull()?.id
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF070B16))
    ) {
        Crossfade(
            targetState = currentRoute,
            animationSpec = tween(220),
            modifier = Modifier.fillMaxSize(),
            label = "ScreenTransition"
        ) { route ->
            when (route) {
                is AppRoute.Main -> {
                    when (route.screen) {
                        ScreenType.HOME -> HomeScreen(onPlayMovie = ::openPlayerFromHome)
                        ScreenType.SEARCH -> SearchScreen(onOpenMovieDetail = ::openMovieDetail)
                        ScreenType.SUPPORT -> SupportScreen()
                        ScreenType.ACCOUNT -> AccountScreen()
                    }
                }

                is AppRoute.MovieDetail -> {
                    val movie = MovieDetailFakeData.findMovie(route.movieId)
                        ?: MovieDetailFakeData.findDefaultMovie()
                    MovieDetailScreen(
                        movie = movie,
                        onBack = { currentRoute = AppRoute.Main(currentMainScreen) },
                        onPlayMovie = { playingMovie, episode ->
                            currentRoute = AppRoute.Player(
                                movieId = playingMovie.id,
                                episodeId = episode?.id
                            )
                        },
                        onOpenMovie = ::openMovieDetail
                    )
                }

                is AppRoute.Player -> {
                    val movie = MovieDetailFakeData.findMovie(route.movieId)
                        ?: MovieDetailFakeData.findDefaultMovie()
                    val episode = movie.episodes.firstOrNull { it.id == route.episodeId }
                        ?: movie.episodes.firstOrNull()
                    PlayerScreen(
                        movie = movie,
                        episode = episode,
                        onBack = { currentRoute = AppRoute.MovieDetail(movie.id) }
                    )
                }
            }
        }

        if (currentRoute is AppRoute.Main) {
            GlassBottomBar(
                modifier = Modifier.align(Alignment.BottomCenter),
                currentScreen = currentMainScreen,
                onNavigate = { selectedScreen ->
                    currentMainScreen = selectedScreen
                    currentRoute = AppRoute.Main(selectedScreen)
                }
            )
        }
    }
}
