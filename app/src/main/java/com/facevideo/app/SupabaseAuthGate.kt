package com.facevideo.app

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.WindowManager
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.facevideo.app.data.SupabaseClientProvider
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.launch
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

private data class AuthPalette(
    val primaryText: Color,
    val secondaryText: Color,
    val labelText: Color,
    val inputText: Color,
    val placeholderText: Color,
    val accent: Color,
    val icon: Color,
    val fieldGradient: List<Color>,
    val fieldBorder: Color,
    val buttonGradient: List<Color>,
    val buttonBorder: Color,
    val buttonShadow: Color,
    val switchBackground: Color,
    val switchBorder: Color,
    val divider: Color,
    val successBackground: Color,
    val successBorder: Color,
    val successText: Color,
    val errorBackground: Color,
    val errorBorder: Color,
    val errorText: Color,
    val securityText: Color
)

@Composable
private fun currentAuthPalette(): AuthPalette = when {
    appOrangeChocolateTheme.value -> AuthPalette(
        primaryText = Color(0xFFFFF1E6),
        secondaryText = Color(0xFFD8B89F),
        labelText = Color(0xFFFFD4AF),
        inputText = Color(0xFFFFF2E7),
        placeholderText = Color(0xFFC7A58C),
        accent = Color(0xFFFFA347),
        icon = Color(0xFFFFBD83),
        fieldGradient = listOf(Color(0xE6381E10), Color(0xD92A150B), Color(0xE6381E10)),
        fieldBorder = Color(0xFF81502F),
        buttonGradient = listOf(Color(0xFFFFB45F), Color(0xFFFF913B), Color(0xFFC85B20)),
        buttonBorder = Color(0x99FFE0BE),
        buttonShadow = Color(0x995C260D),
        switchBackground = Color(0x9B29150B),
        switchBorder = Color(0xBB9E6843),
        divider = Color(0xB8E6B994),
        successBackground = Color(0xD52B4A35),
        successBorder = Color(0xFF6CB58E),
        successText = Color(0xFFD8F5E4),
        errorBackground = Color(0xD553251E),
        errorBorder = Color(0xFFB86B58),
        errorText = Color(0xFFFFD8CF),
        securityText = Color(0xFFD8B89F)
    )
    appDarkTheme.value -> AuthPalette(
        primaryText = Color(0xFFF1F4FB),
        secondaryText = Color(0xFFB5C1D3),
        labelText = Color(0xFFD3DDF0),
        inputText = Color(0xFFF1F4FB),
        placeholderText = Color(0xFF9AA8BC),
        accent = Color(0xFFA7B6FF),
        icon = Color(0xFFA7B6FF),
        fieldGradient = listOf(Color(0xE9212D40), Color(0xD9182231), Color(0xE9212D40)),
        fieldBorder = Color(0xFF485873),
        buttonGradient = listOf(Color(0xFF7187ED), Color(0xFF5269D6), Color(0xFF3E55B8)),
        buttonBorder = Color(0x997F94D8),
        buttonShadow = Color(0x883B4C91),
        switchBackground = Color(0xA51A2535),
        switchBorder = Color(0xBB53627E),
        divider = Color(0xB4AAB8D5),
        successBackground = Color(0xD51D3B33),
        successBorder = Color(0xFF427F6A),
        successText = Color(0xFFD1F0E2),
        errorBackground = Color(0xD5472731),
        errorBorder = Color(0xFF925465),
        errorText = Color(0xFFFFD9E0),
        securityText = Color(0xFFAEBBD0)
    )
    else -> AuthPalette(
        primaryText = Color(0xFF20294F),
        secondaryText = Color(0xFF56658E),
        labelText = Color(0xFF263355),
        inputText = Color(0xFF20294F),
        placeholderText = Color(0xFF66749A),
        accent = Color(0xFF485DD4),
        icon = Color(0xFF2A365F),
        fieldGradient = listOf(
            Color.White.copy(alpha = 0.48f),
            Color(0xFFEAF0FF).copy(alpha = 0.30f),
            Color.White.copy(alpha = 0.40f)
        ),
        fieldBorder = Color.White.copy(alpha = 0.96f),
        buttonGradient = listOf(Color(0xFF20A8F8), Color(0xFF4E5BE9), Color(0xFF913CF0)),
        buttonBorder = Color.White.copy(alpha = 0.72f),
        buttonShadow = Color(0x66485DD4),
        switchBackground = Color.White.copy(alpha = 0.18f),
        switchBorder = Color.White.copy(alpha = 0.94f),
        divider = Color.White.copy(alpha = 0.88f),
        successBackground = Color(0xCFF0FFF7),
        successBorder = Color(0xFF9AD7BA),
        successText = Color(0xFF247B59),
        errorBackground = Color(0xDDFDF0F4),
        errorBorder = Color(0xFFE5AAB9),
        errorText = Color(0xFFAD4C5B),
        securityText = Color(0xFF52618B)
    )
)

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
            val palette = currentAuthPalette()
            AuthBackdrop {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = palette.accent)
                }
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

