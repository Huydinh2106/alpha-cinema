@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.alphacinema.ui.account

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForwardIos
import androidx.compose.material.icons.automirrored.rounded.ExitToApp
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.PersonOutline
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.RemoveRedEye
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.WatchLater
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import com.example.alphacinema.ui.components.LottieLoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.LaunchedEffect
import com.example.alphacinema.data.local.SettingsManager
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import com.example.alphacinema.util.formatFirestoreDate
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import com.example.alphacinema.data.api.EmailVerificationHelper
import com.example.alphacinema.data.api.OtpVerifyResult
import kotlinx.coroutines.tasks.await

private const val WEB_CLIENT_ID = "1013232588133-86fl74ls04r8fkarnahh2b9pvie5g7kn.apps.googleusercontent.com"

private data class AccountMenuItemUi(
    val title: String,
    val icon: @Composable () -> Unit,
    val action: AccountMenuAction? = null
)

enum class PinDialogMode { SETUP, VERIFY }

private enum class AccountMenuAction {
    WATCH_HISTORY,
    MOVIE_LIBRARY,
    FAVORITES,
    POLICY,
    FEEDBACK,
    ADMIN,
    WATCH_TOGETHER
}

private enum class AccountPanelType {
    MOVIE_LIBRARY,
    FAVORITES,
    POLICY,
    FEEDBACK
}

internal enum class DemoMembershipPlanKey {
    FREE,
    BASIC,
    COUPLE,
    PREMIUM
}

internal data class DemoMembershipPlanUi(
    val key: DemoMembershipPlanKey,
    val badge: String,
    val title: String,
    val price: String,
    val description: String,
    val benefits: List<String>,
    val startedDate: String = "",
    val expiredDate: String = ""
)

private data class DemoUserProfileUi(
    val name: String,
    val avatarUrl: String,
    val currentPlan: DemoMembershipPlanUi
)

