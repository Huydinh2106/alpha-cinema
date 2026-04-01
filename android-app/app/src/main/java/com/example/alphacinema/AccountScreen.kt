@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.alphacinema

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.material.icons.outlined.Policy
import androidx.compose.material.icons.outlined.RemoveRedEye
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material.icons.outlined.WatchLater
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import coil.compose.AsyncImage
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

private const val WEB_CLIENT_ID = "1013232588133-86fl74ls04r8fkarnahh2b9pvie5g7kn.apps.googleusercontent.com"

private data class AccountMenuItemUi(
    val title: String,
    val icon: @Composable () -> Unit
)

@Composable
fun AccountScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val auth = remember { FirebaseAuth.getInstance() }
    var currentUser by remember { mutableStateOf(auth.currentUser) }
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

    val menuItems = listOf(
        AccountMenuItemUi("Đang xem", { Icon(Icons.Outlined.WatchLater, contentDescription = null) }),
        AccountMenuItemUi("Danh sách phim của tôi", { Icon(Icons.AutoMirrored.Outlined.ListAlt, contentDescription = null) }),
        AccountMenuItemUi("Yêu thích", { Icon(Icons.Outlined.FavoriteBorder, contentDescription = null) }),
        AccountMenuItemUi("Chính sách", { Icon(Icons.Outlined.Policy, contentDescription = null) }),
        AccountMenuItemUi("Góp ý", { Icon(Icons.Outlined.ChatBubbleOutline, contentDescription = null) })
    )

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
                .padding(horizontal = 20.dp, vertical = 18.dp),
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (photoUrl != null) {
                        AsyncImage(
                            model = photoUrl,
                            contentDescription = "Avatar",
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .background(
                                    Brush.linearGradient(listOf(Color(0xFFF6E29A), Color(0xFFD4A843))),
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = (currentUser?.displayName?.firstOrNull() ?: currentUser?.email?.firstOrNull() ?: 'A').uppercase(),
                                color = Color(0xFF070B16),
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Column(modifier = Modifier.padding(start = 14.dp)) {
                        Text(
                            text = currentUser?.displayName ?: "Người dùng",
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = currentUser?.email ?: "",
                            color = Color.White.copy(alpha = 0.6f),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                // Logout button
                Button(
                    onClick = {
                        auth.signOut()
                        currentUser = null
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF2A1A1A),
                        contentColor = Color(0xFFFF6B6B)
                    )
                ) {
                    Icon(Icons.Outlined.ExitToApp, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.size(8.dp))
                    Text("Đăng xuất", fontWeight = FontWeight.Bold)
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
                    CircularProgressIndicator(color = Color(0xFFF6E29A), modifier = Modifier.size(28.dp), strokeWidth = 2.dp)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

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
                        .clickable { }
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
