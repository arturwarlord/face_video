package com.facevideo.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.facevideo.app.data.SupabaseClientProvider
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.launch
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

private val AuthBackground = Color(0xFFF4E9E5)
private val AuthAccent = Color(0xFF5D6C84)
private val AuthMuted = Color(0xFF68758A)
private val AuthText = Color(0xFF344052)

@Composable
fun SupabaseAuthGate(
    content: @Composable (userEmail: String?, userId: String?, onSignOut: () -> Unit) -> Unit
) {
    val supabase = SupabaseClientProvider.client

    if (supabase == null) {
        content(null, null) {}
        return
    }

    val sessionStatus by supabase.auth.sessionStatus.collectAsState(
        initial = SessionStatus.Initializing
    )
    val scope = rememberCoroutineScope()

    when (val status = sessionStatus) {
        SessionStatus.Initializing -> {
            Box(
                Modifier.fillMaxSize().background(AuthBackground),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = AuthAccent)
            }
        }
        is SessionStatus.Authenticated -> {
            val user = status.session.user
            content(user?.email ?: "Пользователь", user?.id) {
                scope.launch { supabase.auth.signOut() }
            }
        }
        else -> {
            SupabaseSignInScreen(
                onSignIn = { email, password ->
                    supabase.auth.signInWith(Email) {
                        this.email = email
                        this.password = password
                    }
                },
                onSignUp = { email, password, username ->
                    supabase.auth.signUpWith(Email) {
                        this.email = email
                        this.password = password
                        data = buildJsonObject {
                            put("face_video_app", true)
                            put("username", username)
                            put("display_name", username)
                        }
                    }
                }
            )
        }
    }
}

@Composable
private fun SupabaseSignInScreen(
    onSignIn: suspend (String, String) -> Unit,
    onSignUp: suspend (String, String, String) -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var isRegistration by remember { mutableStateOf(false) }
    var isBusy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Box(
        Modifier.fillMaxSize().background(
            Brush.verticalGradient(listOf(AuthBackground, Color(0xFFE5EAF0), AuthBackground))
        ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 26.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                "ARTURCHAT",
                color = AuthAccent,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 3.sp
            )
            Spacer(Modifier.height(12.dp))
            Text(
                if (isRegistration) "Создать аккаунт" else "С возвращением",
                color = AuthText,
                fontSize = 27.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            Text("Вход в интернет-мессенджер", color = AuthMuted, fontSize = 13.sp)
            Spacer(Modifier.height(28.dp))

            OutlinedTextField(
                value = email,
                onValueChange = { email = it; message = null },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("Электронная почта") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                colors = authFieldColors()
            )
            if (isRegistration) {
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it.lowercase().filter { ch -> ch.isLetterOrDigit() || ch == '_' }.take(24); message = null },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("Псевдоним (username)") },
                    colors = authFieldColors()
                )
            }
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = password,
                onValueChange = { password = it; message = null },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("Пароль") },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                colors = authFieldColors()
            )

            if (message != null) {
                Spacer(Modifier.height(12.dp))
                Text(message.orEmpty(), color = Color(0xFFA85F6E), fontSize = 12.sp)
            }

            Spacer(Modifier.height(20.dp))
            Button(
                onClick = {
                    val cleanEmail = email.trim()
                    val cleanUsername = username.trim()
                    val validUsername = cleanUsername.matches(Regex("^[a-z0-9_]{3,24}$"))
                    if (!cleanEmail.contains("@") || password.length < 6 ||
                        (isRegistration && !validUsername)
                    ) {
                        message = if (isRegistration) {
                            "Проверь email, пароль (минимум 6 символов) и псевдоним (3–24 символа)."
                        } else {
                            "Проверь email и пароль (не менее 6 символов)."
                        }
                    } else {
                        isBusy = true
                        message = null
                        scope.launch {
                            try {
                                if (isRegistration) {
                                    onSignUp(cleanEmail, password, cleanUsername)
                                    message = "Аккаунт создан. Проверь почту, если включено подтверждение email."
                                } else {
                                    onSignIn(cleanEmail, password)
                                }
                            } catch (error: Exception) {
                                message = error.message ?: "Не удалось выполнить операцию."
                            } finally {
                                isBusy = false
                            }
                        }
                    }
                },
                enabled = !isBusy,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(15.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AuthAccent,
                    contentColor = Color.White
                )
            ) {
                Text(
                    when {
                        isBusy -> "Подключение..."
                        isRegistration -> "Зарегистрироваться"
                        else -> "Войти"
                    },
                    fontWeight = FontWeight.Bold
                )
            }

            TextButton(onClick = {
                isRegistration = !isRegistration
                message = null
            }) {
                Text(
                    if (isRegistration) "Уже есть аккаунт? Войти" else "Создать новый аккаунт",
                    color = AuthAccent
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "Регистрация по email. Номер телефона и платные SMS не используются.",
                color = AuthMuted,
                fontSize = 10.sp
            )
        }
    }
}

@Composable
private fun authFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = AuthAccent,
    unfocusedBorderColor = Color(0xFF9EAFC5),
    focusedLabelColor = AuthAccent,
    unfocusedLabelColor = AuthMuted,
    focusedTextColor = AuthText,
    unfocusedTextColor = AuthText,
    cursorColor = AuthAccent
)