@Composable
fun AccountScreen(
    onOpenAdminPanel: () -> Unit = {},
    onOpenMovieDetail: (String) -> Unit = {},
    onOpenWatchHistoryPage: () -> Unit = {},
    onOpenMovieList: (title: String, filterKind: FilterKind, slug: String) -> Unit = { _, _, _ -> },
    onWatchTogether: () -> Unit = {},
    onOpenPayment: () -> Unit = {},
    onOpenProfileSettings: () -> Unit = {},
    onLogout: () -> Unit = {},
    currentPlan: String? = null,
    membershipStartedDate: String? = null,
    membershipExpiredDate: String? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val auth = remember { FirebaseAuth.getInstance() }
    var currentUser by remember { mutableStateOf(auth.currentUser) }

    val firestoreRepository = remember { com.example.alphacinema.data.repository.FirestoreRepository() }
    var userProfile by remember { mutableStateOf<com.example.alphacinema.data.model.UserProfile?>(null) }
    var loadedProfileUid by remember { mutableStateOf<String?>(null) }

    DisposableEffect(auth) {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            currentUser = firebaseAuth.currentUser
        }
        auth.addAuthStateListener(listener)
        onDispose {
            auth.removeAuthStateListener(listener)
        }
    }

    LaunchedEffect(currentUser?.uid) {
        val user = currentUser
        if (user != null) {
            loadedProfileUid = null
            firestoreRepository.saveUser(user)
            userProfile = firestoreRepository.getUserProfile(user.uid)
            loadedProfileUid = user.uid
        } else {
            userProfile = null
            loadedProfileUid = null
        }
    }

    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // ── Trạng thái OTP verification ──────────────────────────────────────────
    var showOtpScreen by remember { mutableStateOf(false) }
    var pendingName by remember { mutableStateOf("") }
    var pendingEmail by remember { mutableStateOf("") }
    var pendingPassword by remember { mutableStateOf("") }
    var otpLoading by remember { mutableStateOf(false) }
    var otpSuccess by remember { mutableStateOf(false) }
    var otpError by remember { mutableStateOf<String?>(null) }
    var clearOtpTrigger by remember { mutableIntStateOf(0) }
    var showForgotPassword by remember { mutableStateOf(false) }

    val authStateHolder = rememberAccountAuthStateHolder(
        onLogin = { email, password, onSuccess ->
            scope.launch {
                isLoading = true
                errorMessage = null
                try {
                    val result = auth.signInWithEmailAndPassword(email, password).await()
                    result.user?.let {
                        firestoreRepository.saveUser(it)
                        userProfile = firestoreRepository.getUserProfile(it.uid)
                    }
                    currentUser = result.user
                    onSuccess()
                } catch (e: Exception) {
                    errorMessage = e.localizedMessage ?: "Đăng nhập thất bại"
                } finally {
                    isLoading = false
                }
            }
        },
        onRegister = { name, email, password, onSuccess ->
            // Gửi mã OTP trước khi tạo tài khoản Firebase
            scope.launch {
                isLoading = true
                errorMessage = null
                try {
                    // 1. Kiểm tra email đã tồn tại hay chưa
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
                        errorMessage = "Email đã được sử dụng. Vui lòng chọn email khác."
                        return@launch
                    }

                    // 2. Gửi mã OTP
                    val sent = EmailVerificationHelper.sendOtp(email, "Đăng ký tài khoản")
                    if (sent) {
                        // Lưu thông tin đăng ký tạm, chờ xác thực OTP
                        pendingName = name
                        pendingEmail = email
                        pendingPassword = password
                        otpError = null
                        otpSuccess = false
                        showOtpScreen = true
                        onSuccess()
                    } else {
                        errorMessage = "Không thể gửi mã xác thực. Vui lòng kiểm tra email hoặc thử lại sau."
                    }
                } catch (e: Exception) {
                    errorMessage = "Lỗi gửi mã xác thực: ${e.localizedMessage}"
                } finally {
                    isLoading = false
                }
            }
        },
        onGoogleSignIn = { onSuccess ->
            scope.launch {
                isLoading = true
                errorMessage = null
                try {
                    val credentialManager = CredentialManager.create(context)
                    val signInWithGoogleOption = GetGoogleIdOption.Builder()
                        .setFilterByAuthorizedAccounts(false)
                        .setServerClientId(WEB_CLIENT_ID)
                        .setAutoSelectEnabled(false)
                        .setNonce(null)
                        .build()
                    val request = GetCredentialRequest.Builder()
                        .addCredentialOption(signInWithGoogleOption)
                        .build()
                    val credentialResponse = credentialManager.getCredential(
                        request = request,
                        context = context as android.app.Activity
                    )
                    val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credentialResponse.credential.data)
                    val firebaseCredential = GoogleAuthProvider.getCredential(googleIdTokenCredential.idToken, null)
                    val result = auth.signInWithCredential(firebaseCredential).await()
                    result.user?.let {
                        firestoreRepository.saveUser(it)
                        userProfile = firestoreRepository.getUserProfile(it.uid)
                    }
                    currentUser = result.user
                    onSuccess()
                } catch (e: androidx.credentials.exceptions.NoCredentialException) {
                    errorMessage = "Không tìm thấy tài khoản Google. Hãy đăng nhập Google trên thiết bị trước."
                } catch (e: Exception) {
                    errorMessage = e.localizedMessage ?: "Đăng nhập Google thất bại"
                } finally {
                    isLoading = false
                }
            }
        }
    )
    val state = authStateHolder.uiState

    val settingsManager = remember { SettingsManager.getInstance() }
    val isKidsModeEnabled by settingsManager.isKidsModeEnabled.collectAsState()
    val favoritesFlow = remember(currentUser?.uid) {
        currentUser?.uid?.let(firestoreRepository::getFavorites) ?: flowOf(emptyList())
    }
    val favorites by favoritesFlow.collectAsState(initial = emptyList())

    var showPinDialog by remember { mutableStateOf(false) }
    var pinDialogMode by remember { mutableStateOf<PinDialogMode>(PinDialogMode.SETUP) }
    var pinInput by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf<String?>(null) }
    var activePanel by remember { mutableStateOf<AccountPanelType?>(null) }
    var feedbackInput by remember { mutableStateOf("") }
    var showPlanManagement by remember { mutableStateOf(false) }
    var showCancelRenewDialog by remember { mutableStateOf(false) }
    var showLoginRequiredDialog by remember { mutableStateOf(false) }
    val isLoggedIn = currentUser != null
    val profileStartedDate = formatFirestoreDate(userProfile?.subscriptionStartedAt)
    val profileExpiredDate = formatFirestoreDate(userProfile?.subscriptionExpiresAt)
    val demoMembershipPlan = remember(
        isLoggedIn,
        currentPlan,
        userProfile?.subscriptionPlan,
        profileStartedDate,
        profileExpiredDate,
        membershipStartedDate,
        membershipExpiredDate
    ) {
        if (isLoggedIn) {
            buildDemoMembershipPlan(
                currentPlan = userProfile?.subscriptionPlan ?: currentPlan,
                expiredDate = profileExpiredDate ?: membershipExpiredDate,
                startedDate = profileStartedDate ?: membershipStartedDate
            )
        } else {
            null
        }
    }
    val profileCache = remember { com.example.alphacinema.data.local.UserProfileCache(context) }
    val demoUser = remember(currentUser, demoMembershipPlan, isLoggedIn, userProfile) {
        if (isLoggedIn && demoMembershipPlan != null) {
            val name = currentUser?.displayName
                ?: userProfile?.displayName
                ?: profileCache.displayName.ifBlank { "Người dùng" }
            // Use local cached file first, then remote URL
            val localFile = profileCache.localAvatarFile
            val avatar = if (localFile != null) {
                localFile.absolutePath
            } else {
                currentUser?.photoUrl?.toString()
                    ?: userProfile?.photoUrl
                    ?: profileCache.avatarUrl
            }
            DemoUserProfileUi(
                name = name,
                avatarUrl = avatar.orEmpty(),
                currentPlan = demoMembershipPlan
            )
        } else {
            null
        }
    }

    val menuItems = mutableListOf(
        AccountMenuItemUi("Lịch sử xem", { Icon(Icons.Rounded.History, contentDescription = null) }),
        AccountMenuItemUi("Yêu thích", { Icon(Icons.Rounded.FavoriteBorder, contentDescription = null) }),
        AccountMenuItemUi("Chính sách", { Icon(Icons.Rounded.Info, contentDescription = null) }),
        AccountMenuItemUi("Góp ý", { Icon(Icons.Rounded.ChatBubbleOutline, contentDescription = null) })
    ).apply {
        val isAdminUser = userProfile?.isAdmin == true || currentUser?.email == "admin@alphacinema.com"
        if (isAdminUser) {
            add(0, AccountMenuItemUi("Quản trị phim", { Icon(Icons.Rounded.Settings, contentDescription = null, tint = Color(0xFFF6E29A)) }))
        }
    }
    
    // Debug log (can be seen in Logcat)
    android.util.Log.d("AccountScreen", "User: ${currentUser?.email}, Profile: ${userProfile?.email}, isAdmin: ${userProfile?.isAdmin}")

    fun openPanel(panel: AccountPanelType, requiresLogin: Boolean = false) {
        if (requiresLogin && currentUser == null) {
            showLoginRequiredDialog = true
            return
        }
        activePanel = panel
    }

    fun handleMenuAction(action: AccountMenuAction) {
        when (action) {
            AccountMenuAction.WATCH_HISTORY -> {
                if (isLoggedIn) onOpenWatchHistoryPage() else showLoginRequiredDialog = true
            }
            AccountMenuAction.MOVIE_LIBRARY -> {
                if (isLoggedIn) openPanel(AccountPanelType.MOVIE_LIBRARY) else showLoginRequiredDialog = true
            }
            AccountMenuAction.FAVORITES -> openPanel(AccountPanelType.FAVORITES, requiresLogin = true)
            AccountMenuAction.POLICY -> openPanel(AccountPanelType.POLICY)
            AccountMenuAction.FEEDBACK -> openPanel(AccountPanelType.FEEDBACK)
            AccountMenuAction.ADMIN -> onOpenAdminPanel()
            AccountMenuAction.WATCH_TOGETHER -> {
                if (isLoggedIn) onWatchTogether() else showLoginRequiredDialog = true
            }
        }
    }

    fun resolveMenuAction(item: AccountMenuItemUi): AccountMenuAction {
        return item.action ?: when (item.title) {
            "Lịch sử xem" -> AccountMenuAction.WATCH_HISTORY
            "Danh sách phim" -> AccountMenuAction.MOVIE_LIBRARY
            "Yêu thích" -> AccountMenuAction.FAVORITES
            "Xem chung" -> AccountMenuAction.WATCH_TOGETHER
            "Chính sách" -> AccountMenuAction.POLICY
            "Góp ý" -> AccountMenuAction.FEEDBACK
            "Quản trị phim" -> AccountMenuAction.ADMIN
            else -> AccountMenuAction.POLICY
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 18.dp)
                // Extra bottom space to avoid content hiding behind GlassBottomBar
                .padding(bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Tài khoản",
                color = Color.White,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold
            )

            if (isLoggedIn && demoUser != null && demoMembershipPlan != null) {
                LoggedInProfileCard(
                    demoUser = demoUser,
                    onClick = onOpenProfileSettings
                )
                if (
                    demoMembershipPlan.key == DemoMembershipPlanKey.FREE &&
                    loadedProfileUid == currentUser?.uid
                ) {
                    FreePlanUpgradeBanner(onClick = onOpenPayment)
                }
            } else {
                GuestProfileCard(
                    onLogin = { authStateHolder.onEvent(AccountAuthEvent.OpenDialog(AuthMode.LOGIN)) },
                    onRegister = { authStateHolder.onEvent(AccountAuthEvent.OpenDialog(AuthMode.REGISTER)) }
                )
            }

            // Error message
            if (errorMessage != null) {
                Text(
                    text = errorMessage ?: "",
                    color = Color(0xFFFF6B6B),
                    fontSize = 13.sp,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }

            // Loading indicator
            if (isLoading) {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    LottieLoadingIndicator(size = 80.dp)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            KidsModeCard(
                isKidsModeEnabled = isKidsModeEnabled,
                currentUser = currentUser,
                onRequireLogin = { authStateHolder.onEvent(AccountAuthEvent.OpenDialog(AuthMode.LOGIN)) },
                onToggleMode = { checked ->
                    if (checked) {
                        if (settingsManager.getKidsModePin() == null) {
                            pinDialogMode = PinDialogMode.SETUP
                            pinInput = ""
                            pinError = null
                            showPinDialog = true
                        } else {
                            settingsManager.setKidsMode(true)
                        }
                    } else {
                        if (settingsManager.getKidsModePin() != null) {
                            pinDialogMode = PinDialogMode.VERIFY
                            pinInput = ""
                            pinError = null
                            showPinDialog = true
                        } else {
                            settingsManager.setKidsMode(false)
                        }
                    }
                }
            )

            WatchTogetherHighlightCard(
                onClick = { handleMenuAction(AccountMenuAction.WATCH_TOGETHER) }
            )

            AccountMenuList(
                items = menuItems,
                onItemClick = { item -> handleMenuAction(resolveMenuAction(item)) }
            )


            if (showPinDialog) {
                AlertDialog(
                    onDismissRequest = { showPinDialog = false },
                    containerColor = Color(0xFF141414),
                    titleContentColor = Color.White,
                    textContentColor = Color.White.copy(alpha = 0.8f),
                    title = {
                        Text(if (pinDialogMode == PinDialogMode.SETUP) "Cài đặt mã PIN" else "Nhập mã PIN")
                    },
                    text = {
                        Column {
                            Text(
                                if (pinDialogMode == PinDialogMode.SETUP)
                                    "Thiết lập mã PIN 4 số để bảo vệ chế độ trẻ em."
                                else "Nhập mã PIN để tắt chế độ trẻ em."
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedTextField(
                                value = pinInput,
                                onValueChange = { if (it.length <= 4) pinInput = it.filter { c -> c.isDigit() } },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                isError = pinError != null,
                                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.NumberPassword),
                                visualTransformation = PasswordVisualTransformation(),
                                colors = authTextFieldColors(),
                                supportingText = { if (pinError != null) Text(pinError!!) }
                            )
                        }
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                if (pinInput.length < 4) {
                                    pinError = "Mã PIN phải đủ 4 số"
                                } else {
                                    if (pinDialogMode == PinDialogMode.SETUP) {
                                        settingsManager.setKidsModePin(pinInput)
                                        settingsManager.setKidsMode(true)
                                        showPinDialog = false
                                    } else {
                                        if (pinInput == settingsManager.getKidsModePin()) {
                                            settingsManager.setKidsMode(false)
                                            showPinDialog = false
                                        } else {
                                            pinError = "Mã PIN không đúng"
                                        }
                                    }
                                }
                            }
                        ) {
                            Text("Xác nhận", color = Color(0xFFF6E29A), fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showPinDialog = false }) {
                            Text("Hủy", color = Color.White.copy(alpha = 0.7f))
                        }
                    }
                )
            }

        }

        if (activePanel != null) {
            AccountPanelBottomSheet(
                panel = activePanel!!,
                favorites = favorites,
                feedbackInput = feedbackInput,
                onDismiss = { activePanel = null },
                onFeedbackChange = { feedbackInput = it },
                onSubmitFeedback = {
                    val content = feedbackInput.trim()
                    if (content.isBlank()) {
                        android.widget.Toast.makeText(context, "Vui lòng nhập nội dung góp ý", android.widget.Toast.LENGTH_SHORT).show()
                    } else {
                        android.widget.Toast.makeText(context, "Cảm ơn bạn đã gửi góp ý", android.widget.Toast.LENGTH_SHORT).show()
                        feedbackInput = ""
                        activePanel = null
                    }
                },
                onOpenMovieDetail = { slug ->
                    activePanel = null
                    onOpenMovieDetail(slug)
                },
                onOpenMovieList = { title, filterKind, slug ->
                    activePanel = null
                    onOpenMovieList(title, filterKind, slug)
                }
            )
        }

        if (showPlanManagement && demoMembershipPlan != null) {
            PlanManagementSheet(
                plan = demoMembershipPlan,
                onDismiss = { showPlanManagement = false },
                onChangePlan = {
                    showPlanManagement = false
                    onOpenPayment()
                }
            )
        }

        if (showLoginRequiredDialog) {
            AlertDialog(
                onDismissRequest = { showLoginRequiredDialog = false },
                containerColor = Color(0xFF141414),
                titleContentColor = Color.White,
                textContentColor = Color.White.copy(alpha = 0.78f),
                title = { Text("Cần đăng nhập") },
                text = { Text("Vui lòng đăng nhập để sử dụng tính năng này.") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showLoginRequiredDialog = false
                            authStateHolder.onEvent(AccountAuthEvent.OpenDialog(AuthMode.LOGIN))
                        }
                    ) {
                        Text("Đăng nhập", color = Color(0xFFF6E29A), fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showLoginRequiredDialog = false }) {
                        Text("Để sau", color = Color.White.copy(alpha = 0.7f))
                    }
                }
            )
        }

        if (state.showDialog) {
            AuthBottomSheet(
                state = state,
                isLoading = isLoading,
                errorMessage = errorMessage,
                onEvent = authStateHolder::onEvent,
                onForgotPassword = {
                    authStateHolder.onEvent(AccountAuthEvent.CloseDialog)
                    showForgotPassword = true
                }
            )
        }

        // ── Màn hình xác thực OTP (overlay toàn màn hình) ────────────────────
        if (showOtpScreen) {
            EmailVerificationScreen(
                email = pendingEmail,
                onVerifyCode = { code ->
                    otpLoading = true
                    otpError = null
                    val result = EmailVerificationHelper.verifyOtp(code)
                    when (result) {
                        OtpVerifyResult.SUCCESS -> {
                            // Mã đúng → tạo tài khoản Firebase
                            scope.launch {
                                try {
                                    val authResult = auth.createUserWithEmailAndPassword(
                                        pendingEmail, pendingPassword
                                    ).await()
                                    authResult.user?.updateProfile(
                                        UserProfileChangeRequest.Builder()
                                            .setDisplayName(pendingName)
                                            .build()
                                    )?.await()
                                    otpSuccess = true
                                    otpLoading = false
                                    // Sau 2 giây (EmailVerificationScreen tự redirect),
                                    // cập nhật user và đóng màn hình OTP
                                    kotlinx.coroutines.delay(2200)
                                    currentUser = auth.currentUser
                                    showOtpScreen = false
                                    otpSuccess = false
                                } catch (e: Exception) {
                                    otpError = e.localizedMessage ?: "Đăng ký thất bại"
                                    otpLoading = false
                                }
                            }
                        }
                        OtpVerifyResult.WRONG_CODE -> {
                            otpError = "Mã xác thực không đúng"
                            otpLoading = false
                            clearOtpTrigger++ // Trigger xóa mã trên giao diện
                        }
                        OtpVerifyResult.EXPIRED -> {
                            otpError = "Mã xác thực đã hết hạn. Vui lòng gửi lại."
                            otpLoading = false
                            clearOtpTrigger++ // Trigger xóa mã trên giao diện
                        }
                        OtpVerifyResult.NO_OTP_SENT -> {
                            otpError = "Chưa gửi mã. Vui lòng thử lại."
                            otpLoading = false
                        }
                    }
                },
                onResendCode = {
                    scope.launch {
                        otpError = null
                        val sent = EmailVerificationHelper.sendOtp(pendingEmail, "Đăng ký tài khoản")
                        if (!sent) {
                            otpError = "Không thể gửi lại mã. Vui lòng thử lại."
                        }
                    }
                },
                onBackToLogin = {
                    EmailVerificationHelper.clearOtp()
                    showOtpScreen = false
                    otpError = null
                    otpSuccess = false
                },
                isLoading = otpLoading,
                isSuccess = otpSuccess,
                errorMessage = otpError,
                clearTrigger = clearOtpTrigger
            )
        }

        // ── Màn hình Quên mật khẩu (overlay toàn màn hình) ───────────────
        if (showForgotPassword) {
            ForgotPasswordScreen(
                onCheckEmailExists = { emailToCheck ->
                    try {
                        // Bắt buộc dùng cách này để lách qua Email Enumeration Protection của Firebase
                        auth.signInWithEmailAndPassword(emailToCheck, "DummyWrongPass123!@#").await()
                        true
                    } catch (e: com.google.firebase.auth.FirebaseAuthInvalidUserException) {
                        false // Lỗi này báo tài khoản không tồn tại
                    } catch (e: Exception) {
                        true // Lỗi khác (ví dụ sai mật khẩu) tức là tài khoản ĐÃ TỒN TẠI
                    }
                },
                onSendOtp = { emailAddr ->
                    EmailVerificationHelper.sendOtp(emailAddr, "Quên mật khẩu")
                },
                onVerifyOtp = { code ->
                    EmailVerificationHelper.verifyOtp(code)
                },
                onResetPassword = { emailAddr, newPw ->
                    try {
                        // 1. Gọi Cloud Function để đổi mật khẩu (quyền Admin)
                        val response = com.example.alphacinema.data.api.RetrofitClient.functionsApi.resetPassword(
                            mapOf("email" to emailAddr, "newPassword" to newPw)
                        )
                        
                        if (response.isSuccessful) {
                            // 2. Nếu đổi thành công, tiến hành ĐĂNG NHẬP luôn bằng mật khẩu mới
                            val result = auth.signInWithEmailAndPassword(emailAddr, newPw).await()
                            currentUser = result.user
                            null // Thành công
                        } else {
                            response.errorBody()?.string() ?: "Lỗi khi đổi mật khẩu"
                        }
                    } catch (e: Exception) {
                        e.localizedMessage ?: "Đã xảy ra lỗi kết nối"
                    }
                },
                onClose = {
                    EmailVerificationHelper.clearOtp()
                    showForgotPassword = false
                    currentUser = auth.currentUser
                }
            )
        }
    }
}

