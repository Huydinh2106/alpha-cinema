@file:OptIn(ExperimentalFoundationApi::class)

package com.example.alphacinema.ui.app

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
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
import com.example.alphacinema.ui.components.LottieLoadingIndicator
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.toRoute
import com.example.alphacinema.data.model.SupportChatAction
import com.example.alphacinema.data.model.SupportChatRouteDestination
import com.example.alphacinema.data.model.resolveRoute
import com.example.alphacinema.ui.account.AccountScreen
import com.example.alphacinema.ui.account.ProfileSettingsScreen
import com.example.alphacinema.ui.account.WatchHistoryScreen
import com.example.alphacinema.ui.admin.AdminScreen
import com.example.alphacinema.ui.admin.AdminViewModel
import com.example.alphacinema.ui.home.GlassBottomBar
import com.example.alphacinema.ui.home.HomeScreen
import com.example.alphacinema.ui.home.HomeViewModel
import com.example.alphacinema.ui.home.MovieTypeScreen
import com.example.alphacinema.ui.home.MovieUi
import com.example.alphacinema.ui.movie.detail.EpisodeUi
import com.example.alphacinema.ui.movie.detail.MovieDetailScreen
import com.example.alphacinema.ui.movie.detail.MovieDetailViewModel
import com.example.alphacinema.ui.movie.list.MovieListScreen
import com.example.alphacinema.ui.payment.PaymentScreen
import com.example.alphacinema.ui.player.PlayerScreen
import com.example.alphacinema.ui.player.PlayerViewModel
import com.example.alphacinema.ui.account.FilterKind
import com.example.alphacinema.ui.search.SearchScreen
import com.example.alphacinema.ui.support.SupportScreen
import com.example.alphacinema.ui.watchparty.WatchPartyLobbySheet
import com.example.alphacinema.ui.watchparty.WatchPartyScreen
import com.example.alphacinema.ui.watchparty.WatchPartyViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import androidx.compose.runtime.rememberCoroutineScope
import com.example.alphacinema.ui.account.AccountAuthEvent
import com.example.alphacinema.ui.account.AuthBottomSheet
import com.example.alphacinema.ui.account.AuthMode
import com.example.alphacinema.ui.account.rememberAccountAuthStateHolder
import com.example.alphacinema.util.formatFirestoreDate

// ── Animation Constants ─────────────────────────────────────────────────────

private const val ANIM_DURATION = 350
private const val ANIM_DURATION_FAST = 250

// ── Splash State ────────────────────────────────────────────────────────────

enum class ScreenType {
    HOME,
    SEARCH,
    SUPPORT,
    ACCOUNT
}

sealed interface SplashState {
    object Showing : SplashState
    object Done : SplashState
}

@Composable
fun AppScreen(
    modifier: Modifier = Modifier, 
    initialShowNotification: Boolean = false,
    initialNotificationType: String? = null,
    initialMovieId: String? = null,
    initialPlan: String? = null
) {
    // Tạo HomeViewModel ở đây để dùng chung cho Splash (theo dõi isLoading)
    // và MainContent (truyền vào để HomeScreen không fetch lại lần 2)
    val homeViewModel: HomeViewModel = viewModel()
    val isLoading by homeViewModel.isLoading.collectAsState()

    var splashState by remember { mutableStateOf<SplashState>(SplashState.Showing) }
    var minTimeElapsed by remember { mutableStateOf(false) }
    var animationFinished by remember { mutableStateOf(false) }

    // Thời gian tối thiểu 1.5 giây để splash không chớp tắt quá nhanh khi mạng nhanh
    LaunchedEffect(Unit) {
        delay(1500)
        minTimeElapsed = true
    }

    // Tắt Splash khi CẢ BA điều kiện đều đúng:
    // 1. isLoading = false (dữ liệu HomeScreen đã load xong)
    // 2. Đã qua ít nhất 1.5 giây
    // 3. Animation logo đã chạy xong
    LaunchedEffect(isLoading, minTimeElapsed, animationFinished) {
        if (!isLoading && minTimeElapsed && animationFinished) {
            splashState = SplashState.Done
        }
    }

    Crossfade(
        targetState = splashState,
        animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing),
        modifier = modifier.fillMaxSize(),
        label = "SplashTransition"
    ) { state ->
        when (state) {
            is SplashState.Showing -> {
                SplashScreen(onFinished = { animationFinished = true })
            }
            is SplashState.Done -> {
                MainContent(
                    modifier = modifier, 
                    initialShowNotification = initialShowNotification,
                    initialNotificationType = initialNotificationType,
                    initialMovieId = initialMovieId,
                    initialPlan = initialPlan
                )
            }
        }
    }
}

