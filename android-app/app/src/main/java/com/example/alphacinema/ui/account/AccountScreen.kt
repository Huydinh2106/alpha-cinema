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
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForwardIos
import androidx.compose.material.icons.automirrored.outlined.ListAlt
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ExitToApp
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.RemoveRedEye
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.WatchLater
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
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import coil.compose.AsyncImage
import com.example.alphacinema.ui.search.FilterKind
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

private const val WEB_CLIENT_ID = "1013232588133-86fl74ls04r8fkarnahh2b9pvie5g7kn.apps.googleusercontent.com"

private data class AccountMenuItemUi(
    val title: String,
    val icon: @Composable () -> Unit,
    val action: AccountMenuAction? = null
)

enum class PinDialogMode { SETUP, VERIFY }

private enum class AccountMenuAction {
    WATCHING,
    MOVIE_LIBRARY,
    FAVORITES,
    POLICY,
    FEEDBACK,
    ADMIN,
    WATCH_TOGETHER
}

private enum class AccountPanelType {
    WATCHING,
    MOVIE_LIBRARY,
    FAVORITES,
    POLICY,
    FEEDBACK
}

@Composable
fun AccountScreen(
    onOpenAdminPanel: () -> Unit = {},
    onOpenMovieDetail: (String) -> Unit = {},
    onOpenMovieList: (title: String, filterKind: FilterKind, slug: String) -> Unit = { _, _, _ -> },
    onWatchTogether: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val auth = remember { FirebaseAuth.getInstance() }
    var currentUser by remember { mutableStateOf(auth.currentUser) }

    val firestoreRepository = remember { com.example.alphacinema.data.repository.FirestoreRepository() }
    var userProfile by remember { mutableStateOf<com.example.alphacinema.data.model.UserProfile?>(null) }

    LaunchedEffect(currentUser) {
        currentUser?.let {
            userProfile = firestoreRepository.getUserProfile(it.uid)
        } ?: run {
            userProfile = null
        }
    }

    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val authStateHolder = rememberAccountAuthStateHolder(
        onLogin = { email, password ->
            scope.launch {
                isLoading = true
                errorMessage = null
                try {
                    val result = auth.signInWithEmailAndPassword(email, password).await()
                    currentUser = result.user
                } catch (e: Exception) {
                    errorMessage = e.localizedMessage ?: "Đăng nhập thất bại"
                } finally {
                    isLoading = false
                }
            }
        },
        onRegister = { name, email, password ->
            scope.launch {
                isLoading = true
                errorMessage = null
                try {
                    val result = auth.createUserWithEmailAndPassword(email, password).await()
                    result.user?.updateProfile(
                        UserProfileChangeRequest.Builder()
                            .setDisplayName(name)
                            .build()
                    )?.await()
                    currentUser = auth.currentUser
                } catch (e: Exception) {
                    errorMessage = e.localizedMessage ?: "Đăng ký thất bại"
                } finally {
                    isLoading = false
                }
            }
        },
        onGoogleSignIn = {
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
                    currentUser = result.user
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
    val watchHistoryFlow = remember(currentUser?.uid) {
        currentUser?.uid?.let(firestoreRepository::getWatchHistory) ?: flowOf(emptyList())
    }
    val watchHistory by watchHistoryFlow.collectAsState(initial = emptyList())
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

    val menuItems = mutableListOf(
        AccountMenuItemUi("Đang xem", { Icon(Icons.Outlined.WatchLater, contentDescription = null) }),
        AccountMenuItemUi("Danh sách phim", { Icon(Icons.AutoMirrored.Outlined.ListAlt, contentDescription = null) }),
        AccountMenuItemUi("Yêu thích", { Icon(Icons.Outlined.FavoriteBorder, contentDescription = null) }),
        AccountMenuItemUi("Chính sách", { Icon(Icons.Outlined.Info, contentDescription = null) }),
        AccountMenuItemUi("Góp ý", { Icon(Icons.Outlined.ChatBubbleOutline, contentDescription = null) })
    ).apply {
        val isAdminUser = userProfile?.isAdmin == true || currentUser?.email == "admin@alphacinema.com"
        if (isAdminUser) {
            add(0, AccountMenuItemUi("Quản trị phim", { Icon(Icons.Filled.Settings, contentDescription = null, tint = Color(0xFFF6E29A)) }))
        }
        // Thêm mục Xem chung sau Yêu thích
        val favIndex = indexOfFirst { it.title == "Yêu thích" }
        add(favIndex + 1, AccountMenuItemUi("Xem chung", { Icon(Icons.Outlined.Groups, contentDescription = null) }))
    }
    
    // Debug log (can be seen in Logcat)
    android.util.Log.d("AccountScreen", "User: ${currentUser?.email}, Profile: ${userProfile?.email}, isAdmin: ${userProfile?.isAdmin}")

    fun openPanel(panel: AccountPanelType, requiresLogin: Boolean = false) {
        if (requiresLogin && currentUser == null) {
            authStateHolder.onEvent(AccountAuthEvent.OpenDialog(AuthMode.LOGIN))
            return
        }
        activePanel = panel
    }

    fun resolveMenuAction(item: AccountMenuItemUi): AccountMenuAction {
        return item.action ?: when (item.title) {
            "Đang xem" -> AccountMenuAction.WATCHING
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
            .background(Color(0xFF070B16))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF1D2540),
                            Color(0xFF101726),
                            Color(0xFF070B16)
                        )
                    )
                )
        )

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

            // === User Info / Auth Buttons ===
            if (currentUser != null) {
                // LOGGED IN: show avatar, name, email
                val photoUrl = currentUser?.photoUrl?.toString()
                var showLogoutMenu by remember { mutableStateOf(false) }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color.White.copy(alpha = 0.06f))
                        .border(1.dp, Color.White.copy(alpha = if (showLogoutMenu) 0.18f else 0.10f), RoundedCornerShape(18.dp))
                        .clickable { showLogoutMenu = !showLogoutMenu }
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Avatar
                        if (photoUrl != null) {
                            AsyncImage(
                                model = photoUrl,
                                contentDescription = "Avatar",
                                modifier = Modifier
                                    .size(60.dp)
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(60.dp)
                                    .background(
                                        Brush.linearGradient(listOf(Color(0xFFF6E29A), Color(0xFFD4A843))),
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = (currentUser?.displayName?.firstOrNull() ?: currentUser?.email?.firstOrNull() ?: 'A').uppercase(),
                                    color = Color(0xFF070B16),
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 14.dp)
                        ) {
                            Text(
                                text = currentUser?.displayName ?: "Người dùng",
                                color = Color.White,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = currentUser?.email ?: "",
                                color = Color.White.copy(alpha = 0.55f),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }

                        // Chevron indicator
                        val chevronRotation by androidx.compose.animation.core.animateFloatAsState(
                            targetValue = if (showLogoutMenu) 180f else 0f,
                            animationSpec = tween(250),
                            label = "chevron"
                        )
                        Icon(
                            imageVector = Icons.Outlined.KeyboardArrowDown,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.45f),
                            modifier = Modifier
                                .size(20.dp)
                                .rotate(chevronRotation)
                        )
                    }

                    // --- Expandable logout section ---
                    AnimatedVisibility(
                        visible = showLogoutMenu,
                        enter = expandVertically(animationSpec = tween(250)) + fadeIn(animationSpec = tween(200)),
                        exit = shrinkVertically(animationSpec = tween(200)) + fadeOut(animationSpec = tween(150))
                    ) {
                        Column {
                            // Divider
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 14.dp, bottom = 10.dp)
                                    .height(1.dp)
                                    .background(Color.White.copy(alpha = 0.10f))
                            )

                            // Logout row
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable(
                                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                                        indication = null
                                    ) {
                                        auth.signOut()
                                        currentUser = null
                                        showLogoutMenu = false
                                    }
                                    .padding(vertical = 6.dp, horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .background(Color(0x22FF6B6B), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.ExitToApp,
                                        contentDescription = null,
                                        tint = Color(0xFFFF6B6B),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Text(
                                    text = "Đăng xuất",
                                    color = Color(0xFFFF6B6B),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            } else {
                // NOT LOGGED IN: show default avatar + login/register buttons
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .background(Color.White.copy(alpha = 0.12f), CircleShape)
                        .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "A",
                        color = Color(0xFFF6E29A),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = { authStateHolder.onEvent(AccountAuthEvent.OpenDialog(AuthMode.LOGIN)) },
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFF6E29A),
                            contentColor = Color.Black
                        )
                    ) {
                        Text("Đăng nhập", fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = { authStateHolder.onEvent(AccountAuthEvent.OpenDialog(AuthMode.REGISTER)) },
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp),
                        shape = RoundedCornerShape(14.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.35f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                    ) {
                        Text("Đăng ký", fontWeight = FontWeight.SemiBold)
                    }
                }
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

            menuItems.forEach { item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White.copy(alpha = 0.06f), RoundedCornerShape(14.dp))
                        .border(
                            1.dp,
                            Color.White.copy(alpha = 0.12f),
                            RoundedCornerShape(14.dp)
                        )
                        .clickable {
                            when (resolveMenuAction(item)) {
                                AccountMenuAction.WATCHING -> openPanel(AccountPanelType.WATCHING, requiresLogin = true)
                                AccountMenuAction.MOVIE_LIBRARY -> openPanel(AccountPanelType.MOVIE_LIBRARY)
                                AccountMenuAction.FAVORITES -> openPanel(AccountPanelType.FAVORITES, requiresLogin = true)
                                AccountMenuAction.POLICY -> openPanel(AccountPanelType.POLICY)
                                AccountMenuAction.FEEDBACK -> openPanel(AccountPanelType.FEEDBACK)
                                AccountMenuAction.ADMIN -> onOpenAdminPanel()
                                AccountMenuAction.WATCH_TOGETHER -> onWatchTogether()
                            }
                        }
                        .padding(horizontal = 14.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .background(Color(0xFF1A2237), CircleShape),
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
                        imageVector = Icons.AutoMirrored.Outlined.ArrowForwardIos,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.55f),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }


            if (showPinDialog) {
                AlertDialog(
                    onDismissRequest = { showPinDialog = false },
                    containerColor = Color(0xFF10192E),
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
                watchHistory = watchHistory,
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

        if (state.showDialog) {
            AuthBottomSheet(
                state = state,
                onEvent = authStateHolder::onEvent
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
                    "Đăng nhập để bật bộ lọc nội dung an toàn"
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
                uncheckedTrackColor = Color(0xFF2A344A)
            )
        )
    }
}