@Composable
private fun KidsModeCard(
    isKidsModeEnabled: Boolean,
    currentUser: FirebaseUser?,
    onRequireLogin: () -> Unit,
    onToggleMode: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(14.dp))
            .border(1.dp, Color(0xFFF6E29A).copy(alpha = 0.3f), RoundedCornerShape(14.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Chế độ trẻ em",
                color = Color(0xFFF6E29A),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = if (currentUser == null) {
                    "Đăng nhập để lưu cài đặt an toàn"
                } else {
                    "Lọc nội dung an toàn cho trẻ"
                },
                color = Color.White.copy(alpha = 0.6f),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 2.dp)
            )
        }

        Switch(
            checked = isKidsModeEnabled,
            onCheckedChange = { checked ->
                if (currentUser == null) {
                    onRequireLogin()
                } else {
                    onToggleMode(checked)
                }
            },
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.Black,
                checkedTrackColor = Color(0xFFF6E29A),
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = Color(0xFF333333)
            )
        )
    }
}

@Composable
private fun WatchTogetherHighlightCard(
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(14.dp))
            .border(1.dp, Color(0xFFF6E29A).copy(alpha = 0.3f), RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .background(Color(0xFF1F1F1F), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.Groups,
                contentDescription = null,
                tint = Color(0xFFF6E29A)
            )
        }

        Column(
            modifier = Modifier
                .padding(start = 12.dp)
                .weight(1f)
        ) {
            Text(
                text = "Xem chung",
                color = Color(0xFFF6E29A),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Tạo phòng xem phim cùng bạn bè",
                color = Color.White.copy(alpha = 0.6f),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 2.dp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Icon(
            imageVector = Icons.AutoMirrored.Rounded.ArrowForwardIos,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.62f),
            modifier = Modifier.size(14.dp)
        )
    }
}

