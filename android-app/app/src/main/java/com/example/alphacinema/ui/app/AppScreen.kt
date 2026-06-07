@file:OptIn(ExperimentalFoundationApi::class)

package com.example.alphacinema.ui.app

import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import com.example.alphacinema.ui.components.LottieLoadingIndicator
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import com.example.alphacinema.BuildConfig
import com.example.alphacinema.data.model.SupportChatAction
import com.example.alphacinema.data.model.SupportChatRouteDestination
import com.example.alphacinema.data.model.SubscriptionPlan
import com.example.alphacinema.data.model.activeEntitlements
import com.example.alphacinema.data.model.entitlements
import com.example.alphacinema.data.model.resolveRoute
import com.example.alphacinema.ui.account.AccountPlaylistsScreen
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
import com.example.alphacinema.ui.movie.detail.MovieDetailUi
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
import java.util.concurrent.atomic.AtomicBoolean

// ── Animation Constants ─────────────────────────────────────────────────────

private const val ANIM_DURATION = 220
private const val ANIM_DURATION_FAST = 120
private const val MAIN_RETURN_ANIM_DURATION = 90
private const val TAB_SWITCH_ANIM_DURATION = 90
private const val ALPHA_CINEMA_WEB_LINK_HOST = "alpha-cinema-39dfb.web.app"
private const val MOVIE_LINK_SEGMENT = "movie"
private const val WATCH_PARTY_LINK_SEGMENT = "watchparty"

private fun movieShareLink(slug: String): String {
    return Uri.Builder()
        .scheme("https")
        .authority(ALPHA_CINEMA_WEB_LINK_HOST)
        .appendPath(MOVIE_LINK_SEGMENT)
        .appendPath(slug)
        .appendQueryParameter("v", BuildConfig.VERSION_CODE.toString())
        .build()
        .toString()
}

internal fun watchPartyShareLink(roomId: String): String {
    return Uri.Builder()
        .scheme("https")
        .authority(ALPHA_CINEMA_WEB_LINK_HOST)
        .appendPath(WATCH_PARTY_LINK_SEGMENT)
        .appendPath(roomId)
        .appendQueryParameter("v", BuildConfig.VERSION_CODE.toString())
        .build()
        .toString()
}

private fun Uri.movieSlugFromDeepLink(): String? {
    return when {
        scheme == "alphacinema" && host == MOVIE_LINK_SEGMENT -> pathSegments.firstOrNull()
        scheme == "https" && host == ALPHA_CINEMA_WEB_LINK_HOST &&
            pathSegments.firstOrNull() == MOVIE_LINK_SEGMENT -> pathSegments.getOrNull(1)
        else -> null
    }?.takeIf { it.isNotBlank() }
}

