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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.compose.ui.platform.LocalContext
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser

private val AuthBackground = Color(0xFF0A1020)
private val AuthMint = Color(0xFF65E6D2)
private val AuthMuted = Color(0xFF91A0BA)

@Composable
fun FirebaseAuthGate(
    content: @Composable (userEmail: String?, onSignOut: () -> Unit) -> Unit
) {
    val context = LocalContext.current
    val firebaseApp = remember(context) {
        FirebaseApp.getApps(context).firstOrNull()
    }

    // No Firebase configuration yet: keep the app usable as a local UI prototype.
    if (firebaseApp == null) {
        content(null) {}
        return
    }

    val auth = remember(firebaseApp) { FirebaseAuth.getInstance(firebaseApp) }
    var currentUser by remember(auth) { mutableStateOf<FirebaseUser?>(auth.currentUser) }

    DisposableEffect(auth) {
        val listener = FirebaseAuth.AuthStateListener { state ->
            currentUser = state.currentUser
        }
        auth.addAuthStateListener(listener)
        onDispose { auth.removeAuthStateListener(listener) }
    }

    if (currentUser == null) {
        FirebaseSignInScreen(auth)
    } else {
        content(currentUser?.email, { auth.signOut() })
    }
}

@Composable
private fun FirebaseSignInScreen(auth: FirebaseAuth) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isRegistration by remember { mutableStateOf(false) }
    var isBusy by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

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
                color = Color(0xFFF4F6FC),
                fontSize = 27.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Вход в интернет-мессенджер",
                color = AuthMuted,
                fontSize = 13.sp
            )
            Spacer(Modifier.height(28.dp))

            OutlinedTextField(
                value = email,
                onValueChange = { email = it; errorMessage = null },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("Электронная почта") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                colors = authFieldColors()
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = password,
                onValueChange = { password = it; errorMessage = null },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("Пароль") },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                colors = authFieldColors()
            )

            if (errorMessage != null) {
                Spacer(Modifier.height(12.dp))
                Text(errorMessage.orEmpty(), color = Color(0xFFFFA5B5), fontSize = 12.sp)
            }

            Spacer(Modifier.height(20.dp))
            Button(
                onClick = {
                    val cleanEmail = email.trim()
                    if (!cleanEmail.contains("@") || password.length < 6) {
                        errorMessage = "Проверь email и пароль (не менее 6 символов)."
                    } else {
                        isBusy = true
                        errorMessage = null
                        val operation = if (isRegistration) {
                            auth.createUserWithEmailAndPassword(cleanEmail, password)
                        } else {
                            auth.signInWithEmailAndPassword(cleanEmail, password)
                        }
                        operation.addOnCompleteListener { task ->
                            isBusy = false
                            if (!task.isSuccessful) {
                                errorMessage = task.exception?.localizedMessage
                                    ?: "Не удалось выполнить вход. Проверь данные и настройки Firebase."
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
                errorMessage = null
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
    focusedTextColor = Color(0xFFF4F6FC),
    unfocusedTextColor = Color(0xFFF4F6FC),
    cursorColor = AuthMint
)