internal fun buildDemoMembershipPlan(
    currentPlan: String?,
    expiredDate: String?,
    startedDate: String? = null
): DemoMembershipPlanUi {
    val key = when (currentPlan?.lowercase()) {
        "basic" -> DemoMembershipPlanKey.BASIC
        "couple" -> DemoMembershipPlanKey.COUPLE
        "premium" -> DemoMembershipPlanKey.PREMIUM
        else -> DemoMembershipPlanKey.FREE
    }
    val paidStartedDate = startedDate.orEmpty()
    val paidExpiredDate = expiredDate.orEmpty()

    return when (key) {
        DemoMembershipPlanKey.FREE -> DemoMembershipPlanUi(
            key = key,
            badge = "Free",
            title = "Gói Free",
            price = "0đ / tháng",
            description = "Bạn đang sử dụng gói miễn phí.",
            benefits = emptyList()
        )
        DemoMembershipPlanKey.BASIC -> DemoMembershipPlanUi(
            key = key,
            badge = "Basic",
            title = "Gói Basic",
            price = "29.000đ / tháng",
            description = "Bạn đang sử dụng gói Basic.",
            benefits = listOf(
                "Xem phim không giới hạn",
                "Lưu danh sách yêu thích",
                "Chất lượng HD"
            ),
            startedDate = paidStartedDate,
            expiredDate = paidExpiredDate
        )
        DemoMembershipPlanKey.COUPLE -> DemoMembershipPlanUi(
            key = key,
            badge = "Couple",
            title = "Gói Couple",
            price = "59.000đ / tháng",
            description = "Bạn đang sử dụng gói Couple.",
            benefits = listOf(
                "Tạo phòng xem chung",
                "Đồng bộ thời gian xem phim",
                "Chat trong phòng xem"
            ),
            startedDate = paidStartedDate,
            expiredDate = paidExpiredDate
        )
        DemoMembershipPlanKey.PREMIUM -> DemoMembershipPlanUi(
            key = key,
            badge = "Premium",
            title = "Gói Premium",
            price = "99.000đ / tháng",
            description = "Bạn đang tận hưởng đầy đủ tính năng cao cấp.",
            benefits = listOf(
                "Không quảng cáo",
                "Chất lượng Full HD / 4K",
                "Tạo nhiều phòng xem chung",
                "Mời bạn bè bằng link",
                "Ưu tiên trải nghiệm xem phim"
            ),
            startedDate = paidStartedDate,
            expiredDate = paidExpiredDate
        )
    }
}

