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
import com.example.alphacinema.data.model.SupportChatAction
import com.example.alphacinema.data.model.SupportChatRouteDestination
import com.example.alphacinema.data.model.resolveRoute
import com.example.alphacinema.ui.viewmodel.HomeViewModel
import com.example.alphacinema.ui.viewmodel.MovieDetailViewModel
import kotlinx.coroutines.delay

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
    data class MovieListRoute(
        val title: String,
        val filterKind: FilterKind,
        val slug: String
    ) : AppRoute
    object AdminRoute : AppRoute
}

sealed interface SplashState {
    object Showing : SplashState
    object Done : SplashState
}

@Composable
fun AppScreen(modifier: Modifier = Modifier) {
    // Tạo HomeViewModel ở đây để dùng chung cho Splash (theo dõi isLoading)
    // và MainContent (truyền vào để HomeScreen không fetch lại lần 2)
    val homeViewModel: HomeViewModel = viewModel()
    val isLoading by homeViewModel.isLoading.collectAsState()

    var splashState by remember { mutableStateOf<SplashState>(SplashState.Showing) }
    var minTimeElapsed by remember { mutableStateOf(false) }

    // Thời gian tối thiểu 1.5 giây để splash không chớp tắt quá nhanh khi mạng nhanh
    LaunchedEffect(Unit) {
        delay(1500)
        minTimeElapsed = true
    }

    // Tắt Splash khi CẢ HAI điều kiện đều đúng:
    // 1. isLoading = false (dữ liệu HomeScreen đã load xong)
    // 2. Đã qua ít nhất 1.5 giây
    LaunchedEffect(isLoading, minTimeElapsed) {
        if (!isLoading && minTimeElapsed) {
            splashState = SplashState.Done
        }
    }

    Crossfade(
        targetState = splashState,
        animationSpec = tween(durationMillis = 500, easing = androidx.compose.animation.core.FastOutSlowInEasing),
        modifier = modifier.fillMaxSize(),
        label = "SplashTransition"
    ) { state ->
        when (state) {
            is SplashState.Showing -> {
                SplashScreen(onFinished = { splashState = SplashState.Done })
            }
            is SplashState.Done -> {
                MainContent(modifier = modifier)
            }
        }
    }
}

