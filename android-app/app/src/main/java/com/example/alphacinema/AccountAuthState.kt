package com.example.alphacinema

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.Composable

enum class AuthMode {
    LOGIN,
    REGISTER
}

data class AccountAuthUiState(
    val showDialog: Boolean = false,
    val mode: AuthMode = AuthMode.LOGIN,
    val name: String = "",
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val showPassword: Boolean = false,
    val showConfirmPassword: Boolean = false,
    val nameError: String? = null,
    val emailError: String? = null,
    val passwordError: String? = null,
    val confirmPasswordError: String? = null
)

sealed interface AccountAuthEvent {
    data class OpenDialog(val mode: AuthMode) : AccountAuthEvent
    data object CloseDialog : AccountAuthEvent
    data class SwitchMode(val mode: AuthMode) : AccountAuthEvent
    data class NameChanged(val value: String) : AccountAuthEvent
    data class EmailChanged(val value: String) : AccountAuthEvent
    data class PasswordChanged(val value: String) : AccountAuthEvent
    data class ConfirmPasswordChanged(val value: String) : AccountAuthEvent
    data object TogglePasswordVisibility : AccountAuthEvent
    data object ToggleConfirmPasswordVisibility : AccountAuthEvent
    data object Submit : AccountAuthEvent
    data object ContinueWithGoogle : AccountAuthEvent
}

class AccountAuthStateHolder(
    private val onLogin: (email: String, password: String) -> Unit,
    private val onRegister: (name: String, email: String, password: String) -> Unit,
    private val onGoogleSignIn: () -> Unit
) {
    var uiState by mutableStateOf(AccountAuthUiState())
        private set

    fun onEvent(event: AccountAuthEvent) {
        when (event) {
            is AccountAuthEvent.OpenDialog -> {
                uiState = uiState.copy(
                    showDialog = true,
                    mode = event.mode,
                    nameError = null,
                    emailError = null,
                    passwordError = null,
                    confirmPasswordError = null
                )
            }

            AccountAuthEvent.CloseDialog -> {
                uiState = uiState.copy(showDialog = false)
            }

            is AccountAuthEvent.SwitchMode -> {
                uiState = uiState.copy(
                    mode = event.mode,
                    nameError = null,
                    emailError = null,
                    passwordError = null,
                    confirmPasswordError = null
                )
            }

            is AccountAuthEvent.NameChanged -> {
                uiState = uiState.copy(
                    name = event.value,
                    nameError = null
                )
            }

            is AccountAuthEvent.EmailChanged -> {
                uiState = uiState.copy(
                    email = event.value,
                    emailError = null
                )
            }

            is AccountAuthEvent.PasswordChanged -> {
                uiState = uiState.copy(
                    password = event.value,
                    passwordError = null
                )
            }

            is AccountAuthEvent.ConfirmPasswordChanged -> {
                uiState = uiState.copy(
                    confirmPassword = event.value,
                    confirmPasswordError = null
                )
            }

            AccountAuthEvent.TogglePasswordVisibility -> {
                uiState = uiState.copy(showPassword = !uiState.showPassword)
            }

            AccountAuthEvent.ToggleConfirmPasswordVisibility -> {
                uiState = uiState.copy(showConfirmPassword = !uiState.showConfirmPassword)
            }

            AccountAuthEvent.Submit -> submit()
            AccountAuthEvent.ContinueWithGoogle -> {
                onGoogleSignIn()
                uiState = uiState.copy(showDialog = false)
            }
        }
    }

    private fun submit() {
        if (uiState.mode == AuthMode.LOGIN) {
            val emailError = validateEmail(uiState.email)
            val passwordError = validateRequired(uiState.password, "Mật khẩu")

            uiState = uiState.copy(
                emailError = emailError,
                passwordError = passwordError
            )

            if (emailError == null && passwordError == null) {
                onLogin(uiState.email.trim(), uiState.password)
                uiState = uiState.copy(showDialog = false)
            }
        } else {
            val nameError = validateRequired(uiState.name, "Tên")
            val emailError = validateEmail(uiState.email)
            val passwordError = validateRequired(uiState.password, "Mật khẩu")
            val confirmError = when {
                uiState.confirmPassword.isBlank() -> "Nhập lại mật khẩu"
                uiState.confirmPassword != uiState.password -> "Mật khẩu xác nhận không khớp"
                else -> null
            }

            uiState = uiState.copy(
                nameError = nameError,
                emailError = emailError,
                passwordError = passwordError,
                confirmPasswordError = confirmError
            )

            if (nameError == null && emailError == null && passwordError == null && confirmError == null) {
                onRegister(uiState.name.trim(), uiState.email.trim(), uiState.password)
                uiState = uiState.copy(showDialog = false)
            }
        }
    }

    private fun validateRequired(value: String, label: String): String? {
        return if (value.isBlank()) "$label không được để trống" else null
    }

    private fun validateEmail(value: String): String? {
        if (value.isBlank()) return "Email không được để trống"
        val emailRegex = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")
        return if (!emailRegex.matches(value.trim())) "Email không hợp lệ" else null
    }
}

@Composable
fun rememberAccountAuthStateHolder(
    onLogin: (email: String, password: String) -> Unit = { _, _ -> },
    onRegister: (name: String, email: String, password: String) -> Unit = { _, _, _ -> },
    onGoogleSignIn: () -> Unit = {}
): AccountAuthStateHolder {
    return remember {
        AccountAuthStateHolder(
            onLogin = onLogin,
            onRegister = onRegister,
            onGoogleSignIn = onGoogleSignIn
        )
    }
}