@Composable
private fun LoggedInProfileCard(
    demoUser: DemoUserProfileUi,
    onClick: () -> Unit
) {
    val plan = demoUser.currentPlan
    val isPaid = plan.key != DemoMembershipPlanKey.FREE
    val shape = RoundedCornerShape(24.dp)
    val background = when (plan.key) {
        DemoMembershipPlanKey.FREE -> Brush.linearGradient(listOf(Color(0xFF141414), Color(0xFF0D0D0D)))
        DemoMembershipPlanKey.BASIC -> Brush.linearGradient(
            listOf(
                Color(0xFF2A2D31),
                Color(0xFF151515),
                Color(0xFF0D0D0D)
            )
        )
        DemoMembershipPlanKey.COUPLE -> Brush.linearGradient(
            listOf(
                Color(0xFF2D1724),
                Color(0xFF171214),
                Color(0xFF0D0D0D)
            )
        )
        DemoMembershipPlanKey.PREMIUM -> Brush.linearGradient(
            listOf(
                Color(0xFF302817),
                Color(0xFF181511),
                Color(0xFF0D0D0D)
            )
        )
    }
    val border = when (plan.key) {
        DemoMembershipPlanKey.FREE -> Brush.linearGradient(listOf(Color(0xFF8A93A7).copy(alpha = 0.22f), Color.White.copy(alpha = 0.06f)))
        DemoMembershipPlanKey.BASIC -> Brush.linearGradient(listOf(Color(0xFFF2F4F8).copy(alpha = 0.72f), Color(0xFF9CA3AF).copy(alpha = 0.28f), Color.White.copy(alpha = 0.08f)))
        DemoMembershipPlanKey.COUPLE -> Brush.linearGradient(listOf(Color(0xFFFFC2D8).copy(alpha = 0.72f), Color(0xFFFF6FA8).copy(alpha = 0.34f), Color.White.copy(alpha = 0.08f)))
        DemoMembershipPlanKey.PREMIUM -> Brush.linearGradient(listOf(Color(0xFFFFE2A8).copy(alpha = 0.76f), Color(0xFFF6E29A).copy(alpha = 0.42f), Color.White.copy(alpha = 0.08f)))
    }
    val glowColor = when (plan.key) {
        DemoMembershipPlanKey.FREE -> Color.Transparent
        DemoMembershipPlanKey.BASIC -> Color(0xFFF2F4F8).copy(alpha = 0.18f)
        DemoMembershipPlanKey.COUPLE -> Color(0xFFFF8AB8).copy(alpha = 0.18f)
        DemoMembershipPlanKey.PREMIUM -> Color(0xFFFFD76A).copy(alpha = 0.20f)
    }
    val sheen = when (plan.key) {
        DemoMembershipPlanKey.FREE -> Color.Transparent
        DemoMembershipPlanKey.BASIC -> Color(0xFFF2F4F8).copy(alpha = 0.12f)
        DemoMembershipPlanKey.COUPLE -> Color(0xFFFF8AB8).copy(alpha = 0.12f)
        DemoMembershipPlanKey.PREMIUM -> Color(0xFFFFD76A).copy(alpha = 0.14f)
    }
    val planStatus = if (isPaid) {
        if (plan.expiredDate.isNotBlank()) {
            "Gói ${plan.badge} • Đang hoạt động đến ${plan.expiredDate}"
        } else {
            "Gói ${plan.badge} • Đang hoạt động"
        }
    } else {
        "Gói Free"
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(background)
            .border(1.dp, border, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 24.dp)
    ) {
        if (glowColor != Color.Transparent) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(glowColor, Color.Transparent),
                            radius = 520f
                        )
                    )
            )
        }
        if (sheen != Color.Transparent) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                sheen,
                                Color.Transparent,
                                sheen.copy(alpha = 0.04f)
                            )
                        )
                    )
            )
        }

        MembershipBadge(
            plan = plan,
            modifier = Modifier.align(Alignment.TopEnd).offset(y = (-8).dp)
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()

        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ProfileAvatar(demoUser = demoUser)
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 14.dp, end = 12.dp)
                ) {
                    Text(
                        text = demoUser.name,
                        color = Color.White,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowForwardIos,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.46f),
                    modifier = Modifier.size(15.dp)
                )
            }
        }
    }
}

@Composable
private fun FreePlanUpgradeBanner(
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(
                        Color(0xFF1D1A12),
                        Color(0xFF141414),
                        Color(0xFF101010)
                    )
                )
            )
            .border(
                1.dp,
                Color(0xFFF6E29A).copy(alpha = 0.24f),
                RoundedCornerShape(18.dp)
            )
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(Color(0xFFF6E29A).copy(alpha = 0.13f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.WorkspacePremium,
                contentDescription = null,
                tint = Color(0xFFF6E29A),
                modifier = Modifier.size(22.dp)
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Bạn đang dùng gói Free",
                color = Color.White,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "Đăng ký gói trả phí để mở khóa trải nghiệm cao cấp.",
                color = Color.White.copy(alpha = 0.64f),
                style = MaterialTheme.typography.bodySmall,
                lineHeight = 18.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp)
            )
        }

        Button(
            onClick = onClick,
            modifier = Modifier.height(40.dp),
            shape = RoundedCornerShape(13.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFF6E29A),
                contentColor = Color.Black
            )
        ) {
            Text(
                text = "Đăng ký",
                fontWeight = FontWeight.ExtraBold,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun GuestProfileCard(
    onLogin: () -> Unit,
    onRegister: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.linearGradient(listOf(Color(0xFF141414), Color(0xFF0D0D0D))))
            .border(1.dp, Color(0xFF8A93A7).copy(alpha = 0.20f), RoundedCornerShape(24.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(62.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.08f))
                    .border(1.dp, Color.White.copy(alpha = 0.14f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.PersonOutline,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.72f),
                    modifier = Modifier.size(30.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Bạn chưa đăng nhập",
                    color = Color.White,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "Đăng nhập để đồng bộ dữ liệu và sử dụng đầy đủ tính năng",
                    color = Color.White.copy(alpha = 0.62f),
                    style = MaterialTheme.typography.bodySmall,
                    lineHeight = 18.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = onLogin,
                modifier = Modifier
                    .weight(1f)
                    .height(46.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFE6EDF7),
                    contentColor = Color.Black
                )
            ) {
                Text("Đăng nhập", fontWeight = FontWeight.Bold)
            }

            OutlinedButton(
                onClick = onRegister,
                modifier = Modifier
                    .weight(1f)
                    .height(46.dp),
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.24f)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
            ) {
                Text("Đăng ký", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun MembershipUpgradeCard(
    plan: DemoMembershipPlanUi,
    onUpgradeClick: () -> Unit,
    onManageClick: () -> Unit
) {
    val isPremium = plan.key == DemoMembershipPlanKey.PREMIUM
    val title = if (isPremium) "Gói Premium đang hoạt động" else "Nâng cấp trải nghiệm"
    val description = if (isPremium) {
        "Quản lý quyền lợi và thời hạn gói của bạn"
    } else {
        "Mở khóa xem chung, tạo phòng, chất lượng cao hơn và nhiều quyền lợi khác"
    }
    val buttonText = if (isPremium) "Quản lý gói" else "Nâng cấp gói"
    val onClick = if (isPremium) onManageClick else onUpgradeClick

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(
                Brush.linearGradient(
                    listOf(
                        Color(0xFF171717),
                        if (isPremium) Color(0xFF242424) else Color(0xFF1F1F1F),
                        Color(0xFF0D0D0D)
                    )
                )
            )
            .border(
                1.dp,
                if (isPremium) Color(0xFFF6E29A).copy(alpha = 0.22f) else Color(0xFFD8DEE9).copy(alpha = 0.18f),
                RoundedCornerShape(18.dp)
            )
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(
                    if (isPremium) Color(0xFFF6E29A).copy(alpha = 0.13f) else Color(0xFFD8DEE9).copy(alpha = 0.12f)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isPremium) Icons.Rounded.WorkspacePremium else Icons.Rounded.PlayArrow,
                contentDescription = null,
                tint = if (isPremium) Color(0xFFF6E29A) else Color(0xFFD8DEE9),
                modifier = Modifier.size(22.dp)
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = Color.White,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = description,
                color = Color.White.copy(alpha = 0.62f),
                style = MaterialTheme.typography.bodySmall,
                lineHeight = 18.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp)
            )
        }

        Button(
            onClick = onClick,
            modifier = Modifier.height(40.dp),
            shape = RoundedCornerShape(13.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isPremium) Color(0xFFF6E29A) else Color(0xFFE6EDF7),
                contentColor = Color.Black
            )
        ) {
            Text(
                text = buttonText,
                fontWeight = FontWeight.ExtraBold,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun AccountMenuList(
    items: List<AccountMenuItemUi>,
    onItemClick: (AccountMenuItemUi) -> Unit
) {
    items.forEach { item ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White.copy(alpha = 0.06f), RoundedCornerShape(14.dp))
                .border(
                    1.dp,
                    Color.White.copy(alpha = 0.12f),
                    RoundedCornerShape(14.dp)
                )
                .clickable { onItemClick(item) }
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .background(Color(0xFF1F1F1F), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.runtime.CompositionLocalProvider(
                    androidx.compose.material3.LocalContentColor provides Color(0xFFF6E29A)
                ) {
                    item.icon()
                }
            }

            Text(
                text = item.title,
                color = Color.White,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier
                    .padding(start = 12.dp)
                    .weight(1f)
            )

            Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowForwardIos,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.55f),
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

@Composable
private fun LogoutButton(onLogout: () -> Unit) {
    OutlinedButton(
        onClick = onLogout,
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp),
        shape = RoundedCornerShape(15.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFF7A7A).copy(alpha = 0.24f)),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = Color(0xFFFFA0A0),
            containerColor = Color(0xFFFF6B6B).copy(alpha = 0.05f)
        )
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.ExitToApp,
            contentDescription = null,
            tint = Color(0xFFFFA0A0),
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.size(8.dp))
        Text("Đăng xuất", fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun ProfileAvatar(demoUser: DemoUserProfileUi) {
    if (demoUser.avatarUrl.isNotBlank()) {
        // Use File object for local paths, URL string for remote
        val imageModel: Any = if (demoUser.avatarUrl.startsWith("/")) {
            java.io.File(demoUser.avatarUrl)
        } else {
            demoUser.avatarUrl
        }
        com.example.alphacinema.ui.components.AlphaCinemaImage(
            model = imageModel,
            contentDescription = "Avatar",
            modifier = Modifier
                .size(62.dp)
                .clip(CircleShape)
                .border(1.dp, Color.White.copy(alpha = 0.18f), CircleShape),
            contentScale = ContentScale.Crop
        )
    } else {
        Box(
            modifier = Modifier
                .size(62.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(Color(0xFFF6E29A), Color(0xFFD4A843))))
                .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = (demoUser.name.firstOrNull() ?: 'A').uppercase(),
                color = Color.Black,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold
            )
        }
    }
}

