@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.alphacinema.ui.account

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForwardIos
import androidx.compose.material.icons.automirrored.rounded.ExitToApp
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.storage.FirebaseStorage
import com.example.alphacinema.util.formatFirestoreDate
import com.example.alphacinema.ui.components.clearFocusOnTapOutside
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.io.File

private enum class ProfilePage { MAIN, EDIT_PROFILE, CHANGE_PASSWORD, MANAGE_SUBSCRIPTION }

@Composable
fun ProfileSettingsScreen(
    onBack: () -> Unit,
    onOpenPayment: () -> Unit,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val auth = remember { FirebaseAuth.getInstance() }
    var currentUser by remember { mutableStateOf(auth.currentUser) }
    val firestoreRepo = remember { com.example.alphacinema.data.repository.FirestoreRepository() }
    val profileCache = remember { com.example.alphacinema.data.local.UserProfileCache(context) }
    var userProfile by remember { mutableStateOf<com.example.alphacinema.data.model.UserProfile?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var isLoggingOut by remember { mutableStateOf(false) }
    var currentPage by remember { mutableStateOf(ProfilePage.MAIN) }

    // Use cached values first for instant display, then refresh from server
    var cachedName by remember { mutableStateOf(profileCache.displayName) }
    var cachedAvatar by remember { mutableStateOf(profileCache.avatarUrl) }

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
        currentUser?.let {
            firestoreRepo.saveUser(it)
            val profile = firestoreRepo.getUserProfile(it.uid)
            userProfile = profile
            // Update cache with fresh data
            val freshName = it.displayName ?: profile?.displayName ?: ""
            val freshAvatar = it.photoUrl?.toString() ?: profile?.photoUrl ?: ""
            profileCache.save(it.uid, freshName, freshAvatar)
            cachedName = freshName
            cachedAvatar = freshAvatar
        } ?: run {
            userProfile = null
        }
    }

    val isGoogleUser = currentUser?.providerData?.any { it.providerId == "google.com" } == true
    val displayName = currentUser?.displayName ?: userProfile?.displayName ?: cachedName
    // Use local cached file first for instant loading
    val localFile = profileCache.localAvatarFile
    val avatarUrl = if (localFile != null) {
        localFile.absolutePath
    } else {
        currentUser?.photoUrl?.toString() ?: userProfile?.photoUrl ?: cachedAvatar
    }
    val plan = userProfile?.subscriptionPlan ?: "free"
    val subscriptionStartedDate = formatFirestoreDate(userProfile?.subscriptionStartedAt)
    val subscriptionExpiredDate = formatFirestoreDate(userProfile?.subscriptionExpiresAt)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clearFocusOnTapOutside()
    ) {
        AnimatedContent(
            targetState = currentPage,
            transitionSpec = {
                if (targetState.ordinal > initialState.ordinal) {
                    (slideInHorizontally { it } + fadeIn()) togetherWith
                            (slideOutHorizontally { -it / 3 } + fadeOut())
                } else {
                    (slideInHorizontally { -it / 3 } + fadeIn()) togetherWith
                            (slideOutHorizontally { it } + fadeOut())
                }
            },
            label = "ProfilePageTransition"
        ) { page ->
            when (page) {
                ProfilePage.MAIN -> ProfileMainPage(
                    displayName = displayName,
                    avatarUrl = avatarUrl,
                    plan = plan,
                    isGoogleUser = isGoogleUser,
                    onBack = onBack,
                    onEditProfile = { currentPage = ProfilePage.EDIT_PROFILE },
                    onChangePassword = { currentPage = ProfilePage.CHANGE_PASSWORD },
                    onManageSubscription = {
                        if (plan.lowercase() != "free") {
                            currentPage = ProfilePage.MANAGE_SUBSCRIPTION
                        } else {
                            onOpenPayment()
                        }
                    },
                    onLogout = {
                        if (!isLoggingOut) {
                            isLoggingOut = true
                            profileCache.clear()
                            currentUser = null
                            userProfile = null
                            isLoading = false
                            currentPage = ProfilePage.MAIN
                            onLogout()
                            auth.signOut()
                        }
                    }
                )

                ProfilePage.EDIT_PROFILE -> EditProfilePage(
                    displayName = displayName,
                    avatarUrl = avatarUrl,
                    isLoading = isLoading,
                    onBack = { currentPage = ProfilePage.MAIN },
                    onSave = { newName, imageUri ->
                        scope.launch {
                            isLoading = true
                            try {
                                var downloadUrl = avatarUrl
                                // Upload image to Firebase Storage if a new one was picked
                                if (imageUri != null) {
                                    val uid = currentUser?.uid.orEmpty()
                                    try {
                                        val bytes = context.contentResolver.openInputStream(imageUri)?.use {
                                            it.readBytes()
                                        } ?: throw Exception("Không thể đọc ảnh")

                                        // Save image locally for instant loading next time
                                        profileCache.saveAvatarBytes(bytes)

                                        val storageRef = FirebaseStorage.getInstance()
                                            .reference.child("avatars/$uid.jpg")
                                        val metadata = com.google.firebase.storage.StorageMetadata.Builder()
                                            .setContentType("image/jpeg")
                                            .build()
                                        storageRef.putBytes(bytes, metadata).await()
                                        downloadUrl = storageRef.downloadUrl.await().toString()
                                    } catch (storageEx: Exception) {
                                        android.util.Log.e("ProfileSettings", "Storage upload failed", storageEx)
                                        Toast.makeText(context,
                                            "Lỗi upload ảnh: ${storageEx.message}\nKiểm tra Firebase Storage rules.",
                                            Toast.LENGTH_LONG).show()
                                        // Continue saving name even if avatar upload fails
                                    }
                                }

                                val builder = UserProfileChangeRequest.Builder()
                                    .setDisplayName(newName)
                                if (downloadUrl.isNotBlank()) {
                                    builder.setPhotoUri(Uri.parse(downloadUrl))
                                }
                                currentUser?.updateProfile(builder.build())?.await()

                                val uid = currentUser?.uid.orEmpty()
                                firestoreRepo.updateUserDisplayName(uid, newName)
                                if (downloadUrl.isNotBlank()) {
                                    firestoreRepo.updateUserPhotoUrl(uid, downloadUrl)
                                }
                                currentUser = auth.currentUser
                                userProfile = firestoreRepo.getUserProfile(uid)
                                // Update local cache
                                profileCache.save(uid, newName, downloadUrl)
                                cachedName = newName
                                cachedAvatar = downloadUrl
                                currentPage = ProfilePage.MAIN
                                Toast.makeText(context, "Đã cập nhật hồ sơ", Toast.LENGTH_SHORT).show()
                            } catch (e: Exception) {
                                android.util.Log.e("ProfileSettings", "Profile update failed", e)
                                Toast.makeText(context, e.localizedMessage ?: "Cập nhật thất bại", Toast.LENGTH_SHORT).show()
                            } finally {
                                isLoading = false
                            }
                        }
                    }
                )

                ProfilePage.CHANGE_PASSWORD -> ChangePasswordPage(
                    isLoading = isLoading,
                    onBack = { currentPage = ProfilePage.MAIN },
                    onChangePassword = { oldPw, newPw ->
                        scope.launch {
                            isLoading = true
                            try {
                                val email = currentUser?.email.orEmpty()
                                val credential = EmailAuthProvider.getCredential(email, oldPw)
                                currentUser?.reauthenticate(credential)?.await()
                                currentUser?.updatePassword(newPw)?.await()
                                currentPage = ProfilePage.MAIN
                                Toast.makeText(context, "Đổi mật khẩu thành công", Toast.LENGTH_SHORT).show()
                            } catch (e: Exception) {
                                Toast.makeText(context, e.localizedMessage ?: "Đổi mật khẩu thất bại", Toast.LENGTH_SHORT).show()
                            } finally {
                                isLoading = false
                            }
                        }
                    }
                )

                ProfilePage.MANAGE_SUBSCRIPTION -> SubscriptionManagementPage(
                    plan = plan,
                    subscriptionStartedDate = subscriptionStartedDate,
                    subscriptionExpiredDate = subscriptionExpiredDate,
                    onBack = { currentPage = ProfilePage.MAIN },
                    onChangePlan = onOpenPayment
                )
            }
        }

        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f)),
                contentAlignment = Alignment.Center
            ) {
                com.example.alphacinema.ui.components.LottieLoadingIndicator(size = 80.dp)
            }
        }
    }
}