@Composable
fun MainContent(
    modifier: Modifier = Modifier,
    initialShowNotification: Boolean = false,
    initialNotificationType: String? = null,
    initialMovieId: String? = null,
    initialPlan: String? = null
) {
    val navController = rememberNavController()
    var currentMainScreen by remember { mutableStateOf(ScreenType.HOME) }
    val scope = rememberCoroutineScope()
    val firestoreRepo = remember { com.example.alphacinema.data.repository.FirestoreRepository() }

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

    val adminViewModel: AdminViewModel = viewModel(
        factory = object : androidx.lifecycle.ViewModelProvider.Factory {
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                return AdminViewModel(
                    apiService = com.example.alphacinema.data.api.RetrofitClient.instance,
                    firestoreRepository = com.example.alphacinema.data.repository.FirestoreRepository()
                ) as T
            }
        }
    )

    val watchPartyViewModel: WatchPartyViewModel = viewModel()
    val wpRoom by watchPartyViewModel.room.collectAsState()
    val wpError by watchPartyViewModel.error.collectAsState()
    val wpIsCreating by watchPartyViewModel.isCreating.collectAsState()
    val wpIsJoining by watchPartyViewModel.isJoining.collectAsState()
    var watchPartyLobbyMovie by remember { mutableStateOf<com.example.alphacinema.ui.movie.detail.MovieDetailUi?>(null) }
    var showWatchPartyLobby by remember { mutableStateOf(false) }
    val auth = remember { com.google.firebase.auth.FirebaseAuth.getInstance() }
    var appAuthLoading by remember { mutableStateOf(false) }
    var appAuthError by remember { mutableStateOf<String?>(null) }

    fun showToast(message: String) {
        android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_SHORT).show()
    }

    val appAuthStateHolder = rememberAccountAuthStateHolder(
        onLogin = { email, password, onSuccess ->
            scope.launch {
                appAuthLoading = true
                appAuthError = null
                try {
                    val result = auth.signInWithEmailAndPassword(email, password).await()
                    result.user?.let { firestoreRepo.saveUser(it) }
                    // Sau khi đăng nhập xong, mở lại Watch Party lobby
                    showWatchPartyLobby = true
                    onSuccess()
                } catch (e: Exception) {
                    appAuthError = e.localizedMessage ?: "Lỗi đăng nhập"
                } finally {
                    appAuthLoading = false
                }
            }
        },
        onRegister = { name, email, password, onSuccess ->
            scope.launch {
                appAuthLoading = true
                appAuthError = null
                try {
                    var emailExists = false
                    try {
                        auth.signInWithEmailAndPassword(email, "DummyWrongPass123!@#").await()
                        emailExists = true
                    } catch (e: com.google.firebase.auth.FirebaseAuthInvalidUserException) {
                        emailExists = false
                    } catch (e: Exception) {
                        emailExists = true
                    }

                    if (emailExists) {
                        appAuthError = "Email đã được sử dụng. Vui lòng chọn email khác."
                        return@launch
                    }

                    val result = auth.createUserWithEmailAndPassword(email, password).await()
                    result.user?.updateProfile(
                        com.google.firebase.auth.UserProfileChangeRequest.Builder()
                            .setDisplayName(name)
                            .build()
                    )?.await()
                    (auth.currentUser ?: result.user)?.let { firestoreRepo.saveUser(it) }
                    showWatchPartyLobby = true
                    onSuccess()
                } catch (e: Exception) {
                    appAuthError = "Đăng ký thất bại: ${e.localizedMessage}"
                } finally {
                    appAuthLoading = false
                }
            }
        },
        onGoogleSignIn = { onSuccess ->
            scope.launch {
                appAuthLoading = true
                appAuthError = null
                try {
                    val credentialManager = androidx.credentials.CredentialManager.create(context)
                    val signInOption = com.google.android.libraries.identity.googleid.GetGoogleIdOption.Builder()
                        .setFilterByAuthorizedAccounts(false)
                        .setServerClientId("1013232588133-86fl74ls04r8fkarnahh2b9pvie5g7kn.apps.googleusercontent.com")
                        .setAutoSelectEnabled(false)
                        .setNonce(null)
                        .build()
                    val request = androidx.credentials.GetCredentialRequest.Builder()
                        .addCredentialOption(signInOption)
                        .build()
                    val credentialResponse = credentialManager.getCredential(
                        request = request,
                        context = context as android.app.Activity
                    )
                    val googleIdTokenCredential = com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.createFrom(credentialResponse.credential.data)
                    val firebaseCredential = com.google.firebase.auth.GoogleAuthProvider.getCredential(googleIdTokenCredential.idToken, null)
                    val result = auth.signInWithCredential(firebaseCredential).await()
                    result.user?.let { firestoreRepo.saveUser(it) }
                    showWatchPartyLobby = true
                    onSuccess()
                } catch (e: Exception) {
                    appAuthError = "Đăng nhập Google thất bại"
                } finally {
                    appAuthLoading = false
                }
            }
        }
    )
    var demoCurrentPlan by rememberSaveable { mutableStateOf<String?>(null) }
    var demoMembershipStartedDate by rememberSaveable { mutableStateOf<String?>(null) }
    var demoMembershipExpiredDate by rememberSaveable { mutableStateOf<String?>(null) }

    // Load subscription plan from Firestore on app start
    LaunchedEffect(Unit) {
        val user = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
        if (user != null && demoCurrentPlan == null) {
            firestoreRepo.saveUser(user)
            val profile = firestoreRepo.getUserProfile(user.uid)
            val plan = profile?.subscriptionPlan
            if (!plan.isNullOrBlank() && plan != "free") {
                demoCurrentPlan = plan
                demoMembershipStartedDate = formatFirestoreDate(profile?.subscriptionStartedAt)
                demoMembershipExpiredDate = formatFirestoreDate(profile?.subscriptionExpiresAt)
            }
        }
    }

    fun openMovieDetail(slug: String) {
        if (slug.isBlank()) {
            showToast("Không tìm thấy phim để mở.")
            return
        }
        movieDetailViewModel.loadMovieDetail(slug)
        navController.navigate(MovieDetailNavRoute(slug = slug))
    }

    fun openPlayer(
        slug: String,
        episodeId: String? = null,
        startPositionMs: Long = 0L
    ) {
        if (slug.isBlank()) {
            showToast("Phim này hiện chưa thể mở để xem.")
            return
        }
        if (movieDetail?.id != slug) {
            movieDetailViewModel.loadMovieDetail(slug)
        }
        navController.navigate(
            PlayerNavRoute(
                slug = slug,
                episodeId = episodeId,
                startPositionMs = startPositionMs.coerceAtLeast(0L)
            )
        )
    }

    fun openPlayerFromHome(movieUi: MovieUi) {
        if (movieUi.slug.isNotBlank()) {
            openPlayer(movieUi.slug)
        }
    }

    fun returnToMainScreen() {
        currentMainScreen = ScreenType.HOME
        navController.navigate(MainRoute) {
            popUpTo(navController.graph.startDestinationId) {
                inclusive = false
            }
            launchSingleTop = true
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

    fun openWatchParty(roomId: String) {
        navController.navigate(WatchPartyNavRoute(roomId = roomId))
    }

    // Handle deep link: alphacinema://watchparty/{roomId}
    val deepLinkHandled = remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (!deepLinkHandled.value) {
            val activity = (context as? android.app.Activity)
            val data = activity?.intent?.data
            if (data != null && data.scheme == "alphacinema" && data.host == "watchparty") {
                val roomId = data.pathSegments?.firstOrNull()
                if (!roomId.isNullOrBlank()) {
                    deepLinkHandled.value = true
                    watchPartyViewModel.joinRoom(context, roomId) {
                        openWatchParty(roomId)
                    }
                }
            }
        }
    }

    // Theo dõi current route để hiện/ẩn bottom bar
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val isOnMainRoute = navBackStackEntry?.destination?.hasRoute<MainRoute>() == true

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF070B16))
    ) {
        NavHost(
            navController = navController,
            startDestination = MainRoute,
            modifier = Modifier.fillMaxSize(),
            // Default enter/exit — dùng cho route không chỉ định riêng
            enterTransition = {
                slideInHorizontally(
                    initialOffsetX = { it },
                    animationSpec = tween(ANIM_DURATION, easing = FastOutSlowInEasing)
                ) + fadeIn(animationSpec = tween(ANIM_DURATION))
            },
            exitTransition = {
                slideOutHorizontally(
                    targetOffsetX = { -it / 4 },
                    animationSpec = tween(ANIM_DURATION, easing = FastOutSlowInEasing)
                ) + fadeOut(animationSpec = tween(ANIM_DURATION_FAST))
            },
            popEnterTransition = {
                slideInHorizontally(
                    initialOffsetX = { -it / 4 },
                    animationSpec = tween(ANIM_DURATION, easing = FastOutSlowInEasing)
                ) + fadeIn(animationSpec = tween(ANIM_DURATION))
            },
            popExitTransition = {
                slideOutHorizontally(
                    targetOffsetX = { it },
                    animationSpec = tween(ANIM_DURATION, easing = FastOutSlowInEasing)
                ) + fadeOut(animationSpec = tween(ANIM_DURATION_FAST))
            }
        ) {
            // ── Main (Home / Search / Support / Account) ────────────────
            composable<MainRoute>(
                enterTransition = { fadeIn(tween(300)) },
                exitTransition = {
                    // Khi navigate đi: slide nhẹ sang trái + fade
                    slideOutHorizontally(
                        targetOffsetX = { -it / 4 },
                        animationSpec = tween(ANIM_DURATION)
                    ) + fadeOut(tween(ANIM_DURATION_FAST))
                },
                popEnterTransition = {
                    // Khi quay lại Main: slide nhẹ từ trái + fade in
                    slideInHorizontally(
                        initialOffsetX = { -it / 4 },
                        animationSpec = tween(ANIM_DURATION)
                    ) + fadeIn(tween(ANIM_DURATION))
                },
                popExitTransition = { fadeOut(tween(200)) }
            ) {
                // Giữ tab navigation bằng Crossfade bên trong MainRoute
                Crossfade(
                    targetState = currentMainScreen,
                    animationSpec = tween(220),
                    modifier = Modifier.fillMaxSize(),
                    label = "TabTransition"
                ) { screen ->
                    when (screen) {
                        ScreenType.HOME -> HomeScreen(
                            initialShowNotification = initialShowNotification,
                            onPlayMovie = ::openPlayerFromHome,
                            onOpenMovieDetail = { slug ->
                                navController.navigate(MovieDetailNavRoute(slug = slug))
                            },
                            onNavigateToPlan = {
                                navController.navigate(PaymentNavRoute)
                            },
                            onSeeMore = { kind, slug, title ->
                                navController.navigate(
                                    MovieListNavRoute(
                                        title = title,
                                        filterKindName = kind.name,
                                        slug = slug
                                    )
                                )
                            },
                            onNavigateToMovieType = { type, title ->
                                navController.navigate(
                                    MovieTypeNavRoute(type = type, title = title)
                                )
                            }
                        )
                        ScreenType.SEARCH -> SearchScreen(onOpenMovieDetail = ::openMovieDetail)
                        ScreenType.SUPPORT -> SupportScreen(
                            onMovieAction = ::handleSupportMovieAction
                        )
                        ScreenType.ACCOUNT -> AccountScreen(
                            onOpenAdminPanel = { navController.navigate(AdminNavRoute) },
                            onOpenMovieDetail = ::openMovieDetail,
                            onOpenWatchHistoryPage = { navController.navigate(WatchHistoryNavRoute) },
                            onOpenMovieList = { title, filterKind, slug ->
                                navController.navigate(
                                    MovieListNavRoute(
                                        title = title,
                                        filterKindName = filterKind.name,
                                        slug = slug
                                    )
                                )
                            },
                            onWatchTogether = {
                                watchPartyLobbyMovie = null
                                showWatchPartyLobby = true
                            },
                            onOpenPayment = {
                                navController.navigate(PaymentNavRoute)
                            },
                            onOpenProfileSettings = {
                                navController.navigate(ProfileSettingsNavRoute)
                            },
                            onLogout = {
                                demoCurrentPlan = null
                                demoMembershipStartedDate = null
                                demoMembershipExpiredDate = null
                            },
                            currentPlan = demoCurrentPlan,
                            membershipStartedDate = demoMembershipStartedDate,
                            membershipExpiredDate = demoMembershipExpiredDate
                        )
                    }
                }
            }

            // ── Movie Detail ────────────────────────────────────────────
            composable<MovieDetailNavRoute>(
                // Slide từ phải vào
                enterTransition = {
                    slideInHorizontally(
                        initialOffsetX = { it },
                        animationSpec = tween(ANIM_DURATION, easing = FastOutSlowInEasing)
                    ) + fadeIn(tween(ANIM_DURATION))
                },
                exitTransition = {
                    // Khi mở Player từ Detail: bị đẩy xuống dưới nhẹ + fade
                    fadeOut(tween(ANIM_DURATION_FAST))
                },
                popEnterTransition = {
                    // Khi quay lại Detail từ Player: fade in
                    fadeIn(tween(ANIM_DURATION))
                },
                popExitTransition = {
                    // Khi quay lại Main: slide về phải
                    slideOutHorizontally(
                        targetOffsetX = { it },
                        animationSpec = tween(ANIM_DURATION, easing = FastOutSlowInEasing)
                    ) + fadeOut(tween(ANIM_DURATION_FAST))
                }
            ) { backStackEntry ->
                val route = backStackEntry.toRoute<MovieDetailNavRoute>()
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
                        onBack = { navController.popBackStack() },
                        onPlayMovie = { playingMovie, episode ->
                            openPlayer(
                                slug = route.slug,
                                episodeId = episode?.id
                            )
                        },
                        onWatchTogether = {
                            val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
                            if (currentUser == null) {
                                // Chưa đăng nhập -> hiện popup đăng nhập ngay
                                appAuthStateHolder.onEvent(AccountAuthEvent.OpenDialog(AuthMode.LOGIN))
                            } else if (wpRoom != null && watchPartyViewModel.isHost) {
                                // Đang là chủ phòng -> đổi phim cho phòng hiện tại
                                val activeEp = detailMovie.episodes.firstOrNull()
                                watchPartyViewModel.changeMovie(
                                    movieSlug = detailMovie.id,
                                    movieTitle = detailMovie.title,
                                    moviePosterUrl = detailMovie.posterUrl,
                                    episodeId = activeEp?.id,
                                    episodeName = activeEp?.name
                                )
                                showToast("Đã đổi phim phòng xem chung!")
                                val currentRoomId = wpRoom?.roomId ?: ""
                                navController.navigate(WatchPartyNavRoute(roomId = currentRoomId))
                            } else {
                                // Mở sheet Tạo/Tham gia phòng
                                watchPartyLobbyMovie = detailMovie
                                showWatchPartyLobby = true
                            }
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

            // ── Player (slide từ dưới lên — kiểu Netflix fullscreen) ──
            composable<PlayerNavRoute>(
                enterTransition = {
                    slideInVertically(
                        initialOffsetY = { it },
                        animationSpec = tween(ANIM_DURATION, easing = FastOutSlowInEasing)
                    ) + fadeIn(tween(ANIM_DURATION))
                },
                exitTransition = { fadeOut(tween(ANIM_DURATION_FAST)) },
                popEnterTransition = { fadeIn(tween(ANIM_DURATION)) },
                popExitTransition = {
                    slideOutVertically(
                        targetOffsetY = { it },
                        animationSpec = tween(ANIM_DURATION, easing = FastOutSlowInEasing)
                    ) + fadeOut(tween(ANIM_DURATION_FAST))
                }
            ) { backStackEntry ->
                val route = backStackEntry.toRoute<PlayerNavRoute>()
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

                    if (videoUrl.isBlank()) {
                        RouteErrorState(
                            title = "Phim đang được cập nhật",
                            message = "Vui lòng quay lại sau.",
                            actionLabel = "Quay lại giao diện chính",
                            onAction = ::returnToMainScreen
                        )
                    } else {
                        PlayerScreen(
                            movie = playerMovie,
                            episode = episode,
                            onBack = { navController.popBackStack() },
                            videoUrl = videoUrl,
                            episodeVideoUrls = episodeVideoUrls,
                            startPositionMs = route.startPositionMs,
                            onSelectEpisode = { ep ->
                                // Thay thế route hiện tại bằng episode mới (không thêm vào back stack)
                                navController.navigate(
                                    PlayerNavRoute(slug = route.slug, episodeId = ep.id)
                                ) {
                                    popUpTo<PlayerNavRoute> { inclusive = true }
                                }
                            }
                        )
                    }
                } else if (detailError != null) {
                    RouteErrorState(
                        title = "Phim đang được cập nhật",
                        message = "Vui lòng quay lại sau.",
                        actionLabel = "Quay lại giao diện chính",
                        onAction = ::returnToMainScreen
                    )
                }
            }

            // ── Movie List ──────────────────────────────────────────────
            composable<MovieListNavRoute> { backStackEntry ->
                val route = backStackEntry.toRoute<MovieListNavRoute>()
                val filterKind = try {
                    FilterKind.valueOf(route.filterKindName)
                } catch (_: Exception) {
                    FilterKind.ALL
                }
                MovieListScreen(
                    title = route.title,
                    filterKind = filterKind,
                    slug = route.slug,
                    onBack = { navController.popBackStack() },
                    onOpenMovieDetail = ::openMovieDetail
                )
            }

            // ── Movie Type (Phim bộ / Phim lẻ) ──────────────────────
            composable<MovieTypeNavRoute> { backStackEntry ->
                val route = backStackEntry.toRoute<MovieTypeNavRoute>()
                MovieTypeScreen(
                    type = route.type,
                    title = route.title,
                    onBack = { navController.popBackStack() },
                    onOpenMovieDetail = ::openMovieDetail
                )
            }

            // ── Admin ───────────────────────────────────────────────────
            composable<AdminNavRoute> {
                AdminScreen(
                    onBack = { navController.popBackStack() },
                    viewModel = adminViewModel
                )
            }

            composable<PaymentNavRoute> {
                PaymentScreen(
                    onBack = { navController.popBackStack() },
                    onPaymentConfirmed = {
                        val user = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
                        if (user != null) {
                            scope.launch {
                                val profile = firestoreRepo.getUserProfile(user.uid)
                                demoCurrentPlan = profile?.subscriptionPlan
                                demoMembershipStartedDate = formatFirestoreDate(profile?.subscriptionStartedAt)
                                demoMembershipExpiredDate = formatFirestoreDate(profile?.subscriptionExpiresAt)
                            }
                        }
                        showToast("MoMo đã xác nhận thanh toán")
                    }
                )
            }

            // ── Profile Settings ─────────────────────────────────────────
            composable<ProfileSettingsNavRoute> {
                ProfileSettingsScreen(
                    onBack = { navController.popBackStack() },
                    onOpenPayment = { navController.navigate(PaymentNavRoute) },
                    onLogout = {
                        demoCurrentPlan = null
                        demoMembershipStartedDate = null
                        demoMembershipExpiredDate = null
                        navController.popBackStack()
                    }
                )
            }

            composable<WatchHistoryNavRoute> {
                WatchHistoryScreen(
                    onBack = { navController.popBackStack() },
                    onOpenWatchHistoryItem = { slug, episodeId, startPositionMs ->
                        openPlayer(
                            slug = slug,
                            episodeId = episodeId,
                            startPositionMs = startPositionMs
                        )
                    }
                )
            }

            // ── Watch Party ──────────────────────────────────────────────
            composable<WatchPartyNavRoute>(
                enterTransition = {
                    slideInVertically(
                        initialOffsetY = { it },
                        animationSpec = tween(ANIM_DURATION, easing = FastOutSlowInEasing)
                    ) + fadeIn(tween(ANIM_DURATION))
                },
                popExitTransition = {
                    slideOutVertically(
                        targetOffsetY = { it },
                        animationSpec = tween(ANIM_DURATION, easing = FastOutSlowInEasing)
                    ) + fadeOut(tween(ANIM_DURATION_FAST))
                }
            ) { backStackEntry ->
                val route = backStackEntry.toRoute<WatchPartyNavRoute>()
                val wpRoom by watchPartyViewModel.room.collectAsState()
                val playerMovie = wpRoom?.let { room ->
                    movieDetail?.takeIf { it.id == room.movieSlug }
                }

                LaunchedEffect(wpRoom?.movieSlug) {
                    val slug = wpRoom?.movieSlug
                    if (!slug.isNullOrBlank() && movieDetail?.id != slug) {
                        movieDetailViewModel.loadMovieDetail(slug)
                    }
                }

                if (wpRoom != null && wpRoom?.movieSlug.isNullOrBlank()) {
                    WatchPartyScreen(
                        videoUrl = "",
                        episodes = emptyList(),
                        currentEpisodeId = null,
                        viewModel = watchPartyViewModel,
                        onChangeMovieClick = {
                            navController.navigate(WatchPartySearchNavRoute)
                        },
                        onBack = { navController.popBackStack() }
                    )
                } else if (detailLoading || playerMovie == null) {
                    RouteLoadingState(message = "Đang chuẩn bị phòng xem chung...")
                } else {
                    val episode = wpRoom?.episodeId?.let { epId ->
                        playerMovie.episodes.firstOrNull { it.id == epId }
                    } ?: playerMovie.episodes.firstOrNull()
                    val videoUrl = episode?.let { episodeVideoUrls[it.id] } ?: ""

                    WatchPartyScreen(
                        videoUrl = videoUrl,
                        episodes = playerMovie.episodes,
                        currentEpisodeId = episode?.id,
                        viewModel = watchPartyViewModel,
                        onChangeMovieClick = {
                            navController.navigate(WatchPartySearchNavRoute)
                        },
                        onBack = { navController.popBackStack() }
                    )
                }
            }

            composable<WatchPartySearchNavRoute>(
                enterTransition = {
                    slideInHorizontally(
                        initialOffsetX = { it },
                        animationSpec = tween(ANIM_DURATION, easing = FastOutSlowInEasing)
                    ) + fadeIn(tween(ANIM_DURATION))
                },
                exitTransition = { fadeOut(tween(ANIM_DURATION_FAST)) },
                popEnterTransition = { fadeIn(tween(ANIM_DURATION)) },
                popExitTransition = {
                    slideOutHorizontally(
                        targetOffsetX = { it },
                        animationSpec = tween(ANIM_DURATION, easing = FastOutSlowInEasing)
                    ) + fadeOut(tween(ANIM_DURATION_FAST))
                }
            ) {
                val coroutineScope = rememberCoroutineScope()
                var isChangingMovie by remember { mutableStateOf(false) }

                Box(modifier = Modifier.fillMaxSize()) {
                    com.example.alphacinema.ui.search.SearchScreen(
                        showBackButton = true,
                        onBackClick = { navController.popBackStack() },
                        onOpenMovieDetail = { slug ->
                            if (!isChangingMovie) {
                                isChangingMovie = true
                                coroutineScope.launch {
                                    try {
                                        val api = com.example.alphacinema.data.api.RetrofitClient.instance
                                        val response = api.getMovieDetail(slug)
                                        val movie = response.movie
                                        val firstEp = response.episodes?.firstOrNull()?.server_data?.firstOrNull()
                                        
                                        if (movie != null) {
                                            val posterUrl = movie.thumb_url?.let {
                                                if (it.startsWith("http")) it else "https://phimimg.com/$it"
                                            } ?: movie.getFullPosterUrl()
                                            
                                            watchPartyViewModel.changeMovie(
                                                movieSlug = slug,
                                                movieTitle = movie.name,
                                                moviePosterUrl = posterUrl,
                                                episodeId = firstEp?.name?.let { "${slug}-ep-0" } ?: "",
                                                episodeName = firstEp?.name ?: ""
                                            )
                                            showToast("Đã đổi phim phòng xem chung!")
                                            navController.popBackStack()
                                        }
                                    } catch (e: Exception) {
                                        showToast("Lỗi khi tải thông tin phim")
                                    } finally {
                                        isChangingMovie = false
                                    }
                                }
                            }
                        }
                    )
                    
                    if (isChangingMovie) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.6f)),
                            contentAlignment = Alignment.Center
                        ) {
                            androidx.compose.material3.CircularProgressIndicator(color = Color(0xFFF6E29A))
                        }
                    }
                }
            }
        }

        // Bottom bar — chỉ hiển thị khi đang ở MainRoute
        // Watch Party Lobby Sheet
        if (showWatchPartyLobby) {
            val movie = watchPartyLobbyMovie
            val activeEp = movie?.episodes?.firstOrNull()
            WatchPartyLobbySheet(
                movieTitle = movie?.title ?: "Phòng xem chung (chưa chọn phim)",
                isCreating = wpIsCreating,
                isJoining = wpIsJoining,
                error = wpError,
                joinOnly = false,
                onDismiss = {
                    showWatchPartyLobby = false
                    watchPartyViewModel.clearError()
                },
                onCreateRoom = {
                    watchPartyViewModel.createRoom(
                        context = context,
                        movieSlug = movie?.id ?: "",
                        movieTitle = movie?.title ?: "",
                        moviePosterUrl = movie?.posterUrl ?: "",
                        episodeId = activeEp?.id ?: "",
                        episodeName = activeEp?.name ?: ""
                    ) { roomId ->
                        showWatchPartyLobby = false
                        openWatchParty(roomId)
                    }
                },
                onJoinRoom = { roomId ->
                    watchPartyViewModel.joinRoom(context, roomId) {
                        showWatchPartyLobby = false
                        openWatchParty(roomId)
                    }
                }
            )
        }

        // Popup đăng nhập khi bấm Xem chung mà chưa login
        if (appAuthStateHolder.uiState.showDialog) {
            AuthBottomSheet(
                state = appAuthStateHolder.uiState,
                isLoading = appAuthLoading,
                errorMessage = appAuthError,
                onEvent = { event -> 
                    appAuthError = null
                    appAuthStateHolder.onEvent(event)
                },
                onForgotPassword = { appAuthStateHolder.onEvent(AccountAuthEvent.CloseDialog) }
            )
        }

        // Xử lý Deep Link khi khởi chạy App từ Thông báo (Status bar)
        LaunchedEffect(initialNotificationType, initialMovieId) {
            if (initialNotificationType == "new_movie" && initialMovieId != null) {
                navController.navigate(MovieDetailNavRoute(slug = initialMovieId))
            } else if (initialNotificationType == "billing") {
                navController.navigate(PaymentNavRoute)
            }
        }

        if (isOnMainRoute) {
            GlassBottomBar(
                modifier = Modifier.align(Alignment.BottomCenter),
                currentScreen = currentMainScreen,
                onNavigate = { selectedScreen ->
                    currentMainScreen = selectedScreen
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
            LottieLoadingIndicator(size = 120.dp)
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
    message: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
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
            if (actionLabel != null && onAction != null) {
                Spacer(modifier = Modifier.height(18.dp))
                Button(
                    onClick = onAction,
                    modifier = Modifier
                        .fillMaxWidth(0.72f)
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFF6E29A),
                        contentColor = Color(0xFF070B16)
                    )
                ) {
                    Text(
                        text = actionLabel,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