@Composable
private fun MembershipBadge(
    plan: DemoMembershipPlanUi,
    modifier: Modifier = Modifier
) {
    val textColor = when (plan.key) {
        DemoMembershipPlanKey.FREE -> Color(0xFFD8DEE9)
        DemoMembershipPlanKey.BASIC -> Color(0xFFD8DEE9)
        DemoMembershipPlanKey.COUPLE -> Color(0xFFFFB2D2)
        DemoMembershipPlanKey.PREMIUM -> Color.Black
    }
    val badgeBrush = when (plan.key) {
        DemoMembershipPlanKey.FREE -> Brush.horizontalGradient(listOf(Color.White.copy(alpha = 0.08f), Color.White.copy(alpha = 0.05f)))
        DemoMembershipPlanKey.BASIC -> Brush.horizontalGradient(listOf(Color(0xFFD8DEE9).copy(alpha = 0.18f), Color.White.copy(alpha = 0.04f)))
        DemoMembershipPlanKey.COUPLE -> Brush.horizontalGradient(listOf(Color(0xFFE8A0FF).copy(alpha = 0.18f), Color(0xFFFF8AB8).copy(alpha = 0.12f)))
        DemoMembershipPlanKey.PREMIUM -> Brush.horizontalGradient(listOf(Color(0xFFF6E29A), Color(0xFFFFD8A8)))
    }
    val borderColor = when (plan.key) {
        DemoMembershipPlanKey.FREE -> Color(0xFF8A93A7).copy(alpha = 0.28f)
        DemoMembershipPlanKey.BASIC -> Color(0xFFD8DEE9).copy(alpha = 0.34f)
        DemoMembershipPlanKey.COUPLE -> Color(0xFFFF8AB8).copy(alpha = 0.32f)
        DemoMembershipPlanKey.PREMIUM -> Color.Transparent
    }

    Text(
        text = plan.badge,
        color = textColor,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.ExtraBold,
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(badgeBrush)
            .border(
                1.dp,
                borderColor,
                RoundedCornerShape(999.dp)
            )
            .padding(horizontal = 10.dp, vertical = 4.dp)
    )
}

@Composable
private fun ProfilePlanIcon(plan: DemoMembershipPlanUi) {
    val icon = when (plan.key) {
        DemoMembershipPlanKey.FREE -> Icons.Rounded.PersonOutline
        DemoMembershipPlanKey.BASIC -> Icons.Rounded.PlayArrow
        DemoMembershipPlanKey.COUPLE -> Icons.Rounded.Groups
        DemoMembershipPlanKey.PREMIUM -> Icons.Rounded.WorkspacePremium
    }
    val tint = when (plan.key) {
        DemoMembershipPlanKey.FREE -> Color.White.copy(alpha = 0.72f)
        DemoMembershipPlanKey.BASIC -> Color(0xFFD8DEE9)
        DemoMembershipPlanKey.COUPLE -> Color(0xFFFF8AB8)
        DemoMembershipPlanKey.PREMIUM -> Color(0xFFF6E29A)
    }

    Box(
        modifier = Modifier
            .size(18.dp)
            .clip(CircleShape)
            .background(tint.copy(alpha = 0.14f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(12.dp)
        )
    }
}

@Composable
internal fun MembershipBenefitRow(text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(Color(0xFF63E6D8).copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.CheckCircle,
                contentDescription = null,
                tint = Color(0xFF63E6D8),
                modifier = Modifier.size(15.dp)
            )
        }
        Text(
            text = text,
            color = Color.White.copy(alpha = 0.78f),
            style = MaterialTheme.typography.bodyMedium,
            lineHeight = 20.sp
        )
    }
}