private fun Uri.watchPartyRoomIdFromDeepLink(): String? {
    return when {
        scheme == "alphacinema" && host == WATCH_PARTY_LINK_SEGMENT -> pathSegments.firstOrNull()
        scheme == "https" && host == ALPHA_CINEMA_WEB_LINK_HOST &&
            pathSegments.firstOrNull() == WATCH_PARTY_LINK_SEGMENT -> pathSegments.getOrNull(1)
        else -> null
    }?.takeIf { it.isNotBlank() }
}

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
                    initialPlan = initialPlan,
                    homeViewModel = homeViewModel
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
    initialPlan: String? = null,
    homeViewModel: HomeViewModel = viewModel()
) {
    val navController = rememberNavController()
    var currentMainScreen by remember { mutableStateOf(ScreenType.HOME) }
    val scope = rememberCoroutineScope()
    val backNavigationLock = remember { AtomicBoolean(false) }
    val firestoreRepo = remember { com.example.alphacinema.data.repository.FirestoreRepository() }

    val context = androidx.compose.ui.platform.LocalContext.current
    val isKidsMode by homeViewModel.isKidsMode.collectAsState()
    val profileCache = remember { com.example.alphacinema.data.local.UserProfileCache(context) }
    val movieDetailViewModel: MovieDetailViewModel = viewModel()
    val movieDetail by movieDetailViewModel.movieDetail.collectAsState()
    val detailLoading by movieDetailViewModel.isLoading.collectAsState()
    val detailError by movieDetailViewModel.error.collectAsState()
    val episodeVideoUrls by movieDetailViewModel.episodeVideoUrls.collectAsState()
    val isFavorite by movieDetailViewModel.isFavorite.collectAsState()
    val playlists by movieDetailViewModel.playlists.collectAsState()
    val playlistIdsForCurrentMovie by movieDetailViewModel.playlistIdsForCurrentMovie.collectAsState()
    val playlistActionInProgress by movieDetailViewModel.playlistActionInProgress.collectAsState()
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
    val wpRoomDismissed by watchPartyViewModel.roomDismissed.collectAsState()
    val wpError by watchPartyViewModel.error.collectAsState()
    val wpIsCreating by watchPartyViewModel.isCreating.collectAsState()
    val wpIsJoining by watchPartyViewModel.isJoining.collectAsState()
    var watchPartyLobbyMovie by remember { mutableStateOf<com.example.alphacinema.ui.movie.detail.MovieDetailUi?>(null) }
    var pendingWatchPartyMovie by remember { mutableStateOf<com.example.alphacinema.ui.movie.detail.MovieDetailUi?>(null) }
    var showWatchPartyLobby by remember { mutableStateOf(false) }
    val auth = remember { com.google.firebase.auth.FirebaseAuth.getInstance() }
    var appCurrentUser by remember { mutableStateOf(auth.currentUser) }
    var appAuthLoading by remember { mutableStateOf(false) }
    var appAuthError by remember { mutableStateOf<String?>(null) }
    var appLogoutInProgress by remember { mutableStateOf(false) }

    fun showToast(message: String) {
        android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_SHORT).show()
    }

    fun popBackStackSafely(): Boolean {
        val currentDestination = navController.currentBackStackEntry?.destination
        if (currentDestination == null) {
            return false
        }
        if (currentDestination.hasRoute<MainRoute>()) {
            return false
        }
        if (!backNavigationLock.compareAndSet(false, true)) {
            return false
        }

        val popped = navController.popBackStack()
        if (!popped) {
            backNavigationLock.set(false)
        } else {
            scope.launch {
                delay(ANIM_DURATION.toLong())
                backNavigationLock.set(false)
            }
        }
        return popped
    }

    fun currentCommentAvatarUrl(user: com.google.firebase.auth.FirebaseUser?): String {
        if (user == null) return ""
        val cachedAvatar = profileCache.avatarUrl
            .takeIf { profileCache.uid == user.uid && it.isNotBlank() }
        return cachedAvatar ?: user.photoUrl?.toString().orEmpty()
    }

    var demoCurrentPlan by rememberSaveable { mutableStateOf<String?>(null) }
    var demoMembershipStartedDate by rememberSaveable { mutableStateOf<String?>(null) }
    var demoMembershipExpiredDate by rememberSaveable { mutableStateOf<String?>(null) }
    var currentPlanEntitlements by remember { mutableStateOf(SubscriptionPlan.FREE.entitlements()) }

    fun applySubscriptionProfile(profile: com.example.alphacinema.data.model.UserProfile?) {
        val entitlements = profile.activeEntitlements()
        currentPlanEntitlements = entitlements
        if (entitlements.plan == SubscriptionPlan.FREE) {
            demoCurrentPlan = null
            demoMembershipStartedDate = null
            demoMembershipExpiredDate = null
        } else {
            demoCurrentPlan = entitlements.plan.id
            demoMembershipStartedDate = formatFirestoreDate(profile?.subscriptionStartedAt)
            demoMembershipExpiredDate = formatFirestoreDate(profile?.subscriptionExpiresAt)
        }
    }

    fun continueWatchPartyAfterAuth(canCreateWatchParty: Boolean) {
        watchPartyLobbyMovie = pendingWatchPartyMovie
        pendingWatchPartyMovie = null
        showWatchPartyLobby = true
        if (!canCreateWatchParty) {
            showToast("Bạn có thể tham gia bằng mã phòng. Tạo phòng cần gói Couple hoặc Premium")
        }
    }

    val appAuthStateHolder = rememberAccountAuthStateHolder(
        onLogin = { email, password, onSuccess ->
            scope.launch {
                appAuthLoading = true
                appAuthError = null
                try {
                    val result = auth.signInWithEmailAndPassword(email, password).await()
                    val entitlements = result.user?.let {
                        firestoreRepo.saveUser(it)
                        val profile = firestoreRepo.getUserProfile(it.uid)
                        applySubscriptionProfile(profile)
                        profile.activeEntitlements()
                    } ?: SubscriptionPlan.FREE.entitlements()
                    continueWatchPartyAfterAuth(entitlements.canCreateWatchParty)
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
                    val entitlements = (auth.currentUser ?: result.user)?.let {
                        firestoreRepo.saveUser(it)
                        val profile = firestoreRepo.getUserProfile(it.uid)
                        applySubscriptionProfile(profile)
                        profile.activeEntitlements()
                    } ?: SubscriptionPlan.FREE.entitlements()
                    continueWatchPartyAfterAuth(entitlements.canCreateWatchParty)
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
                    val entitlements = result.user?.let {
                        firestoreRepo.saveUser(it)
                        val profile = firestoreRepo.getUserProfile(it.uid)
                        applySubscriptionProfile(profile)
                        profile.activeEntitlements()
                    } ?: SubscriptionPlan.FREE.entitlements()
                    continueWatchPartyAfterAuth(entitlements.canCreateWatchParty)
                    onSuccess()
                } catch (e: Exception) {
                    appAuthError = "Đăng nhập Google thất bại"
                } finally {
                    appAuthLoading = false
                }
            }
        }
    )
    DisposableEffect(auth) {
        val listener = com.google.firebase.auth.FirebaseAuth.AuthStateListener { firebaseAuth ->
            appCurrentUser = firebaseAuth.currentUser
        }
        auth.addAuthStateListener(listener)
        onDispose { auth.removeAuthStateListener(listener) }
    }

    // Keep feature entitlements in sync with login/logout changes from any screen.
    LaunchedEffect(appCurrentUser?.uid) {
        val user = appCurrentUser
        if (user != null) {
            try {
                firestoreRepo.saveUser(user)
                val profile = firestoreRepo.getUserProfile(user.uid)
                applySubscriptionProfile(profile)
            } catch (e: Exception) {
                android.util.Log.e("AppScreen", "Failed to sync subscription profile", e)
                applySubscriptionProfile(null)
            }
        } else {
            applySubscriptionProfile(null)
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

    fun shareMovie(movie: MovieDetailUi) {
        if (movie.id.isBlank()) {
            showToast("Không tìm thấy phim để chia sẻ.")
            return
        }

        val shareText = movieShareLink(movie.id)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, shareText)
            putExtra(Intent.EXTRA_TITLE, movie.title)
            putExtra(Intent.EXTRA_SUBJECT, movie.title)
        }

        runCatching {
            context.startActivity(Intent.createChooser(intent, "Chia sẻ phim"))
        }.onFailure {
            showToast("Không tìm thấy ứng dụng để chia sẻ.")
        }
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
        val isAlreadyOnMain = navController.currentBackStackEntry
            ?.destination
            ?.hasRoute<MainRoute>() == true
        val poppedToMain = if (isAlreadyOnMain) {
            true
        } else {
            navController.popBackStack(navController.graph.startDestinationId, inclusive = false)
        }
        if (!poppedToMain) {
            navController.navigate(MainRoute) {
                launchSingleTop = true
            }
        }
    }

    fun returnToAccountScreen() {
        currentMainScreen = ScreenType.ACCOUNT
        val isAlreadyOnMain = navController.currentBackStackEntry
            ?.destination
            ?.hasRoute<MainRoute>() == true
        val poppedToMain = if (isAlreadyOnMain) {
            true
        } else {
            navController.popBackStack(navController.graph.startDestinationId, inclusive = false)
        }
        if (!poppedToMain) {
            navController.navigate(MainRoute) {
                launchSingleTop = true
            }
        }
    }

    fun performLogout() {
        if (appLogoutInProgress) return
        appLogoutInProgress = true

        scope.launch {
            try {
                watchPartyViewModel.leaveRoom()

                // Remove FCM token from Firestore
                val currentUser = auth.currentUser
                if (currentUser != null) {
                    try {
                        val token = com.google.firebase.messaging.FirebaseMessaging.getInstance().token.await()
                        if (!token.isNullOrBlank()) {
                            val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                            db.collection("users").document(currentUser.uid)
                                .update("fcmTokens", com.google.firebase.firestore.FieldValue.arrayRemove(token))
                                .await()
                            android.util.Log.d("AppScreen", "FCM token removed from Firestore")
                        }
                    } catch (fcmEx: Exception) {
                        android.util.Log.e("AppScreen", "Failed to clear FCM token from Firestore", fcmEx)
                    }
                }

                // Delete local FCM token
                try {
                    com.google.firebase.messaging.FirebaseMessaging.getInstance().deleteToken().await()
                    android.util.Log.d("AppScreen", "Local FCM token deleted")
                } catch (fcmEx: Exception) {
                    android.util.Log.e("AppScreen", "Failed to delete local FCM token", fcmEx)
                }

                auth.signOut()
                appCurrentUser = null
                profileCache.clear()
                applySubscriptionProfile(null)
                returnToAccountScreen()
                showToast("Đã đăng xuất")
            } catch (e: Exception) {
                android.util.Log.e("AppScreen", "Logout failed", e)
                showToast("Không thể đăng xuất. Vui lòng thử lại")
            } finally {
                appLogoutInProgress = false
            }
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

    fun leaveWatchParty() {
        watchPartyViewModel.leaveRoom()
        popBackStackSafely()
    }

    // Handle deep links from custom scheme and clickable HTTPS share links.
    val deepLinkHandled = remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (!deepLinkHandled.value) {
            val activity = (context as? android.app.Activity)
            val data = activity?.intent?.data
            if (data != null) {
                val roomId = data.watchPartyRoomIdFromDeepLink()
                val slug = data.movieSlugFromDeepLink()

                when {
                    roomId != null -> {
                        deepLinkHandled.value = true
                        watchPartyViewModel.joinRoom(context, roomId) {
                            openWatchParty(roomId)
                        }
                    }
                    slug != null -> {
                        deepLinkHandled.value = true
                        openMovieDetail(slug)
                    }
                }
            }
        }
    }

    LaunchedEffect(wpRoomDismissed) {
        if (!wpRoomDismissed) return@LaunchedEffect

        showToast("Chủ phòng đã giải tán phòng")
        val destination = navController.currentBackStackEntry?.destination
        val isOnWatchPartyRoute = destination?.hasRoute<WatchPartyNavRoute>() == true ||
            destination?.hasRoute<WatchPartySearchNavRoute>() == true

        if (isOnWatchPartyRoute) {
            returnToMainScreen()
        }
    }

    // Theo dõi current route để hiện/ẩn bottom bar
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val isOnMainRoute = navBackStackEntry?.destination?.hasRoute<MainRoute>() == true
    val isKeyboardVisible = WindowInsets.ime.asPaddingValues().calculateBottomPadding() > 0.dp

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
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
                enterTransition = { fadeIn(tween(MAIN_RETURN_ANIM_DURATION)) },
                exitTransition = {
                    fadeOut(tween(ANIM_DURATION_FAST))
                },
                popEnterTransition = {
                    fadeIn(tween(MAIN_RETURN_ANIM_DURATION))
                },
                popExitTransition = { fadeOut(tween(ANIM_DURATION_FAST)) }
            ) {
                // Giữ tab navigation bằng Crossfade bên trong MainRoute
                Crossfade(
                    targetState = currentMainScreen,
                    animationSpec = tween(TAB_SWITCH_ANIM_DURATION),
                    modifier = Modifier.fillMaxSize(),
                    label = "TabTransition"
                ) { screen ->
                    when (screen) {
                        ScreenType.HOME -> HomeScreen(
                            viewModel = homeViewModel,
                            initialShowNotification = initialShowNotification,
                            onPlayMovie = ::openPlayerFromHome,
                            onOpenMovieDetail = ::openMovieDetail,
                            onNavigateToPlan = {
                                navController.navigate(PaymentNavRoute())
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
                            },
                            onContinueWatchingMovie = { slug, episodeId, startPositionMs ->
                                openPlayer(
                                    slug = slug,
                                    episodeId = episodeId,
                                    startPositionMs = startPositionMs
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
                            onOpenPlaylistsPage = { navController.navigate(PlaylistsNavRoute) },
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
                                navController.navigate(
                                    PaymentNavRoute(currentPlan = currentPlanEntitlements.plan.id)
                                )
                            },
                            onOpenProfileSettings = {
                                navController.navigate(ProfileSettingsNavRoute)
                            },
                            onLogout = {
                                applySubscriptionProfile(null)
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
                        RouteLoadingState()
                } else if (detailMovie != null) {
                    MovieDetailScreen(
                        movie = detailMovie,
                        isFavorite = isFavorite,
                        movieStats = movieStats,
                        userRating = userRating,
                        comments = comments,
                        playlists = playlists,
                        playlistIdsForMovie = playlistIdsForCurrentMovie,
                        playlistActionInProgress = playlistActionInProgress,
                        canUsePlaylist = currentPlanEntitlements.playlist,
                        currentUserId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid,
                        currentUserAvatarUrl = currentCommentAvatarUrl(
                            com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
                        ),
                        onToggleFavorite = { movie ->
                            movieDetailViewModel.toggleFavorite(movie.title, movie.posterUrl) { _, msg ->
                                showToast(msg)
                            }
                        },
                        onCreatePlaylist = { playlistName, movie ->
                            movieDetailViewModel.createPlaylistAndAddMovie(playlistName, movie) { _, msg ->
                                showToast(msg)
                            }
                        },
                        onTogglePlaylistMovie = { playlistId, isInPlaylist, movie ->
                            movieDetailViewModel.toggleMovieInPlaylist(
                                playlistId = playlistId,
                                movie = movie,
                                isInPlaylist = isInPlaylist
                            ) { _, msg -> showToast(msg) }
                        },
                        onRenamePlaylist = { playlistId, playlistName ->
                            movieDetailViewModel.renamePlaylist(playlistId, playlistName) { _, msg ->
                                showToast(msg)
                            }
                        },
                        onDeletePlaylist = { playlistId ->
                            movieDetailViewModel.deletePlaylist(playlistId) { _, msg ->
                                showToast(msg)
                            }
                        },
                        onAddToListLoginRequired = {
                            showToast("Vui lòng đăng nhập để lưu vào danh sách phát")
                        },
                        onPlaylistUpgradeRequired = {
                            showToast("Playlist cần gói Basic trở lên")
                        },
                        onPostComment = { content ->
                            val user = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
                            movieDetailViewModel.postComment(
                                user?.displayName ?: "Ẩn danh",
                                currentCommentAvatarUrl(user),
                                content
                            ) { _, msg -> showToast(msg) }
                        },
                        onReplyComment = { parentComment, content ->
                            val user = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
                            val resolvedParentId = if (parentComment.parentCommentId.isNotBlank()) {
                                parentComment.parentCommentId
                            } else {
                                parentComment.id
                            }
                            movieDetailViewModel.postComment(
                                user?.displayName ?: "Ẩn danh",
                                currentCommentAvatarUrl(user),
                                content,
                                parentCommentId = resolvedParentId,
                                replyToUserName = parentComment.userName.ifBlank { "Người dùng" }
                            ) { _, msg -> showToast(msg) }
                        },
                        onToggleCommentLike = { comment ->
                            movieDetailViewModel.toggleCommentLike(comment.id) { _, msg ->
                                showToast(msg)
                            }
                        },
                        onSubmitRating = { score ->
                            movieDetailViewModel.submitRating(score) { _, msg ->
                                showToast(msg)
                            }
                        },
                        onBack = { popBackStackSafely() },
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
                                pendingWatchPartyMovie = detailMovie
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
                        onShareMovie = ::shareMovie,
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
                    RouteLoadingState()
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
                            onBack = { popBackStackSafely() },
                            videoUrl = videoUrl,
                            episodeVideoUrls = episodeVideoUrls,
                            startPositionMs = route.startPositionMs,
                            adTagUrl = BuildConfig.IMA_AD_TAG_URL,
                            adsEnabled = !currentPlanEntitlements.adFree,
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
                    onBack = { popBackStackSafely() },
                    onOpenMovieDetail = ::openMovieDetail
                )
            }

            // ── Movie Type (Phim bộ / Phim lẻ) ──────────────────────
            composable<MovieTypeNavRoute> { backStackEntry ->
                val route = backStackEntry.toRoute<MovieTypeNavRoute>()
                MovieTypeScreen(
                    type = route.type,
                    title = route.title,
                    onBack = { popBackStackSafely() },
                    onOpenMovieDetail = ::openMovieDetail
                )
            }

            // ── Admin ───────────────────────────────────────────────────
            composable<AdminNavRoute> {
                AdminScreen(
                    onBack = { popBackStackSafely() },
                    viewModel = adminViewModel
                )
            }

            composable<PaymentNavRoute> { backStackEntry ->
                val route = backStackEntry.toRoute<PaymentNavRoute>()
                PaymentScreen(
                    onBack = { popBackStackSafely() },
                    currentPlan = route.currentPlan,
                    onPaymentConfirmed = {
                        val user = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
                        if (user != null) {
                            scope.launch {
                                val profile = firestoreRepo.getUserProfile(user.uid)
                                applySubscriptionProfile(profile)
                            }
                        }
                        showToast("MoMo đã xác nhận thanh toán")
                    }
                )
            }

            // ── Profile Settings ─────────────────────────────────────────
            composable<ProfileSettingsNavRoute> {
                ProfileSettingsScreen(
                    onBack = { popBackStackSafely() },
                    onOpenPayment = {
                        navController.navigate(
                            PaymentNavRoute(currentPlan = currentPlanEntitlements.plan.id)
                        )
                    },
                    onLogout = ::performLogout
                )
            }

            composable<WatchHistoryNavRoute> {
                WatchHistoryScreen(
                    onBack = { popBackStackSafely() },
                    onOpenWatchHistoryItem = { slug, episodeId, startPositionMs ->
                        openPlayer(
                            slug = slug,
                            episodeId = episodeId,
                            startPositionMs = startPositionMs
                        )
                    }
                )
            }

            composable<PlaylistsNavRoute> {
                AccountPlaylistsScreen(
                    onBack = { popBackStackSafely() },
                    onOpenMovieDetail = ::openMovieDetail,
                    onOpenPayment = {
                        navController.navigate(
                            PaymentNavRoute(currentPlan = currentPlanEntitlements.plan.id)
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
                        onBack = ::leaveWatchParty
                    )
                } else if (detailLoading || playerMovie == null) {
                    RouteLoadingState()
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
                        onBack = ::leaveWatchParty
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
                        onBackClick = { popBackStackSafely() },
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
                canCreateRoom = currentPlanEntitlements.canCreateWatchParty,
                maxMembers = currentPlanEntitlements.maxWatchPartyMembers.coerceAtLeast(2),
                onDismiss = {
                    showWatchPartyLobby = false
                    watchPartyViewModel.clearError()
                },
                onCreateRoom = {
                    if (!currentPlanEntitlements.canCreateWatchParty) {
                        showToast("Tạo phòng xem chung cần gói Couple hoặc Premium")
                        return@WatchPartyLobbySheet
                    }
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
                    if (event == AccountAuthEvent.CloseDialog) {
                        pendingWatchPartyMovie = null
                    }
                    appAuthStateHolder.onEvent(event)
                },
                onForgotPassword = {
                    pendingWatchPartyMovie = null
                    appAuthStateHolder.onEvent(AccountAuthEvent.CloseDialog)
                }
            )
        }

        // Xử lý Deep Link khi khởi chạy App từ Thông báo (Status bar)
        LaunchedEffect(initialNotificationType, initialMovieId, isKidsMode) {
            if (isKidsMode) {
                return@LaunchedEffect
            }
            if (initialNotificationType == "new_movie" && initialMovieId != null) {
                navController.navigate(MovieDetailNavRoute(slug = initialMovieId))
            } else if (initialNotificationType == "billing") {
                navController.navigate(PaymentNavRoute())
            }
        }

        if (isOnMainRoute && !isKeyboardVisible) {
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
private fun RouteLoadingState() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        LottieLoadingIndicator(size = 120.dp)
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
            .background(Color.Black),
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
                        contentColor = Color.Black
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