// ── Main Profile Settings Page ──────────────────────────────────────────────

@Composable
private fun ProfileMainPage(
    displayName: String,
    avatarUrl: String,
    plan: String,
    isGoogleUser: Boolean,
    onBack: () -> Unit,
    onEditProfile: () -> Unit,
    onChangePassword: () -> Unit,
    onManageSubscription: () -> Unit,
    onLogout: () -> Unit
) {
    val planLabel = when (plan.lowercase()) {
        "basic" -> "Gói Basic"
        "couple" -> "Gói Couple"
        "premium" -> "Gói Premium"
        else -> "Gói Free"
    }
    val planColor = when (plan.lowercase()) {
        "basic" -> Color(0xFFD8DEE9)
        "couple" -> Color(0xFFFFB2D2)
        "premium" -> Color(0xFFF6E29A)
        else -> Color.White.copy(alpha = 0.6f)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp)
    ) {
        // Top bar
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.08f))
            ) {
                Icon(
                    Icons.AutoMirrored.Rounded.ArrowBack, null,
                    tint = Color.White, modifier = Modifier.size(20.dp)
                )
            }
            Text(
                "Cài đặt tài khoản",
                color = Color.White,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.padding(start = 14.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Avatar + Name
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ProfileAvatarLarge(name = displayName, url = avatarUrl)
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                displayName.ifBlank { "Người dùng" },
                color = Color.White,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(planLabel, color = planColor, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Menu
        SettingsMenuItem(
            icon = Icons.Rounded.Edit,
            iconTint = Color(0xFFF6E29A),
            title = "Hồ sơ cá nhân",
            onClick = onEditProfile
        )
        Spacer(modifier = Modifier.height(16.dp))

        SettingsMenuItem(
            icon = Icons.Rounded.Lock,
            iconTint = Color(0xFFF6E29A),
            title = "Đổi mật khẩu",
            onClick = if (isGoogleUser) ({}) else onChangePassword,
            enabled = !isGoogleUser
        )
        Spacer(modifier = Modifier.height(16.dp))

        SettingsMenuItem(
            icon = Icons.Rounded.WorkspacePremium,
            iconTint = Color(0xFFF6E29A),
            title = "Quản lý gói đăng ký",
            onClick = onManageSubscription
        )

        // Push logout to bottom
        Spacer(modifier = Modifier.weight(1f))

        // Logout
        Button(
            onClick = onLogout,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFF6E29A),
                contentColor = Color.Black
            )
        ) {
            Icon(Icons.AutoMirrored.Rounded.ExitToApp, null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.size(8.dp))
            Text("Đăng xuất", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

// ── Edit Profile Page ───────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditProfilePage(
    displayName: String,
    avatarUrl: String,
    isLoading: Boolean,
    onBack: () -> Unit,
    onSave: (name: String, imageUri: Uri?) -> Unit
) {
    val context = LocalContext.current
    var editName by remember { mutableStateOf(displayName) }
    var nameError by remember { mutableStateOf<String?>(null) }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var showPickerSheet by remember { mutableStateOf(false) }

    // Camera temp file URI
    var cameraUri by remember { mutableStateOf<Uri?>(null) }

    // Gallery picker
    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri -> if (uri != null) selectedImageUri = uri }

    // Camera capture
    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success -> if (success && cameraUri != null) selectedImageUri = cameraUri }

    // File picker
    val fileLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> if (uri != null) selectedImageUri = uri }

    // Camera permission
    val cameraPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            val photoFile = File(context.cacheDir, "camera_photos").apply { mkdirs() }
                .let { File(it, "avatar_${System.currentTimeMillis()}.jpg") }
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", photoFile)
            cameraUri = uri
            cameraLauncher.launch(uri)
        } else {
            Toast.makeText(context, "Cần quyền camera để chụp ảnh", Toast.LENGTH_SHORT).show()
        }
    }

    // Display URL: selected local image or existing remote avatar
    val previewModel: Any = selectedImageUri ?: avatarUrl
    val previewName = editName

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .clearFocusOnTapOutside()
            .padding(horizontal = 20.dp)
    ) {
        // Top bar
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.size(40.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.08f))
            ) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, null, tint = Color.White, modifier = Modifier.size(20.dp))
            }
            Text(
                "Hồ sơ cá nhân",
                color = Color.White,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.padding(start = 14.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Avatar - tap to change
        Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                contentAlignment = Alignment.BottomEnd,
                modifier = Modifier.clickable { showPickerSheet = true }
            ) {
                val urlStr = (previewModel as? Uri)?.toString() ?: (previewModel as? String) ?: ""
                if (urlStr.isNotBlank()) {
                    com.example.alphacinema.ui.components.AlphaCinemaImage(
                        model = previewModel, contentDescription = "Avatar",
                        modifier = Modifier.size(96.dp).clip(CircleShape)
                            .border(2.dp, Brush.linearGradient(listOf(Color(0xFFF6E29A), Color(0xFFD4A843))), CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier.size(96.dp).clip(CircleShape)
                            .background(Brush.linearGradient(listOf(Color(0xFFF6E29A), Color(0xFFD4A843))))
                            .border(2.dp, Color.White.copy(alpha = 0.2f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            (previewName.firstOrNull() ?: 'A').uppercase(),
                            color = Color.Black,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
                Box(
                    modifier = Modifier.size(32.dp).clip(CircleShape)
                        .background(Color(0xFFF6E29A)).border(2.dp, Color.Black, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.CameraAlt, null, tint = Color.Black, modifier = Modifier.size(16.dp))
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                if (selectedImageUri != null) "Đã chọn ảnh mới" else "Nhấn để đổi ảnh đại diện",
                color = if (selectedImageUri != null) Color(0xFFD8DEE9) else Color.White.copy(alpha = 0.5f),
                style = MaterialTheme.typography.labelSmall
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Name field
        OutlinedTextField(
            value = editName,
            onValueChange = { editName = it; nameError = null },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Tên hiển thị") },
            singleLine = true,
            isError = nameError != null,
            shape = RoundedCornerShape(13.dp),
            textStyle = LocalTextStyle.current.copy(fontSize = 15.sp),
            supportingText = { if (nameError != null) Text(nameError!!) },
            colors = authTextFieldColors()
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                if (editName.isBlank()) nameError = "Tên không được để trống"
                else onSave(editName.trim(), selectedImageUri)
            },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(16.dp),
            enabled = !isLoading,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF6E29A), contentColor = Color.Black)
        ) {
            Text("Lưu thay đổi", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, lineHeight = 20.sp)
        }

        Spacer(modifier = Modifier.height(32.dp))
    }

    // Image source picker sheet
    if (showPickerSheet) {
        ModalBottomSheet(
            onDismissRequest = { showPickerSheet = false },
            containerColor = Color(0xFF141414),
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            dragHandle = {
                Box(
                    Modifier.padding(top = 10.dp, bottom = 6.dp)
                        .size(width = 40.dp, height = 4.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(Color.White.copy(alpha = 0.2f))
                )
            }
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text("Chọn ảnh đại diện", color = Color.White, fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 8.dp))

                ImageSourceOption(
                    icon = Icons.Rounded.PhotoLibrary, iconTint = Color(0xFFD8DEE9),
                    label = "Thư viện ảnh"
                ) {
                    showPickerSheet = false
                    galleryLauncher.launch("image/*")
                }

                ImageSourceOption(
                    icon = Icons.Rounded.CameraAlt, iconTint = Color(0xFFF6E29A),
                    label = "Chụp ảnh"
                ) {
                    showPickerSheet = false
                    cameraPermLauncher.launch(android.Manifest.permission.CAMERA)
                }

                ImageSourceOption(
                    icon = Icons.Rounded.Folder, iconTint = Color(0xFFE8A0FF),
                    label = "Chọn từ tệp"
                ) {
                    showPickerSheet = false
                    fileLauncher.launch(arrayOf("image/*"))
                }

                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun ImageSourceOption(
    icon: ImageVector,
    iconTint: Color,
    label: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White.copy(alpha = 0.06f))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(38.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = iconTint, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.width(14.dp))
        Text(label, color = Color.White, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyLarge)
    }
}

// ── Change Password Page ────────────────────────────────────────────────────

@Composable
private fun ChangePasswordPage(
    isLoading: Boolean,
    onBack: () -> Unit,
    onChangePassword: (oldPassword: String, newPassword: String) -> Unit
) {
    var currentPw by remember { mutableStateOf("") }
    var newPw by remember { mutableStateOf("") }
    var confirmPw by remember { mutableStateOf("") }
    var showCurrent by remember { mutableStateOf(false) }
    var showNew by remember { mutableStateOf(false) }
    var showConfirm by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .clearFocusOnTapOutside()
            .padding(horizontal = 20.dp)
    ) {
        // Top bar
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.size(40.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.08f))
            ) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, null, tint = Color.White, modifier = Modifier.size(20.dp))
            }
            Text(
                "Đổi mật khẩu",
                color = Color.White,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.padding(start = 14.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Lock icon
        Box(
            modifier = Modifier.align(Alignment.CenterHorizontally).size(72.dp).clip(CircleShape)
                .background(Color(0xFFE8A0FF).copy(alpha = 0.10f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.Lock, null, tint = Color(0xFFE8A0FF), modifier = Modifier.size(34.dp))
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Cập nhật mật khẩu để bảo vệ tài khoản",
            color = Color.White.copy(alpha = 0.6f),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(24.dp))

        AuthPasswordField(
            value = currentPw, onValueChanged = { currentPw = it; error = null },
            label = "Mật khẩu hiện tại", visible = showCurrent,
            onToggleVisibility = { showCurrent = !showCurrent }, errorText = null
        )
        Spacer(modifier = Modifier.height(10.dp))
        AuthPasswordField(
            value = newPw, onValueChanged = { newPw = it; error = null },
            label = "Mật khẩu mới", visible = showNew,
            onToggleVisibility = { showNew = !showNew }, errorText = null
        )
        Spacer(modifier = Modifier.height(10.dp))
        AuthPasswordField(
            value = confirmPw, onValueChanged = { confirmPw = it; error = null },
            label = "Nhập lại mật khẩu mới", visible = showConfirm,
            onToggleVisibility = { showConfirm = !showConfirm }, errorText = null
        )

        if (error != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(error!!, color = Color(0xFFFF6B6B), fontSize = 13.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                when {
                    currentPw.isBlank() -> error = "Vui lòng nhập mật khẩu hiện tại"
                    newPw.length < 6 -> error = "Mật khẩu mới phải có ít nhất 6 ký tự"
                    newPw != confirmPw -> error = "Mật khẩu xác nhận không khớp"
                    newPw == currentPw -> error = "Mật khẩu mới phải khác mật khẩu cũ"
                    else -> onChangePassword(currentPw, newPw)
                }
            },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(16.dp),
            enabled = !isLoading,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE8A0FF), contentColor = Color.Black)
        ) {
            Text("Xác nhận đổi mật khẩu", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, lineHeight = 20.sp)
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

// ── Shared Components ───────────────────────────────────────────────────────

@Composable
private fun ProfileAvatarLarge(name: String, url: String) {
    if (url.isNotBlank()) {
        val imageModel: Any = if (url.startsWith("/")) File(url) else url
        com.example.alphacinema.ui.components.AlphaCinemaImage(
            model = imageModel, contentDescription = "Avatar",
            modifier = Modifier.size(96.dp).clip(CircleShape)
                .border(2.dp, Brush.linearGradient(listOf(Color(0xFFF6E29A), Color(0xFFD4A843))), CircleShape),
            contentScale = ContentScale.Crop
        )
    } else {
        Box(
            modifier = Modifier.size(96.dp).clip(CircleShape)
                .background(Brush.linearGradient(listOf(Color(0xFFF6E29A), Color(0xFFD4A843))))
                .border(2.dp, Color.White.copy(alpha = 0.2f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                (name.firstOrNull() ?: 'A').uppercase(),
                color = Color.Black,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold
            )
        }
    }
}

@Composable
private fun SettingsMenuItem(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    Row(
        modifier = Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (enabled) Color.White.copy(alpha = 0.06f) else Color.White.copy(alpha = 0.03f))
            .border(1.dp, if (enabled) Color.White.copy(alpha = 0.10f) else Color.White.copy(alpha = 0.06f), RoundedCornerShape(16.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(42.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = if (enabled) iconTint else iconTint.copy(alpha = 0.4f), modifier = Modifier.size(22.dp))
        }
        Column(modifier = Modifier.weight(1f).padding(start = 14.dp, end = 8.dp)) {
            Text(
                text = title,
                color = if (enabled) Color.White else Color.White.copy(alpha = 0.5f),
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Icon(Icons.AutoMirrored.Rounded.ArrowForwardIos, null,
            tint = if (enabled) Color.White.copy(alpha = 0.4f) else Color.White.copy(alpha = 0.15f), modifier = Modifier.size(14.dp))
    }
}

// ── Subscription Management Page ────────────────────────────────────────────

@Composable
private fun SubscriptionManagementPage(
    plan: String,
    subscriptionStartedDate: String?,
    subscriptionExpiredDate: String?,
    onBack: () -> Unit,
    onChangePlan: () -> Unit
) {
    val membershipPlan = buildDemoMembershipPlan(
        currentPlan = plan,
        expiredDate = subscriptionExpiredDate,
        startedDate = subscriptionStartedDate
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
    ) {
        // Top bar
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(top = 8.dp, bottom = 24.dp)
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "Quay lại",
                    tint = Color.White
                )
            }
            Text(
                "Quản lý gói đăng ký",
                color = Color.White,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.padding(start = 4.dp)
            )
        }

        // Scrollable content
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            // Plan badge
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(
                                Color.White.copy(alpha = 0.075f),
                                Color.White.copy(alpha = 0.035f)
                            )
                        )
                    )
                    .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(24.dp))
                    .padding(20.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(48.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Rounded.WorkspacePremium, null,
                                tint = Color(0xFFF6E29A),
                                modifier = Modifier.size(26.dp)
                            )
                        }
                        Column(modifier = Modifier.padding(start = 14.dp)) {
                            Text(
                                membershipPlan.badge,
                                color = Color(0xFFF6E29A),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                membershipPlan.price,
                                color = Color.White.copy(alpha = 0.6f),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    PlanInfoRow(
                        icon = Icons.Rounded.CheckCircle,
                        label = "Ngày đăng ký",
                        value = if (membershipPlan.key == DemoMembershipPlanKey.FREE) {
                            "Không áp dụng"
                        } else {
                            membershipPlan.startedDate.ifBlank { "Chưa lưu" }
                        }
                    )
                    PlanInfoRow(
                        icon = Icons.Rounded.WatchLater,
                        label = "Ngày hết hạn",
                        value = if (membershipPlan.key == DemoMembershipPlanKey.FREE) {
                            "Không áp dụng"
                        } else {
                            membershipPlan.expiredDate.ifBlank { "Chưa lưu" }
                        }
                    )
                    PlanInfoRow(
                        icon = Icons.Rounded.CheckCircle,
                        label = "Trạng thái",
                        value = "Đang hoạt động"
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Benefits
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
                    "Quyền lợi",
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold
                )
                membershipPlan.benefits.ifEmpty {
                    listOf("Các tính năng xem phim cơ bản")
                }.forEach { benefit ->
                    MembershipBenefitRow(text = benefit)
                }
            }
        }

        // Bottom buttons - pinned
        if (membershipPlan.key != DemoMembershipPlanKey.PREMIUM) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(top = 12.dp, bottom = 16.dp)
            ) {
                Button(
                    onClick = onChangePlan,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFF6E29A),
                        contentColor = Color.Black
                    )
                ) {
                    Text("Nâng cấp gói", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, lineHeight = 20.sp)
                }
            }
        }
    }
}