@Composable
internal fun PlanManagementSheet(
    plan: DemoMembershipPlanUi,
    onDismiss: () -> Unit,
    onChangePlan: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color(0xFF0D0D0D),
        scrimColor = Color.Black.copy(alpha = 0.62f),
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = 2.dp, bottom = 4.dp)
                    .size(width = 42.dp, height = 4.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(Color.White.copy(alpha = 0.24f))
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Quản lý gói",
                        color = Color.White,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "Thông tin gói thành viên hiện tại",
                        color = Color.White.copy(alpha = 0.68f),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.06f))
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(19.dp)
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(Color.White.copy(alpha = 0.075f), Color.White.copy(alpha = 0.035f))
                        )
                    )
                    .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(24.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                PlanInfoRow(
                    icon = Icons.Rounded.WorkspacePremium,
                    label = "Gói hiện tại",
                    value = plan.badge
                )
                PlanInfoRow(
                    icon = Icons.Rounded.Info,
                    label = "Giá gói",
                    value = plan.price
                )
                PlanInfoRow(
                    icon = Icons.Rounded.CheckCircle,
                    label = "Ngày đăng ký",
                    value = if (plan.key == DemoMembershipPlanKey.FREE) {
                        "Không áp dụng"
                    } else {
                        plan.startedDate.ifBlank { "Chưa lưu" }
                    }
                )
                PlanInfoRow(
                    icon = Icons.Rounded.WatchLater,
                    label = "Ngày hết hạn",
                    value = if (plan.key == DemoMembershipPlanKey.FREE) {
                        "Không áp dụng"
                    } else {
                        plan.expiredDate.ifBlank { "Chưa lưu" }
                    }
                )
                PlanInfoRow(
                    icon = Icons.Rounded.CheckCircle,
                    label = "Trạng thái",
                    value = if (plan.key == DemoMembershipPlanKey.FREE) "Miễn phí" else "Đang hoạt động"
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.White.copy(alpha = 0.045f))
                    .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(24.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Quyền lợi",
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold
                )
                plan.benefits.ifEmpty { listOf("Các tính năng xem phim cơ bản") }.forEach { benefit ->
                    MembershipBenefitRow(text = benefit)
                }
            }

            Button(
                onClick = onChangePlan,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFF6E29A),
                    contentColor = Color.Black
                )
            ) {
                Text("Đổi gói", fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleSmall)
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
internal fun PlanInfoRow(
    icon: ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(Color(0xFFF6E29A).copy(alpha = 0.10f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color(0xFFF6E29A),
                modifier = Modifier.size(18.dp)
            )
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp)
        ) {
            Text(
                text = label,
                color = Color.White.copy(alpha = 0.52f),
                style = MaterialTheme.typography.labelMedium
            )
            Text(
                text = value,
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 1.dp)
            )
        }
    }
}

@Composable
private fun AccountPanelBottomSheet(
    panel: AccountPanelType,
    favorites: List<com.example.alphacinema.data.model.FavoriteItem>,
    feedbackInput: String,
    onDismiss: () -> Unit,
    onFeedbackChange: (String) -> Unit,
    onSubmitFeedback: () -> Unit,
    onOpenMovieDetail: (String) -> Unit,
    onOpenMovieList: (String, FilterKind, String) -> Unit
) {
    val sheetTitle = when (panel) {
        AccountPanelType.MOVIE_LIBRARY -> "Danh sách phim"
        AccountPanelType.FAVORITES -> "Yêu thích"
        AccountPanelType.POLICY -> "Chính sách"
        AccountPanelType.FEEDBACK -> "Góp ý"
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color(0xFF141414),
        scrimColor = Color.Black.copy(alpha = 0.62f),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = sheetTitle,
                        color = Color.White,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = when (panel) {
                            AccountPanelType.MOVIE_LIBRARY -> "Mở nhanh các danh sách phim phổ biến"
                            AccountPanelType.FAVORITES -> "Những phim bạn đã lưu yêu thích"
                            AccountPanelType.POLICY -> "Thông tin sử dụng và quyền riêng tư"
                            AccountPanelType.FEEDBACK -> "Chia sẻ góp ý để cải thiện AlphaCinema"
                        },
                        color = Color.White.copy(alpha = 0.68f),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.76f)
                    )
                }
            }

            when (panel) {
                AccountPanelType.MOVIE_LIBRARY -> {
                    LibraryShortcutRow(
                        title = "Phim bộ",
                        description = "Series cập nhật liên tục",
                        onClick = { onOpenMovieList("Phim bộ", FilterKind.MOVIE_TYPE, "series") }
                    )
                    LibraryShortcutRow(
                        title = "Phim lẻ",
                        description = "Các phim chiếu rạp và phim đơn",
                        onClick = { onOpenMovieList("Phim lẻ", FilterKind.MOVIE_TYPE, "single") }
                    )
                    LibraryShortcutRow(
                        title = "Hoạt hình",
                        description = "Danh sách phim cho gia đình và thiếu nhi",
                        onClick = { onOpenMovieList("Hoạt hình", FilterKind.MOVIE_TYPE, "hoathinh") }
                    )
                    LibraryShortcutRow(
                        title = "TV Shows",
                        description = "Các chương trình và nội dung giải trí",
                        onClick = { onOpenMovieList("TV Shows", FilterKind.MOVIE_TYPE, "tvshows") }
                    )
                }

                AccountPanelType.FAVORITES -> {
                    if (favorites.isEmpty()) {
                        AccountEmptyState("Bạn chưa có phim yêu thích nào.")
                    } else {
                        favorites.forEach { item ->
                            AccountMediaRow(
                                title = item.movieName,
                                subtitle = "Phim đã lưu",
                                meta = "Mở lại chi tiết phim",
                                posterUrl = item.posterUrl,
                                onClick = { onOpenMovieDetail(item.movieId) }
                            )
                        }
                    }
                }

                AccountPanelType.POLICY -> {
                    PolicyBlock(
                        title = "Quyền riêng tư",
                        content = "AlphaCinema chỉ sử dụng dữ liệu tài khoản để đồng bộ lịch sử xem, phim yêu thích và các tính năng cá nhân hóa."
                    )
                    PolicyBlock(
                        title = "Nội dung và cộng đồng",
                        content = "Người dùng chịu trách nhiệm với bình luận, đánh giá và hành vi sử dụng tài khoản. Nội dung vi phạm có thể bị ẩn hoặc xóa."
                    )
                    PolicyBlock(
                        title = "Tài khoản",
                        content = "Bạn nên bảo vệ mật khẩu, đăng xuất trên thiết bị lạ và không chia sẻ thông tin đăng nhập cho người khác."
                    )
                }

                AccountPanelType.FEEDBACK -> {
                    OutlinedTextField(
                        value = feedbackInput,
                        onValueChange = onFeedbackChange,
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 4,
                        maxLines = 6,
                        label = { Text("Nội dung góp ý") },
                        colors = authTextFieldColors()
                    )
                    Button(
                        onClick = onSubmitFeedback,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFF6E29A),
                            contentColor = Color.Black
                        )
                    ) {
                        Text("Gửi góp ý", fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
        }
    }
}

@Composable
private fun AccountMediaRow(
    title: String,
    subtitle: String,
    meta: String,
    posterUrl: String,
    progressFraction: Float? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.06f))
            .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        com.example.alphacinema.ui.components.AlphaCinemaImage(
            model = posterUrl,
            contentDescription = title,
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(12.dp)),
            contentScale = ContentScale.Crop
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp)
        ) {
            Text(
                title,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                subtitle,
                color = Color.White.copy(alpha = 0.72f),
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                meta,
                color = Color(0xFFF6E29A),
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (progressFraction != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(Color.White.copy(alpha = 0.16f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progressFraction.coerceIn(0f, 1f))
                            .height(3.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(Color(0xFFF6E29A))
                    )
                }
            }
        }
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.ArrowForwardIos,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.55f),
            modifier = Modifier.size(14.dp)
        )
    }
}

@Composable
private fun LibraryShortcutRow(
    title: String,
    description: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.06f))
            .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            Text(description, color = Color.White.copy(alpha = 0.68f), style = MaterialTheme.typography.bodySmall)
        }
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.ArrowForwardIos,
            contentDescription = null,
            tint = Color(0xFFF6E29A),
            modifier = Modifier.size(14.dp)
        )
    }
}