private fun Context.findActivity(): Activity? {
    var current: Context? = this
    while (current != null) {
        if (current is Activity) return current
        current = (current as? ContextWrapper)?.baseContext
    }
    return null
}

@Composable
private fun AuthBackdrop(
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val window = remember(context) { context.findActivity()?.window }
    val isOrange = appOrangeChocolateTheme.value
    val isBlack = appDarkTheme.value

    DisposableEffect(window) {
        val oldSoftInputMode = window?.attributes?.softInputMode
        window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING)
        onDispose {
            if (oldSoftInputMode != null) {
                window?.setSoftInputMode(oldSoftInputMode)
            }
        }
    }

    val artwork = remember(context) {
        runCatching {
            val encoded = context.assets.open("auth_wallpaper.b64")
                .bufferedReader()
                .use { it.readText().trim() }
            val bytes = Base64.decode(encoded, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
        }.getOrNull()
    }

    Box(Modifier.fillMaxSize()) {
        when {
            isOrange -> {
                // MainActivity already draws the original Orange & Chocolate wallpaper.
                // Use only a warm translucent scrim so the shared wallpaper stays visible.
                Box(
                    Modifier.fillMaxSize().background(
                        Brush.verticalGradient(
                            listOf(
                                Color(0x66140702),
                                Color(0x22140702),
                                Color(0x55140702),
                                Color(0xAA140702)
                            )
                        )
                    )
                )
            }
            isBlack -> {
                Box(
                    Modifier.fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color(0xFF111827), Color(0xFF0D1420), Color(0xFF090D14))
                            )
                        )
                        .drawBehind {
                            val radius = size.width * 0.92f
                            drawCircle(
                                brush = Brush.radialGradient(
                                    listOf(Color(0x334F65B7), Color.Transparent),
                                    center = Offset(size.width * 0.04f, size.height * 0.12f),
                                    radius = radius
                                ),
                                radius = radius,
                                center = Offset(size.width * 0.04f, size.height * 0.12f)
                            )
                            drawCircle(
                                brush = Brush.radialGradient(
                                    listOf(Color(0x22477C9C), Color.Transparent),
                                    center = Offset(size.width * 0.96f, size.height * 0.82f),
                                    radius = radius * 0.82f
                                ),
                                radius = radius * 0.82f,
                                center = Offset(size.width * 0.96f, size.height * 0.82f)
                            )
                        }
                )
            }
            artwork != null -> {
                Image(
                    bitmap = artwork,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
            else -> {
                Box(
                    Modifier.fillMaxSize().background(
                        Brush.linearGradient(
                            listOf(Color(0xFFB3C8FF), Color(0xFF8975E8), Color(0xFFE2C6F8))
                        )
                    )
                )
            }
        }
        content()
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
    var passwordVisible by remember { mutableStateOf(false) }
    var isRegistration by remember { mutableStateOf(false) }
    var isBusy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var messageIsSuccess by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun submit() {
        val cleanEmail = email.trim()
        val cleanUsername = username.trim()
        val validUsername = cleanUsername.matches(Regex("^[a-z0-9_]{3,24}$"))
        if (!cleanEmail.contains("@") || password.length < 6 ||
            (isRegistration && !validUsername)
        ) {
            messageIsSuccess = false
            message = if (isRegistration) {
                "Проверь email, пароль (минимум 6 символов) и псевдоним (3–24 символа)."
            } else {
                "Проверь email и пароль (не менее 6 символов)."
            }
            return
        }

        scope.launch {
            isBusy = true
            message = null
            try {
                if (isRegistration) {
                    onSignUp(cleanEmail, password, cleanUsername)
                    messageIsSuccess = true
                    message = "Аккаунт создан. Проверь почту, если включено подтверждение email."
                } else {
                    onSignIn(cleanEmail, password)
                }
            } catch (error: Exception) {
                messageIsSuccess = false
                message = error.message ?: "Не удалось выполнить операцию."
            } finally {
                isBusy = false
            }
        }
    }

    val palette = currentAuthPalette()

    AuthBackdrop {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val compact = maxHeight < 700.dp
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth()
                    .widthIn(max = 440.dp)
                    .padding(horizontal = 28.dp)
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = if (compact) 16.dp else 30.dp),
                verticalArrangement = Arrangement.spacedBy(if (compact) 7.dp else 9.dp)
            ) {
                Text(
                    "ARTURCHAT",
                    color = palette.accent,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 2.4.sp
                )
                Text(
                    if (isRegistration) "Создание аккаунта" else "С возвращением",
                    color = palette.primaryText,
                    fontSize = 27.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.8).sp
                )
                Text(
                    if (isRegistration) {
                        "Создай профиль и оставайся на связи"
                    } else {
                        "Войди, чтобы продолжить общение"
                    },
                    color = palette.secondaryText,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )

                Spacer(Modifier.height(5.dp))
                AuthFieldLabel("ЭЛЕКТРОННАЯ ПОЧТА", "mail", palette)
                AuthInputField(
                    value = email,
                    onValueChange = { email = it; message = null },
                    placeholder = "name@example.com",
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next,
                    palette = palette
                )

                if (isRegistration) {
                    AuthFieldLabel("ПСЕВДОНИМ", "user", palette)
                    AuthInputField(
                        value = username,
                        onValueChange = {
                            username = it.lowercase()
                                .filter { ch -> ch.isLetterOrDigit() || ch == '_' }
                                .take(24)
                            message = null
                        },
                        placeholder = "например, artur_01",
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Next,
                        palette = palette
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AuthFieldLabel("ПАРОЛЬ", "lock", palette)
                    Spacer(Modifier.weight(1f))
                    Text(
                        if (passwordVisible) "Скрыть" else "Показать",
                        modifier = Modifier.clickable { passwordVisible = !passwordVisible }
                            .padding(horizontal = 5.dp, vertical = 4.dp),
                        color = palette.accent,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                AuthInputField(
                    value = password,
                    onValueChange = { password = it; message = null },
                    placeholder = "Не менее 6 символов",
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done,
                    visualTransformation = if (passwordVisible) {
                        VisualTransformation.None
                    } else {
                        PasswordVisualTransformation()
                    },
                    palette = palette
                )

                if (message != null) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        color = if (messageIsSuccess) palette.successBackground else palette.errorBackground,
                        border = BorderStroke(
                            1.dp,
                            if (messageIsSuccess) palette.successBorder else palette.errorBorder
                        )
                    ) {
                        Text(
                            message.orEmpty(),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                            color = if (messageIsSuccess) palette.successText else palette.errorText,
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        )
                    }
                }

                Spacer(Modifier.height(3.dp))
                Box(
                    Modifier.fillMaxWidth().height(54.dp)
                        .shadow(15.dp, RoundedCornerShape(18.dp), ambientColor = palette.buttonShadow, spotColor = palette.buttonShadow)
                        .clip(RoundedCornerShape(18.dp))
                        .background(Brush.horizontalGradient(palette.buttonGradient))
                        .border(1.dp, palette.buttonBorder, RoundedCornerShape(18.dp))
                        .clickable(enabled = !isBusy) { submit() },
                    contentAlignment = Alignment.Center
                ) {
                    if (isBusy) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(21.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            if (isRegistration) "Создать аккаунт" else "Войти в ArturChat",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier.weight(1f).height(1.dp)
                            .background(palette.divider.copy(alpha = 0.78f))
                    )
                    Text(
                        "ИЛИ",
                        modifier = Modifier.padding(horizontal = 12.dp),
                        color = palette.secondaryText,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.4.sp
                    )
                    Box(
                        Modifier.weight(1f).height(1.dp)
                            .background(palette.divider.copy(alpha = 0.78f))
                    )
                }

                Box(
                    Modifier.fillMaxWidth().height(48.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(palette.switchBackground)
                        .border(1.2.dp, palette.switchBorder, RoundedCornerShape(16.dp))
                        .clickable {
                            isRegistration = !isRegistration
                            passwordVisible = false
                            message = null
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        if (isRegistration) "Уже есть аккаунт? Войти" else "Создать новый аккаунт",
                        color = palette.primaryText,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 5.dp, bottom = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AuthLineIcon("shield", Modifier.size(19.dp), palette.icon)
                    Spacer(Modifier.width(9.dp))
                    Text(
                        if (isRegistration) {
                            "Регистрация по email · без номера телефона и платных SMS"
                        } else {
                            "Регистрация по email. Номер телефона и платные SMS не используются."
                        },
                        modifier = Modifier.weight(1f),
                        color = palette.securityText,
                        fontSize = 10.sp,
                        lineHeight = 14.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun AuthInputField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    keyboardType: KeyboardType,
    imeAction: ImeAction,
    palette: AuthPalette,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailingContent: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(17.dp))
            .background(Brush.linearGradient(palette.fieldGradient))
            .border(1.2.dp, palette.fieldBorder, RoundedCornerShape(17.dp))
            .padding(start = 16.dp, end = 12.dp, top = 2.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.weight(1f).padding(vertical = 14.dp)) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                textStyle = TextStyle(
                    color = palette.inputText,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium
                ),
                keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
                visualTransformation = visualTransformation,
                cursorBrush = Brush.verticalGradient(listOf(palette.accent, palette.accent.copy(alpha = 0.52f))),
                decorationBox = { innerTextField ->
                    if (value.isEmpty()) {
                        Text(
                            placeholder,
                            color = palette.placeholderText.copy(alpha = 0.92f),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Normal
                        )
                    }
                    innerTextField()
                }
            )
        }
        if (trailingContent != null) {
            Spacer(Modifier.width(7.dp))
            trailingContent()
        }
    }
}

