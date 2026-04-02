@file:OptIn(ExperimentalFoundationApi::class)

package com.example.alphacinema

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.alphacinema.ui.viewmodel.MovieDetailViewModel

enum class ScreenType {
    HOME,
    SEARCH,
    SUPPORT,
    ACCOUNT
}

sealed interface AppRoute {
    data class Main(val screen: ScreenType) : AppRoute
    data class MovieDetailRoute(val slug: String) : AppRoute
    data class PlayerRoute(val slug: String, val episodeId: String?) : AppRoute
}

@Composable
fun AppScreen(modifier: Modifier = Modifier) {
    var currentMainScreen by remember { mutableStateOf(ScreenType.HOME) }
    var currentRoute by remember { mutableStateOf<AppRoute>(AppRoute.Main(ScreenType.HOME)) }

    val context = androidx.compose.ui.platform.LocalContext.current
    val movieDetailViewModel: MovieDetailViewModel = viewModel()
    val movieDetail by movieDetailViewModel.movieDetail.collectAsState()
    val detailLoading by movieDetailViewModel.isLoading.collectAsState()
    val detailError by movieDetailViewModel.error.collectAsState()
    val episodeVideoUrls by movieDetailViewModel.episodeVideoUrls.collectAsState()
    val isFavorite by movieDetailViewModel.isFavorite.collectAsState()
    val movieStats by movieDetailViewModel.movieStats.collectAsState()
    val comments by movieDetailViewModel.comments.collectAsState()
    val userRating by movieDetailViewModel.userRating.collectAsState()

    fun openMovieDetail(slug: String) {
        movieDetailViewModel.loadMovieDetail(slug)
        currentRoute = AppRoute.MovieDetailRoute(slug = slug)
    }

    fun openPlayerFromHome(movieUi: MovieUi) {
        if (movieUi.slug.isNotBlank()) {
            openMovieDetail(movieUi.slug)
        }
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

                is AppRoute.MovieDetailRoute -> {
                    if (detailLoading) {
                        // Loading state
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0xFF070B16)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(
                                    color = Color(0xFFF6E29A),
                                    modifier = Modifier.size(42.dp)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    "Đang tải chi tiết phim...",
                                    color = Color.White.copy(alpha = 0.7f),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    } else if (movieDetail != null) {
                        MovieDetailScreen(
                            movie = movieDetail!!,
                            isFavorite = isFavorite,
                            movieStats = movieStats,
                            userRating = userRating,
                            comments = comments,
                            onToggleFavorite = { movie -> 
                                movieDetailViewModel.toggleFavorite(movie.title, movie.posterUrl) { _, msg ->
                                    android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
                                }
                            },
                            onPostComment = { content -> 
                                val user = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
                                movieDetailViewModel.postComment(user?.displayName ?: "Ẩn danh", user?.photoUrl?.toString() ?: "", content) { _, msg ->
                                    android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
                                }
                            },
                            onSubmitRating = { score -> 
                                movieDetailViewModel.submitRating(score) { _, msg ->
                                    android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
                                }
                            },
                            onBack = { currentRoute = AppRoute.Main(currentMainScreen) },
                            onPlayMovie = { playingMovie, episode ->
                                currentRoute = AppRoute.PlayerRoute(
                                    slug = route.slug,
                                    episodeId = episode?.id
                                )
                            },
                            onOpenMovie = ::openMovieDetail
                        )
                    } else if (detailError != null) {
                        // Error state
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0xFF070B16)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    "Lỗi tải phim",
                                    color = Color(0xFFFF6B6B),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    detailError ?: "",
                                    color = Color.White.copy(alpha = 0.5f),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }

                is AppRoute.PlayerRoute -> {
                    val movie = movieDetail
                    if (movie != null) {
                        val episode = movie.episodes.firstOrNull { it.id == route.episodeId }
                            ?: movie.episodes.firstOrNull()
                        val videoUrl = episode?.let { episodeVideoUrls[it.id] } ?: ""

                        PlayerScreen(
                            movie = movie,
                            episode = episode,
                            onBack = { currentRoute = AppRoute.MovieDetailRoute(route.slug) },
                            videoUrl = videoUrl,
                            episodeVideoUrls = episodeVideoUrls,
                            onSelectEpisode = { ep ->
                                currentRoute = AppRoute.PlayerRoute(
                                    slug = route.slug,
                                    episodeId = ep.id
                                )
                            }
                        )
                    }
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
