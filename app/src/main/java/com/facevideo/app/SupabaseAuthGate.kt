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
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.launch

private val AuthBackground = Color(0xFF0A1020)
private val AuthMint = Color(0xFF65E6D2)
private val AuthMuted = Color(0xFF91A0BA)
private val AuthText = Color(0xFFF4F6FC)

@Composable
fun SupabaseAuthGate(
    content: @Composable (userEmail: String?, onSignOut: () -> Unit) -> Unit
) {
    val supabase = SupabaseClientProvider.client

    // Without project credentials, the app remains a clearly marked UI demo.
    if (supabase == null) {
        content(null) {}
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
                CircularProgressIndicator(color = AuthMint)
            }
        }
        is SessionStatus.Authenticated -> {
            val email = status.session.user?.email ?: "Пользователь"
            content(email) {
                scope.launch { supabase.auth.signOut() }
            }
        }
        else -> {
            SupabaseSignInScreen(
                onSignIn = { email, password -> supabase.auth.signInWith(Email) {
                    this.email = email
                    this.password = password
                } },
                onSignUp = { email, password -> supabase.auth.signUpWith(Email) {
                    this.email = email
                    this.password = password
                } }
            )
        }
    }
}

@Composable
private fun SupabaseSignInScreen(
    onSignIn: suspend (String, String) -> Unit,
    onSignUp: suspend (String, String) -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isRegistration by remember { mutableStateOf(false) }
    var isBusy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Box(
        Modifier.fillMaxSize().background(
            Brush.verticalGradient(listOf(AuthBackground, Color(0xFF14223A), AuthBackground))
        ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 26.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                "FACE VIDEO",
                color = AuthMint,
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
                Text(message.orEmpty(), color = Color(0xFFFFA5B5), fontSize = 12.sp)
            }

            Spacer(Modifier.height(20.dp))
            Button(
                onClick = {
                    val cleanEmail = email.trim()
                    if (!cleanEmail.contains("@") || password.length < 6) {
                        message = "Проверь email и пароль (не менее 6 символов)."
                    } else {
                        isBusy = true
                        message = null
                        scope.launch {
                            try {
                                if (isRegistration) {
                                    onSignUp(cleanEmail, password)
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
                    containerColor = AuthMint,
                    contentColor = AuthBackground
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
                    color = AuthMint
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
    focusedBorderColor = AuthMint,
    unfocusedBorderColor = Color(0xFF35445E),
    focusedLabelColor = AuthMint,
    unfocusedLabelColor = AuthMuted,
    focusedTextColor = AuthText,
    unfocusedTextColor = AuthText,
    cursorColor = AuthMint
)