@Composable
private fun AuthFieldLabel(value: String, icon: String, palette: AuthPalette) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        AuthLineIcon(icon, Modifier.size(19.dp), palette.icon)
        Text(
            value,
            color = palette.labelText,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.15.sp
        )
    }
}

@Composable
private fun AuthLineIcon(kind: String, modifier: Modifier = Modifier, iconColor: Color = Color(0xFF2A365F)) {
    Canvas(modifier) {
        val stroke = Stroke(width = 1.7.dp.toPx(), cap = StrokeCap.Round)
        val w = size.width
        val h = size.height
        when (kind) {
            "mail" -> {
                val left = w * 0.09f
                val top = h * 0.20f
                val right = w * 0.91f
                val bottom = h * 0.82f
                drawRoundRect(
                    color = iconColor,
                    topLeft = Offset(left, top),
                    size = Size(right - left, bottom - top),
                    cornerRadius = CornerRadius(w * 0.12f),
                    style = stroke
                )
                val flap = Path().apply {
                    moveTo(left + w * 0.03f, top + h * 0.06f)
                    lineTo(w * 0.5f, h * 0.56f)
                    lineTo(right - w * 0.03f, top + h * 0.06f)
                }
                drawPath(flap, iconColor, style = stroke)
            }
            "lock" -> {
                val shackle = Path().apply {
                    moveTo(w * 0.29f, h * 0.48f)
                    lineTo(w * 0.29f, h * 0.34f)
                    cubicTo(w * 0.29f, h * 0.04f, w * 0.71f, h * 0.04f, w * 0.71f, h * 0.34f)
                    lineTo(w * 0.71f, h * 0.48f)
                }
                drawPath(shackle, iconColor, style = stroke)
                drawRoundRect(
                    color = iconColor,
                    topLeft = Offset(w * 0.15f, h * 0.43f),
                    size = Size(w * 0.70f, h * 0.49f),
                    cornerRadius = CornerRadius(w * 0.10f),
                    style = stroke
                )
                drawCircle(iconColor, radius = w * 0.045f, center = Offset(w * 0.5f, h * 0.62f))
                drawLine(
                    iconColor,
                    Offset(w * 0.5f, h * 0.65f),
                    Offset(w * 0.5f, h * 0.76f),
                    strokeWidth = 1.4.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
            "user" -> {
                drawCircle(
                    color = iconColor,
                    radius = w * 0.18f,
                    center = Offset(w * 0.5f, h * 0.30f),
                    style = stroke
                )
                val body = Path().apply {
                    moveTo(w * 0.14f, h * 0.91f)
                    cubicTo(w * 0.16f, h * 0.63f, w * 0.34f, h * 0.54f, w * 0.5f, h * 0.54f)
                    cubicTo(w * 0.66f, h * 0.54f, w * 0.84f, h * 0.63f, w * 0.86f, h * 0.91f)
                }
                drawPath(body, iconColor, style = stroke)
            }
            "shield" -> {
                val shield = Path().apply {
                    moveTo(w * 0.5f, h * 0.06f)
                    lineTo(w * 0.88f, h * 0.22f)
                    lineTo(w * 0.83f, h * 0.62f)
                    cubicTo(w * 0.79f, h * 0.80f, w * 0.62f, h * 0.91f, w * 0.5f, h * 0.97f)
                    cubicTo(w * 0.38f, h * 0.91f, w * 0.21f, h * 0.80f, w * 0.17f, h * 0.62f)
                    lineTo(w * 0.12f, h * 0.22f)
                    close()
                }
                drawPath(shield, iconColor, style = stroke)
            }
        }
    }
}

