@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.alphacinema.ui.account

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val GoldAccent = Color(0xFFF6E29A)
private val DarkBg = Color.Black
private val CardBg = Color(0xFF141414)
private val CardBorder = Color(0xFF333333)
private val ErrorRed = Color(0xFFFF6B6B)
private val SuccessGreen = Color(0xFF4ADE80)

private enum class ForgotStep { EMAIL, OTP, NEW_PASSWORD, SUCCESS }

@Composable
fun ForgotPasswordScreen(
    onCheckEmailExists: suspend (String) -> Boolean,
    onSendOtp: suspend (String) -> Boolean,
    onVerifyOtp: (String) -> com.example.alphacinema.data.api.OtpVerifyResult,
    onResetPassword: suspend (email: String, newPw: String) -> String?,
    onClose: () -> Unit
) {
    var step by remember { mutableStateOf(ForgotStep.EMAIL) }
    var email by remember { mutableStateOf("") }
    var emailError by remember { mutableStateOf<String?>(null) }
    var otpValue by remember { mutableStateOf(TextFieldValue("")) }
    var otpError by remember { mutableStateOf<String?>(null) }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var showNewPw by remember { mutableStateOf(false) }
    var showConfirmPw by remember { mutableStateOf(false) }
    var passwordError by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var resendCountdown by remember { mutableIntStateOf(0) }
    var canResend by remember { mutableStateOf(true) }

    val scope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val context = androidx.compose.ui.platform.LocalContext.current

    LaunchedEffect(resendCountdown) {
        if (resendCountdown > 0) {
            canResend = false
            delay(1000L)
            resendCountdown--
        } else {
            canResend = true
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { focusManager.clearFocus() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(8.dp))
            IconButton(onClick = {
                if (step == ForgotStep.EMAIL) onClose()
                else if (step == ForgotStep.OTP) step = ForgotStep.EMAIL
                else step = ForgotStep.OTP
            }) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Quay lại", tint = Color.White)
            }

            Spacer(modifier = Modifier.height(16.dp))

            val title = when (step) {
                ForgotStep.EMAIL -> "Quên mật khẩu"
                ForgotStep.OTP -> "Xác thực email"
                ForgotStep.NEW_PASSWORD -> "Đặt mật khẩu mới"
                ForgotStep.SUCCESS -> "Thành công!"
            }
            val subtitle = when (step) {
                ForgotStep.EMAIL -> "Nhập email tài khoản của bạn để nhận mã xác thực"
                ForgotStep.OTP -> "Chúng tôi đã gửi mã xác thực 6 số đến"
                ForgotStep.NEW_PASSWORD -> "Nhập mật khẩu mới cho tài khoản của bạn"
                ForgotStep.SUCCESS -> "Đã đổi mật khẩu và đăng nhập thành công"
            }

            Text(title, color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(6.dp))
            Text(subtitle, color = Color.White.copy(alpha = 0.6f), fontSize = 14.sp)
            if (step == ForgotStep.OTP) {
                Text(email, color = GoldAccent, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }

            Spacer(modifier = Modifier.height(28.dp))

            if (step == ForgotStep.EMAIL) {
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it; emailError = null },
                    label = { Text("Email") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = emailError != null,
                    shape = RoundedCornerShape(13.dp),
                    supportingText = { emailError?.let { Text(it) } },
                    colors = forgotTextFieldColors()
                )
                Spacer(modifier = Modifier.height(20.dp))
                Button(
                    onClick = {
                        val trimmed = email.trim()
                        if (trimmed.isBlank()) { emailError = "Email không được để trống"; return@Button }
                        isLoading = true
                        scope.launch {
                            if (onCheckEmailExists(trimmed)) {
                                if (onSendOtp(trimmed)) {
                                    otpValue = TextFieldValue(""); otpError = null; resendCountdown = 30; step = ForgotStep.OTP
                                } else emailError = "Không thể gửi mã xác thực. Vui lòng kiểm tra lại email hoặc thử lại sau."
                            } else emailError = "Email chưa được đăng ký trong hệ thống."
                            isLoading = false
                        }
                    },
                    enabled = !isLoading,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GoldAccent, 
                        contentColor = Color.Black,
                        disabledContainerColor = GoldAccent.copy(alpha = 0.5f),
                        disabledContentColor = Color.Black.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                ) {
                    if (isLoading) CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.Black, strokeWidth = 2.dp)
                    else Text("Tiếp tục", fontWeight = FontWeight.Bold)
                }
            }

            if (step == ForgotStep.OTP) {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    BasicTextField(
                        value = otpValue,
                        onValueChange = { newVal ->
                            val filtered = newVal.text.filter { it.isDigit() }.take(6)
                            otpValue = TextFieldValue(filtered, TextRange(filtered.length))
                            otpError = null
                        },
                        modifier = Modifier.focusRequester(focusRequester).fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        decorationBox = {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
                            ) {
                                repeat(6) { i ->
                                    val ch = otpValue.text.getOrNull(i)?.toString() ?: ""
                                    val isFocused = otpValue.text.length == i
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .aspectRatio(1f)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(CardBg)
                                            .border(1.5.dp, if (otpError != null) ErrorRed else if (isFocused) GoldAccent else CardBorder, RoundedCornerShape(12.dp)), 
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(ch, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    )
                }
                AnimatedVisibility(otpError != null) {
                    Text(otpError ?: "", color = ErrorRed, fontSize = 13.sp, modifier = Modifier.fillMaxWidth().padding(top = 10.dp), textAlign = TextAlign.Center)
                }
                Spacer(modifier = Modifier.height(20.dp))
                Button(
                    onClick = {
                        if (otpValue.text.length != 6) { otpError = "Nhập đủ 6 số"; return@Button }
                        isLoading = true
                        val result = onVerifyOtp(otpValue.text)
                        if (result == com.example.alphacinema.data.api.OtpVerifyResult.SUCCESS) {
                            step = ForgotStep.NEW_PASSWORD
                        } else {
                            otpError = "Mã không đúng"; otpValue = TextFieldValue("")
                        }
                        isLoading = false
                    },
                    enabled = !isLoading,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GoldAccent, 
                        contentColor = Color.Black,
                        disabledContainerColor = GoldAccent.copy(alpha = 0.5f),
                        disabledContentColor = Color.Black.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                ) {
                    if (isLoading) CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.Black, strokeWidth = 2.dp)
                    else Text("Xác nhận", fontWeight = FontWeight.Bold) 
                }
                
                Spacer(modifier = Modifier.height(24.dp))

                // Giao diện Gửi lại mã
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Không nhận được mã? ",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 13.sp
                    )
                    if (canResend) {
                        Text(
                            "Gửi lại mã",
                            color = GoldAccent,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable {
                                scope.launch {
                                    otpError = null
                                    val sent = onSendOtp(email.trim())
                                    if (sent) {
                                        resendCountdown = 30
                                        android.widget.Toast.makeText(context, "Đã gửi lại mã", android.widget.Toast.LENGTH_SHORT).show()
                                    } else {
                                        otpError = "Không thể gửi lại mã."
                                    }
                                }
                            }
                        )
                    } else {
                        Text(
                            "Gửi lại mã sau ${resendCountdown}s",
                            color = Color.White.copy(alpha = 0.35f),
                            fontSize = 13.sp
                        )
                    }
                }
                
                LaunchedEffect(Unit) { delay(300); focusRequester.requestFocus() }
            }

            if (step == ForgotStep.NEW_PASSWORD) {
                OutlinedTextField(
                    value = newPassword,
                    onValueChange = { newPassword = it; passwordError = null },
                    label = { Text("Mật khẩu mới") },
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = if (showNewPw) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = { IconButton(onClick = { showNewPw = !showNewPw }) { Icon(if (showNewPw) Icons.Rounded.VisibilityOff else Icons.Rounded.RemoveRedEye, null, tint = Color.White.copy(0.6f)) } },
                    colors = forgotTextFieldColors()
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it; passwordError = null },
                    label = { Text("Nhập lại mật khẩu mới") },
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = if (showConfirmPw) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = { IconButton(onClick = { showConfirmPw = !showConfirmPw }) { Icon(if (showConfirmPw) Icons.Rounded.VisibilityOff else Icons.Rounded.RemoveRedEye, null, tint = Color.White.copy(0.6f)) } },
                    supportingText = { passwordError?.let { Text(it) } },
                    colors = forgotTextFieldColors()
                )
                Spacer(modifier = Modifier.height(20.dp))
                Button(
                    onClick = {
                        if (newPassword.length < 6) { passwordError = "Tối thiểu 6 ký tự"; return@Button }
                        if (newPassword != confirmPassword) { passwordError = "Mật khẩu không khớp"; return@Button }
                        isLoading = true
                        scope.launch {
                            val err = onResetPassword(email.trim(), newPassword)
                            if (err == null) step = ForgotStep.SUCCESS
                            else passwordError = err
                            isLoading = false
                        }
                    },
                    enabled = !isLoading,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GoldAccent, 
                        contentColor = Color.Black,
                        disabledContainerColor = GoldAccent.copy(alpha = 0.5f),
                        disabledContentColor = Color.Black.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                ) {
                    if (isLoading) CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.Black, strokeWidth = 2.dp)
                    else Text("Đổi mật khẩu", fontWeight = FontWeight.Bold)
                }
            }

            if (step == ForgotStep.SUCCESS) {
                Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Rounded.CheckCircle, null, tint = SuccessGreen, modifier = Modifier.size(72.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Đổi mật khẩu thành công!\nĐang vào ứng dụng...", color = Color.White, textAlign = TextAlign.Center)
                }
                LaunchedEffect(Unit) { delay(2000); onClose() }
            }
        }
    }
}

@Composable
private fun forgotTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = Color.White, unfocusedTextColor = Color.White,
    focusedLabelColor = GoldAccent, unfocusedLabelColor = Color.White.copy(0.6f),
    focusedBorderColor = GoldAccent, unfocusedBorderColor = Color.White.copy(0.2f),
    cursorColor = GoldAccent
)