@Composable
private fun PolicyBlock(
    title: String,
    content: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.06f))
            .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(16.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(title, color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
        Text(content, color = Color.White.copy(alpha = 0.72f), style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun AccountEmptyState(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.06f))
            .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(16.dp))
            .padding(18.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            color = Color.White.copy(alpha = 0.72f),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
internal fun AuthBottomSheet(
    state: AccountAuthUiState,
    isLoading: Boolean,
    errorMessage: String?,
    onEvent: (AccountAuthEvent) -> Unit,
    onForgotPassword: () -> Unit = {}
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = { onEvent(AccountAuthEvent.CloseDialog) },
        sheetState = sheetState,
        containerColor = Color(0xFF141414),
        scrimColor = Color.Black.copy(alpha = 0.62f),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (state.mode == AuthMode.LOGIN) "Đăng nhập" else "Đăng ký",
                        color = Color.White,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (state.mode == AuthMode.LOGIN) {
                            "Đăng nhập để đồng bộ danh sách và lịch sử xem"
                        } else {
                            "Tạo tài khoản mới để bắt đầu trải nghiệm"
                        },
                        color = Color.White.copy(alpha = 0.68f),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                IconButton(onClick = { onEvent(AccountAuthEvent.CloseDialog) }) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.76f)
                    )
                }
            }

            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = errorMessage,
                    color = Color(0xFFFF6B6B),
                    fontSize = 14.sp,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            if (state.mode == AuthMode.REGISTER) {
                AuthTextField(
                    value = state.name,
                    onValueChanged = { onEvent(AccountAuthEvent.NameChanged(it)) },
                    label = "Tên",
                    errorText = state.nameError
                )
                Spacer(modifier = Modifier.height(10.dp))
            }

            AuthTextField(
                value = state.email,
                onValueChanged = { onEvent(AccountAuthEvent.EmailChanged(it)) },
                label = "Email",
                errorText = state.emailError
            )

            Spacer(modifier = Modifier.height(10.dp))

            AuthPasswordField(
                value = state.password,
                onValueChanged = { onEvent(AccountAuthEvent.PasswordChanged(it)) },
                label = "Mật khẩu",
                visible = state.showPassword,
                onToggleVisibility = { onEvent(AccountAuthEvent.TogglePasswordVisibility) },
                errorText = state.passwordError
            )

            if (state.mode == AuthMode.REGISTER) {
                Spacer(modifier = Modifier.height(10.dp))
                AuthPasswordField(
                    value = state.confirmPassword,
                    onValueChanged = { onEvent(AccountAuthEvent.ConfirmPasswordChanged(it)) },
                    label = "Nhập lại mật khẩu",
                    visible = state.showConfirmPassword,
                    onToggleVisibility = { onEvent(AccountAuthEvent.ToggleConfirmPasswordVisibility) },
                    errorText = state.confirmPasswordError
                )
            }

            // Nút Quên mật khẩu (chỉ hiện ở chế độ Đăng nhập)
            if (state.mode == AuthMode.LOGIN) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Quên mật khẩu?",
                    color = Color(0xFFF6E29A),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    modifier = Modifier
                        .align(Alignment.End)
                        .clickable { onForgotPassword() }
                        .padding(vertical = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { onEvent(AccountAuthEvent.Submit) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFF6E29A),
                    contentColor = Color.Black,
                    disabledContainerColor = Color(0xFFF6E29A).copy(alpha = 0.5f),
                    disabledContentColor = Color.Black.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    androidx.compose.material3.CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.Black,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = if (state.mode == AuthMode.LOGIN) "Đăng nhập" else "Đăng ký",
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedButton(
                onClick = { onEvent(AccountAuthEvent.ContinueWithGoogle) },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.24f)),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color.White,
                    containerColor = Color(0xFF1F1F1F),
                    disabledContentColor = Color.White.copy(alpha = 0.5f),
                    disabledContainerColor = Color(0xFF1F1F1F).copy(alpha = 0.5f)
                ),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    androidx.compose.material3.CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        painter = androidx.compose.ui.res.painterResource(id = com.example.alphacinema.R.drawable.ic_google),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = Color.Unspecified
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Đăng nhập bằng Google", fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val switchPrefix = if (state.mode == AuthMode.LOGIN) {
                    "Chưa có tài khoản?"
                } else {
                    "Đã có tài khoản?"
                }

                Text(
                    text = switchPrefix,
                    color = Color.White.copy(alpha = 0.55f),
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(modifier = Modifier.padding(horizontal = 3.dp))

                Text(
                    text = if (state.mode == AuthMode.LOGIN) "Đăng ký" else "Đăng nhập",
                    color = Color(0xFFF6E29A),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    ),
                    modifier = Modifier.clickable {
                        onEvent(
                            AccountAuthEvent.SwitchMode(
                                if (state.mode == AuthMode.LOGIN) AuthMode.REGISTER else AuthMode.LOGIN
                            )
                        )
                    }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            TextButton(
                onClick = { onEvent(AccountAuthEvent.CloseDialog) },
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text("Để sau", color = Color.White.copy(alpha = 0.7f))
            }

            Spacer(modifier = Modifier.height(6.dp))
        }
    }
}

@Composable
internal fun AuthTextField(
    value: String,
    onValueChanged: (String) -> Unit,
    label: String,
    errorText: String?
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChanged,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label) },
        textStyle = LocalTextStyle.current.copy(fontSize = 15.sp),
        singleLine = true,
        isError = errorText != null,
        shape = RoundedCornerShape(13.dp),
        supportingText = {
            if (errorText != null) {
                Text(errorText)
            }
        },
        colors = authTextFieldColors()
    )
}

@Composable
internal fun AuthPasswordField(
    value: String,
    onValueChanged: (String) -> Unit,
    label: String,
    visible: Boolean,
    onToggleVisibility: () -> Unit,
    errorText: String?
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChanged,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label) },
        textStyle = LocalTextStyle.current.copy(fontSize = 15.sp),
        singleLine = true,
        isError = errorText != null,
        shape = RoundedCornerShape(13.dp),
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        trailingIcon = {
            Icon(
                imageVector = if (visible) Icons.Rounded.VisibilityOff else Icons.Rounded.RemoveRedEye,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.7f),
                modifier = Modifier.clickable(onClick = onToggleVisibility)
            )
        },
        supportingText = {
            if (errorText != null) {
                Text(errorText)
            }
        },
        colors = authTextFieldColors()
    )
}

@Composable
internal fun authTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = Color.White,
    unfocusedTextColor = Color.White,
    focusedLabelColor = Color(0xFFF6E29A),
    unfocusedLabelColor = Color.White.copy(alpha = 0.65f),
    focusedBorderColor = Color(0xFFF6E29A),
    unfocusedBorderColor = Color.White.copy(alpha = 0.28f),
    focusedContainerColor = Color(0xFF1F1F1F),
    unfocusedContainerColor = Color(0xFF1A1A1A),
    cursorColor = Color(0xFFF6E29A),
    errorBorderColor = Color(0xFFFF7A7A),
    errorLabelColor = Color(0xFFFFA7A7),
    errorTextColor = Color(0xFFFFA7A7),
    errorContainerColor = Color(0xFF2A1D2C)
)