@Composable
fun MainContent(
    modifier: Modifier = Modifier
) {
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

    val adminViewModel: com.example.alphacinema.ui.viewmodel.AdminViewModel = viewModel(
        factory = object : androidx.lifecycle.ViewModelProvider.Factory {
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                return com.example.alphacinema.ui.viewmodel.AdminViewModel(
                    apiService = com.example.alphacinema.data.api.RetrofitClient.instance,
                    firestoreRepository = com.example.alphacinema.data.repository.FirestoreRepository()
                ) as T
            }
        }
    )

    fun showToast(message: String) {
        android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_SHORT).show()
    }

    fun openMovieDetail(slug: String) {
        if (slug.isBlank()) {
            showToast("Không tìm thấy phim để mở.")
            return
        }
        movieDetailViewModel.loadMovieDetail(slug)
        currentRoute = AppRoute.MovieDetailRoute(slug = slug)
    }

    fun openPlayer(slug: String, episodeId: String? = null) {
        if (slug.isBlank()) {
            showToast("Phim này hiện chưa thể mở để xem.")
            return
        }
        if (movieDetail?.id != slug) {
            movieDetailViewModel.loadMovieDetail(slug)
        }
        currentRoute = AppRoute.PlayerRoute(
            slug = slug,
            episodeId = episodeId
        )
    }

    fun openPlayerFromHome(movieUi: MovieUi) {
        if (movieUi.slug.isNotBlank()) {
            openMovieDetail(movieUi.slug)
        }
    }

    fun handleSupportMovieAction(action: SupportChatAction) {
        val route = action.resolveRoute()
        val slug = route?.slug?.takeIf { it.isNotBlank() }
            ?: route?.movieId?.takeIf { it.isNotBlank() }

        if (route == null || slug == null) {
            showToast("Phim này hiện chưa có đường mở phù hợp.")
            return
        }

        when (route.destination) {
            SupportChatRouteDestination.PLAYER -> openPlayer(
                slug = slug,
                episodeId = route.episodeId
            )
            SupportChatRouteDestination.DETAIL -> openMovieDetail(slug)
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
                        ScreenType.HOME -> HomeScreen(
                            onPlayMovie = ::openPlayerFromHome,
                            onSeeMore = { kind, slug, title ->
                                currentRoute = AppRoute.MovieListRoute(
                                    title = title,
                                    filterKind = kind,
                                    slug = slug
                                )
                            }
                        )
                        ScreenType.SEARCH -> SearchScreen(onOpenMovieDetail = ::openMovieDetail)
                        ScreenType.SUPPORT -> SupportScreen(
                            onMovieAction = ::handleSupportMovieAction
                        )
                        ScreenType.ACCOUNT -> AccountScreen(
                            onOpenAdminPanel = { currentRoute = AppRoute.AdminRoute },
                            onOpenMovieDetail = ::openMovieDetail,
                            onOpenMovieList = { title, filterKind, slug ->
                                currentRoute = AppRoute.MovieListRoute(
                                    title = title,
                                    filterKind = filterKind,
                                    slug = slug
                                )
                            }
                        )
                    }
                }

                is AppRoute.AdminRoute -> {
                    AdminScreen(
                        onBack = { currentRoute = AppRoute.Main(ScreenType.ACCOUNT) },
                        viewModel = adminViewModel
                    )
                }

                is AppRoute.MovieDetailRoute -> {
                    val detailMovie = movieDetail?.takeIf { it.id == route.slug }

                    LaunchedEffect(route.slug) {
                        if (detailMovie == null) {
                            movieDetailViewModel.loadMovieDetail(route.slug)
                        }
                    }

                    if (detailLoading || detailMovie == null && detailError == null) {
                        RouteLoadingState(message = "Đang tải chi tiết phim...")
                    } else if (detailMovie != null) {
                        MovieDetailScreen(
                            movie = detailMovie,
                            isFavorite = isFavorite,
                            movieStats = movieStats,
                            userRating = userRating,
                            comments = comments,
                            onToggleFavorite = { movie ->
                                movieDetailViewModel.toggleFavorite(movie.title, movie.posterUrl) { _, msg ->
                                    showToast(msg)
                                }
                            },
                            onPostComment = { content ->
                                val user = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
                                movieDetailViewModel.postComment(user?.displayName ?: "Ẩn danh", user?.photoUrl?.toString() ?: "", content) { _, msg ->
                                    showToast(msg)
                                }
                            },
                            onSubmitRating = { score ->
                                movieDetailViewModel.submitRating(score) { _, msg ->
                                    showToast(msg)
                                }
                            },
                            onBack = { currentRoute = AppRoute.Main(currentMainScreen) },
                            onPlayMovie = { playingMovie, episode ->
                                openPlayer(
                                    slug = route.slug,
                                    episodeId = episode?.id
                                )
                            },
                            onOpenMovie = ::openMovieDetail
                        )
                    } else if (detailError != null) {
                        RouteErrorState(
                            title = "Lỗi tải phim",
                            message = detailError ?: ""
                        )
                    }
                }

                is AppRoute.PlayerRoute -> {
                    val playerMovie = movieDetail?.takeIf { it.id == route.slug }

                    LaunchedEffect(route.slug) {
                        if (playerMovie == null) {
                            movieDetailViewModel.loadMovieDetail(route.slug)
                        }
                    }

                    if (detailLoading || playerMovie == null && detailError == null) {
                        RouteLoadingState(message = "Đang chuẩn bị trình phát...")
                    } else if (playerMovie != null) {
                        val episode = playerMovie.episodes.firstOrNull { it.id == route.episodeId }
                            ?: playerMovie.episodes.firstOrNull()
                        val videoUrl = episode?.let { episodeVideoUrls[it.id] } ?: ""

                        LaunchedEffect(route.slug, videoUrl) {
                            if (videoUrl.isBlank()) {
                                currentRoute = AppRoute.MovieDetailRoute(route.slug)
                                showToast("Phim này hiện chưa có nguồn phát. Mở trang chi tiết để bạn xem thêm.")
                            }
                        }

                        PlayerScreen(
                            movie = playerMovie,
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
                    } else if (detailError != null) {
                        LaunchedEffect(route.slug, detailError) {
                            showToast("Không tìm thấy phim để phát.")
                        }
                        RouteErrorState(
                            title = "Không thể mở trình phát",
                            message = detailError ?: ""
                        )
                    }
                }

                is AppRoute.MovieListRoute -> {
                    MovieListScreen(
                        title = route.title,
                        filterKind = route.filterKind,
                        slug = route.slug,
                        onBack = { currentRoute = AppRoute.Main(currentMainScreen) },
                        onOpenMovieDetail = ::openMovieDetail
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

@Composable
private fun RouteLoadingState(message: String) {
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
                text = message,
                color = Color.White.copy(alpha = 0.7f),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun RouteErrorState(
    title: String,
    message: String
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF070B16)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = title,
                color = Color(0xFFFF6B6B),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            if (message.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = message,
                    color = Color.White.copy(alpha = 0.5f),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}