@Composable
private fun AccountPanelBottomSheet(
    panel: AccountPanelType,
    watchHistory: List<com.example.alphacinema.data.model.WatchHistoryItem>,
    favorites: List<com.example.alphacinema.data.model.FavoriteItem>,
    feedbackInput: String,
    onDismiss: () -> Unit,
    onFeedbackChange: (String) -> Unit,
    onSubmitFeedback: () -> Unit,
    onOpenMovieDetail: (String) -> Unit,
    onOpenMovieList: (String, FilterKind, String) -> Unit
) {
    val sheetTitle = when (panel) {
        AccountPanelType.WATCHING -> "Đang xem"
        AccountPanelType.MOVIE_LIBRARY -> "Danh sách phim"
        AccountPanelType.FAVORITES -> "Yêu thích"
        AccountPanelType.POLICY -> "Chính sách"
        AccountPanelType.FEEDBACK -> "Góp ý"
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color(0xFF10192E),
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
                            AccountPanelType.WATCHING -> "Tiếp tục những nội dung bạn đang theo dõi"
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
                        imageVector = Icons.Outlined.Close,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.76f)
                    )
                }
            }

            when (panel) {
                AccountPanelType.WATCHING -> {
                    if (watchHistory.isEmpty()) {
                        AccountEmptyState("Bạn chưa có lịch sử xem nào.")
                    } else {
                        watchHistory.forEach { item ->
                            AccountMediaRow(
                                title = item.movieName,
                                subtitle = item.episodeName.ifBlank { "Tiếp tục xem" },
                                meta = if (item.duration > 0) {
                                    "${(item.progress * 100 / item.duration).coerceIn(0, 100)}% đã xem"
                                } else {
                                    "Tiếp tục xem"
                                },
                                posterUrl = item.posterUrl,
                                onClick = { onOpenMovieDetail(item.movieId) }
                            )
                        }
                    }
                }

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
        AsyncImage(
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
            Text(title, color = Color.White, fontWeight = FontWeight.Bold)
            Text(subtitle, color = Color.White.copy(alpha = 0.72f), style = MaterialTheme.typography.bodySmall)
            Text(meta, color = Color(0xFFF6E29A), style = MaterialTheme.typography.labelMedium)
        }
        Icon(
            imageVector = Icons.AutoMirrored.Outlined.ArrowForwardIos,
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
            imageVector = Icons.AutoMirrored.Outlined.ArrowForwardIos,
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
private fun AuthBottomSheet(
    state: AccountAuthUiState,
    onEvent: (AccountAuthEvent) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = { onEvent(AccountAuthEvent.CloseDialog) },
        sheetState = sheetState,
        containerColor = Color(0xFF10192E),
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
                        imageVector = Icons.Outlined.Close,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.76f)
                    )
                }
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

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { onEvent(AccountAuthEvent.Submit) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFF6E29A),
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Text(
                    text = if (state.mode == AuthMode.LOGIN) "Đăng nhập" else "Đăng ký",
                    fontWeight = FontWeight.Bold
                )
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
                    containerColor = Color(0xFF18233F)
                )
            ) {
                Text("Đăng nhập bằng Google", fontWeight = FontWeight.SemiBold)
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
private fun AuthTextField(
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
private fun AuthPasswordField(
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
                imageVector = if (visible) Icons.Outlined.VisibilityOff else Icons.Outlined.RemoveRedEye,
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
private fun authTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = Color.White,
    unfocusedTextColor = Color.White,
    focusedLabelColor = Color(0xFFF6E29A),
    unfocusedLabelColor = Color.White.copy(alpha = 0.65f),
    focusedBorderColor = Color(0xFFF6E29A),
    unfocusedBorderColor = Color.White.copy(alpha = 0.28f),
    focusedContainerColor = Color(0xFF1A2542),
    unfocusedContainerColor = Color(0xFF172039),
    cursorColor = Color(0xFFF6E29A),
    errorBorderColor = Color(0xFFFF7A7A),
    errorLabelColor = Color(0xFFFFA7A7),
    errorTextColor = Color(0xFFFFA7A7),
    errorContainerColor = Color(0xFF2A1D2C)
)


