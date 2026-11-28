package com.facevideo.app

import android.Manifest
import android.content.Intent
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.ContextWrapper
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.view.LayoutInflater
import android.graphics.BitmapFactory
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.WindowCompat
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.FallbackStrategy
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.Image
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.window.DialogWindowProvider
import com.facevideo.app.calls.WebRtcAudioCallClient
import com.facevideo.app.calls.OutgoingCallRingbackTone
import com.facevideo.app.data.FaceVideoCall
import com.facevideo.app.data.SupabaseCallRepository
import com.facevideo.app.data.DemoData
import com.facevideo.app.data.SupabaseClientProvider
import com.facevideo.app.data.SupabaseChatRepository
import com.facevideo.app.data.ArturChatImageCache
import com.facevideo.app.data.ArturChatLocalCache
import com.facevideo.app.data.ArturChatNavigationState
import com.facevideo.app.data.FaceVideoChatSummary
import com.facevideo.app.data.GroupTopicDestination
import com.facevideo.app.data.FaceVideoScheduledMessage
import com.facevideo.app.data.FaceVideoProfile
import com.facevideo.app.data.FaceVideoProfileStats
import com.facevideo.app.data.FaceVideoStoryCard
import com.facevideo.app.data.FaceVideoPresence
import com.facevideo.app.data.FaceVideoChatUnreadSummary
import com.facevideo.app.data.formatChatMessageTime
import com.facevideo.app.data.formatUnreadBadge
import com.facevideo.app.data.mergeChatMessageSnapshots
import com.facevideo.app.data.buildChatInboxPreview
import com.facevideo.app.data.resolveMessageDeliveryStatus
import com.facevideo.app.data.sortChatMessagesChronologically
import com.facevideo.app.data.shouldShowChatMessageTime
import com.facevideo.app.update.ArturChatUpdateHost
import kotlinx.coroutines.delay
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import com.facevideo.app.notifications.AppNotificationManager
import com.facevideo.app.notifications.CallForegroundService
import com.facevideo.app.notifications.CallNotificationIntents
import com.facevideo.app.notifications.ReleaseNotificationIntents
import com.facevideo.app.notifications.PushTokenRegistrar
import com.facevideo.app.model.ChatMessage
import com.facevideo.app.model.ChatSummary
import com.facevideo.app.model.MessageDeliveryStatus
import com.facevideo.app.model.formatVoiceDuration
import com.facevideo.app.model.buildVoiceWaveform
import com.facevideo.app.model.VOICE_WAVEFORM_BAR_COUNT
import com.facevideo.app.model.STORY_REACTION_OPTIONS
import com.facevideo.app.model.MESSAGE_REACTION_OPTIONS
import com.facevideo.app.model.MessageReactionCount
import com.facevideo.app.model.MessageReactionEntry
import com.facevideo.app.model.summarizeMessageReactions
import com.facevideo.app.model.StoryReactionSummary
import com.facevideo.app.model.nextStoryReaction
import com.facevideo.app.audio.decodeVoiceWaveformFromFile
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt
import java.time.Instant
import java.util.UUID
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.CancellationException
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.presenceDataFlow
import io.github.jan.supabase.realtime.track
import io.github.jan.supabase.realtime.realtime

private const val ARTURCHAT_THEME_PREFERENCES = "arturchat_appearance"
private const val ARTURCHAT_THEME_DARK_KEY = "black_theme_enabled"
private const val ARTURCHAT_THEME_ORANGE_KEY = "orange_chocolate_theme_enabled"
internal val appDarkTheme = mutableStateOf(false)
internal val appOrangeChocolateTheme = mutableStateOf(false)
internal val releaseUpdateCheckSignal = mutableStateOf(0)

private val isDarkAppearance: Boolean
    get() = appDarkTheme.value || appOrangeChocolateTheme.value

// Three coordinated ArturChat palettes: pastel default, graphite black, and Orange & Chocolate.
private val AppBackground: Color get() = when {
    appOrangeChocolateTheme.value -> Color(0xFF160802)
    appDarkTheme.value -> Color(0xFF090D14)
    else -> Color(0xFFF0F3FF)
}
private val Panel: Color get() = when {
    appOrangeChocolateTheme.value -> Color(0xFF281307)
    appDarkTheme.value -> Color(0xFF151C28)
    else -> Color(0xFFFAFBFF)
}
private val PanelLight: Color get() = when {
    appOrangeChocolateTheme.value -> Color(0xFF3A1C0C)
    appDarkTheme.value -> Color(0xFF202B3B)
    else -> Color(0xFFEDF1FF)
}
private val IncomingMessage: Color get() = when {
    appOrangeChocolateTheme.value -> Color(0xFF422515)
    appDarkTheme.value -> Color(0xFF273246)
    else -> Color(0xFFE5EAF6)
}
private val AccentSlate: Color get() = when {
    appOrangeChocolateTheme.value -> Color(0xFFFF9B3D)
    appDarkTheme.value -> Color(0xFF91A4FF)
    else -> Color(0xFF5266D8)
}
private val MainText: Color get() = when {
    appOrangeChocolateTheme.value -> Color(0xFFFFF1E6)
    appDarkTheme.value -> Color(0xFFF1F4FB)
    else -> Color(0xFF253047)
}
private val Muted: Color get() = when {
    appOrangeChocolateTheme.value -> Color(0xFFC7A58C)
    appDarkTheme.value -> Color(0xFF9AA8BC)
    else -> Color(0xFF77839A)
}
private val PaletteBlush: Color get() = when {
    appOrangeChocolateTheme.value -> Color(0xFF472313)
    appDarkTheme.value -> Color(0xFF3A304C)
    else -> Color(0xFFE6DFFF)
}
private val PaletteSlate: Color get() = when {
    appOrangeChocolateTheme.value -> Color(0xFF57301A)
    appDarkTheme.value -> Color(0xFF293B59)
    else -> Color(0xFFC8D7F2)
}
private val PaletteBlueGray: Color get() = when {
    appOrangeChocolateTheme.value -> Color(0xFF673718)
    appDarkTheme.value -> Color(0xFF263C50)
    else -> Color(0xFFD2E3F8)
}
private val PaletteMist: Color get() = when {
    appOrangeChocolateTheme.value -> Color(0xFF382015)
    appDarkTheme.value -> Color(0xFF283347)
    else -> Color(0xFFE0E7F6)
}
private val AvatarColors: List<Color> get() = listOf(PaletteBlush, PaletteSlate, PaletteBlueGray, PaletteMist)
private val ThemeSurface: Color get() = when {
    appOrangeChocolateTheme.value -> Color(0xFF2B160B)
    appDarkTheme.value -> Color(0xFF192231)
    else -> Color.White
}
private val ThemeSurfaceRaised: Color get() = when {
    appOrangeChocolateTheme.value -> Color(0xFF3A1D0D)
    appDarkTheme.value -> Color(0xFF202B3A)
    else -> Color(0xFFFDFEFF)
}
private val ThemeBorder: Color get() = when {
    appOrangeChocolateTheme.value -> Color(0xFF654024)
    appDarkTheme.value -> Color(0xFF344155)
    else -> Color(0xFFE1E6EC)
}

// Inbox tokens share the selected appearance and keep the same warm accent hierarchy.
private val InboxBackground: Color get() = when {
    appOrangeChocolateTheme.value -> Color(0xFF160802)
    appDarkTheme.value -> Color(0xFF090D14)
    else -> Color(0xFFF2F4FF)
}
private val InboxSurface: Color get() = when {
    appOrangeChocolateTheme.value -> Color(0xFF2B160B)
    appDarkTheme.value -> Color(0xFF151E2B)
    else -> Color(0xFFFFFFFF)
}
private val InboxSurfaceSoft: Color get() = when {
    appOrangeChocolateTheme.value -> Color(0xFF41200F)
    appDarkTheme.value -> Color(0xFF222D3F)
    else -> Color(0xFFE8EBFF)
}
private val InboxMessageIncoming: Color get() = when {
    appOrangeChocolateTheme.value -> Color(0xFF3E2416)
    appDarkTheme.value -> Color(0xFF293548)
    else -> Color(0xFFDDE3EA)
}
private val InboxAccent: Color get() = when {
    appOrangeChocolateTheme.value -> Color(0xFFFF9837)
    appDarkTheme.value -> Color(0xFF8FA4FF)
    else -> Color(0xFF5267DB)
}
private val InboxAccentStrong: Color get() = when {
    appOrangeChocolateTheme.value -> Color(0xFFFFB15C)
    appDarkTheme.value -> Color(0xFFADB9FF)
    else -> Color(0xFF3F50C9)
}
private val InboxPrimary: Color get() = when {
    appOrangeChocolateTheme.value -> Color(0xFFFFF1E6)
    appDarkTheme.value -> Color(0xFFF1F4FB)
    else -> Color(0xFF202936)
}
private val InboxSecondary: Color get() = when {
    appOrangeChocolateTheme.value -> Color(0xFFD8B89F)
    appDarkTheme.value -> Color(0xFFC3CCDA)
    else -> Color(0xFF485464)
}
private val InboxMuted: Color get() = when {
    appOrangeChocolateTheme.value -> Color(0xFFB7957C)
    appDarkTheme.value -> Color(0xFF929FB2)
    else -> Color(0xFF788493)
}
private val InboxBorder: Color get() = when {
    appOrangeChocolateTheme.value -> Color(0xFF603A22)
    appDarkTheme.value -> Color(0xFF303B4E)
    else -> Color(0xFFE1E6EC)
}
private val InboxOnline: Color get() = when {
    appOrangeChocolateTheme.value -> Color(0xFF75D59A)
    appDarkTheme.value -> Color(0xFF62C99D)
    else -> Color(0xFF2F9A72)
}
private val InboxDanger: Color get() = when {
    appOrangeChocolateTheme.value -> Color(0xFFFF8171)
    appDarkTheme.value -> Color(0xFFF08D98)
    else -> Color(0xFFBE5E67)
}

private enum class MainTab(val title: String) {
    CHATS("Чаты"),
    GROUPS("Группы"),
    CALLS("Звонки"),
    CONTACTS("Друзья"),
    PROFILE("Профиль")
}

private data class MessageReplyPreview(
    val sender: String,
    val text: String
)

internal data class PreparedChatImage(
    val bytes: ByteArray,
    val fileName: String
)

/**
 * Decodes modern formats through ImageDecoder (including HEIF/HEIC on supported
 * Android versions), scales before allocating large bitmaps, then normalizes
 * selected chat photos and group/channel avatars to JPEG.
 */
internal fun prepareChatImage(context: Context, uri: Uri, maxDimension: Int = 1920, maxBytes: Int = 10 * 1024 * 1024): PreparedChatImage {
    val resolver = context.contentResolver

    val decoded = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        val source = android.graphics.ImageDecoder.createSource(resolver, uri)
        android.graphics.ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
            val width = info.size.width
            val height = info.size.height
            require(width > 0 && height > 0) {
                "Не удалось определить размер выбранного изображения."
            }
            val longestSide = maxOf(width, height)
            if (longestSide > maxDimension) {
                val scale = maxDimension.toFloat() / longestSide
                decoder.setTargetSize(
                    (width * scale).toInt().coerceAtLeast(1),
                    (height * scale).toInt().coerceAtLeast(1)
                )
            }
            decoder.allocator = android.graphics.ImageDecoder.ALLOCATOR_SOFTWARE
            decoder.isMutableRequired = true
        }
    } else {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { input ->
            BitmapFactory.decodeStream(input, null, bounds)
        } ?: throw IllegalArgumentException("Не удалось открыть выбранное фото.")

        require(bounds.outWidth > 0 && bounds.outHeight > 0) {
            "Выбранный файл не является поддерживаемым изображением."
        }

        var sampleSize = 1
        while (maxOf(bounds.outWidth / sampleSize, bounds.outHeight / sampleSize) > maxDimension) {
            sampleSize *= 2
        }
        val options = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = android.graphics.Bitmap.Config.ARGB_8888
            inMutable = true
        }
        resolver.openInputStream(uri)?.use { input ->
            BitmapFactory.decodeStream(input, null, options)
        } ?: throw IllegalArgumentException("Не удалось прочитать выбранное фото.")
    } ?: throw IllegalArgumentException("Не удалось декодировать выбранное изображение.")

    try {
        val output = java.io.ByteArrayOutputStream()
        var encoded = false
        for (quality in listOf(85, 75, 65)) {
            output.reset()
            if (!decoded.compress(android.graphics.Bitmap.CompressFormat.JPEG, quality, output)) {
                throw IllegalStateException("Не удалось преобразовать фото в JPEG.")
            }
            if (output.size() <= maxBytes) {
                encoded = true
                break
            }
        }
        require(encoded && output.size() in 1..maxBytes) {
            "Фото слишком большое. Выбери изображение поменьше."
        }

        val originalName = runCatching {
            resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                ?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (index >= 0) cursor.getString(index) else null
                    } else null
                }
        }.getOrNull().orEmpty()
        val baseName = originalName.substringBeforeLast('.', originalName)
            .replace(Regex("[^A-Za-zА-Яа-я0-9 _-]"), "_")
            .trim()
            .take(220)
            .ifBlank { "photo" }

        return PreparedChatImage(
            bytes = output.toByteArray(),
            fileName = "$baseName.jpg"
        )
    } finally {
        decoded.recycle()
    }
}

data class CallNotificationAction(
    val callId: String,
    val action: String,
    val nonce: Long
)

private fun Modifier.arturChatGradientBackground(): Modifier =
    if (appOrangeChocolateTheme.value) {
        // The Orange & Chocolate theme uses the supplied full-screen wallpaper.
        // Keep section containers transparent so the image remains visible beneath UI.
        this
    } else this
    .background(
        Brush.verticalGradient(
            if (appDarkTheme.value) {
                listOf(Color(0xFF111827), Color(0xFF0D1420), Color(0xFF090D14))
            } else {
                listOf(Color(0xFFE2DCFF), Color(0xFFE2E9FF), Color(0xFFE8F0FF), Color(0xFFF2F5FF), Color(0xFFE8E9FF))
            }
        )
    )
    .drawBehind {
        val w = size.width
        val h = size.height
        val orange = appOrangeChocolateTheme.value
        val dark = isDarkAppearance
        val glowTopLeft = Offset(-w * 0.10f, h * 0.10f)
        val glowTopRight = Offset(w * 1.02f, h * 0.22f)
        val glowCenter = Offset(w * 0.46f, h * 0.56f)
        val glowBottomLeft = Offset(-w * 0.08f, h * 0.94f)

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    (when { orange -> Color(0xFFFF842A); dark -> Color(0xFF6660A8); else -> Color(0xFFB49BFF) })
                        .copy(alpha = when { orange -> 0.22f; dark -> 0.12f; else -> 0.58f }),
                    (when { orange -> Color(0xFFB84E18); dark -> Color(0xFF4D5E85); else -> Color(0xFFC9BFFF) })
                        .copy(alpha = when { orange -> 0.08f; dark -> 0.05f; else -> 0.22f }),
                    Color.Transparent
                ), center = glowTopLeft, radius = w * 0.90f
            ), radius = w * 0.90f, center = glowTopLeft
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    (when { orange -> Color(0xFFFFA044); dark -> Color(0xFF3E6388); else -> Color(0xFF9BCBFF) })
                        .copy(alpha = when { orange -> 0.20f; dark -> 0.10f; else -> 0.56f }),
                    (when { orange -> Color(0xFF9D4316); dark -> Color(0xFF334967); else -> Color(0xFFB8D9FF) })
                        .copy(alpha = when { orange -> 0.07f; dark -> 0.04f; else -> 0.18f }),
                    Color.Transparent
                ), center = glowTopRight, radius = w * 0.82f
            ), radius = w * 0.82f, center = glowTopRight
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    (when { orange -> Color(0xFF9E4719); dark -> Color(0xFF514274); else -> Color(0xFFD0B8FF) })
                        .copy(alpha = when { orange -> 0.12f; dark -> 0.08f; else -> 0.30f }),
                    Color.Transparent
                ), center = glowCenter, radius = w * 0.78f
            ), radius = w * 0.78f, center = glowCenter
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    (when { orange -> Color(0xFF7D310E); dark -> Color(0xFF35516E); else -> Color(0xFFB8D8FF) })
                        .copy(alpha = when { orange -> 0.10f; dark -> 0.08f; else -> 0.30f }),
                    Color.Transparent
                ), center = glowBottomLeft, radius = w * 0.70f
            ), radius = w * 0.70f, center = glowBottomLeft
        )
    }

class MainActivity : ComponentActivity() {
    private val pendingCallNotificationAction = MutableStateFlow<CallNotificationAction?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val appearancePreferences = getSharedPreferences(ARTURCHAT_THEME_PREFERENCES, 0)
        appOrangeChocolateTheme.value = appearancePreferences
            .getBoolean(ARTURCHAT_THEME_ORANGE_KEY, false)
        appDarkTheme.value = appearancePreferences
            .getBoolean(ARTURCHAT_THEME_DARK_KEY, false) && !appOrangeChocolateTheme.value

        // Android 15+ enforces edge-to-edge for targetSdk 35+. Enable it
        // consistently on older Android versions and let Compose apply safe insets.
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.TRANSPARENT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isStatusBarContrastEnforced = false
            window.isNavigationBarContrastEnforced = false
        }
        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = !isDarkAppearance
            isAppearanceLightNavigationBars = !isDarkAppearance
        }

        captureCallNotificationAction(intent)
        setContent {
            val darkTheme = isDarkAppearance
            SideEffect {
                WindowInsetsControllerCompat(window, window.decorView).apply {
                    isAppearanceLightStatusBars = !darkTheme
                    isAppearanceLightNavigationBars = !darkTheme
                }
            }
            MaterialTheme(
                colorScheme = if (darkTheme) {
                    androidx.compose.material3.darkColorScheme(
                        primary = AccentSlate,
                        background = AppBackground,
                        surface = Panel,
                        surfaceVariant = PanelLight,
                        onPrimary = Color.White,
                        onBackground = MainText,
                        onSurface = MainText,
                        onSurfaceVariant = Muted
                    )
                } else {
                    lightColorScheme(
                        primary = AccentSlate,
                        background = AppBackground,
                        surface = Panel,
                        onPrimary = Color.White,
                        onBackground = MainText,
                        onSurface = MainText
                    )
                }
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = AppBackground
                ) {
                    Box(
                        Modifier.fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    when {
                                        appOrangeChocolateTheme.value -> listOf(Color(0xFF2D1004), Color(0xFF210B03), Color(0xFF130602))
                                        darkTheme -> listOf(Color(0xFF111827), Color(0xFF0D1420), Color(0xFF090D14))
                                        else -> listOf(Color(0xFFE2DCFF), Color(0xFFE2E9FF), Color(0xFFE8F0FF), Color(0xFFF2F5FF))
                                    }
                                )
                            )
                    ) {
                        if (appOrangeChocolateTheme.value) {
                            Image(
                                painter = painterResource(id = R.drawable.orange_chocolate_wallpaper),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        // Keep the wallpaper edge-to-edge; only app content respects system insets.
                        Box(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
                            ArturChatUpdateHost {
                                SupabaseAuthGate { userEmail, userId, onSignOut ->
                                    FaceVideoApp(
                                        userEmail = userEmail,
                                        userId = userId,
                                        onSignOut = onSignOut,
                                        callNotificationActionFlow = pendingCallNotificationAction,
                                        onCallNotificationActionHandled = { nonce ->
                                            if (pendingCallNotificationAction.value?.nonce == nonce) {
                                                pendingCallNotificationAction.value = null
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        captureCallNotificationAction(intent)
    }

    private fun captureCallNotificationAction(intent: Intent?) {
        val releaseVersion = intent?.getStringExtra(ReleaseNotificationIntents.EXTRA_RELEASE_VERSION)
        if (!releaseVersion.isNullOrBlank() && Regex("^[0-9]+\\.[0-9]+\\.[0-9]+$").matches(releaseVersion)) {
            releaseUpdateCheckSignal.value += 1
        }
        val callId = intent?.getStringExtra(CallNotificationIntents.EXTRA_CALL_ID)
            ?: return
        val action = intent.getStringExtra(CallNotificationIntents.EXTRA_CALL_ACTION)
            ?: return
        if (action !in setOf(
                CallNotificationIntents.ACTION_ANSWER,
                CallNotificationIntents.ACTION_DECLINE
            )
        ) return
        pendingCallNotificationAction.value = CallNotificationAction(
            callId = callId,
            action = action,
            nonce = System.nanoTime()
        )
    }
}

private sealed interface PendingCallAction {
    data class Outgoing(
        val chat: FaceVideoChatSummary,
        val mediaType: String = "audio"
    ) : PendingCallAction
    data class Incoming(val call: FaceVideoCall) : PendingCallAction
}

@Composable
private fun FaceVideoApp(
    userEmail: String?,
    userId: String?,
    onSignOut: () -> Unit,
    callNotificationActionFlow: StateFlow<CallNotificationAction?>,
    onCallNotificationActionHandled: (Long) -> Unit
) {
    val callNotificationAction by callNotificationActionFlow.collectAsState()

    val context = LocalContext.current
    val localCache = remember(context) { ArturChatLocalCache(context) }
    // Restore the last destination from disk as well as from the Activity state.
    // This survives Android reclaiming the process while ArturChat is backgrounded.
    val initialNavigationState = remember(userId, localCache) {
        userId?.let(localCache::readNavigationState) ?: ArturChatNavigationState()
    }
    var selectedTab by remember(userId, initialNavigationState) {
        mutableStateOf(
            runCatching { MainTab.valueOf(initialNavigationState.selectedTab) }
                .getOrDefault(MainTab.CHATS)
        )
    }
    var openedDemoChatId by remember(userId, initialNavigationState) {
        mutableStateOf(initialNavigationState.demoChatId)
    }
    var openedChat by remember(userId, initialNavigationState) {
        mutableStateOf(
            initialNavigationState.demoChatId?.let { id -> DemoData.chats.firstOrNull { it.id == id } }
        )
    }
    var openedLiveChatId by remember(userId, initialNavigationState) {
        mutableStateOf(initialNavigationState.liveChatId)
    }
    var openedLiveChat by remember(userId, initialNavigationState, localCache) {
        mutableStateOf(
            initialNavigationState.liveChatId?.let { id ->
                userId?.let(localCache::readChats)?.firstOrNull { it.id == id }
            }
        )
    }
    var openedProfileUserId by remember(userId, initialNavigationState) {
        mutableStateOf(initialNavigationState.profileUserId)
    }
    var openedStoriesUserId by remember(userId, initialNavigationState) {
        mutableStateOf(initialNavigationState.storiesUserId)
    }

    fun setOpenedLiveChat(chat: FaceVideoChatSummary?) {
        openedLiveChat = chat
        openedLiveChatId = chat?.id
        if (chat != null) {
            openedChat = null
            openedDemoChatId = null
        }
    }

    fun setOpenedDemoChat(chat: ChatSummary?) {
        openedChat = chat
        openedDemoChatId = chat?.id
        if (chat != null) setOpenedLiveChat(null)
    }

    var lastSavedNavigationState by remember(userId, initialNavigationState) {
        mutableStateOf(initialNavigationState)
    }
    SideEffect {
        if (userId != null) {
            val currentNavigationState = ArturChatNavigationState(
                selectedTab = selectedTab.name,
                liveChatId = openedLiveChatId,
                profileUserId = openedProfileUserId,
                storiesUserId = openedStoriesUserId,
                demoChatId = openedDemoChatId
            )
            if (currentNavigationState != lastSavedNavigationState) {
                localCache.saveNavigationState(userId, currentNavigationState)
                lastSavedNavigationState = currentNavigationState
            }
        }
    }
    var unreadSummaries by remember(userId, localCache) {
        mutableStateOf(userId?.let(localCache::readUnreadSummaries) ?: emptyMap())
    }
    var unreadRefreshKey by remember(userId) { mutableStateOf(0) }
    var mutedChatIds by remember(userId, localCache) {
        mutableStateOf(userId?.let(localCache::readMutedChatIds) ?: emptySet())
    }
    var showCallInfo by remember { mutableStateOf(false) }
    val callInfoMessage = "Открой личную переписку и нажми значок трубки для аудиозвонка или камеры для видеозвонка."

    val supabase = SupabaseClientProvider.client
    val liveRepository = remember(supabase) { supabase?.let { SupabaseChatRepository(it) } }
    val callRepository = remember(supabase) { supabase?.let { SupabaseCallRepository(it) } }
    val scope = rememberCoroutineScope()
    var realtimeOnlineUserIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var databaseOnlineUserIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    val onlineUserIds = realtimeOnlineUserIds + databaseOnlineUserIds
    var presenceAvailable by remember { mutableStateOf(false) }

    // Share online state only with authenticated ArturChat profiles through a private Realtime channel.
    LaunchedEffect(supabase, userId) {
        val currentClient = supabase ?: return@LaunchedEffect
        val currentUserId = userId ?: return@LaunchedEffect
        val presenceChannel = currentClient.channel("face_video:online") {
            isPrivate = true
        }
        val presenceFlow = presenceChannel.presenceDataFlow<FaceVideoPresence>()
        val presenceCollector = launch {
            presenceFlow.collect { presences ->
                realtimeOnlineUserIds = presences.mapTo(mutableSetOf()) { it.userId }
            }
        }

        try {
            presenceChannel.subscribe(blockUntilSubscribed = true)
            presenceChannel.track(FaceVideoPresence(userId = currentUserId))
            presenceAvailable = true
            android.util.Log.i("ArturChatPresence", "Online presence connected")
            awaitCancellation()
        } catch (error: kotlinx.coroutines.CancellationException) {
            throw error
        } catch (error: Exception) {
            android.util.Log.w("ArturChatPresence", "Could not connect online presence", error)
            realtimeOnlineUserIds = emptySet()
            presenceAvailable = false
        } finally {
            presenceCollector.cancel()
            runCatching { currentClient.realtime.removeChannel(presenceChannel) }
                .onFailure {
                    android.util.Log.w("ArturChatPresence", "Could not remove online presence channel", it)
                }
            realtimeOnlineUserIds = emptySet()
            presenceAvailable = false
        }
    }

    // Database heartbeat is the reliable fallback when Realtime Presence cannot
    // reserve a connection. A user stays online while at least one device pings.
    LaunchedEffect(liveRepository, userId) {
        val repository = liveRepository ?: return@LaunchedEffect
        val currentUserId = userId ?: return@LaunchedEffect
        while (currentCoroutineContext().isActive) {
            try {
                repository.heartbeatOnlinePresence(currentUserId)
                databaseOnlineUserIds = repository.listOnlineUserIds()
            } catch (error: kotlinx.coroutines.CancellationException) {
                throw error
            } catch (error: Exception) {
                android.util.Log.w("ArturChatPresence", "Database online heartbeat failed", error)
            }
            delay(15_000L)
        }
    }

    val appNotifications = remember(context) { AppNotificationManager(context) }

    LaunchedEffect(appNotifications, unreadSummaries, userId) {
        val unreadCount = if (userId == null) {
            0
        } else {
            unreadSummaries.values.sumOf { it.unreadCount.coerceAtLeast(0L) }
                .coerceAtMost(999L)
                .toInt()
        }
        appNotifications.updateUnreadMessageCount(unreadCount)
    }

    val outgoingCallRingback = remember { OutgoingCallRingbackTone() }
    val incomingCallsFlow = remember(callRepository, userId) {
        if (callRepository != null && userId != null) {
            callRepository.observeIncomingCalls(userId)
        } else {
            flowOf(emptyList())
        }
    }
    val incomingCalls by incomingCallsFlow.collectAsState(initial = emptyList())
    var incomingCallerProfile by remember { mutableStateOf<FaceVideoProfile?>(null) }
    var activeCall by remember { mutableStateOf<FaceVideoCall?>(null) }
    var callDurationRefreshTick by remember { mutableStateOf(0L) }
    // The media session remains alive when the full call UI is collapsed.
    var showActiveCallScreen by remember { mutableStateOf(false) }
    var activeCallTitle by remember { mutableStateOf("") }
    var activeCallIsOutgoing by remember { mutableStateOf(false) }
    var callStatus by remember { mutableStateOf("Подготовка звонка…") }
    var callError by remember { mutableStateOf<String?>(null) }
    var callMuted by remember { mutableStateOf(false) }
    var callCameraEnabled by remember { mutableStateOf(true) }
    var callSpeakerEnabled by remember { mutableStateOf(true) }
    var callClient by remember { mutableStateOf<WebRtcAudioCallClient?>(null) }
    var pendingCallAction by remember { mutableStateOf<PendingCallAction?>(null) }

    // Switching to another app collapses the call UI into the in-app mini bar.
    // The foreground service and WebRTC client continue running unchanged.
    val hostActivity = remember(context) {
        generateSequence(context as Context) { current ->
            (current as? ContextWrapper)?.baseContext
        }.filterIsInstance<ComponentActivity>().firstOrNull()
    }
    DisposableEffect(hostActivity, activeCall?.id) {
        val hasActiveCall = activeCall != null
        val observer = LifecycleEventObserver { _, event ->
            if (hasActiveCall && event == Lifecycle.Event.ON_STOP) {
                showActiveCallScreen = false
            }
        }
        hostActivity?.lifecycle?.addObserver(observer)
        onDispose {
            hostActivity?.lifecycle?.removeObserver(observer)
        }
    }

    LaunchedEffect(activeCall?.id, activeCall?.status, activeCall?.answeredAt) {
        val call = activeCall ?: return@LaunchedEffect
        if (call.status != "accepted" || parseCallInstant(call.answeredAt) == null) {
            callDurationRefreshTick = 0L
            return@LaunchedEffect
        }
        while (currentCoroutineContext().isActive) {
            callDurationRefreshTick = SystemClock.elapsedRealtime()
            delay(1_000L)
        }
    }
    val activeCallDuration = remember(activeCall, callDurationRefreshTick) {
        activeCall?.let(::formatCallDuration)
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    LaunchedEffect(userId, liveRepository) {
        if (
            userId != null &&
            Build.VERSION.SDK_INT >= 33 &&
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        val currentUserId = userId ?: return@LaunchedEffect
        val repository = liveRepository ?: return@LaunchedEffect
        runCatching {
            PushTokenRegistrar.register(context, repository, currentUserId)
        }.onFailure {
            android.util.Log.w("ArturChatPush", "FCM token registration failed", it)
        }
    }

    val currentOpenedLiveChatId by rememberUpdatedState(openedLiveChatId)
    val currentMutedChatIds by rememberUpdatedState(mutedChatIds)

    LaunchedEffect(liveRepository, userId) {
        val repository = liveRepository ?: return@LaunchedEffect
        val currentUserId = userId ?: return@LaunchedEffect
        try {
            repository.observeMutedChatIds(currentUserId).collect { updatedMutedChatIds ->
                mutedChatIds = updatedMutedChatIds
                withContext(Dispatchers.IO) {
                    localCache.saveMutedChatIds(currentUserId, updatedMutedChatIds)
                }
            }
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            android.util.Log.w(
                "ArturChatNotifications",
                "Could not observe muted chat settings",
                error
            )
            mutedChatIds = runCatching { repository.listMutedChatIds(currentUserId) }
                .onFailure {
                    android.util.Log.w("ArturChatNotifications", "Could not load muted chats", it)
                }
                .getOrDefault(mutedChatIds)
        }
    }

    // If the Activity was recreated and the chat was not in the first-paint cache,
    // resolve it from Supabase before rendering the conversation.
    LaunchedEffect(userId, openedLiveChatId, liveRepository) {
        val currentUserId = userId ?: return@LaunchedEffect
        val chatId = openedLiveChatId ?: return@LaunchedEffect
        if (openedLiveChat?.id == chatId) return@LaunchedEffect

        val cachedChat = localCache.readChats(currentUserId).firstOrNull { it.id == chatId }
        if (cachedChat != null) {
            openedLiveChat = cachedChat
            return@LaunchedEffect
        }

        val repository = liveRepository ?: return@LaunchedEffect
        val refreshedChats = runCatching { repository.listMyChats(currentUserId) }
            .getOrNull() ?: return@LaunchedEffect
        withContext(Dispatchers.IO) {
            localCache.saveChats(currentUserId, refreshedChats)
        }
        val restoredChat = refreshedChats.firstOrNull { it.id == chatId }
        if (restoredChat != null) {
            openedLiveChat = restoredChat
        } else {
            // The saved chat may have been deleted or the user may have been removed.
            openedLiveChatId = null
        }
    }

    // Unread summaries refresh independently from the long-lived realtime subscription.
    LaunchedEffect(liveRepository, userId, unreadRefreshKey) {
        val repository = liveRepository ?: return@LaunchedEffect
        val currentUserId = userId ?: return@LaunchedEffect
        runCatching { repository.getUnreadSummaries() }
            .onSuccess { summaries ->
                unreadSummaries = summaries
                withContext(Dispatchers.IO) {
                    localCache.saveUnreadSummaries(currentUserId, summaries)
                }
            }
            .onFailure {
                android.util.Log.w("ArturChatUnread", "Could not refresh unread summaries", it)
            }
    }

    // Keep one realtime listener for the whole signed-in session; screen navigation
    // must not tear it down and reload the entire message snapshot.
    LaunchedEffect(liveRepository, userId) {
        val repository = liveRepository ?: return@LaunchedEffect
        val currentUserId = userId ?: return@LaunchedEffect
        val chatTitles = localCache.readChats(currentUserId).associate { it.id to it.title }

        suspend fun markIncomingMessagesDelivered(messages: List<com.facevideo.app.data.FaceVideoMessage>) {
            val incomingIds = messages
                .filter { it.senderId != currentUserId }
                .map { it.id }
            if (incomingIds.isNotEmpty()) {
                runCatching { repository.markMessagesDelivered(incomingIds) }
                    .onFailure {
                        android.util.Log.w("ArturChatReceipts", "Could not mark messages delivered", it)
                    }
            }
        }

        val knownMessageIds = mutableSetOf<String>()
        var initialSnapshotReceived = false

        repository.observeMyMessages().collect { messages ->
            if (!initialSnapshotReceived) {
                knownMessageIds.addAll(messages.map { it.id })
                initialSnapshotReceived = true
                // Warm all per-chat caches from this snapshot without additional chat queries.
                launch(Dispatchers.IO) {
                    messages.groupBy { it.chatId }.forEach { (chatId, chatMessages) ->
                        localCache.seedMessagesIfMissing(currentUserId, chatId, chatMessages)
                    }
                }
                markIncomingMessagesDelivered(messages)
                return@collect
            }

            val newMessages = messages.filter { knownMessageIds.add(it.id) }
            newMessages
                .filter {
                    it.senderId != currentUserId &&
                        it.chatId != currentOpenedLiveChatId &&
                        it.chatId !in currentMutedChatIds
                }
                .forEach { message ->
                    appNotifications.showMessage(
                        notificationId = message.id,
                        title = chatTitles[message.chatId] ?: "Новое сообщение",
                        body = message.body
                    )
                }

            if (newMessages.any { it.senderId != currentUserId }) {
                unreadRefreshKey += 1
            }

            if (newMessages.isNotEmpty()) {
                val changedChatIds = newMessages.map { it.chatId }.distinct()
                launch(Dispatchers.IO) {
                    changedChatIds.forEach { chatId ->
                        localCache.saveMessages(
                            currentUserId,
                            chatId,
                            messages.filter { it.chatId == chatId }
                        )
                    }
                }
            }
            markIncomingMessagesDelivered(newMessages)
        }
    }

    suspend fun performCallAction(action: PendingCallAction) {
        val repository = callRepository ?: throw IllegalStateException("Сервер звонков недоступен.")
        val currentUser = userId ?: throw IllegalStateException("Не удалось определить пользователя.")
        when (action) {
            is PendingCallAction.Outgoing -> {
                callError = null
                showActiveCallScreen = true
                // Start before the signaling request so the user can minimize
                // immediately without missing Android's while-in-use FGS window.
                CallForegroundService.start(context, action.chat.title, action.mediaType)
                val call = try {
                    repository.startDirectCall(action.chat.id, currentUser, action.mediaType)
                } catch (error: Exception) {
                    CallForegroundService.stop(context)
                    throw error
                }
                activeCall = call
                activeCallTitle = action.chat.title
                activeCallIsOutgoing = true
                callCameraEnabled = true
                callSpeakerEnabled = true
                callStatus = if (call.mediaType == "video") "Вызов видеосвязи…" else "Вызов собеседника…"
                val client = WebRtcAudioCallClient(
                    context = context,
                    repository = repository,
                    call = call,
                    currentUserId = currentUser,
                                        onStatus = { status ->
                        callStatus = status
                        if (status == "Звонок активен" || status.startsWith("Собеседник ответил")) {
                            outgoingCallRingback.stop()
                        }
                    },
                    onError = {
                        callError = it
                        outgoingCallRingback.stop()
                    }
                )
                callClient = client
                client.startOutgoing()
                if (
                    activeCall?.id == call.id &&
                    activeCallIsOutgoing &&
                    activeCall?.status == "ringing" &&
                    callError == null
                ) {
                    outgoingCallRingback.start()
                }
            }
            is PendingCallAction.Incoming -> {
                callError = null
                showActiveCallScreen = true
                appNotifications.cancelNotification(action.call.id)
                // Start from the visible accept flow before awaiting Supabase.
                CallForegroundService.start(context, "Собеседник", action.call.mediaType)
                val acceptedCall = try {
                    repository.answerCall(action.call.id)
                } catch (error: Exception) {
                    CallForegroundService.stop(context)
                    throw error
                }
                val profile = runCatching {
                    repository.getProfile(action.call.callerId)
                }.getOrNull()
                activeCall = acceptedCall
                activeCallTitle = profile?.displayName?.ifBlank { profile.username } ?: "Собеседник"
                // Refresh the persistent notification with the resolved caller name.
                CallForegroundService.start(context, activeCallTitle, action.call.mediaType)
                activeCallIsOutgoing = false
                callCameraEnabled = true
                callSpeakerEnabled = true
                callStatus = if (action.call.mediaType == "video") "Подключаем видео…" else "Подключаем аудио…"
                val client = WebRtcAudioCallClient(
                    context = context,
                    repository = repository,
                    call = acceptedCall,
                    currentUserId = currentUser,
                    onStatus = { callStatus = it },
                    onError = { callError = it }
                )
                callClient = client
                client.startIncoming()
            }
        }
    }

    val microphonePermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        val action = pendingCallAction
        pendingCallAction = null
        if (granted && action != null) {
            scope.launch {
                runCatching { performCallAction(action) }
                    .onFailure { callError = it.message ?: "Не удалось начать звонок." }
            }
        } else if (!granted) {
            callError = "Для аудиозвонка разреши A-Chat доступ к микрофону."
        }
    }

    val videoPermissionsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        val action = pendingCallAction
        pendingCallAction = null
        val microphoneGranted =
            context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        val cameraGranted =
            context.checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        if (action != null && microphoneGranted && cameraGranted) {
            scope.launch {
                runCatching { performCallAction(action) }
                    .onFailure { callError = it.message ?: "Не удалось начать видеозвонок." }
            }
        } else {
            callError = "Для видеозвонка нужны разрешения на микрофон и камеру."
        }
    }

    fun requestCallPermission(action: PendingCallAction) {
        val needsVideo = when (action) {
            is PendingCallAction.Outgoing -> action.mediaType == "video"
            is PendingCallAction.Incoming -> action.call.mediaType == "video"
        }
        val microphoneGranted =
            context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        val cameraGranted =
            context.checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

        when {
            needsVideo && (!microphoneGranted || !cameraGranted) -> {
                pendingCallAction = action
                val missingPermissions = buildList {
                    if (!microphoneGranted) add(Manifest.permission.RECORD_AUDIO)
                    if (!cameraGranted) add(Manifest.permission.CAMERA)
                }.toTypedArray()
                videoPermissionsLauncher.launch(missingPermissions)
            }
            !microphoneGranted -> {
                pendingCallAction = action
                microphonePermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
            else -> {
                scope.launch {
                    runCatching { performCallAction(action) }
                        .onFailure { callError = it.message ?: "Не удалось начать звонок." }
                }
            }
        }
    }

    LaunchedEffect(callNotificationAction?.nonce, callRepository, userId) {
        val request = callNotificationAction ?: return@LaunchedEffect
        val repository = callRepository ?: return@LaunchedEffect
        val currentUser = userId ?: return@LaunchedEffect

        val call = runCatching { repository.getCallById(request.callId) }.getOrNull()
        if (call != null && call.calleeId == currentUser && call.status == "ringing") {
            when (request.action) {
                CallNotificationIntents.ACTION_ANSWER -> {
                    requestCallPermission(PendingCallAction.Incoming(call))
                }
                CallNotificationIntents.ACTION_DECLINE -> {
                    runCatching { repository.declineCall(call.id) }
                        .onFailure { callError = it.message ?: "Не удалось отклонить вызов." }
                }
            }
        }
        appNotifications.cancelNotification(request.callId)
        onCallNotificationActionHandled(request.nonce)
    }

    fun finishCall() {
        outgoingCallRingback.stop()
        CallForegroundService.stop(context)
        val call = activeCall ?: return
        val shouldCancel = activeCallIsOutgoing && call.status == "ringing"
        val clientToClose = callClient
        val repository = callRepository

        // Stop local media and restore the chat screen immediately. Do not keep
        // the call screen waiting for a network status update from Supabase.
        callClient = null
        activeCall = null
        showActiveCallScreen = false
        callMuted = false
        callCameraEnabled = true
        callSpeakerEnabled = true
        runCatching { clientToClose?.end() }
            .onFailure {
                android.util.Log.w("ArturChatCall", "Could not close local call resources", it)
            }

        // Persist the terminal state in the background; a network failure must
        // never close the Activity or prevent the user from returning to chat.
        scope.launch {
            runCatching {
                if (shouldCancel) {
                    repository?.cancelCall(call.id)
                } else {
                    repository?.endCall(call.id)
                }
            }.onFailure {
                android.util.Log.w("ArturChatCall", "Could not update cancelled call status", it)
            }
        }
    }

    // Back navigation follows the visible screen stack. A profile is shown
    // on top of the existing chat, so dismiss it first and keep that chat open.
    BackHandler(
        enabled = (activeCall != null && showActiveCallScreen) ||
            openedProfileUserId != null ||
            openedStoriesUserId != null ||
            openedLiveChatId != null ||
            openedChat != null
    ) {
        when {
            activeCall != null && showActiveCallScreen -> showActiveCallScreen = false
            openedProfileUserId != null -> openedProfileUserId = null
            openedStoriesUserId != null -> openedStoriesUserId = null
            openedLiveChatId != null -> setOpenedLiveChat(null)
            openedChat != null -> setOpenedDemoChat(null)
        }
    }

    val incomingCall = incomingCalls.firstOrNull { it.id != activeCall?.id }
    LaunchedEffect(incomingCall?.id) {
        if (incomingCall != null && activeCall == null) {
            appNotifications.startIncomingCallAlert()
        } else {
            appNotifications.stopIncomingCallAlert()
        }
    }
    DisposableEffect(appNotifications, outgoingCallRingback) {
        onDispose {
            appNotifications.stopIncomingCallAlert()
            outgoingCallRingback.stop()
        }
    }
    LaunchedEffect(incomingCall?.id) {
        incomingCallerProfile = if (incomingCall != null) {
            runCatching { callRepository?.getProfile(incomingCall.callerId) }.getOrNull()
        } else null
    }

    LaunchedEffect(activeCall?.id, callRepository) {
        val call = activeCall ?: return@LaunchedEffect
        val repository = callRepository ?: return@LaunchedEffect
        repository.observeCall(call.id).collect { rows ->
            val latest = rows.firstOrNull() ?: return@collect
            if (activeCall?.id == call.id && activeCall != latest) {
                // Keep the server's answered_at and ended_at timestamps in sync.
                activeCall = latest
            }
            when (latest.status) {
                "accepted" -> {
                    outgoingCallRingback.stop()
                    if (activeCallIsOutgoing && callStatus.startsWith("Вызов")) {
                        callStatus = if (call.mediaType == "video") {
                            "Собеседник ответил. Подключаем видео…"
                        } else {
                            "Собеседник ответил. Подключаем аудио…"
                        }
                    }
                }
                "declined" -> {
                    outgoingCallRingback.stop()
                    callStatus = "Собеседник отклонил вызов."
                    delay(1800)
                    if (activeCall?.id == call.id) {
                        callClient?.end()
                        callClient = null
                        activeCall = null
                        CallForegroundService.stop(context)
                    }
                }
                "cancelled" -> {
                    outgoingCallRingback.stop()
                    callStatus = "Вызов отменён."
                    delay(1500)
                    if (activeCall?.id == call.id) {
                        callClient?.end()
                        callClient = null
                        activeCall = null
                        CallForegroundService.stop(context)
                    }
                }
                "ended" -> {
                    outgoingCallRingback.stop()
                    callStatus = "Звонок завершён."
                    delay(1200)
                    if (activeCall?.id == call.id) {
                        callClient?.end()
                        callClient = null
                        activeCall = null
                        CallForegroundService.stop(context)
                    }
                }
            }
        }
    }

    LaunchedEffect(activeCall?.id, activeCallIsOutgoing) {
        val call = activeCall ?: return@LaunchedEffect
        if (activeCallIsOutgoing && call.status == "ringing") {
            delay(45_000)
            if (activeCall?.id == call.id && callStatus.startsWith("Вызов")) {
                outgoingCallRingback.stop()
                runCatching { callRepository?.cancelCall(call.id) }
                callStatus = "Нет ответа."
                delay(1500)
                if (activeCall?.id == call.id) {
                    callClient?.end()
                    callClient = null
                    activeCall = null
                    CallForegroundService.stop(context)
                }
            }
        }
    }

    val messagesByChat = remember {
        mutableStateMapOf<String, SnapshotStateList<ChatMessage>>().apply {
            DemoData.chats.forEach { chat ->
                put(chat.id, mutableStateListOf<ChatMessage>().apply {
                    addAll(DemoData.messages[chat.id].orEmpty())
                })
            }
        }
    }

    Column(
        Modifier.fillMaxSize().then(
            if (appOrangeChocolateTheme.value) {
                // Let the exact Orange & Chocolate wallpaper from MainActivity
                // remain visible underneath every tab and the bottom navigation.
                Modifier
            } else {
                Modifier.background(
                    Brush.verticalGradient(
                        if (appDarkTheme.value) {
                            listOf(Color(0xFF0E1520), Color(0xFF090D14), Color(0xFF0E1520))
                        } else {
                            listOf(AppBackground, Color(0xFFE5EAF0), AppBackground)
                        }
                    )
                )
            }
        )
    ) {
        if (activeCall != null && !showActiveCallScreen) {
            ActiveCallMiniBar(
                peerName = activeCallTitle,
                mediaType = activeCall?.mediaType ?: "audio",
                status = callStatus,
                duration = activeCallDuration,
                onOpen = { showActiveCallScreen = true }
            )
        }
        Box(Modifier.weight(1f).fillMaxWidth()) {
        if (activeCall != null && showActiveCallScreen) {
            if (activeCall?.mediaType == "video" && callClient != null) {
                ActiveVideoCallScreen(
                    peerName = activeCallTitle,
                    status = callStatus,
                    duration = activeCallDuration,
                    error = callError,
                    muted = callMuted,
                    cameraEnabled = callCameraEnabled,
                    client = callClient!!,
                    onToggleMute = {
                        callMuted = !callMuted
                        callClient?.setMuted(callMuted)
                    },
                    onToggleCamera = {
                        callCameraEnabled = !callCameraEnabled
                        callClient?.setCameraEnabled(callCameraEnabled)
                    },
                    onSwitchCamera = { callClient?.switchCamera() },
                    onMinimize = { showActiveCallScreen = false },
                    onEnd = { finishCall() }
                )
            } else {
                ActiveAudioCallScreen(
                    peerName = activeCallTitle,
                    status = callStatus,
                    duration = activeCallDuration,
                    error = callError,
                    muted = callMuted,
                    speakerEnabled = callSpeakerEnabled,
                    onToggleMute = {
                        callMuted = !callMuted
                        callClient?.setMuted(callMuted)
                    },
                    onToggleSpeaker = {
                        callSpeakerEnabled = !callSpeakerEnabled
                        callClient?.setSpeakerEnabled(callSpeakerEnabled)
                    },
                    onMinimize = { showActiveCallScreen = false },
                    onEnd = { finishCall() }
                )
            }
        } else if (openedStoriesUserId != null && liveRepository != null && userId != null) {
            StoryViewerScreen(
                storyOwnerId = openedStoriesUserId!!,
                currentUserId = userId,
                repository = liveRepository,
                cache = localCache,
                onBack = { openedStoriesUserId = null }
            )
        } else if (openedProfileUserId != null && liveRepository != null && userId != null) {
            UserProfileScreen(
                userId = openedProfileUserId!!,
                currentUserId = userId,
                repository = liveRepository,
                cache = localCache,
                onlineUserIds = onlineUserIds,
                onBack = { openedProfileUserId = null },
                onOpenStories = { openedStoriesUserId = openedProfileUserId!! },
                onOpenChat = { chat ->
                    openedProfileUserId = null
                    setOpenedLiveChat(chat)
                }
            )
        } else if (openedLiveChatId != null && openedLiveChat == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                androidx.compose.material3.CircularProgressIndicator(color = AccentSlate)
            }
        } else if (openedLiveChat != null && liveRepository != null && userId != null) {
            val currentChat = openedLiveChat!!
            LiveChatScreen(
                chat = currentChat,
                repository = liveRepository,
                callRepository = callRepository,
                cache = localCache,
                userId = userId,
                onlineUserIds = onlineUserIds,
                presenceAvailable = presenceAvailable,
                onBack = { setOpenedLiveChat(null) },
                onMarkedRead = { unreadRefreshKey += 1 },
                onOpenProfile = { profileUserId -> openedProfileUserId = profileUserId },
                onCall = { isVideo ->
                    requestCallPermission(
                        PendingCallAction.Outgoing(
                            currentChat,
                            if (isVideo) "video" else "audio"
                        )
                    )
                },
                notificationsMuted = currentChat.id in mutedChatIds,
                onNotificationMuteChanged = { muted ->
                    mutedChatIds = if (muted) mutedChatIds + currentChat.id
                    else mutedChatIds - currentChat.id
                },
                onGroupUpdated = { newTitle ->
                    setOpenedLiveChat(openedLiveChat?.copy(title = newTitle))
                    scope.launch {
                        runCatching {
                            val latestChats = liveRepository.listMyChats(userId)
                            latestChats.firstOrNull { it.id == currentChat.id }?.let { refreshed ->
                                setOpenedLiveChat(refreshed)
                            }
                            withContext(Dispatchers.IO) { localCache.saveChats(userId, latestChats) }
                        }.onFailure {
                            android.util.Log.w("ArturChatGroups", "Could not refresh group cache", it)
                        }
                    }
                },
                onGroupClosed = {
                    setOpenedLiveChat(null)
                    unreadRefreshKey += 1
                }
            )
        } else if (openedChat != null) {
            val chat = openedChat!!
            ChatScreen(
                chat = chat,
                messages = messagesByChat[chat.id] ?: mutableStateListOf(),
                onBack = { setOpenedDemoChat(null) },
                onCall = { showCallInfo = true }
            )
        } else {
            Column(
                Modifier.fillMaxSize().arturChatGradientBackground()
            ) {
                Box(Modifier.weight(1f)) {
                    when (selectedTab) {
                        MainTab.CHATS -> {
                            if (liveRepository != null && userId != null) {
                                LiveChatListScreen(
                                    repository = liveRepository,
                                    cache = localCache,
                                    userId = userId,
                                    unreadSummaries = unreadSummaries,
                                    onOpenChat = { setOpenedLiveChat(it) },
                                    onOpenStories = { openedStoriesUserId = it },
                                    mutedChatIds = mutedChatIds,
                                    onNotificationMuteChanged = { chatId, muted ->
                                        mutedChatIds = if (muted) mutedChatIds + chatId
                                        else mutedChatIds - chatId
                                    },
                                    onOpenProfile = { openedProfileUserId = it }
                                )
                            } else {
                                ChatListScreen(onOpenChat = { setOpenedDemoChat(it) })
                            }
                        }
                        MainTab.GROUPS -> {
                            if (liveRepository != null && userId != null) {
                                GroupsScreen(
                                    repository = liveRepository,
                                    cache = localCache,
                                    userId = userId,
                                    isOrangeChocolateTheme = appOrangeChocolateTheme.value,
                                    onOpenGroup = { setOpenedLiveChat(it) }
                                )
                            } else {
                                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text(
                                        "Для работы раздела «Группы» необходимо подключение к аккаунту.",
                                        color = Muted,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                        MainTab.CALLS -> {
                            CallsScreen(
                                repository = callRepository,
                                cache = localCache,
                                userId = userId,
                                onOpenProfile = { openedProfileUserId = it }
                            )
                        }
                        MainTab.CONTACTS -> {
                            if (liveRepository != null && userId != null) {
                                FriendsScreen(
                                    repository = liveRepository,
                                    cache = localCache,
                                    userId = userId,
                                    onlineUserIds = onlineUserIds,
                                    onOpenChat = { setOpenedLiveChat(it) },
                                    onOpenProfile = { openedProfileUserId = it }
                                )
                            } else {
                                ContactsScreen(onOpenChat = { setOpenedDemoChat(it) })
                            }
                        }
                        MainTab.PROFILE -> ProfileScreen(
                            userEmail = userEmail,
                            userId = userId,
                            repository = liveRepository,
                            cache = localCache,
                            onOpenFriends = { selectedTab = MainTab.CONTACTS },
                            onOpenGroups = { selectedTab = MainTab.GROUPS },
                            onOpenStories = { if (userId != null) openedStoriesUserId = userId },
                            onSignOut = {
                                val currentUserId = userId
                                scope.launch {
                                    if (currentUserId != null && liveRepository != null) {
                                        runCatching {
                                            PushTokenRegistrar.unregister(context, liveRepository, currentUserId)
                                        }
                                    }
                                    onSignOut()
                                }
                            }
                        )
                    }
                }
                BottomNavigation(
                    selectedTab = selectedTab,
                    unreadChatsCount = unreadSummaries.values.sumOf { it.unreadCount.coerceAtLeast(0L) }.coerceAtMost(999L).toInt(),
                    onSelect = { selectedTab = it }
                )
            }
        }
    }
    }

    if (incomingCall != null && activeCall == null) {
        AlertDialog(
            onDismissRequest = {
                appNotifications.cancelNotification(incomingCall.id)
                scope.launch { runCatching { callRepository?.declineCall(incomingCall.id) } }
            },
            containerColor = PanelLight,
            title = {
                Text(
                    if (incomingCall.mediaType == "video") "Входящий видеозвонок" else "Входящий аудиозвонок",
                    color = MainText,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        incomingCallerProfile?.displayName?.ifBlank { incomingCallerProfile?.username ?: "Пользователь" }
                            ?: "Пользователь звонит тебе",
                        color = MainText,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        if (incomingCall.mediaType == "video") "A-Chat · видеовызов" else "A-Chat · голосовой вызов",
                        color = Muted,
                        fontSize = 12.sp
                    )
                    if (callError != null) {
                        Spacer(Modifier.height(8.dp))
                        Text(callError.orEmpty(), color = InboxDanger, fontSize = 11.sp)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { requestCallPermission(PendingCallAction.Incoming(incomingCall)) }) {
                    Text("Принять", color = AccentSlate, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    appNotifications.cancelNotification(incomingCall.id)
                    scope.launch { runCatching { callRepository?.declineCall(incomingCall.id) } }
                }) {
                    Text("Отклонить", color = Muted)
                }
            }
        )
    }

    if (showCallInfo) {
        AlertDialog(
            onDismissRequest = { showCallInfo = false },
            containerColor = PanelLight,
            title = { Text("Видеозвонки", color = MainText, fontWeight = FontWeight.Bold) },
            text = { Text(callInfoMessage, color = Muted) },
            confirmButton = {
                TextButton(onClick = { showCallInfo = false }) {
                    Text("Понятно", color = AccentSlate)
                }
            }
        )
    }
}

@Composable
private fun LiveChatListScreen(
    repository: SupabaseChatRepository,
    cache: ArturChatLocalCache,
    userId: String,
    unreadSummaries: Map<String, FaceVideoChatUnreadSummary>,
    onOpenChat: (FaceVideoChatSummary) -> Unit,
    onOpenStories: (String) -> Unit,
    mutedChatIds: Set<String>,
    onNotificationMuteChanged: (String, Boolean) -> Unit,
    onOpenProfile: (String) -> Unit,
    contactsMode: Boolean = false
) {
    val cachedChats = remember(cache, userId) { cache.readChats(userId) }
    var chats by remember(cache, userId) { mutableStateOf(cachedChats) }
    var isLoading by remember(cache, userId) { mutableStateOf(cachedChats.isEmpty()) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var query by remember { mutableStateOf("") }
    var showSearch by remember { mutableStateOf(false) }
    var showNewChat by remember { mutableStateOf(false) }
    var reloadKey by remember { mutableStateOf(0) }
    var revealedChatId by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val latestChats by rememberUpdatedState(chats)

    LaunchedEffect(repository, userId) {
        while (currentCoroutineContext().isActive) {
            delay(15_000L)
            val snapshot = latestChats
            if (snapshot.isEmpty()) continue
            runCatching { repository.refreshChatPeerActivity(snapshot) }
                .onSuccess { refreshed ->
                    if (refreshed != snapshot) {
                        chats = refreshed
                        withContext(Dispatchers.IO) {
                            cache.saveChats(userId, refreshed)
                        }
                    }
                }
                .onFailure {
                    android.util.Log.w("ArturChatPresence", "Inbox activity refresh failed", it)
                }
        }
    }

    LaunchedEffect(repository, reloadKey, userId) {
        if (chats.isEmpty()) isLoading = true
        loadError = null
        try {
            val refreshedChats = repository.listMyChats(userId)
            chats = refreshedChats
            withContext(Dispatchers.IO) {
                cache.saveChats(userId, refreshedChats)
            }
        } catch (error: Exception) {
            loadError = error.message ?: "Не удалось загрузить переписки."
            android.util.Log.w("ArturChatCache", "Keeping cached chat list after refresh failure", error)
        } finally {
            isLoading = false
        }
    }

    fun openCachedChat(chat: FaceVideoChatSummary) {
        revealedChatId = null
        chats = (chats.filterNot { it.id == chat.id } + chat)
        cache.saveChats(userId, chats)
        onOpenChat(chat)
    }

    // Keep the Chats tab strictly for one-to-one conversations.
    // Group conversations are loaded independently in the Groups tab.
    val visibleChats = remember(chats, query, unreadSummaries) {
        val directChats = chats.filterNot { it.isGroup }
        val filteredChats = if (query.isBlank()) directChats else directChats.filter { chat ->
            chat.title.contains(query.trim(), ignoreCase = true) ||
                chat.lastMessagePreview.contains(query.trim(), ignoreCase = true) ||
                unreadSummaries[chat.id]?.lastUnreadBody.orEmpty().contains(query.trim(), ignoreCase = true)
        }
        filteredChats.sortedByDescending { chat ->
            latestChatActivityInstant(chat) ?: java.time.Instant.MIN
        }
    }

    // The parent main-tab container paints one continuous background behind
    // the list and bottom navigation. Keep this surface transparent to avoid
    // a nested rectangular panel inside the main gradient.
    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth()
                .padding(start = 20.dp, end = 18.dp, top = 7.dp, bottom = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "A-CHAT",
                modifier = Modifier.weight(1f),
                color = InboxAccentStrong.copy(alpha = 0.88f),
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.55.sp
            )
            // Keep a comfortable touch target while drawing only the icon:
            // no circular fill, gradient rim, shadow or outline.
            Box(
                modifier = Modifier.size(40.dp)
                    .clickable(
                        interactionSource = null,
                        indication = null
                    ) { showSearch = !showSearch },
                contentAlignment = Alignment.Center
            ) {
                InboxSearchGlyph(iconSize = 18.dp)
            }
        }

        AnimatedVisibility(
            visible = showSearch,
            enter = fadeIn(animationSpec = tween(190)) +
                slideInVertically(animationSpec = tween(240)) { fullHeight -> -fullHeight / 3 } +
                expandVertically(animationSpec = tween(240), expandFrom = Alignment.Top),
            exit = fadeOut(animationSpec = tween(130)) +
                slideOutVertically(animationSpec = tween(180)) { fullHeight -> -fullHeight / 4 } +
                shrinkVertically(animationSpec = tween(180), shrinkTowards = Alignment.Top)
        ) {
            Column {
                SearchField(
                    value = query,
                    onValueChange = { query = it },
                    compact = true
                )
                Spacer(Modifier.height(7.dp))
            }
        }

        if (!contactsMode) {
            StoriesStrip(
                repository = repository,
                cache = cache,
                userId = userId,
                onOpenStories = onOpenStories
            )
        }

        Box(Modifier.weight(1f).fillMaxWidth()) {
            when {
                isLoading && chats.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        androidx.compose.material3.CircularProgressIndicator(color = InboxAccent)
                    }
                }
                visibleChats.isEmpty() && loadError != null -> {
                    Column(
                        Modifier.fillMaxSize().padding(26.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text("Не удалось подключиться к перепискам", color = MainText, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(8.dp))
                        Text(loadError.orEmpty(), color = Muted, fontSize = 11.sp)
                        Spacer(Modifier.height(14.dp))
                        TextButton(onClick = { reloadKey++ }) { Text("Повторить", color = InboxAccent) }
                    }
                }
                visibleChats.isEmpty() -> {
                    Column(
                        Modifier.fillMaxSize().padding(horizontal = 30.dp, vertical = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Avatar("FV", 1, Modifier.size(68.dp))
                        Spacer(Modifier.height(16.dp))
                        Text(if (query.isBlank()) "Пока нет личных чатов" else "Ничего не найдено", color = MainText, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(7.dp))
                        Text(
                            if (query.isBlank()) "Начни переписку с другом — нажми кнопку создания чата внизу."
                            else "Попробуй изменить поисковый запрос.",
                            color = Muted,
                            fontSize = 11.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
                else -> {
                    Column(Modifier.fillMaxSize()) {
                        if (loadError != null) {
                            Text(
                                "Нет соединения — показаны сохранённые переписки",
                                color = Muted,
                                fontSize = 10.sp,
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 4.dp)
                            )
                        }
                        Row(
                            Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Недавние",
                                color = InboxPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(Modifier.weight(1f))
                            Text(
                                "Личные чаты: ${visibleChats.size}",
                                color = InboxMuted,
                                fontSize = 10.sp
                            )
                        }
                        LazyColumn(
                            Modifier.weight(1f),
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 1.dp, bottom = 88.dp),
                            verticalArrangement = Arrangement.spacedBy(0.dp)
                        ) {
                            items(visibleChats, key = { it.id }) { chat ->
                                Column(Modifier.fillMaxWidth()) {
                                    LiveChatRow(
                                        chat = chat,
                                        unreadSummary = unreadSummaries[chat.id],
                                        isDeleteRevealed = revealedChatId == chat.id,
                                        isNotificationsMuted = chat.id in mutedChatIds,
                                        onRevealDelete = { revealedChatId = chat.id },
                                        onHideDelete = { if (revealedChatId == chat.id) revealedChatId = null },
                                        onClick = { openCachedChat(chat) },
                                        onOpenProfile = { peerId -> onOpenProfile(peerId) },
                                        onToggleNotifications = { muted ->
                                            scope.launch {
                                                runCatching {
                                                    repository.setChatNotificationsMuted(chat.id, muted)
                                                }.onSuccess {
                                                    onNotificationMuteChanged(chat.id, muted)
                                                    Toast.makeText(
                                                        context,
                                                        if (muted) "Уведомления отключены" else "Уведомления включены",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                }.onFailure { error ->
                                                    android.util.Log.w(
                                                        "ArturChatNotifications",
                                                        "Could not update chat mute setting from inbox",
                                                        error
                                                    )
                                                    Toast.makeText(
                                                        context,
                                                        "Не удалось изменить уведомления",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                }
                                            }
                                        },
                                        onDelete = {
                                            revealedChatId = null
                                            scope.launch {
                                                runCatching { repository.hideChatForMe(chat.id, userId) }
                                                    .onSuccess {
                                                        chats = chats.filterNot { it.id == chat.id }
                                                        withContext(Dispatchers.IO) { cache.saveChats(userId, chats) }
                                                        reloadKey++
                                                    }
                                                    .onFailure { loadError = it.message ?: "Не удалось удалить чат." }
                                            }
                                        }
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Box(
                                        Modifier.fillMaxWidth().height(1.dp).background(
                                            Brush.horizontalGradient(
                                                listOf(
                                                    Color.Transparent,
                                                    if (appOrangeChocolateTheme.value) Color(0xFFFFB36B).copy(alpha = 0.44f) else Color.White.copy(alpha = 0.78f),
                                                    if (appOrangeChocolateTheme.value) Color(0xFF8A4C27).copy(alpha = 0.38f) else Color(0xFFD6DDF0).copy(alpha = 0.58f),
                                                    Color.Transparent
                                                )
                                            )
                                        )
                                    )
                                    Spacer(Modifier.height(4.dp))
                                }
                            }
                        }
                    }
                }
            }

            Surface(
                color = Color.Transparent,
                shape = RoundedCornerShape(28.dp),
                shadowElevation = 4.dp,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 10.dp)
                    .height(50.dp)
                    .clickable { showNewChat = true }
            ) {
                Row(
                    Modifier
                        .clip(RoundedCornerShape(28.dp))
                        .background(
                            Brush.horizontalGradient(
                                if (appOrangeChocolateTheme.value) listOf(Color(0xFFFFA33D), Color(0xFFFF8527), Color(0xFFC85A16)) else listOf(Color(0xFF28A8FF), Color(0xFF4E63F0), Color(0xFF9562ED))
                            )
                        )
                        .padding(horizontal = 24.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    NewChatGlyph()
                    Spacer(Modifier.width(10.dp))
                    Text("Начать диалог", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }

    if (showNewChat) {
        NewChatDialog(
            repository = repository,
            currentUserId = userId,
            onDismiss = { showNewChat = false },
            onOpenChat = { chat ->
                showNewChat = false
                openCachedChat(chat)
            }
        )
    }
}

@Composable
private fun StoriesStrip(
    repository: SupabaseChatRepository,
    cache: ArturChatLocalCache,
    userId: String,
    onOpenStories: (String) -> Unit
) {
    val cachedStories = remember(cache, userId) { cache.readStories(userId) }
    val cachedMyProfile = remember(cache, userId) { cache.readProfile(userId, userId) }
    var stories by remember(userId, cache) { mutableStateOf(cachedStories) }
    var myProfile by remember(userId, cache) { mutableStateOf(cachedMyProfile) }
    var loadError by remember(userId) { mutableStateOf<String?>(null) }
    var refreshKey by remember(userId) { mutableStateOf(0) }
    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var caption by remember { mutableStateOf("") }
    var showComposer by remember { mutableStateOf(false) }
    var isPublishing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val mediaCache = remember(context) { ArturChatImageCache(context) }

    LaunchedEffect(repository, userId, refreshKey) {
        loadError = null
        runCatching {
            val visible = repository.listVisibleStories(includeMediaUrls = false)
            val own = repository.getMyProfile(userId)
            stories = visible
            myProfile = own
            withContext(Dispatchers.IO) {
                cache.saveStories(userId, visible)
                if (own != null) cache.saveProfile(userId, own)
                mediaCache.pruneStories(userId, visible.mapTo(mutableSetOf()) { it.storyId })
            }
            // Warm one story per author in the background. The story list itself
            // no longer waits for a signed URL for every photo.
            val storiesToPreload = visible.groupBy { it.userId }.values
                .mapNotNull { group -> group.minByOrNull { it.createdAt } }
                .take(8)
            scope.launch(Dispatchers.IO) {
                storiesToPreload.forEach { story ->
                    runCatching {
                        val signedUrl = repository.createStoryMediaUrl(story.mediaPath)
                        mediaCache.fetchAndStore(
                            kind = "stories",
                            ownerId = userId,
                            mediaId = story.storyId,
                            sourceUrl = signedUrl,
                            freshnessMillis = 15 * 60 * 1000L,
                            refreshWhenSourceChanges = false
                        )
                    }.onFailure {
                        android.util.Log.w("ArturChatStories", "Story prefetch failed", it)
                    }
                }
            }
        }.onFailure {
            loadError = it.message ?: "Не удалось загрузить истории."
            android.util.Log.w("ArturChatStories", "Could not refresh visible stories; keeping local cache", it)
        }
    }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            selectedUri = uri
            caption = ""
            showComposer = true
        }
    }

    val ownStories = stories.filter { it.userId == userId }
    val friendGroups = stories
        .filter { it.userId != userId }
        .groupBy { it.userId }
        .values
        .map { group -> group.sortedByDescending { it.createdAt }.first() }
        .sortedByDescending { it.createdAt }

    Column(Modifier.fillMaxWidth().padding(top = 13.dp, bottom = 12.dp)) {
        Row(
            Modifier.fillMaxWidth().padding(start = 19.dp, end = 18.dp, bottom = 11.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    "Истории",
                    color = InboxPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Моменты твоих друзей",
                    color = InboxMuted,
                    fontSize = 10.sp
                )
            }
        }

        Row(
            Modifier.fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(11.dp),
            verticalAlignment = Alignment.Top
        ) {
            val latestOwnStory = ownStories.maxByOrNull { it.createdAt }
            StoryAvatarItem(
                label = "Ваша история",
                initials = (myProfile?.displayName?.ifBlank { myProfile?.username.orEmpty() }
                    ?: myProfile?.username.orEmpty()).take(1).uppercase().ifBlank { "Я" },
                userId = userId,
                avatarUrl = myProfile?.avatarUrl,
                hasStory = latestOwnStory != null,
                isOwn = true,
                onOpen = {
                    if (latestOwnStory != null) onOpenStories(userId)
                    else imagePicker.launch("image/*")
                },
                onAddStory = { imagePicker.launch("image/*") }
            )

            friendGroups.forEach { story ->
                StoryAvatarItem(
                    label = story.displayName.ifBlank { story.username },
                    initials = story.displayName.ifBlank { story.username }.take(1).uppercase().ifBlank { "?" },
                    userId = story.userId,
                    avatarUrl = story.avatarUrl,
                    hasStory = true,
                    onOpen = { onOpenStories(story.userId) }
                )
            }
        }

        if (loadError != null) {
            Text(
                loadError.orEmpty(),
                color = InboxMuted,
                fontSize = 9.sp,
                modifier = Modifier.padding(start = 19.dp, top = 5.dp)
            )
        }
    }

    if (showComposer && selectedUri != null) {
        AlertDialog(
            onDismissRequest = { if (!isPublishing) showComposer = false },
            containerColor = PanelLight,
            title = { Text("Новая история", color = MainText, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        "Фото будет видно только вашим друзьям и автоматически исчезнет через 24 часа.",
                        color = Muted,
                        fontSize = 12.sp
                    )
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = caption,
                        onValueChange = { caption = it.take(500) },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        maxLines = 4,
                        label = { Text("Подпись к истории") },
                        supportingText = { Text("${caption.length}/500") }
                    )
                    if (isPublishing) {
                        Spacer(Modifier.height(8.dp))
                        androidx.compose.material3.LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth(),
                            color = AccentSlate
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = !isPublishing,
                    onClick = {
                        val uri = selectedUri ?: return@TextButton
                        scope.launch {
                            isPublishing = true
                            runCatching {
                                val prepared = withContext(Dispatchers.IO) {
                                    prepareChatImage(context, uri)
                                }
                                repository.publishStory(userId, prepared.bytes, caption).also { published ->
                                    withContext(Dispatchers.IO) {
                                        mediaCache.writeBytes("stories", userId, published.storyId, prepared.bytes)
                                    }
                                }
                            }.onSuccess { published ->
                                stories = listOf(published) + stories.filterNot { it.storyId == published.storyId }
                                withContext(Dispatchers.IO) { cache.saveStories(userId, stories) }
                                showComposer = false
                                selectedUri = null
                                refreshKey++
                                Toast.makeText(context, "История опубликована", Toast.LENGTH_SHORT).show()
                            }.onFailure { error ->
                                Toast.makeText(
                                    context,
                                    error.message ?: "Не удалось опубликовать историю.",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                            isPublishing = false
                        }
                    }
                ) { Text(if (isPublishing) "Публикуем…" else "Опубликовать", color = AccentSlate) }
            },
            dismissButton = {
                TextButton(onClick = { if (!isPublishing) showComposer = false }) {
                    Text("Отмена", color = Muted)
                }
            }
        )
    }
}

@Composable
private fun StoryAvatarItem(
    label: String,
    initials: String,
    userId: String,
    avatarUrl: String?,
    hasStory: Boolean,
    isOwn: Boolean = false,
    onOpen: () -> Unit,
    onAddStory: (() -> Unit)? = null
) {
    Column(
        modifier = Modifier.width(74.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(68.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier.size(66.dp)
                    .background(
                        brush = if (hasStory) {
                            Brush.sweepGradient(
                                if (appOrangeChocolateTheme.value) {
                                    listOf(Color(0xFFFFD08A), Color(0xFFFF9B3D), Color(0xFFC75A19), Color(0xFFFFB04F), Color(0xFFFFD08A))
                                } else {
                                    listOf(Color(0xFFFEDA75), Color(0xFFFA7E1E), Color(0xFFD62976), Color(0xFF962FBF), Color(0xFF4F5BD5), Color(0xFFFEDA75))
                                }
                            )
                        } else {
                            Brush.linearGradient(listOf(Color(0xFFD4DCE8), Color(0xFFE7EBF4)))
                        },
                        shape = CircleShape
                    )
                    .clickable(onClick = onOpen)
                    .padding(3.dp)
                    .background(if (appOrangeChocolateTheme.value) Color(0xFF2B1408) else Color.White, CircleShape)
                    .padding(2.dp)
            ) {
                Avatar(
                    initials = initials,
                    style = userId.hashCode().ushr(1) % 4,
                    modifier = Modifier.fillMaxSize(),
                    imageUrl = avatarUrl,
                    imageCacheKey = userId
                )
            }

            if (isOwn && onAddStory != null) {
                Surface(
                    onClick = onAddStory,
                    shape = CircleShape,
                    color = InboxAccentStrong,
                    border = BorderStroke(2.dp, Color.White),
                    modifier = Modifier.align(Alignment.BottomEnd).size(23.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("+", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(Modifier.height(6.dp))
        Text(
            text = label,
            color = InboxPrimary,
            fontSize = 10.sp,
            fontWeight = if (isOwn) FontWeight.SemiBold else FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.fillMaxWidth().clickable(onClick = onOpen)
        )
    }
}

@Composable
private fun StoryImage(
    story: FaceVideoStoryCard,
    viewerUserId: String,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    fillWithBlurredCopy: Boolean = false
) {
    val context = LocalContext.current
    val mediaCache = remember(context) { ArturChatImageCache(context) }
    var bitmap by remember(viewerUserId, story.storyId) { mutableStateOf<android.graphics.Bitmap?>(null) }
    LaunchedEffect(viewerUserId, story.storyId, story.mediaUrl) {
        val cachedBytes = withContext(Dispatchers.IO) {
            mediaCache.readBytes("stories", viewerUserId, story.storyId)
        }
        bitmap = cachedBytes?.let { decodeCachedImage(it, 1800) }

        if (story.mediaUrl.isNotBlank()) {
            val refreshedBytes = withContext(Dispatchers.IO) {
                runCatching {
                    mediaCache.fetchAndStore(
                        kind = "stories",
                        ownerId = viewerUserId,
                        mediaId = story.storyId,
                        sourceUrl = story.mediaUrl,
                        freshnessMillis = 15 * 60 * 1000L,
                        refreshWhenSourceChanges = false
                    )
                }.onFailure {
                    android.util.Log.w("ArturChatStories", "Could not refresh story image", it)
                }.getOrNull()
            }
            if (refreshedBytes != null) bitmap = decodeCachedImage(refreshedBytes, 1800)
        }
    }
    val loaded = bitmap
    if (loaded != null) {
        Box(modifier.fillMaxSize()) {
            if (fillWithBlurredCopy) {
                Image(
                    bitmap = loaded.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize().blur(34.dp).alpha(0.54f),
                    contentScale = ContentScale.Crop
                )
                Box(
                    Modifier.fillMaxSize().background(
                        when {
                            appOrangeChocolateTheme.value -> Color(0x66371A0A)
                            appDarkTheme.value -> Color(0x66090D14)
                            else -> Color(0x66405070)
                        }
                    )
                )
            }
            Image(
                bitmap = loaded.asImageBitmap(),
                contentDescription = "История",
                modifier = Modifier.fillMaxSize(),
                contentScale = contentScale
            )
        }
    } else {
        Box(modifier.background(Color(0xFF293444)), contentAlignment = Alignment.Center) {
            androidx.compose.material3.CircularProgressIndicator(color = Color.White)
        }
    }
}

private fun decodeCachedImage(
    bytes: ByteArray,
    maxDimension: Int
): android.graphics.Bitmap? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

    var sampleSize = 1
    while (maxOf(bounds.outWidth / sampleSize, bounds.outHeight / sampleSize) > maxDimension) {
        sampleSize *= 2
    }
    val options = BitmapFactory.Options().apply { inSampleSize = sampleSize }
    return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
}

@Composable
private fun StoryViewerScreen(
    storyOwnerId: String,
    currentUserId: String,
    repository: SupabaseChatRepository,
    cache: ArturChatLocalCache,
    onBack: () -> Unit
) {
    val cachedStories = remember(cache, currentUserId, storyOwnerId) {
        cache.readStories(currentUserId).filter { it.userId == storyOwnerId }.sortedBy { it.createdAt }
    }
    var stories by remember(storyOwnerId, currentUserId, cache) { mutableStateOf(cachedStories) }
    var index by remember(storyOwnerId) { mutableStateOf(0) }
    var isLoading by remember(storyOwnerId, cache, currentUserId) {
        mutableStateOf(!cache.hasStoriesSnapshot(currentUserId))
    }
    var errorMessage by remember(storyOwnerId) { mutableStateOf<String?>(null) }
    var deleting by remember { mutableStateOf(false) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var showStoryMoreMenu by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val mediaCache = remember(context) { ArturChatImageCache(context) }

    LaunchedEffect(repository, storyOwnerId, currentUserId, cache) {
        isLoading = !cache.hasStoriesSnapshot(currentUserId)
        runCatching {
            val ownerStories = repository.listVisibleStories(storyOwnerId = storyOwnerId)
            stories = ownerStories.sortedBy { it.createdAt }
            index = 0
            errorMessage = null
            withContext(Dispatchers.IO) {
                val otherCachedStories = cache.readStories(currentUserId)
                    .filterNot { it.userId == storyOwnerId }
                cache.saveStories(currentUserId, otherCachedStories + ownerStories)
            }
        }.onFailure {
            errorMessage = it.message ?: "Не удалось загрузить историю."
            android.util.Log.w("ArturChatStories", "Could not refresh story viewer; keeping local cache", it)
        }
        isLoading = false
    }

    val current = stories.getOrNull(index)
    val canDelete = current?.userId == currentUserId
    val canReact = current != null && current.userId != currentUserId
    var reactionSummary by remember(current?.storyId, currentUserId, cache) {
        mutableStateOf(
            current?.storyId?.let { cache.readStoryReactionSummary(currentUserId, it) }
                ?: StoryReactionSummary()
        )
    }
    var isLoadingReactions by remember { mutableStateOf(false) }
    var isSavingReaction by remember(current?.storyId) { mutableStateOf(false) }
    var reactionError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(current?.storyId, currentUserId, cache) {
        val storyId = current?.storyId
        val cachedSummary = storyId?.let { cache.readStoryReactionSummary(currentUserId, it) }
        reactionSummary = cachedSummary ?: StoryReactionSummary()
        reactionError = null
        if (storyId == null) {
            isLoadingReactions = false
            return@LaunchedEffect
        }
        // Cached reactions can be used instantly; only a cold cache blocks reaction taps.
        isLoadingReactions = cachedSummary == null
        try {
            val summary = repository.getStoryReactionSummary(storyId, currentUserId)
            if (stories.getOrNull(index)?.storyId == storyId) {
                reactionSummary = summary
                withContext(Dispatchers.IO) {
                    cache.saveStoryReactionSummary(currentUserId, storyId, summary)
                }
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            if (stories.getOrNull(index)?.storyId == storyId) {
                reactionError = error.message ?: "Не удалось загрузить реакции."
            }
            android.util.Log.w("ArturChatStories", "Could not load story reactions", error)
        } finally {
            if (stories.getOrNull(index)?.storyId == storyId) {
                isLoadingReactions = false
            }
        }
    }

    fun onStoryReactionTapped(emoji: String) {
        val story = current ?: return
        if (!canReact || isLoadingReactions || isSavingReaction) return
        val nextReaction = runCatching {
            nextStoryReaction(reactionSummary.currentUserReaction, emoji)
        }.getOrElse {
            reactionError = it.message
            return
        }
        scope.launch {
            isSavingReaction = true
            reactionError = null
            try {
                val updated = repository.setStoryReaction(
                    storyId = story.storyId,
                    currentUserId = currentUserId,
                    reaction = nextReaction
                )
                if (stories.getOrNull(index)?.storyId == story.storyId) {
                    reactionSummary = updated
                    withContext(Dispatchers.IO) {
                        cache.saveStoryReactionSummary(currentUserId, story.storyId, updated)
                    }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                if (stories.getOrNull(index)?.storyId == story.storyId) {
                    reactionError = error.message ?: "Не удалось сохранить реакцию."
                }
                android.util.Log.w("ArturChatStories", "Could not save story reaction", error)
            } finally {
                if (stories.getOrNull(index)?.storyId == story.storyId) {
                    isSavingReaction = false
                }
            }
        }
    }

    val ownerName = current?.let { it.displayName.ifBlank { it.username } } ?: "История"
    val ownerInitial = ownerName.trim().take(1).uppercase().ifBlank { "?" }
    val storyAccent = when {
        appOrangeChocolateTheme.value -> Color(0xFFFFA347)
        appDarkTheme.value -> Color(0xFFA7B6FF)
        else -> Color(0xFFBFD0FF)
    }
    val storyTopGradient = when {
        appOrangeChocolateTheme.value -> listOf(Color(0xF0180903), Color(0xB01A0A04), Color.Transparent)
        appDarkTheme.value -> listOf(Color(0xF0090D14), Color(0xB0090D14), Color.Transparent)
        else -> listOf(Color(0xDB26334D), Color(0x9926334D), Color.Transparent)
    }
    val storyBottomGradient = when {
        appOrangeChocolateTheme.value -> listOf(Color.Transparent, Color(0xAA1A0A04), Color(0xF0180903))
        appDarkTheme.value -> listOf(Color.Transparent, Color(0xAA090D14), Color(0xF0090D14))
        else -> listOf(Color.Transparent, Color(0x9926334D), Color(0xDB26334D))
    }
    val storyScreenGradient = when {
        appOrangeChocolateTheme.value -> listOf(Color(0xFF160802), Color(0xFF281307), Color(0xFF160802))
        appDarkTheme.value -> listOf(Color(0xFF090D14), Color(0xFF151E2B), Color(0xFF090D14))
        else -> listOf(Color(0xFF253047), Color(0xFF354766), Color(0xFF253047))
    }

    Box(
        Modifier.fillMaxSize()
            .background(Brush.verticalGradient(storyScreenGradient))
    ) {
        // One decoded story image renders both the fitted foreground and the blurred ambient backdrop.
        if (current != null) {
            Crossfade(
                targetState = current.storyId,
                animationSpec = tween(240),
                label = "story-media-transition"
            ) { storyId ->
                val visibleStory = stories.firstOrNull { it.storyId == storyId }
                if (visibleStory != null) {
                    StoryImage(
                        story = visibleStory,
                        viewerUserId = currentUserId,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit,
                        fillWithBlurredCopy = true
                    )
                }
            }
        }

        Column(Modifier.fillMaxSize()) {
            // Top progress and creator controls use a restrained glass surface.
            Column(
                Modifier.fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            storyTopGradient
                        )
                    )
                    .padding(start = 13.dp, end = 13.dp, top = 10.dp, bottom = 18.dp)
            ) {
                if (stories.isNotEmpty()) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        stories.forEachIndexed { storyIndex, _ ->
                            Box(
                                Modifier.weight(1f)
                                    .height(3.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when {
                                            storyIndex < index -> Color.White
                                            storyIndex == index -> Color(0xFFBFD7FF)
                                            else -> Color.White.copy(alpha = 0.32f)
                                        }
                                    )
                            )
                        }
                    }
                    Spacer(Modifier.height(13.dp))
                }

                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        modifier = Modifier.size(48.dp).clickable(onClick = onBack),
                        shape = CircleShape,
                        color = Color.Transparent,
                        border = null
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                "×",
                                color = Color.White,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Light,
                                modifier = Modifier.padding(bottom = 2.dp)
                            )
                        }
                    }
                    Spacer(Modifier.width(10.dp))
                    Box(
                        Modifier.size(40.dp)
                            .background(
                                Brush.linearGradient(
                                    when {
                                        appOrangeChocolateTheme.value -> listOf(Color(0xFFFFD08A), Color(0xFFFF9B3D), Color(0xFFC75A19))
                                        appDarkTheme.value -> listOf(Color(0xFF7188D8), Color(0xFFA7B6FF), Color(0xFF5267A8))
                                        else -> listOf(Color(0xFF8EA4F0), Color(0xFFB8C7FF), Color(0xFF7C8FE0))
                                    }
                                ),
                                CircleShape
                            )
                            .padding(2.dp)
                    ) {
                        Avatar(
                            initials = ownerInitial,
                            style = storyOwnerId.hashCode().ushr(1) % 4,
                            modifier = Modifier.fillMaxSize(),
                            imageUrl = current?.avatarUrl,
                            imageCacheKey = storyOwnerId
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            ownerName,
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            current?.let { storyTimeLabel(it.createdAt) }.orEmpty(),
                            color = Color.White.copy(alpha = 0.68f),
                            fontSize = 10.sp,
                            maxLines = 1
                        )
                    }
                    if (canDelete) {
                        Box {
                            Surface(
                                onClick = { showStoryMoreMenu = true },
                                enabled = !deleting,
                                shape = CircleShape,
                                color = Color.Transparent,
                                border = null,
                                modifier = Modifier.size(38.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text("⋯", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            DropdownMenu(
                                expanded = showStoryMoreMenu,
                                onDismissRequest = { showStoryMoreMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Удалить историю", color = Color(0xFFB45F6D)) },
                                    onClick = {
                                        showStoryMoreMenu = false
                                        showDeleteConfirmation = true
                                    }
                                )
                            }
                        }
                    }
                }
            }

            Box(
                Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                when {
                    isLoading -> {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            androidx.compose.material3.CircularProgressIndicator(
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                            Spacer(Modifier.height(12.dp))
                            Text(
                                "Загружаем историю…",
                                color = Color.White.copy(alpha = 0.76f),
                                fontSize = 11.sp
                            )
                        }
                    }
                    current != null -> {
                        // Tap left/right on the media to navigate, as in modern story viewers.
                        Row(
                            Modifier.fillMaxSize()
                                .pointerInput(index, stories.size) {
                                    detectTapGestures { tap ->
                                        when {
                                            tap.x < size.width * 0.34f ->
                                                index = (index - 1).coerceAtLeast(0)
                                            tap.x > size.width * 0.66f -> {
                                                if (index < stories.lastIndex) index++ else onBack()
                                            }
                                        }
                                    }
                                }
                                .pointerInput(storyOwnerId) {
                                    var downwardDrag = 0f
                                    detectVerticalDragGestures(
                                        onDragEnd = {
                                            if (downwardDrag > 110f) onBack()
                                            downwardDrag = 0f
                                        },
                                        onVerticalDrag = { _, dragAmount ->
                                            downwardDrag += dragAmount
                                        }
                                    )
                                }
                        ) {
                            Spacer(Modifier.weight(1f))
                        }
                    }
                    else -> {
                        Column(
                            Modifier.fillMaxWidth().padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                errorMessage ?: "Истории пока нет или срок её действия истёк.",
                                color = Color.White.copy(alpha = 0.82f),
                                fontSize = 13.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Spacer(Modifier.height(16.dp))
                            Surface(
                                onClick = onBack,
                                modifier = Modifier.heightIn(min = 48.dp),
                                shape = RoundedCornerShape(18.dp),
                                color = Color.Transparent,
                                border = null
                            ) {
                                Text("Вернуться", color = Color.White, modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp))
                            }
                        }
                    }
                }
            }

            Column(
                Modifier.fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            storyBottomGradient
                        )
                    )
                    .padding(start = 14.dp, end = 14.dp, top = 18.dp, bottom = 12.dp)
            ) {
                if (current?.caption?.isNotBlank() == true) {
                    Text(
                        current.caption,
                        color = Color.White,
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 3.dp, vertical = 3.dp)
                    )
                    Spacer(Modifier.height(12.dp))
                }

                if (current != null) {
                    if (canReact) {
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 2.dp, vertical = 3.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            STORY_REACTION_OPTIONS.forEach { reaction ->
                                val selected = reactionSummary.currentUserReaction == reaction
                                Box(
                                    modifier = Modifier.weight(1f).height(44.dp)
                                        .clip(CircleShape)
                                        .then(
                                            if (selected) {
                                                Modifier.background(storyAccent.copy(alpha = 0.20f))
                                                    .border(1.dp, storyAccent.copy(alpha = 0.82f), CircleShape)
                                            } else {
                                                Modifier
                                            }
                                        )
                                        .clickable(
                                            enabled = !isLoadingReactions && !isSavingReaction,
                                            interactionSource = null,
                                            indication = null
                                        ) { onStoryReactionTapped(reaction) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    StoryReactionGlyph(
                                        reaction = reaction,
                                        tint = if (selected) storyAccent else Color.White.copy(alpha = 0.94f),
                                        modifier = Modifier.size(if (selected) 24.dp else 22.dp)
                                    )
                                }
                            }
                        }
                    } else if (canDelete) {
                        val ownerReactionCounts = STORY_REACTION_OPTIONS.mapNotNull { reaction ->
                            val count = reactionSummary.counts[reaction] ?: 0
                            if (count > 0) reaction to count else null
                        }
                        if (ownerReactionCounts.isNotEmpty()) {
                            Row(
                                Modifier.fillMaxWidth().padding(vertical = 5.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                ownerReactionCounts.forEach { (reaction, count) ->
                                    Row(
                                        modifier = Modifier.padding(horizontal = 9.dp),
                                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        StoryReactionGlyph(
                                            reaction = reaction,
                                            tint = storyAccent,
                                            modifier = Modifier.size(17.dp)
                                        )
                                        Text(
                                            count.toString(),
                                            color = Color.White.copy(alpha = 0.96f),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }
                    }
                    if (isSavingReaction) {
                        Text(
                            "Сохраняем реакцию…",
                            color = Color.White.copy(alpha = 0.66f),
                            fontSize = 8.sp,
                            modifier = Modifier.padding(start = 5.dp, top = 4.dp)
                        )
                    }
                    if (reactionError != null) {
                        Text(
                            reactionError.orEmpty(),
                            color = Color(0xFFFFC3CD),
                            fontSize = 9.sp,
                            modifier = Modifier.padding(start = 5.dp, top = 4.dp)
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                }

            }
        }
    }

    if (showDeleteConfirmation && current != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text("Удалить историю?") },
            text = { Text("История исчезнет у всех друзей.") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirmation = false
                    scope.launch {
                        deleting = true
                        runCatching {
                            repository.deleteStory(
                                current.storyId,
                                current.mediaPath.ifBlank { "${current.userId}/${current.storyId}.jpg" }
                            )
                        }.onSuccess {
                            stories = stories.filterNot { it.storyId == current.storyId }
                            withContext(Dispatchers.IO) {
                                cache.saveStories(currentUserId, cache.readStories(currentUserId)
                                    .filterNot { it.storyId == current.storyId })
                                cache.removeStoryReactionSummary(currentUserId, current.storyId)
                                mediaCache.remove("stories", currentUserId, current.storyId)
                            }
                            index = (index - 1).coerceAtLeast(0)
                            if (stories.isEmpty()) onBack()
                        }
                            .onFailure { errorMessage = it.message ?: "Не удалось удалить историю." }
                        deleting = false
                    }
                }) { Text("Удалить", color = Color(0xFFA85F6E)) }
            },
            dismissButton = { TextButton(onClick = { showDeleteConfirmation = false }) { Text("Отмена") } }
        )
    }
}

private fun latestChatActivityInstant(chat: FaceVideoChatSummary): java.time.Instant? {
    val lastMessage = parseCallInstant(chat.lastMessageAt)
    val peerLastSeen = parseCallInstant(chat.peerLastSeenAt)
    return listOfNotNull(lastMessage, peerLastSeen).maxOrNull()
}

private fun latestChatActivityLabelTime(chat: FaceVideoChatSummary): String? {
    val lastMessage = parseCallInstant(chat.lastMessageAt)
    val peerLastSeen = parseCallInstant(chat.peerLastSeenAt)
    return if (peerLastSeen != null && (lastMessage == null || peerLastSeen.isAfter(lastMessage))) {
        chat.peerLastSeenAt
    } else {
        chat.lastMessageAt
    }
}

private fun storyTimeLabel(value: String): String = runCatching {
    val created = java.time.Instant.parse(value)
    val minutes = java.time.Duration.between(created, java.time.Instant.now()).toMinutes().coerceAtLeast(0)
    when {
        minutes < 1 -> "только что"
        minutes < 60 -> "${minutes} мин. назад"
        minutes < 24 * 60 -> "${minutes / 60} ч. назад"
        else -> "более суток назад"
    }
}.getOrDefault("История")

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LiveChatRow(
    chat: FaceVideoChatSummary,
    unreadSummary: FaceVideoChatUnreadSummary?,
    isDeleteRevealed: Boolean,
    isNotificationsMuted: Boolean,
    onRevealDelete: () -> Unit,
    onHideDelete: () -> Unit,
    onClick: () -> Unit,
    onOpenProfile: (String) -> Unit,
    onToggleNotifications: (Boolean) -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember(chat.id) { mutableStateOf(false) }
    var confirmDelete by remember(chat.id) { mutableStateOf(false) }
    var showGroupAvatarPreview by remember(chat.id) { mutableStateOf(false) }
    val deleteActionWidth = 84.dp
    val density = LocalDensity.current
    val deleteActionWidthPx = with(density) { deleteActionWidth.toPx() }
    var dragOffset by remember(chat.id) { mutableFloatStateOf(0f) }
    var isDragging by remember(chat.id) { mutableStateOf(false) }
    val animatedRowOffset by animateFloatAsState(
        targetValue = if (isDeleteRevealed) -deleteActionWidthPx else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium),
        label = "chat-row-delete-reveal"
    )
    val latestAnimatedRowOffset by rememberUpdatedState(animatedRowOffset)
    val visibleRowOffset = if (isDragging) dragOffset else animatedRowOffset
    val revealedWidthPx = (-visibleRowOffset).coerceIn(0f, deleteActionWidthPx)
    val revealedWidth = with(density) { revealedWidthPx.toDp() }
    val revealProgress = (revealedWidthPx / deleteActionWidthPx).coerceIn(0f, 1f)
    val unreadCount = unreadSummary?.unreadCount?.coerceAtLeast(0)?.coerceAtMost(Int.MAX_VALUE.toLong())?.toInt() ?: 0
    val preview = buildChatInboxPreview(chat.lastMessagePreview, chat.lastMessageAt, unreadSummary)
    val activityTimestamp = latestChatActivityLabelTime(chat) ?: preview.timestamp
    val initials = chat.title.trim().take(2).uppercase().ifBlank { "FV" }
    Box(
        modifier = Modifier.fillMaxWidth().height(80.dp)
            .clip(RectangleShape)
    ) {
        if (revealedWidthPx > 0.5f) {
            Column(
                modifier = Modifier.align(Alignment.CenterEnd)
                    .fillMaxHeight()
                    .width(revealedWidth)
                    .padding(vertical = 5.dp)
                    .clip(RectangleShape)
                    .background(Color(0xFFE66F80))
                    .clickable(enabled = !isDragging && revealProgress >= 0.98f) {
                        onHideDelete()
                        confirmDelete = true
                    },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // The action itself grows with the drag; its contents appear only
                // when there is enough room, so the label never pops out early.
                if (revealProgress >= 0.68f) {
                    InboxDeleteGlyph()
                    Spacer(Modifier.height(3.dp))
                    Text("Удалить", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
        Surface(
            modifier = Modifier.fillMaxSize()
                .offset { IntOffset(visibleRowOffset.roundToInt(), 0) }
                .pointerInput(chat.id, deleteActionWidthPx) {
                    detectHorizontalDragGestures(
                        onDragStart = {
                            dragOffset = latestAnimatedRowOffset
                            isDragging = true
                        },
                        onHorizontalDrag = { change, dragAmount ->
                            if (dragAmount < 0f || dragOffset < 0f) {
                                change.consume()
                                dragOffset = (dragOffset + dragAmount).coerceIn(-deleteActionWidthPx, 0f)
                            }
                        },
                        onDragEnd = {
                            val reveal = dragOffset <= -deleteActionWidthPx * 0.42f
                            isDragging = false
                            if (reveal) onRevealDelete() else onHideDelete()
                        },
                        onDragCancel = {
                            isDragging = false
                            onHideDelete()
                        }
                    )
                }
                .combinedClickable(
                    onClick = { if (isDeleteRevealed) onHideDelete() else onClick() },
                    onLongClick = { onHideDelete(); showMenu = true }
                ),
            color = Color.Transparent,
            shape = RoundedCornerShape(22.dp),
            shadowElevation = 0.dp
        ) {
        Row(
            Modifier.fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
        if (chat.isGroup) {
            if (!chat.avatarUrl.isNullOrBlank()) {
                Avatar(
                    initials = chat.title.take(1).uppercase().ifBlank { "G" },
                    style = 1,
                    modifier = Modifier.size(52.dp),
                    imageUrl = chat.avatarUrl,
                    imageCacheKey = "group:${chat.id}",
                    onClick = { showGroupAvatarPreview = true }
                )
            } else {
                ArturChatTypeAvatar(ArturChatIconType.GROUP, Modifier.size(52.dp))
            }
        } else {
            Avatar(
                initials,
                chat.id.hashCode().ushr(1) % 4,
                Modifier.size(52.dp),
                imageUrl = chat.peerAvatarUrl,
                imageCacheKey = chat.peerUserId
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(chat.title, color = InboxPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
            Spacer(Modifier.height(6.dp))
            Text(
                preview.text.ifBlank { "Начните переписку" },
                color = if (unreadCount > 0) InboxSecondary else InboxMuted,
                fontSize = 12.sp,
                fontWeight = if (unreadCount > 0) FontWeight.SemiBold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(Modifier.width(8.dp))
        Column(horizontalAlignment = Alignment.End) {
            Text(
                activityTimestamp?.let { formatChatMessageTime(it) }.orEmpty(),
                color = if (unreadCount > 0) InboxAccent else InboxMuted,
                fontSize = 10.sp
            )
            formatUnreadBadge(unreadCount)?.let { badgeText ->
                Spacer(Modifier.height(6.dp))
                Surface(
                    color = InboxAccent,
                    shape = CircleShape,
                    modifier = Modifier.height(21.dp).widthIn(min = 21.dp)
                ) {
                    Box(
                        modifier = Modifier.height(21.dp).padding(horizontal = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            badgeText,
                            color = Color.White,
                            fontSize = 10.sp,
                            lineHeight = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
            ChatChromeActionButton(
                onClick = { onHideDelete(); showMenu = true },
                size = 34.dp,
                transparent = true
            ) {
                InboxMoreGlyph()
            }
        }
    }
    }
    DropdownMenu(
        expanded = showMenu,
        onDismissRequest = { showMenu = false },
        containerColor = InboxSurface,
        tonalElevation = 0.dp,
        shadowElevation = 9.dp,
        shape = RoundedCornerShape(18.dp)
    ) {
        if (!chat.peerUserId.isNullOrBlank()) {
            DropdownMenuItem(
                text = { Text("Открыть профиль", color = InboxPrimary, fontSize = 13.sp) },
                leadingIcon = { ProfileMenuGlyph("person", InboxAccentStrong) },
                onClick = {
                    showMenu = false
                    onOpenProfile(chat.peerUserId!!)
                },
                colors = androidx.compose.material3.MenuDefaults.itemColors(textColor = InboxPrimary)
            )
        }
        DropdownMenuItem(
            text = {
                Text(
                    if (isNotificationsMuted) "Включить уведомления" else "Отключить уведомления",
                    color = InboxPrimary,
                    fontSize = 13.sp
                )
            },
            leadingIcon = { ProfileMenuGlyph("bell", InboxAccentStrong) },
            onClick = {
                showMenu = false
                onToggleNotifications(!isNotificationsMuted)
            },
            colors = androidx.compose.material3.MenuDefaults.itemColors(textColor = InboxPrimary)
        )
        DropdownMenuItem(
            text = { Text("Удалить чат", color = Color(0xFFB85E69), fontSize = 13.sp) },
            leadingIcon = { InboxDeleteGlyph() },
            onClick = { showMenu = false; confirmDelete = true },
            colors = androidx.compose.material3.MenuDefaults.itemColors(
                textColor = Color(0xFFB85E69)
            )
        )
    }
    if (showGroupAvatarPreview && !chat.avatarUrl.isNullOrBlank()) {
        FullscreenAvatarDialog(
            imageUrl = chat.avatarUrl!!,
            imageCacheKey = "group:${chat.id}",
            title = chat.title,
            onDismiss = { showGroupAvatarPreview = false }
        )
    }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Удалить чат?") },
            text = { Text("Переписка исчезнет только из вашего списка. У второго участника чат останется.") },
            confirmButton = { TextButton(onClick = { confirmDelete = false; onDelete() }) { Text("Удалить", color = Color(0xFFA85F6E)) } },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Отмена") } }
        )
    }
}

@Composable
private fun NewChatDialog(
    repository: SupabaseChatRepository,
    currentUserId: String,
    onDismiss: () -> Unit,
    onOpenChat: (FaceVideoChatSummary) -> Unit
) {
    var username by remember { mutableStateOf("") }
    var result by remember { mutableStateOf<FaceVideoProfile?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    var isBusy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = PanelLight,
        title = { Text("Новый чат", color = MainText, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text("Введи псевдоним пользователя. Если переписка уже существует, откроется текущий чат.", color = Muted, fontSize = 12.sp)
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = username,
                    onValueChange = {
                        username = it.lowercase().filter { ch -> ch in 'a'..'z' || ch in '0'..'9' || ch == '_' }.take(24)
                        result = null
                        message = null
                    },
                    singleLine = true,
                    label = { Text("Например, alex_24") },
                    modifier = Modifier.fillMaxWidth()
                )
                if (result != null) {
                    Spacer(Modifier.height(12.dp))
                    Text("@${result!!.username} · ${result!!.displayName}", color = AccentSlate, fontWeight = FontWeight.SemiBold)
                }
                if (message != null) {
                    Spacer(Modifier.height(10.dp))
                    Text(message.orEmpty(), color = Color(0xFFA85F6E), fontSize = 11.sp)
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = !isBusy && username.length >= 3,
                onClick = {
                    scope.launch {
                        isBusy = true
                        message = null
                        try {
                            val profile = result
                            if (profile == null) {
                                val found = repository.findUser(username)
                                if (found == null) {
                                    message = "Пользователь не найден. Проверь псевдоним."
                                } else if (found.id == currentUserId) {
                                    message = "Это твой собственный аккаунт."
                                } else {
                                    result = found
                                }
                            } else {
                                val chat = repository.getOrCreateDirectChat(profile.id, currentUserId)
                                if (chat == null) {
                                    message = "Не удалось открыть переписку. Попробуй ещё раз."
                                } else {
                                    onOpenChat(chat)
                                }
                            }
                        } catch (error: Exception) {
                            message = error.message ?: "Не удалось открыть переписку."
                        } finally {
                            isBusy = false
                        }
                    }
                }
            ) {
                Text(
                    when {
                        isBusy -> "Подключение..."
                        result != null -> "Открыть чат"
                        else -> "Найти"
                    },
                    color = AccentSlate
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена", color = Muted) }
        }
    )
}

@Composable
private fun LiveChatScreen(
    chat: FaceVideoChatSummary,
    repository: SupabaseChatRepository,
    callRepository: SupabaseCallRepository?,
    cache: ArturChatLocalCache,
    userId: String,
    onlineUserIds: Set<String>,
    presenceAvailable: Boolean,
    onBack: () -> Unit,
    onMarkedRead: () -> Unit,
    onOpenProfile: (String) -> Unit,
    onCall: (Boolean) -> Unit,
    notificationsMuted: Boolean,
    onNotificationMuteChanged: (Boolean) -> Unit,
    onGroupUpdated: (String) -> Unit,
    onGroupClosed: () -> Unit
) {
    var activeTopic by remember(chat.id) { mutableStateOf<GroupTopicDestination?>(null) }
    val activeTopicId = activeTopic?.id
    val draftCacheKey = remember(chat.id, activeTopicId) {
        if (activeTopicId == null) chat.id else "${chat.id}:topic:$activeTopicId"
    }
    val cachedMessages = remember(cache, userId, chat.id) {
        cache.readMessages(userId, chat.id)
    }
    val messagesFlow = remember(repository, chat.id) { repository.observeMessages(chat.id) }
    val messages by messagesFlow.collectAsState(initial = cachedMessages)
    val cachedCallHistory = remember(cache, userId, chat.id) {
        cache.readChatCallHistory(userId, chat.id)
    }
    val callHistoryFlow = remember(callRepository, chat.id, chat.isGroup) {
        if (callRepository != null && !chat.isGroup) {
            callRepository.observeChatCallHistory(chat.id)
        } else {
            flowOf(emptyList())
        }
    }
    val callHistory by callHistoryFlow.collectAsState(initial = cachedCallHistory)
    LaunchedEffect(cache, userId, chat.id, callHistory) {
        withContext(Dispatchers.IO) {
            cache.saveChatCallHistory(userId, chat.id, callHistory)
        }
    }
    var peerUserId by remember(repository, chat.id, userId) { mutableStateOf(chat.peerUserId) }
    var peerAvatarUrl by remember(repository, chat.id, userId) { mutableStateOf(chat.peerAvatarUrl) }
    LaunchedEffect(repository, chat.id, userId, chat.isGroup, chat.peerUserId, chat.peerAvatarUrl) {
        if (chat.isGroup) {
            peerUserId = null
            peerAvatarUrl = null
        } else {
            val resolvedPeerId = chat.peerUserId ?: runCatching {
                repository.getDirectChatPeerId(chat.id, userId)
            }.onFailure {
                android.util.Log.w("ArturChatPresence", "Could not resolve chat peer", it)
            }.getOrNull()
            peerUserId = resolvedPeerId
            peerAvatarUrl = chat.peerAvatarUrl ?: resolvedPeerId?.let { peerId ->
                runCatching { repository.getProfile(peerId)?.avatarUrl }
                    .onFailure {
                        android.util.Log.w("ArturChatAvatar", "Could not load peer avatar", it)
                    }
                    .getOrNull()
            }
        }
    }
    var locallySentMessages by remember(chat.id, activeTopicId) { mutableStateOf<List<com.facevideo.app.data.FaceVideoMessage>>(emptyList()) }
    var locallyEditedMessages by remember(chat.id, activeTopicId) {
        mutableStateOf<Map<String, com.facevideo.app.data.FaceVideoMessage>>(emptyMap())
    }
    LaunchedEffect(messages, locallySentMessages, locallyEditedMessages) {
        if (locallySentMessages.isNotEmpty()) {
            val serverIds = messages.mapTo(mutableSetOf()) { it.id }
            locallySentMessages = locallySentMessages.filterNot { it.id in serverIds }
        }
        if (locallyEditedMessages.isNotEmpty()) {
            val confirmedIds = locallyEditedMessages.filter { (id, localMessage) ->
                val serverMessage = messages.firstOrNull { it.id == id }
                serverMessage?.body == localMessage.body &&
                    serverMessage.editedAt == localMessage.editedAt
            }.keys
            if (confirmedIds.isNotEmpty()) {
                locallyEditedMessages = locallyEditedMessages - confirmedIds
            }
        }
    }
    val cachedReceipts = remember(cache, userId, chat.id) {
        cache.readMessageReceipts(userId, chat.id)
    }
    val receiptsFlow = remember(repository, chat.id) { repository.observeMessageReceipts(chat.id) }
    val receipts by receiptsFlow.collectAsState(initial = cachedReceipts)
    LaunchedEffect(cache, userId, chat.id, receipts) {
        withContext(Dispatchers.IO) { cache.saveMessageReceipts(userId, chat.id, receipts) }
    }

    val cachedMessageReactions = remember(cache, userId, chat.id) {
        cache.readMessageReactions(userId, chat.id)
    }
    val messageReactionsFlow = remember(repository, chat.id) {
        repository.observeMessageReactions(chat.id)
    }
    val messageReactionRows by messageReactionsFlow.collectAsState(initial = cachedMessageReactions)
    LaunchedEffect(cache, userId, chat.id, messageReactionRows) {
        withContext(Dispatchers.IO) { cache.saveMessageReactions(userId, chat.id, messageReactionRows) }
    }
    val reactionSummaries = remember(messageReactionRows, userId) {
        summarizeMessageReactions(
            messageReactionRows.map { row ->
                MessageReactionEntry(
                    messageId = row.messageId,
                    userId = row.userId,
                    reaction = row.reaction
                )
            },
            currentUserId = userId
        )
    }
    var hiddenMessageIds by remember(chat.id, activeTopicId) { mutableStateOf<Set<String>>(emptySet()) }
    // Keep the entire group's message cache intact while rendering only the selected topic.
    val allChatMessages = remember(messages, locallySentMessages, locallyEditedMessages) {
        mergeChatMessageSnapshots(messages, locallySentMessages)
            .map { locallyEditedMessages[it.id] ?: it }
    }
    // Realtime rows can arrive in primary-key order rather than message time order.
    val orderedMessages = remember(allChatMessages, activeTopicId, hiddenMessageIds) {
        allChatMessages
            .filter { it.topicId == activeTopicId }
            .filterNot { it.id in hiddenMessageIds }
    }
    // Keep the durable call log separate from ordinary messages and their actions/cache.
    val timelineMessages = remember(orderedMessages, callHistory, chat.isGroup, activeTopicId) {
        val callEvents = if (!chat.isGroup && activeTopicId == null) {
            callHistory.map { call ->
                com.facevideo.app.data.FaceVideoMessage(
                    id = "call-${call.callId}",
                    chatId = call.chatId,
                    senderId = call.callerId,
                    body = "",
                    createdAt = call.createdAt,
                    messageType = "call",
                    callId = call.callId,
                    callMediaType = call.mediaType,
                    callStatus = call.status,
                    callDurationSeconds = call.durationSeconds
                )
            }
        } else emptyList()
        sortChatMessagesChronologically(orderedMessages + callEvents)
    }
    LaunchedEffect(userId, chat.id, allChatMessages) {
        withContext(Dispatchers.IO) {
            cache.saveMessages(userId, chat.id, allChatMessages)
        }
    }
    // Keep each chat/topic's scroll position independent when the user switches conversations.
    val listState = androidx.compose.runtime.key(chat.id, activeTopicId) {
        androidx.compose.foundation.lazy.rememberLazyListState()
    }
    // Track the actual message viewport size: the keyboard resizes this area on devices
    // where the current Compose version does not expose a direct IME visibility property.
    var messageViewportHeight by remember(chat.id, activeTopicId) { mutableStateOf(0) }
    val composerFocusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    var draft by remember(chat.id, activeTopicId, userId, cache) {
        mutableStateOf(cache.readDraft(userId, draftCacheKey))
    }
    var replyingTo by remember(chat.id, activeTopicId) { mutableStateOf<com.facevideo.app.data.FaceVideoMessage?>(null) }
    var editingMessage by remember(chat.id, activeTopicId) { mutableStateOf<com.facevideo.app.data.FaceVideoMessage?>(null) }
    var editingScheduledMessage by remember(chat.id, activeTopicId) {
        mutableStateOf<FaceVideoScheduledMessage?>(null)
    }

    // Keep the latest composer values available to the lifecycle callback without
    // re-registering it for every character typed.
    val latestDraft by rememberUpdatedState(draft)
    val latestDraftCacheKey by rememberUpdatedState(draftCacheKey)
    val latestEditingMessage by rememberUpdatedState(editingMessage)
    val latestEditingScheduledMessage by rememberUpdatedState(editingScheduledMessage)
    val composerContext = LocalContext.current
    // Resolve the host Activity directly; this avoids relying on an unavailable
    // ViewTreeLifecycleOwner API in the app's current AndroidX dependency set.
    val composerLifecycleOwner = remember(composerContext) {
        generateSequence(composerContext as Context) { current ->
            (current as? ContextWrapper)?.baseContext
        }.filterIsInstance<ComponentActivity>().firstOrNull()
    }
    DisposableEffect(composerLifecycleOwner, userId, chat.id, draftCacheKey, cache) {
        val observer = LifecycleEventObserver { _, event ->
            if (
                event == Lifecycle.Event.ON_PAUSE &&
                latestEditingMessage == null &&
                latestEditingScheduledMessage == null
            ) {
                cache.flushDraft(userId, latestDraftCacheKey, latestDraft)
            }
        }
        composerLifecycleOwner?.lifecycle?.addObserver(observer)
        onDispose {
            composerLifecycleOwner?.lifecycle?.removeObserver(observer)
            if (latestEditingMessage == null && latestEditingScheduledMessage == null) {
                cache.flushDraft(userId, latestDraftCacheKey, latestDraft)
            }
        }
    }

    // Backstop for programmatic changes (for example, clearing after a successful send).
    SideEffect {
        if (editingMessage == null && editingScheduledMessage == null) {
            cache.saveDraft(userId, draftCacheKey, draft)
        }
    }
    var selectedMessageIds by remember(chat.id, activeTopicId) { mutableStateOf<Set<String>>(emptySet()) }
    var showGroupManagement by remember(chat.id) { mutableStateOf(false) }
    var showGroupTopics by remember(chat.id) { mutableStateOf(false) }
    var showDeleteSelection by remember { mutableStateOf(false) }
    var showForwardPicker by remember { mutableStateOf(false) }
    // Exit message-selection mode before the parent screen handles system Back.
    BackHandler(
        enabled = selectedMessageIds.isNotEmpty() && !showDeleteSelection && !showForwardPicker
    ) {
        selectedMessageIds = emptySet()
    }

    BackHandler(
        enabled = chat.isGroup &&
            selectedMessageIds.isEmpty() &&
            (showGroupManagement || showGroupTopics || activeTopic != null)
    ) {
        when {
            showGroupManagement -> showGroupManagement = false
            showGroupTopics -> showGroupTopics = false
            activeTopic != null -> {
                activeTopic = null
                showGroupTopics = true
            }
        }
    }
    var forwardTargets by remember { mutableStateOf<List<com.facevideo.app.data.FaceVideoChatSummary>>(emptyList()) }
    var actionBusy by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isSending by remember { mutableStateOf(false) }
    var notificationMuteBusy by remember(chat.id) { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var scheduledMessages by remember(chat.id, cache, userId) {
        mutableStateOf(cache.readScheduledMessages(userId, chat.id))
    }
    var showScheduledMessages by remember(chat.id) { mutableStateOf(false) }

    suspend fun reloadScheduledMessages() {
        val loaded = repository.listScheduledMessages(chat.id)
        scheduledMessages = loaded
        withContext(Dispatchers.IO) {
            cache.saveScheduledMessages(userId, chat.id, loaded)
        }
    }

    fun openScheduledMessages() {
        if (activeTopicId != null) {
            Toast.makeText(context, "Отложенные сообщения пока доступны в «Общих».", Toast.LENGTH_SHORT).show()
            return
        }
        showScheduledMessages = true
        scope.launch {
            runCatching { reloadScheduledMessages() }
                .onSuccess { showScheduledMessages = true }
                .onFailure {
                    Toast.makeText(
                        context,
                        it.message ?: "Не удалось загрузить отложенные сообщения",
                        Toast.LENGTH_SHORT
                    ).show()
                }
        }
    }

    fun scheduleCurrentDraft() {
        if (activeTopicId != null) {
            Toast.makeText(context, "Отложенные сообщения пока доступны в «Общих».", Toast.LENGTH_SHORT).show()
            return
        }
        val textToSchedule = draft.trim()
        if (textToSchedule.isBlank() || isSending) return
        showScheduledMessages = false
        showArturChatScheduleDateTimePicker(
            context = context,
            initialTimeMillis = System.currentTimeMillis() + 60L * 60L * 1000L
        ) { selectedTime ->
            scope.launch {
                isSending = true
                runCatching {
                    repository.createScheduledMessage(
                        chatId = chat.id,
                        text = textToSchedule,
                        scheduledFor = Instant.ofEpochMilli(selectedTime).toString()
                    )
                }.onSuccess {
                    draft = ""
                    cache.saveDraft(userId, draftCacheKey, "")
                    reloadScheduledMessages()
                    Toast.makeText(context, "Сообщение запланировано", Toast.LENGTH_SHORT).show()
                }.onFailure { error ->
                    Toast.makeText(
                        context,
                        error.message ?: "Не удалось запланировать сообщение",
                        Toast.LENGTH_LONG
                    ).show()
                }
                isSending = false
            }
        }
    }

    fun rescheduleMessage(item: FaceVideoScheduledMessage) {
        showScheduledMessages = false
        val initialTime = runCatching {
            Instant.parse(item.scheduledFor).toEpochMilli()
        }.getOrDefault(System.currentTimeMillis() + 60L * 60L * 1000L)
        showArturChatScheduleDateTimePicker(context, initialTime) { selectedTime ->
            scope.launch {
                runCatching {
                    repository.updateScheduledMessage(
                        id = item.id,
                        text = item.body,
                        scheduledFor = Instant.ofEpochMilli(selectedTime).toString()
                    )
                }.onSuccess {
                    reloadScheduledMessages()
                    Toast.makeText(context, "Время отправки изменено", Toast.LENGTH_SHORT).show()
                }.onFailure { error ->
                    Toast.makeText(
                        context,
                        error.message ?: "Не удалось перенести сообщение",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    fun saveEditedScheduledMessage() {
        val item = editingScheduledMessage ?: return
        val textToSave = draft.trim()
        if (textToSave.isBlank() || isSending) return
        scope.launch {
            isSending = true
            runCatching {
                repository.updateScheduledMessage(
                    id = item.id,
                    text = textToSave,
                    scheduledFor = item.scheduledFor
                )
            }.onSuccess {
                // Scheduled-message editing is separate from the unsent composer draft.
                // Restore the user's previous ordinary draft instead of clearing it.
                draft = cache.readDraft(userId, draftCacheKey)
                editingScheduledMessage = null
                reloadScheduledMessages()
                Toast.makeText(context, "Отложенное сообщение изменено", Toast.LENGTH_SHORT).show()
            }.onFailure { error ->
                Toast.makeText(
                    context,
                    error.message ?: "Не удалось изменить сообщение",
                    Toast.LENGTH_LONG
                ).show()
            }
            isSending = false
        }
    }

    fun deleteScheduledMessage(item: FaceVideoScheduledMessage) {
        scope.launch {
            runCatching { repository.cancelScheduledMessage(item.id) }
                .onSuccess {
                    reloadScheduledMessages()
                    Toast.makeText(context, "Отложенное сообщение удалено", Toast.LENGTH_SHORT).show()
                }
                .onFailure { error ->
                    Toast.makeText(
                        context,
                        error.message ?: "Не удалось удалить сообщение",
                        Toast.LENGTH_LONG
                    ).show()
                }
        }
    }

    LaunchedEffect(repository, chat.id, userId, cache) {
        runCatching { reloadScheduledMessages() }
            .onFailure {
                android.util.Log.w("ArturChatScheduled", "Could not refresh scheduled messages", it)
            }
    }
    val voiceRecorder = remember(chat.id) { ChatVoiceRecorder(context.cacheDir) }
    var showVideoNoteRecorder by remember(chat.id) { mutableStateOf(false) }
    var isVoiceRecording by remember(chat.id) { mutableStateOf(false) }
    var voiceRecordingStartedAt by remember(chat.id) { mutableStateOf(0L) }
    var voiceRecordingDurationMs by remember(chat.id) { mutableStateOf(0L) }
    val voiceAmplitudeSamples = remember(chat.id) { mutableStateListOf<Int>() }

    DisposableEffect(chat.id, voiceRecorder) {
        onDispose { voiceRecorder.cancel() }
    }

    LaunchedEffect(chat.id, orderedMessages.lastOrNull()?.id) {
        runCatching { repository.markChatRead(chat.id) }
            .onSuccess { onMarkedRead() }
            .onFailure {
                android.util.Log.w("ArturChatUnread", "Could not mark chat as read", it)
            }
    }

    // Pin directly to the final item of the list that is actually rendered.
    // scrollToItem is immediate (no animation), so opening the chat, receiving/sending
    // a message, or resizing the viewport for the keyboard never causes a visible glide.
    // Draft text changes are deliberately not a trigger: typing cannot move the timeline.
    val newestTimelineMessage = timelineMessages.lastOrNull()
    // The anchor is the newest message in the whole conversation, regardless of sender.
    // Include sender and timestamp as well as id so both outgoing and incoming last messages
    // explicitly trigger the same bottom-pinning behavior.
    LaunchedEffect(
        chat.id,
        activeTopicId,
        timelineMessages.size,
        newestTimelineMessage?.id,
        newestTimelineMessage?.senderId,
        newestTimelineMessage?.createdAt,
        newestTimelineMessage?.messageType,
        messageViewportHeight
    ) {
        if (timelineMessages.isNotEmpty() && messageViewportHeight > 0) {
            listState.scrollToItem(timelineMessages.lastIndex)
        }
    }

    fun toggleChatNotifications() {
        if (notificationMuteBusy) return
        val nextMuted = !notificationsMuted
        scope.launch {
            notificationMuteBusy = true
            runCatching {
                repository.setChatNotificationsMuted(chat.id, nextMuted)
            }.onSuccess {
                onNotificationMuteChanged(nextMuted)
                Toast.makeText(
                    context,
                    if (nextMuted) "Уведомления отключены" else "Уведомления включены",
                    Toast.LENGTH_SHORT
                ).show()
            }.onFailure { error ->
                errorMessage = error.message ?: "Не удалось изменить настройки уведомлений."
                Toast.makeText(context, "Не удалось изменить уведомления", Toast.LENGTH_SHORT).show()
                android.util.Log.w("ArturChatNotifications", "Could not update chat mute setting", error)
            }
            notificationMuteBusy = false
        }
    }

    fun sendMessage() {
        val text = draft.trim()
        if (text.isBlank() || isSending) return

        if (editingScheduledMessage != null && editingMessage == null) {
            saveEditedScheduledMessage()
            return
        }

        val messageBeingEdited = editingMessage
        if (messageBeingEdited != null) {
            if (text == messageBeingEdited.body.trim()) {
                draft = ""
                editingMessage = null
                return
            }
            scope.launch {
                isSending = true
                errorMessage = null
                try {
                    val updatedMessage = repository.editMessage(
                        messageId = messageBeingEdited.id,
                        chatId = chat.id,
                        text = text
                    )
                    locallyEditedMessages = locallyEditedMessages + (updatedMessage.id to updatedMessage)
                    draft = ""
                    editingMessage = null
                    replyingTo = null
                    Toast.makeText(context, "Сообщение изменено", Toast.LENGTH_SHORT).show()
                } catch (error: Exception) {
                    errorMessage = error.message ?: "Не удалось изменить сообщение."
                } finally {
                    isSending = false
                }
            }
            return
        }

        scope.launch {
            isSending = true
            errorMessage = null
            try {
                val persistedMessage = repository.sendText(
                    chatId = chat.id,
                    text = text,
                    replyToMessageId = replyingTo?.id,
                    topicId = activeTopicId
                )
                locallySentMessages = mergeChatMessageSnapshots(
                    locallySentMessages,
                    listOf(persistedMessage)
                )
                draft = ""
                replyingTo = null
            } catch (error: Exception) {
                errorMessage = error.message ?: "Не удалось отправить сообщение."
            } finally {
                isSending = false
            }
        }
    }

    fun sendImage(uri: Uri) {
        if (isSending) return
        scope.launch {
            isSending = true
            errorMessage = null
            var stage = "Подготовка выбранного изображения"
            try {
                val prepared = withContext(Dispatchers.IO) {
                    prepareChatImage(context, uri)
                }
                stage = "Загрузка изображения в защищённое хранилище"
                val path = repository.uploadChatImage(
                    chatId = chat.id,
                    senderId = userId,
                    bytes = prepared.bytes
                )
                stage = "Сохранение сообщения в переписке"
                val persistedMessage = repository.sendImage(
                    chatId = chat.id,
                    imagePath = path,
                    fileName = prepared.fileName,
                    fileSize = prepared.bytes.size.toLong(),
                    replyToMessageId = replyingTo?.id,
                    topicId = activeTopicId
                )
                locallySentMessages = mergeChatMessageSnapshots(
                    locallySentMessages,
                    listOf(persistedMessage)
                )
                replyingTo = null
                Toast.makeText(context, "Фото отправлено", Toast.LENGTH_SHORT).show()
            } catch (error: Exception) {
                android.util.Log.e("ArturChatMedia", "Image send failed at stage: $stage", error)
                errorMessage = "$stage: ${error.message ?: error.javaClass.simpleName}"
            } finally {
                isSending = false
            }
        }
    }

    val imagePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) sendImage(uri)
    }

    fun startVoiceRecording() {
        if (isSending || isVoiceRecording) return
        try {
            voiceRecorder.start()
            voiceRecordingStartedAt = SystemClock.elapsedRealtime()
            voiceRecordingDurationMs = 0L
            voiceAmplitudeSamples.clear()
            isVoiceRecording = true
            errorMessage = null
        } catch (error: Exception) {
            android.util.Log.e("ArturChatVoice", "Could not start recording", error)
            errorMessage = error.message ?: "Не удалось начать запись. Проверьте доступ к микрофону."
        }
    }

    val voicePermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) startVoiceRecording()
        else errorMessage = "Для записи голосовых сообщений нужен доступ к микрофону."
    }

    val videoNotePermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val cameraGranted = permissions[Manifest.permission.CAMERA] == true ||
            context.checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        val microphoneGranted = permissions[Manifest.permission.RECORD_AUDIO] == true ||
            context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        if (cameraGranted && microphoneGranted) {
            showVideoNoteRecorder = true
            errorMessage = null
        } else {
            errorMessage = "Для круглых видеосообщений нужны доступы к камере и микрофону."
        }
    }

    fun openVideoNoteRecorder() {
        if (isSending || isVoiceRecording) return
        val missingPermissions = buildList {
            if (context.checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                add(Manifest.permission.CAMERA)
            }
            if (context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                add(Manifest.permission.RECORD_AUDIO)
            }
        }
        if (missingPermissions.isEmpty()) {
            showVideoNoteRecorder = true
        } else {
            videoNotePermissionLauncher.launch(missingPermissions.toTypedArray())
        }
    }

    fun cancelVoiceRecording() {
        if (!isVoiceRecording) return
        isVoiceRecording = false
        voiceRecorder.cancel()
        voiceRecordingDurationMs = 0L
        voiceAmplitudeSamples.clear()
    }

    fun sendVoiceRecording() {
        if (!isVoiceRecording || isSending) return
        val durationMs = (SystemClock.elapsedRealtime() - voiceRecordingStartedAt).coerceAtLeast(0L)
        val recorderSamplesFallback = voiceAmplitudeSamples.toList()
        isVoiceRecording = false
        voiceRecordingDurationMs = durationMs
        isSending = true
        errorMessage = null
        scope.launch {
            var recordedFile: File? = null
            try {
                val completedFile = withContext(Dispatchers.IO) { voiceRecorder.stop() }
                recordedFile = completedFile
                require(durationMs >= 1_000L) { "Запишите голосовое сообщение хотя бы одну секунду." }
                val waveform = withContext(Dispatchers.IO) {
                    decodeVoiceWaveformFromFile(completedFile)
                } ?: buildVoiceWaveform(recorderSamplesFallback)
                val audioBytes = withContext(Dispatchers.IO) { completedFile.readBytes() }
                val audioPath = repository.uploadChatVoice(chat.id, userId, audioBytes)
                val persistedMessage = repository.sendVoiceMessage(
                    chatId = chat.id,
                    audioPath = audioPath,
                    fileName = "voice-message.m4a",
                    fileSize = audioBytes.size.toLong(),
                    durationMs = durationMs,
                    waveform = waveform,
                    replyToMessageId = replyingTo?.id,
                    topicId = activeTopicId
                )
                locallySentMessages = mergeChatMessageSnapshots(locallySentMessages, listOf(persistedMessage))
                replyingTo = null
                Toast.makeText(context, "Голосовое сообщение отправлено", Toast.LENGTH_SHORT).show()
            } catch (error: Exception) {
                android.util.Log.e("ArturChatVoice", "Voice send failed", error)
                errorMessage = error.message ?: "Не удалось отправить голосовое сообщение."
            } finally {
                withContext(Dispatchers.IO) { recordedFile?.delete() }
                isSending = false
                voiceRecordingDurationMs = 0L
                voiceAmplitudeSamples.clear()
            }
        }
    }

    fun sendVideoNote(file: File, durationMs: Long) {
        if (isSending) return
        isSending = true
        showVideoNoteRecorder = false
        errorMessage = null
        scope.launch {
            var stage = "Подготовка видеосообщения"
            try {
                require(durationMs in 1_000L..MAX_CHAT_VIDEO_NOTE_DURATION_MS) {
                    "Запишите кружочек длительностью от 1 до 60 секунд."
                }
                val videoBytes = withContext(Dispatchers.IO) { file.readBytes() }
                require(videoBytes.isNotEmpty() && videoBytes.size <= MAX_CHAT_VIDEO_NOTE_BYTES) {
                    "Видеосообщение должно быть не больше 10 МБ."
                }
                stage = "Загрузка видеосообщения в защищённое хранилище"
                val path = repository.uploadChatVideoNote(
                    chatId = chat.id,
                    senderId = userId,
                    bytes = videoBytes
                )
                stage = "Сохранение видеосообщения в переписке"
                val persistedMessage = repository.sendVideoNote(
                    chatId = chat.id,
                    videoPath = path,
                    fileSize = videoBytes.size.toLong(),
                    durationMs = durationMs,
                    replyToMessageId = replyingTo?.id,
                    topicId = activeTopicId
                )
                locallySentMessages = mergeChatMessageSnapshots(locallySentMessages, listOf(persistedMessage))
                replyingTo = null
                showVideoNoteRecorder = false
                Toast.makeText(context, "Видеосообщение отправлено", Toast.LENGTH_SHORT).show()
            } catch (error: Exception) {
                android.util.Log.e("ArturChatVideoNote", "Video note send failed at stage: $stage", error)
                errorMessage = "$stage: ${error.message ?: error.javaClass.simpleName}"
            } finally {
                withContext(Dispatchers.IO) { file.delete() }
                isSending = false
            }
        }
    }

    LaunchedEffect(isVoiceRecording, voiceRecordingStartedAt) {
        if (isVoiceRecording) {
            while (isVoiceRecording) {
                val elapsed = (SystemClock.elapsedRealtime() - voiceRecordingStartedAt).coerceAtLeast(0L)
                voiceRecordingDurationMs = elapsed
                voiceAmplitudeSamples.add(voiceRecorder.readAmplitudeLevel())
                if (elapsed >= MAX_CHAT_VOICE_DURATION_MS) {
                    sendVoiceRecording()
                    break
                }
                delay(40L)
            }
        }
    }


    if (chat.isGroup && showGroupManagement) {
        GroupManagementScreen(
            repository = repository,
            cache = cache,
            userId = userId,
            chatId = chat.id,
            onDismiss = { showGroupManagement = false },
            onGroupUpdated = onGroupUpdated,
            onGroupClosed = onGroupClosed
        )
    } else if (chat.isGroup && showGroupTopics) {
        GroupTopicsScreen(
            repository = repository,
            cache = cache,
            userId = userId,
            chatId = chat.id,
            groupTitle = chat.title,
            onDismiss = { showGroupTopics = false },
            onOpenTopic = { destination ->
                activeTopic = destination
                showGroupTopics = false
                selectedMessageIds = emptySet()
                replyingTo = null
                editingMessage = null
                editingScheduledMessage = null
            }
        )
    } else {
        Column(
            Modifier.fillMaxSize()
                .arturChatGradientBackground()
                .imePadding()
        ) {
        Surface(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
            color = Color.Transparent,
            shape = RoundedCornerShape(28.dp),
            shadowElevation = 0.dp
        ) {
            Row(
                Modifier.fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (selectedMessageIds.isNotEmpty()) {
                    ChatChromeActionButton(
                        onClick = { selectedMessageIds = emptySet() },
                        filled = false
                    ) {
                        Text("×", color = InboxAccentStrong, fontSize = 26.sp, fontWeight = FontWeight.Light)
                    }
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            "${selectedMessageIds.size} выбрано",
                            color = InboxPrimary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )
                        Text("Сообщения отмечены", color = InboxMuted, fontSize = 9.sp)
                    }
                } else {
                    ChatChromeActionButton(
                        onClick = {
                            if (activeTopic != null) {
                                activeTopic = null
                                showGroupTopics = true
                            } else {
                                onBack()
                            }
                        },
                        filled = false,
                        transparent = true,
                        size = 40.dp
                    ) {
                        InboxBackGlyph()
                    }
                    Spacer(Modifier.width(9.dp))
                    Box(
                        Modifier.size(48.dp)
                            .background(
                                Brush.linearGradient(
                                    if (appOrangeChocolateTheme.value) listOf(Color(0xFFFFC27A), Color(0xFFFF8D2A), Color(0xFFA84512)) else listOf(Color(0xFF32ADFF), Color(0xFF596BE8), Color(0xFF9A68E9))
                                ),
                                CircleShape
                            )
                            .padding(2.dp)
                    ) {
                        if (chat.isGroup) {
                            if (!chat.avatarUrl.isNullOrBlank()) {
                                Avatar(
                                    initials = chat.title.take(1).uppercase().ifBlank { "G" },
                                    style = 1,
                                    modifier = Modifier.fillMaxSize().clickable { showGroupManagement = true },
                                    imageUrl = chat.avatarUrl,
                                    imageCacheKey = "group:${chat.id}"
                                )
                            } else {
                                ArturChatTypeAvatar(
                                    ArturChatIconType.GROUP,
                                    Modifier.fillMaxSize().clickable { showGroupManagement = true }
                                )
                            }
                        } else {
                            Avatar(
                                chat.title.take(2).uppercase(),
                                chat.id.hashCode().ushr(1) % 4,
                                Modifier.fillMaxSize().clip(CircleShape)
                                    .clickable(enabled = peerUserId != null) {
                                        peerUserId?.let(onOpenProfile)
                                    },
                                imageUrl = peerAvatarUrl,
                                imageCacheKey = peerUserId
                            )
                        }
                    }
                    Spacer(Modifier.width(10.dp))
                    Column(
                        Modifier.weight(1f).clickable(enabled = chat.isGroup || peerUserId != null) {
                            if (chat.isGroup) showGroupTopics = true
                            else peerUserId?.let(onOpenProfile)
                        }
                    ) {
                        Text(
                            activeTopic?.title ?: chat.title,
                            color = InboxPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(3.dp))
                        val peerIsOnline = peerUserId != null && peerUserId in onlineUserIds
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                Modifier.size(6.dp).background(
                                    if (peerIsOnline) InboxOnline else InboxMuted.copy(alpha = 0.68f),
                                    CircleShape
                                )
                            )
                            Spacer(Modifier.width(5.dp))
                            Text(
                                when {
                                    chat.isGroup && activeTopic != null -> "Тема · ${chat.title}"
                                    chat.isGroup -> "Нажмите, чтобы открыть темы"
                                    peerUserId == null -> "Проверяем статус…"
                                    peerIsOnline -> "В сети"
                                    !presenceAvailable -> "Статус обновляется…"
                                    else -> "Не в сети"
                                },
                                color = if (peerIsOnline) InboxOnline else InboxMuted,
                                fontSize = 9.sp,
                                maxLines = 1
                            )
                        }
                    }
                    Spacer(Modifier.width(7.dp))
                    ChatChromeActionButton(
                        onClick = { toggleChatNotifications() },
                        filled = false,
                        transparent = true,
                        size = 40.dp
                    ) {
                        ChatNotificationGlyph(
                            muted = notificationsMuted,
                            tint = InboxAccentStrong
                        )
                    }
                    if (chat.isGroup) {
                        Spacer(Modifier.width(4.dp))
                        ChatChromeActionButton(
                            onClick = { showGroupTopics = true },
                            filled = false,
                            transparent = true,
                            size = 40.dp
                        ) {
                            Text("☷", color = InboxAccentStrong, fontSize = 21.sp)
                        }
                        Spacer(Modifier.width(4.dp))
                        ChatChromeActionButton(
                            onClick = { showGroupManagement = true },
                            filled = false,
                            transparent = true,
                            size = 40.dp
                        ) {
                            Text("⋯", color = InboxAccentStrong, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Spacer(Modifier.width(4.dp))
                        ChatChromeActionButton(onClick = { onCall(false) }, filled = false, transparent = true, size = 40.dp) {
                            ChatCallGlyph(video = false, tint = InboxAccentStrong)
                        }
                        Spacer(Modifier.width(4.dp))
                        ChatChromeActionButton(onClick = { onCall(true) }, filled = false, transparent = true, size = 40.dp) {
                            ChatCallGlyph(video = true, tint = InboxAccentStrong)
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(3.dp))

        // Date separators follow the device's local calendar day and roll over at midnight.
        val chatTimeZone = remember { java.time.ZoneId.systemDefault() }
        var chatToday by remember(chatTimeZone) {
            mutableStateOf(java.time.LocalDate.now(chatTimeZone))
        }
        LaunchedEffect(chatTimeZone) {
            while (true) {
                val now = java.time.ZonedDateTime.now(chatTimeZone)
                val nextMidnight = now.toLocalDate().plusDays(1).atStartOfDay(chatTimeZone)
                val millisUntilMidnight = java.time.Duration.between(now, nextMidnight)
                    .toMillis()
                    .coerceAtLeast(1L)
                delay(millisUntilMidnight)
                chatToday = java.time.LocalDate.now(chatTimeZone)
            }
        }

        if (timelineMessages.isEmpty()) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("Это начало вашей переписки. Напиши первое сообщение.", color = Muted, fontSize = 12.sp)
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f).fillMaxWidth()
                    .onSizeChanged { size ->
                        if (messageViewportHeight != size.height) {
                            messageViewportHeight = size.height
                        }
                    }
                    .pointerInput(selectedMessageIds.isNotEmpty()) {
                        if (selectedMessageIds.isNotEmpty()) {
                            var horizontalDragDistance = 0f
                            detectHorizontalDragGestures(
                                onDragStart = { horizontalDragDistance = 0f },
                                onHorizontalDrag = { change, dragAmount ->
                                    horizontalDragDistance += dragAmount
                                    change.consume()
                                },
                                onDragEnd = {
                                    if (horizontalDragDistance <= -64.dp.toPx()) {
                                        selectedMessageIds = emptySet()
                                    }
                                    horizontalDragDistance = 0f
                                },
                                onDragCancel = { horizontalDragDistance = 0f }
                            )
                        }
                    },
                contentPadding = PaddingValues(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                itemsIndexed(timelineMessages, key = { _, message -> message.id }) { index, message ->
                    val messageDate = parseCallInstant(message.createdAt)
                        ?.atZone(chatTimeZone)
                        ?.toLocalDate()
                    val previousMessageDate = if (index > 0) {
                        parseCallInstant(timelineMessages[index - 1].createdAt)
                            ?.atZone(chatTimeZone)
                            ?.toLocalDate()
                    } else null

                    Column(Modifier.fillMaxWidth()) {
                        if (messageDate != null && (index == 0 || messageDate != previousMessageDate)) {
                            ChatDateDivider(formatChatDateDivider(messageDate, chatToday))
                        }

                        if (message.messageType == "call") {
                            CallHistoryBubble(
                                mediaType = message.callMediaType ?: "audio",
                                status = message.callStatus ?: "ended",
                                durationSeconds = message.callDurationSeconds ?: 0,
                                isOutgoing = message.senderId == userId,
                                time = formatChatMessageTime(message.createdAt)
                            )
                        } else {
                    val startsNewSenderGroup = index > 0 &&
                        timelineMessages[index - 1].messageType != "call" &&
                        timelineMessages[index - 1].senderId != message.senderId
                    val followsSameSender = index > 0 &&
                        timelineMessages[index - 1].messageType != "call" &&
                        !startsNewSenderGroup
                    val repliedMessage = message.replyToMessageId
                        ?.let { replyId -> timelineMessages.firstOrNull { it.id == replyId } }
                    val replyPreview = repliedMessage?.let { original ->
                        MessageReplyPreview(
                            sender = if (original.senderId == userId) "Вы"
                                else if (chat.isGroup) "Участник" else chat.title,
                            text = original.body
                        )
                    }
                    MessageBubble(
                        message = ChatMessage(
                            id = message.id,
                            text = if (message.deletedAt != null) "Это сообщение удалено" else message.body,
                            time = formatChatMessageTime(message.createdAt),
                            isMine = message.senderId == userId,
                            deliveryStatus = if (message.senderId == userId && message.deletedAt == null) resolveMessageDeliveryStatus(message.id, receipts) else null,
                            forwardedFromName = message.forwardedFromName,
                            isDeleted = message.deletedAt != null,
                            isEdited = message.editedAt != null && message.deletedAt == null,
                            attachmentMimeType = if (message.deletedAt == null) message.attachmentMimeType else null,
                            attachmentDurationMs = if (message.deletedAt == null) message.attachmentDurationMs else null,
                            attachmentWaveform = if (message.deletedAt == null) message.attachmentWaveform else null
                        ),
                        selected = message.id in selectedMessageIds,
                        onSelect = if (message.deletedAt == null) ({ selectedMessageIds = selectedMessageIds + message.id }) else null,
                        onToggleSelection = if (message.deletedAt == null && selectedMessageIds.isNotEmpty()) ({ selectedMessageIds = if (message.id in selectedMessageIds) selectedMessageIds - message.id else selectedMessageIds + message.id }) else null,
                        showTime = shouldShowChatMessageTime(timelineMessages, index) || message.editedAt != null,
                        separateFromPrevious = startsNewSenderGroup,
                        sameSenderPrevious = followsSameSender,
                        replyPreview = replyPreview,
                        attachmentPath = if (message.deletedAt == null) message.attachmentPath else null,
                        attachmentRepository = repository,
                        reactions = if (message.deletedAt == null) reactionSummaries[message.id].orEmpty() else emptyList(),
                        onReaction = if (message.deletedAt == null) ({ reaction ->
                            scope.launch {
                                runCatching {
                                    repository.setMessageReaction(message.id, chat.id, reaction)
                                }.onFailure {
                                    errorMessage = it.message ?: "Не удалось поставить реакцию."
                                }
                            }
                        }) else null,
                        onEdit = if (
                            message.senderId == userId &&
                            message.deletedAt == null &&
                            message.attachmentPath == null &&
                            selectedMessageIds.isEmpty() &&
                            !isVoiceRecording
                        ) ({
                            editingMessage = message
                            replyingTo = null
                            selectedMessageIds = emptySet()
                            draft = message.body
                            composerFocusRequester.requestFocus()
                            keyboardController?.show()
                        }) else null,
                        onReply = if (message.deletedAt == null && selectedMessageIds.isEmpty()) ({ replyingTo = message }) else null,
                        onCopy = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("A-Chat", message.body))
                            Toast.makeText(context, "Текст скопирован", Toast.LENGTH_SHORT).show()
                        },
                        onForward = if (message.deletedAt == null && selectedMessageIds.isEmpty()) ({
                            selectedMessageIds = setOf(message.id)
                            scope.launch {
                                actionBusy = true
                                forwardTargets = runCatching {
                                    repository.listMyChats(userId).filter { it.id != chat.id }
                                }.getOrDefault(emptyList())
                                actionBusy = false
                                showForwardPicker = true
                            }
                        }) else null,
                        onDelete = if (message.deletedAt == null && selectedMessageIds.isEmpty()) ({
                            selectedMessageIds = setOf(message.id)
                            showDeleteSelection = true
                        }) else null,
                        onReplyPreviewClick = repliedMessage?.let { original ->
                            {
                                val targetIndex = timelineMessages.indexOfFirst { it.id == original.id }
                                if (targetIndex >= 0) {
                                    scope.launch { listState.animateScrollToItem(targetIndex) }
                                }
                            }
                        }
                    )
                        }
                    }
                }
            }
        }

        if (showDeleteSelection) {
            val selectedRows = orderedMessages.filter { it.id in selectedMessageIds }
            val allMine = selectedRows.isNotEmpty() && selectedRows.all { it.senderId == userId }
            AlertDialog(
                onDismissRequest = { showDeleteSelection = false },
                title = { Text("Удалить сообщения?") },
                text = { Text("Выбрано: ${selectedRows.size}. Можно скрыть сообщения только у себя.") },
                confirmButton = {
                    Column {
                        TextButton(onClick = {
                            scope.launch {
                                actionBusy = true
                                runCatching { repository.hideMessagesForMe(selectedMessageIds.toList(), userId) }
                                    .onSuccess { hiddenMessageIds = hiddenMessageIds + selectedMessageIds; selectedMessageIds = emptySet(); showDeleteSelection = false }
                                    .onFailure { errorMessage = it.message ?: "Не удалось удалить сообщения." }
                                actionBusy = false
                            }
                        }, enabled = !actionBusy) { Text("Удалить у меня", color = AccentSlate) }
                        if (allMine) TextButton(onClick = {
                            scope.launch {
                                actionBusy = true
                                runCatching { selectedRows.forEach { repository.deleteMessageForEveryone(it.id, userId) } }
                                    .onSuccess { selectedMessageIds = emptySet(); showDeleteSelection = false }
                                    .onFailure { errorMessage = it.message ?: "Не удалось удалить у всех." }
                                actionBusy = false
                            }
                        }, enabled = !actionBusy) { Text("Удалить у всех", color = Color(0xFFA85F6E)) }
                    }
                },
                dismissButton = { TextButton(onClick = { showDeleteSelection = false }) { Text("Отмена") } }
            )
        }
        if (showForwardPicker) {
            AlertDialog(
                onDismissRequest = { showForwardPicker = false },
                title = { Text("Переслать в чат") },
                text = {
                    Column(Modifier.heightIn(max = 360.dp).verticalScroll(rememberScrollState())) {
                        if (forwardTargets.isEmpty()) Text("Нет других чатов", color = Muted)
                        forwardTargets.forEach { target ->
                            TextButton(onClick = {
                                scope.launch {
                                    actionBusy = true
                                    runCatching {
                                        orderedMessages.filter { it.id in selectedMessageIds }.forEach { source ->
                                            repository.forwardMessage(
                                                source, target.id, userId,
                                                if (source.senderId == userId) "Вы" else chat.title
                                            )
                                        }
                                    }.onSuccess {
                                        Toast.makeText(context, "Сообщения пересланы", Toast.LENGTH_SHORT).show()
                                        selectedMessageIds = emptySet()
                                        showForwardPicker = false
                                    }.onFailure { errorMessage = it.message ?: "Не удалось переслать сообщения." }
                                    actionBusy = false
                                }
                            }, enabled = !actionBusy) { Text(target.title, color = MainText) }
                        }
                    }
                },
                confirmButton = { TextButton(onClick = { showForwardPicker = false }) { Text("Закрыть") } }
            )
        }

        if (errorMessage != null) {
            Text(errorMessage.orEmpty(), color = Color(0xFFA85F6E), fontSize = 10.sp, modifier = Modifier.padding(horizontal = 16.dp))
        }

        if (selectedMessageIds.isNotEmpty()) {
            val selectedRows = orderedMessages.filter { it.id in selectedMessageIds }
            Surface(
                color = ThemeSurface.copy(alpha = if (isDarkAppearance) 0.98f else 0.90f),
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, ThemeBorder),
                shadowElevation = 5.dp,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 5.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (selectedRows.size == 1) {
                        SelectionActionButton(
                            icon = "↩",
                            label = "Ответить",
                            color = AccentSlate,
                            modifier = Modifier.weight(1f)
                        ) {
                            replyingTo = selectedRows.single()
                            selectedMessageIds = emptySet()
                        }
                    }
                    SelectionActionButton(
                        icon = "▢",
                        label = "Копировать",
                        color = AccentSlate,
                        modifier = Modifier.weight(1f)
                    ) {
                        val selectedText = selectedRows.joinToString("\n") { it.body }
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("A-Chat", selectedText))
                        Toast.makeText(context, "Текст скопирован", Toast.LENGTH_SHORT).show()
                        selectedMessageIds = emptySet()
                    }
                    SelectionActionButton(
                        icon = "➦",
                        label = "Переслать",
                        color = AccentSlate,
                        modifier = Modifier.weight(1f)
                    ) {
                        scope.launch {
                            actionBusy = true
                            forwardTargets = runCatching {
                                repository.listMyChats(userId).filter { it.id != chat.id }
                            }.getOrDefault(emptyList())
                            actionBusy = false
                            showForwardPicker = true
                        }
                    }
                    SelectionActionButton(
                        icon = "⌫",
                        label = "Удалить",
                        color = Color(0xFFA85F6E),
                        modifier = Modifier.weight(1f)
                    ) {
                        showDeleteSelection = true
                    }
                }
            }
        }

        if (showVideoNoteRecorder) {
            ChatVideoNoteRecorderDialog(
                onDismiss = { showVideoNoteRecorder = false },
                onSend = { file, durationMs -> sendVideoNote(file, durationMs) }
            )
        }

        if (showScheduledMessages) {
            AlertDialog(
                onDismissRequest = { showScheduledMessages = false },
                title = {
                    Text("Отложенные сообщения", color = InboxPrimary, fontWeight = FontWeight.SemiBold)
                },
                text = {
                    Column(
                        Modifier.fillMaxWidth()
                            .heightIn(max = 420.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            "Сообщения отправляются автоматически в выбранное время, даже если A-Chat закрыт.",
                            color = InboxSecondary,
                            fontSize = 12.sp,
                            lineHeight = 17.sp
                        )
                        Spacer(Modifier.height(12.dp))
                        if (scheduledMessages.isEmpty()) {
                            Text(
                                "Запланированных сообщений пока нет.",
                                color = InboxMuted,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(vertical = 12.dp)
                            )
                        }
                        scheduledMessages.forEach { item ->
                            Surface(
                                modifier = Modifier.fillMaxWidth().padding(bottom = 9.dp),
                                color = ThemeSurface.copy(alpha = if (isDarkAppearance) 0.98f else 0.78f),
                                shape = RoundedCornerShape(15.dp),
                                border = BorderStroke(1.dp, InboxBorder)
                            ) {
                                Column(Modifier.padding(11.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        ChatScheduleGlyph()
                                        Spacer(Modifier.width(7.dp))
                                        Text(
                                            if (item.status == "failed") "Не удалось отправить"
                                            else formatScheduledMessageTime(item.scheduledFor),
                                            color = if (item.status == "failed") InboxDanger else InboxAccentStrong,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                    Spacer(Modifier.height(6.dp))
                                    Text(
                                        item.body,
                                        color = InboxPrimary,
                                        fontSize = 13.sp,
                                        maxLines = 4,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Row(
                                        Modifier.fillMaxWidth().padding(top = 5.dp),
                                        horizontalArrangement = Arrangement.End,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        if (item.status == "pending") {
                                            TextButton(
                                                onClick = {
                                                    editingScheduledMessage = item
                                                    draft = item.body
                                                    showScheduledMessages = false
                                                }
                                            ) {
                                                Text("Изменить", color = InboxAccentStrong, fontSize = 11.sp)
                                            }
                                            TextButton(onClick = { rescheduleMessage(item) }) {
                                                Text("Перенести", color = InboxAccentStrong, fontSize = 11.sp)
                                            }
                                        }
                                        TextButton(onClick = { deleteScheduledMessage(item) }) {
                                            Text(
                                                "Удалить",
                                                color = InboxDanger,
                                                fontSize = 11.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        if (draft.isNotBlank() && editingScheduledMessage == null && editingMessage == null) {
                            Spacer(Modifier.height(3.dp))
                            Button(
                                onClick = { scheduleCurrentDraft() },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = !isSending
                            ) {
                                Text("Запланировать текущее сообщение")
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showScheduledMessages = false }) {
                        Text("Закрыть", color = InboxAccentStrong)
                    }
                },
                containerColor = Panel
            )
        }

        if (editingMessage != null) {
            val selectedEditingMessage = editingMessage!!
            Row(
                Modifier.fillMaxWidth().padding(start = 14.dp, end = 14.dp, top = 6.dp, bottom = 2.dp)
                    .clip(RoundedCornerShape(12.dp)).background(PanelLight)
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.width(3.dp).height(34.dp).clip(CircleShape).background(AccentSlate))
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        "Редактирование сообщения",
                        color = AccentSlate,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        selectedEditingMessage.body,
                        color = MainText,
                        fontSize = 11.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    "×",
                    color = Muted,
                    fontSize = 22.sp,
                    modifier = Modifier.clickable {
                        editingMessage = null
                        draft = ""
                    }.padding(start = 10.dp)
                )
            }
        }

        if (editingScheduledMessage != null && editingMessage == null) {
            val selectedScheduledMessage = editingScheduledMessage!!
            Row(
                Modifier.fillMaxWidth().padding(start = 14.dp, end = 14.dp, top = 6.dp, bottom = 2.dp)
                    .clip(RoundedCornerShape(12.dp)).background(PanelLight)
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.width(3.dp).height(34.dp).clip(CircleShape).background(InboxAccent))
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        "Изменение отложенного сообщения",
                        color = InboxAccentStrong,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        "Отправка: ${formatScheduledMessageTime(selectedScheduledMessage.scheduledFor)}",
                        color = InboxSecondary,
                        fontSize = 11.sp,
                        maxLines = 1
                    )
                }
                Text(
                    "×",
                    color = Muted,
                    fontSize = 22.sp,
                    modifier = Modifier.clickable {
                        editingScheduledMessage = null
                        draft = cache.readDraft(userId, draftCacheKey)
                    }.padding(start = 10.dp)
                )
            }
        }

        if (replyingTo != null && editingMessage == null) {
            val selectedReply = replyingTo!!
            Row(
                Modifier.fillMaxWidth().padding(start = 14.dp, end = 14.dp, top = 6.dp, bottom = 2.dp)
                    .clip(RoundedCornerShape(12.dp)).background(PanelLight)
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.width(3.dp).height(34.dp).clip(CircleShape).background(AccentSlate))
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        if (selectedReply.senderId == userId) "Ответ себе"
                        else "Ответ: " + if (chat.isGroup) "участнику" else chat.title,
                        color = AccentSlate, fontSize = 10.sp, fontWeight = FontWeight.SemiBold
                    )
                    Text(selectedReply.body, color = MainText, fontSize = 11.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
                Text(
                    "×",
                    color = Muted,
                    fontSize = 22.sp,
                    modifier = Modifier.clickable { replyingTo = null }.padding(start = 10.dp)
                )
            }
        }

        if (isSending) {
            Text(
                "Отправляем сообщение или аудио…",
                color = InboxMuted,
                fontSize = 9.sp,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 2.dp)
            )
        }

        if (isVoiceRecording) {
            Surface(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
                color = ThemeSurface.copy(alpha = if (isDarkAppearance) 0.98f else 0.58f),
                shape = RoundedCornerShape(28.dp),
                border = BorderStroke(1.dp, ThemeBorder),
                shadowElevation = 8.dp
            ) {
                Row(
                    Modifier.fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                when { appOrangeChocolateTheme.value -> listOf(Color(0xFF4A2410), Color(0xFF35180B), Color(0xFF4A2410)); appDarkTheme.value -> listOf(Color(0xFF263247), Color(0xFF1D293B), Color(0xFF263247)); else -> listOf(Color(0xFFFDFEFF), Color(0xFFE7EDFF), Color(0xFFF3EAFF)) }
                            )
                        )
                        .padding(9.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f).padding(horizontal = 8.dp, vertical = 3.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(7.dp).clip(CircleShape).background(Color(0xFFE36C8A)))
                            Spacer(Modifier.width(7.dp))
                            Text("Идёт запись", color = InboxPrimary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.weight(1f))
                            Text(
                                formatVoiceDuration(voiceRecordingDurationMs),
                                color = Color(0xFFCB587A),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(Modifier.height(7.dp))
                        VoiceWaveformBars(
                            waveform = buildVoiceWaveform(voiceAmplitudeSamples.toList()),
                            progress = 1f,
                            modifier = Modifier.fillMaxWidth().height(24.dp)
                        )
                    }
                    Spacer(Modifier.width(6.dp))
                    ChatChromeActionButton(
                        onClick = { cancelVoiceRecording() },
                        filled = false
                    ) {
                        Text("×", color = Color(0xFFCC6681), fontSize = 24.sp, fontWeight = FontWeight.Light)
                    }
                    Spacer(Modifier.width(6.dp))
                    ChatChromeActionButton(
                        onClick = { sendVoiceRecording() },
                        enabled = !isSending,
                        filled = true
                    ) {
                        ChatSendGlyph()
                    }
                }
            }
        } else {
            Surface(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
                color = ThemeSurface.copy(alpha = if (isDarkAppearance) 0.98f else 0.60f),
                shape = RoundedCornerShape(28.dp),
                border = BorderStroke(1.dp, ThemeBorder),
                shadowElevation = 4.dp
            ) {
                Row(
                    Modifier.fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                when { appOrangeChocolateTheme.value -> listOf(Color(0xF2381B0B), Color(0xED2A1308), Color(0xF2381B0B)); appDarkTheme.value -> listOf(Color(0xF21D2736), Color(0xE91A2432), Color(0xF21D2736)); else -> listOf(Color(0xEFFFFFFF), Color(0xD8E8EEFC), Color(0xDDE3EAF8)) }
                            )
                        )
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (editingMessage == null) {
                        ChatChromeActionButton(
                            onClick = { if (!isSending) imagePicker.launch("image/*") },
                            enabled = !isSending,
                            filled = false,
                            size = 40.dp
                        ) {
                            InboxCameraGlyph(tint = InboxAccentStrong, iconSize = 22.dp)
                        }
                        Spacer(Modifier.width(8.dp))
                    }
                    Surface(
                        modifier = Modifier.weight(1f),
                        color = ThemeSurfaceRaised,
                        shape = RoundedCornerShape(23.dp),
                        border = BorderStroke(1.dp, ThemeBorder)
                    ) {
                        BasicTextField(
                            value = draft,
                            onValueChange = { value ->
                                draft = value
                                // Save immediately in the input callback; do not wait
                                // for the next Compose SideEffect/recomposition.
                                if (editingMessage == null && editingScheduledMessage == null) {
                                    cache.saveDraft(userId, draftCacheKey, value)
                                }
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Text,
                                capitalization = KeyboardCapitalization.None,
                                imeAction = ImeAction.Send
                            ),
                            keyboardActions = KeyboardActions(onSend = { sendMessage() }),
                            textStyle = TextStyle(
                                color = InboxPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            ),
                            modifier = Modifier.fillMaxWidth()
                                .focusRequester(composerFocusRequester)
                                .padding(horizontal = 15.dp, vertical = 14.dp),
                            decorationBox = { inner ->
                                Box(contentAlignment = Alignment.CenterStart) {
                                    if (draft.isEmpty()) {
                                        Text(
                                            if (editingMessage != null) "Изменить сообщение…" else "Сообщение…",
                                            color = InboxMuted,
                                            fontSize = 12.sp
                                        )
                                    }
                                    inner()
                                }
                            }
                        )
                    }
                    if (editingMessage == null) {
                        Spacer(Modifier.width(8.dp))
                        ChatChromeActionButton(
                            onClick = {
                                if (!isSending) {
                                    if (context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                                        startVoiceRecording()
                                    } else {
                                        voicePermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                    }
                                }
                            },
                            enabled = !isSending,
                            filled = false,
                            size = 38.dp
                        ) {
                            ChatMicrophoneGlyph(tint = InboxAccentStrong)
                        }
                        Spacer(Modifier.width(4.dp))
                        ChatChromeActionButton(
                            onClick = { openVideoNoteRecorder() },
                            enabled = !isSending && !isVoiceRecording,
                            filled = false,
                            size = 38.dp
                        ) {
                            ChatVideoNoteGlyph(tint = InboxAccentStrong)
                        }
                        Spacer(Modifier.width(4.dp))
                    }
                    if (editingMessage == null && activeTopicId == null) {
                        ChatChromeActionButton(
                            onClick = { openScheduledMessages() },
                            enabled = !isSending,
                            filled = false,
                            size = 34.dp
                        ) {
                            ChatScheduleGlyph()
                        }
                        Spacer(Modifier.width(4.dp))
                    }
                    ChatChromeActionButton(
                        onClick = {
                            if (activeTopicId == null && draft.isBlank() && scheduledMessages.isNotEmpty()) {
                                openScheduledMessages()
                            } else {
                                sendMessage()
                            }
                        },
                        enabled = !isSending && (
                            draft.isNotBlank() ||
                                (activeTopicId == null && scheduledMessages.isNotEmpty())
                            ) &&
                            (editingMessage == null || draft.trim() != editingMessage?.body?.trim()),
                        filled = true
                    ) {
                        ChatSendGlyph()
                    }
                }
            }
        }
    }
    }

}


private fun formatScheduledMessageTime(value: String): String = runCatching {
    val instant = Instant.parse(value)
    SimpleDateFormat("d MMMM, HH:mm", Locale.getDefault())
        .format(Date(instant.toEpochMilli()))
}.getOrDefault(value)

private fun showArturChatScheduleDateTimePicker(
    context: Context,
    initialTimeMillis: Long,
    onSelected: (Long) -> Unit
) {
    val initial = Calendar.getInstance().apply { timeInMillis = initialTimeMillis }
    DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            TimePickerDialog(
                context,
                { _, hourOfDay, minute ->
                    val selected = Calendar.getInstance().apply {
                        set(Calendar.YEAR, year)
                        set(Calendar.MONTH, month)
                        set(Calendar.DAY_OF_MONTH, dayOfMonth)
                        set(Calendar.HOUR_OF_DAY, hourOfDay)
                        set(Calendar.MINUTE, minute)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                    onSelected(selected.timeInMillis)
                },
                initial.get(Calendar.HOUR_OF_DAY),
                initial.get(Calendar.MINUTE),
                true
            ).show()
        },
        initial.get(Calendar.YEAR),
        initial.get(Calendar.MONTH),
        initial.get(Calendar.DAY_OF_MONTH)
    ).apply {
        datePicker.minDate = System.currentTimeMillis()
    }.show()
}

@Composable
private fun ChatListScreen(onOpenChat: (ChatSummary) -> Unit) {
    var query by remember { mutableStateOf("") }
    val filtered = remember(query) {
        com.facevideo.app.model.filterChats(DemoData.chats, query)
    }

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(start = 22.dp, end = 22.dp, top = 22.dp, bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text("A-CHAT", color = AccentSlate, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                Spacer(Modifier.height(5.dp))
                Text("Сообщения", color = MainText, fontSize = 29.sp, fontWeight = FontWeight.Bold)
            }
            Surface(
                color = Color(0xFFE5EAF0),
                shape = RoundedCornerShape(50),
                border = BorderStroke(1.dp, Color(0xFF9EAFC5))
            ) {
                Row(Modifier.padding(horizontal = 11.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(7.dp).clip(CircleShape).background(AccentSlate))
                    Spacer(Modifier.width(7.dp))
                    Text("DEMO", color = AccentSlate, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                }
            }
        }

        SearchField(value = query, onValueChange = { query = it })

        Text(
            "БЫСТРЫЙ ДОСТУП",
            color = Muted,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.6.sp,
            modifier = Modifier.padding(start = 22.dp, top = 22.dp, bottom = 12.dp)
        )

        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(17.dp)
        ) {
            DemoData.chats.filter { it.online }.forEach { chat ->
                Column(
                    Modifier.width(62.dp).clickable { onOpenChat(chat) },
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Avatar(chat.initials, chat.avatarStyle, Modifier.size(54.dp), highlighted = true)
                    Spacer(Modifier.height(7.dp))
                    Text(chat.title.substringBefore(" "), color = MainText, fontSize = 11.sp, maxLines = 1)
                }
            }
        }

        Row(
            Modifier.fillMaxWidth().padding(start = 22.dp, end = 22.dp, top = 24.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Недавние", color = MainText, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.weight(1f))
            Text(filtered.size.toString() + " диалогов", color = Muted, fontSize = 11.sp)
        }

        LazyColumn(
            Modifier.weight(1f),
            contentPadding = PaddingValues(start = 14.dp, end = 14.dp, bottom = 14.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            items(filtered, key = { it.id }) { chat ->
                ChatRow(chat = chat, onClick = { onOpenChat(chat) })
            }
            if (filtered.isEmpty()) {
                item {
                    Text("Ничего не найдено", color = Muted, modifier = Modifier.fillMaxWidth().padding(28.dp))
                }
            }
        }
    }
}

@Composable
private fun SearchField(value: String, onValueChange: (String) -> Unit, compact: Boolean = false, placeholder: String = "Поиск по чатам") {
    Surface(
        Modifier.fillMaxWidth().padding(horizontal = if (compact) 18.dp else 20.dp, vertical = if (compact) 2.dp else 0.dp).heightIn(min = if (compact) 46.dp else 0.dp),
        color = if (compact) InboxSurface else Panel,
        shape = RoundedCornerShape(if (compact) 13.dp else 16.dp),
        border = BorderStroke(1.dp, if (compact) InboxBorder else Color(0xFFC9D3DF))
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = TextStyle(color = if (compact) InboxPrimary else MainText, fontSize = if (compact) 13.sp else 14.sp),
            modifier = Modifier.fillMaxWidth().padding(
                horizontal = if (compact) 13.dp else 15.dp,
                vertical = if (compact) 12.dp else 14.dp
            ),
            decorationBox = { inner ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("⌕", color = if (compact) InboxAccent else AccentSlate, fontSize = if (compact) 19.sp else 22.sp)
                    Spacer(Modifier.width(if (compact) 8.dp else 10.dp))
                    Box(Modifier.weight(1f)) {
                        if (value.isEmpty()) Text(placeholder, color = if (compact) InboxMuted else Muted, fontSize = if (compact) 12.sp else 14.sp)
                        inner()
                    }
                }
            }
        )
    }
}

@Composable
private fun ChatRow(chat: ChatSummary, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).clickable(onClick = onClick)
            .background(Color(0xFFF4F6FF)).padding(horizontal = 11.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Avatar(chat.initials, chat.avatarStyle, Modifier.size(53.dp), highlighted = chat.online)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(chat.title, color = MainText, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                if (chat.online) {
                    Spacer(Modifier.width(6.dp))
                    Box(Modifier.size(6.dp).clip(CircleShape).background(AccentSlate))
                }
            }
            Spacer(Modifier.height(5.dp))
            Text(chat.preview, color = Muted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Spacer(Modifier.width(8.dp))
        Column(horizontalAlignment = Alignment.End) {
            Text(chat.time, color = Muted, fontSize = 10.sp)
            if (chat.unreadCount > 0) {
                Spacer(Modifier.height(7.dp))
                Box(Modifier.size(21.dp).clip(CircleShape).background(AccentSlate), contentAlignment = Alignment.Center) {
                    Text(chat.unreadCount.toString(), color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
internal fun Avatar(
    initials: String,
    style: Int,
    modifier: Modifier = Modifier,
    highlighted: Boolean = false,
    imageUrl: String? = null,
    imageCacheKey: String? = null,
    onClick: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val imageCache = remember(context) { ArturChatImageCache(context) }
    val stableCacheKey = imageCacheKey?.takeIf { it.isNotBlank() }
        ?: imageUrl?.takeIf { it.isNotBlank() }
    var avatarBitmap by remember(stableCacheKey) { mutableStateOf<android.graphics.Bitmap?>(null) }

    LaunchedEffect(imageUrl, stableCacheKey) {
        if (imageUrl.isNullOrBlank() || stableCacheKey.isNullOrBlank()) {
            avatarBitmap = null
            return@LaunchedEffect
        }

        val cachedBytes = withContext(Dispatchers.IO) {
            imageCache.readBytes("avatars", "public", stableCacheKey)
        }
        if (cachedBytes != null) avatarBitmap = decodeCachedImage(cachedBytes, 512)

        val freshBytes = withContext(Dispatchers.IO) {
            runCatching {
                imageCache.fetchAndStore(
                    kind = "avatars",
                    ownerId = "public",
                    mediaId = stableCacheKey,
                    sourceUrl = imageUrl,
                    freshnessMillis = 24 * 60 * 60 * 1000L,
                    refreshWhenSourceChanges = true
                )
            }.onFailure {
                android.util.Log.w("ArturChatAvatar", "Avatar cache refresh failed", it)
            }.getOrNull()
        }
        if (freshBytes != null) avatarBitmap = decodeCachedImage(freshBytes, 512)
    }
    Box(
        modifier.clip(CircleShape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .background(
                Brush.linearGradient(listOf(AvatarColors[style % AvatarColors.size], PanelLight))
            ),
        contentAlignment = Alignment.Center
    ) {
        val bitmap = avatarBitmap
        if (bitmap != null) {
            Image(bitmap.asImageBitmap(), contentDescription = "Фото профиля", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        } else {
            Text(initials, color = MainText, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        }
        if (highlighted) {
            Box(Modifier.align(Alignment.BottomEnd).padding(1.dp).size(10.dp).clip(CircleShape).background(AccentSlate))
        }
    }
}

@Composable
internal fun FullscreenAvatarDialog(
    imageUrl: String,
    imageCacheKey: String,
    title: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val imageCache = remember(context) { ArturChatImageCache(context) }
    var bitmap by remember(imageUrl, imageCacheKey) {
        mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null)
    }
    var isLoading by remember(imageUrl, imageCacheKey) { mutableStateOf(true) }
    var loadError by remember(imageUrl, imageCacheKey) { mutableStateOf(false) }

    LaunchedEffect(imageUrl, imageCacheKey) {
        isLoading = true
        loadError = false
        val bytes = withContext(Dispatchers.IO) {
            val cached = runCatching {
                imageCache.readBytes("avatars", "public", imageCacheKey)
            }.getOrNull()
            val fresh = runCatching {
                imageCache.fetchAndStore(
                    kind = "avatars",
                    ownerId = "public",
                    mediaId = imageCacheKey,
                    sourceUrl = imageUrl,
                    freshnessMillis = 0,
                    refreshWhenSourceChanges = true
                )
            }.getOrNull()
            fresh ?: cached
        }
        bitmap = bytes?.let { decodeCachedImage(it, 2560)?.asImageBitmap() }
        loadError = bitmap == null
        isLoading = false
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        val loaded = bitmap
        if (loaded != null) {
            FullscreenChatImageViewer(
                bitmap = loaded,
                title = title,
                onDismiss = onDismiss
            )
        } else {
            Box(
                Modifier.fillMaxSize().background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (isLoading) {
                        androidx.compose.material3.CircularProgressIndicator(color = Color.White)
                        Spacer(Modifier.height(12.dp))
                    }
                    Text(
                        if (loadError) "Не удалось загрузить фотографию" else "Загружаем фото…",
                        color = Color.White.copy(alpha = 0.82f),
                        fontSize = 12.sp
                    )
                }
                Text(
                    "‹",
                    modifier = Modifier.align(Alignment.TopStart)
                        .padding(18.dp)
                        .clickable(onClick = onDismiss)
                        .padding(8.dp),
                    color = Color.White,
                    fontSize = 34.sp
                )
            }
        }
    }
}

@Composable
private fun ChatScreen(
    chat: ChatSummary,
    messages: SnapshotStateList<ChatMessage>,
    onBack: () -> Unit,
    onCall: () -> Unit
) {
    var draft by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().imePadding()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 15.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ChatHeaderIconButton(onClick = onBack) {
                InboxBackGlyph()
            }
            Spacer(Modifier.width(8.dp))
            Avatar(chat.initials, chat.avatarStyle, Modifier.size(43.dp), highlighted = chat.online)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(chat.title, color = MainText, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                Text(if (chat.online) "в сети" else "был(а) недавно", color = if (chat.online) AccentSlate else Muted, fontSize = 11.sp)
            }
            Spacer(Modifier.width(7.dp))
            ChatHeaderIconButton(onClick = onCall) {
                ChatCallGlyph(video = false)
            }
            Spacer(Modifier.width(5.dp))
            ChatHeaderIconButton(onClick = onCall) {
                ChatCallGlyph(video = true)
            }
        }
        Box(
            Modifier.fillMaxWidth().height(1.dp).background(
                Brush.horizontalGradient(
                    listOf(Color.Transparent, Color.White.copy(alpha = 0.94f), Color(0xFFD6DDF0), Color.Transparent)
                )
            )
        )

        LazyColumn(
            Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(11.dp)
        ) {
            item {
                Text(
                    "СЕГОДНЯ",
                    color = Muted,
                    fontSize = 10.sp,
                    letterSpacing = 1.4.sp,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
            items(messages, key = { it.id }) { message -> MessageBubble(message) }
        }

        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            InboxHeaderActionButton(onClick = {}) {
                Text("+", color = InboxAccentStrong, fontSize = 22.sp, fontWeight = FontWeight.Light)
            }
            Spacer(Modifier.width(9.dp))
            Surface(
                Modifier.weight(1f),
                color = Panel,
                shape = RoundedCornerShape(22.dp),
                border = BorderStroke(1.dp, Color(0xFFD6DDF0))
            ) {
                BasicTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = {
                        if (draft.isNotBlank()) {
                            messages.add(ChatMessage(UUID.randomUUID().toString(), draft.trim(), "сейчас", true))
                            draft = ""
                        }
                    }),
                    textStyle = TextStyle(color = MainText, fontSize = 14.sp),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 15.dp, vertical = 13.dp),
                    decorationBox = { inner ->
                        Box {
                            if (draft.isEmpty()) Text("Сообщение...", color = Muted, fontSize = 14.sp)
                            inner()
                        }
                    }
                )
            }
            Spacer(Modifier.width(9.dp))
            Box(
                Modifier.size(46.dp).clip(CircleShape)
                    .background(Brush.linearGradient(listOf(Color(0xFF2CA8FF), Color(0xFF5266E8), Color(0xFF9362E8))))
                    .clickable {
                        if (draft.isNotBlank()) {
                            messages.add(ChatMessage(UUID.randomUUID().toString(), draft.trim(), "сейчас", true))
                            draft = ""
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Text("➤", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun ImageMessageMetadata(
    time: String,
    deliveryTicks: Pair<Int, Color>?,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = Color.Black.copy(alpha = 0.48f),
        shape = RoundedCornerShape(20.dp),
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                time,
                color = Color.White,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium
            )
            deliveryTicks?.let { (count, _) ->
                Canvas(
                    modifier = Modifier
                        .padding(start = 1.dp)
                        .size(width = if (count == 1) 8.dp else 13.dp, height = 9.dp)
                ) {
                    val unit = 4.dp.toPx()
                    val arm = 2.2.dp.toPx()
                    val rise = 6.2.dp.toPx()
                    val stroke = 1.25.dp.toPx()
                    repeat(count) { index ->
                        val x = index * unit
                        val joint = Offset(x + arm, size.height * 0.72f)
                        drawLine(
                            color = Color.White,
                            start = Offset(x, size.height * 0.48f),
                            end = joint,
                            strokeWidth = stroke,
                            cap = StrokeCap.Round
                        )
                        drawLine(
                            color = Color.White,
                            start = joint,
                            end = Offset(x + rise, size.height * 0.2f),
                            strokeWidth = stroke,
                            cap = StrokeCap.Round
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FullscreenChatImageViewer(
    bitmap: androidx.compose.ui.graphics.ImageBitmap,
    title: String = "Фото",
    onDismiss: () -> Unit
) {
    var scale by remember(bitmap) { mutableFloatStateOf(1f) }
    var offsetX by remember(bitmap) { mutableFloatStateOf(0f) }
    var offsetY by remember(bitmap) { mutableFloatStateOf(0f) }
    var dismissDrag by remember(bitmap) { mutableFloatStateOf(0f) }
    var controlsVisible by remember(bitmap) { mutableStateOf(true) }
    var viewportSize by remember(bitmap) { mutableStateOf(androidx.compose.ui.unit.IntSize.Zero) }
    val animatedDismissDrag by animateFloatAsState(
        targetValue = dismissDrag,
        animationSpec = tween(durationMillis = 180),
        label = "photoDismissDrag"
    )
    val transformState = rememberTransformableState { zoomChange, panChange, _ ->
        val nextScale = (scale * zoomChange).coerceIn(1f, 5f)
        scale = nextScale
        if (nextScale > 1.001f) {
            val maxX = (viewportSize.width * (nextScale - 1f) / 2f).coerceAtLeast(0f)
            val maxY = (viewportSize.height * (nextScale - 1f) / 2f).coerceAtLeast(0f)
            offsetX = (offsetX + panChange.x).coerceIn(-maxX, maxX)
            offsetY = (offsetY + panChange.y).coerceIn(-maxY, maxY)
        } else {
            offsetX = 0f
            offsetY = 0f
        }
    }

    // Hide Android's status/navigation bars while this dedicated photo viewer is open.
    val dialogView = LocalView.current
    val dialogWindow = (dialogView.parent as? DialogWindowProvider)?.window
    DisposableEffect(dialogWindow) {
        if (dialogWindow == null) {
            onDispose { }
        } else {
            val controller = WindowInsetsControllerCompat(dialogWindow, dialogWindow.decorView)
            val previousBehavior = controller.systemBarsBehavior
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller.hide(WindowInsetsCompat.Type.systemBars())
            onDispose {
                controller.show(WindowInsetsCompat.Type.systemBars())
                controller.systemBarsBehavior = previousBehavior
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { viewportSize = it }
            .background(
                Color.Black.copy(
                    alpha = 1f - (animatedDismissDrag / 520f).coerceIn(0f, 0.58f)
                )
            )
    ) {
        Image(
            bitmap = bitmap,
            contentDescription = "Фотография на весь экран",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = offsetX
                    translationY = offsetY + animatedDismissDrag
                }
                .transformable(
                    state = transformState,
                    canPan = { scale > 1.001f }
                )
                .pointerInput(bitmap, scale) {
                    detectVerticalDragGestures(
                        onVerticalDrag = { change, dragAmount ->
                            if (scale <= 1.001f && dragAmount > 0f) {
                                dismissDrag = (dismissDrag + dragAmount).coerceAtMost(size.height * 0.72f)
                                change.consume()
                            }
                        },
                        onDragEnd = {
                            if (dismissDrag >= 120.dp.toPx()) {
                                onDismiss()
                            } else {
                                dismissDrag = 0f
                            }
                        },
                        onDragCancel = { dismissDrag = 0f }
                    )
                }
                .pointerInput(bitmap, scale) {
                    detectTapGestures(
                        onTap = { controlsVisible = !controlsVisible },
                        onDoubleTap = {
                            if (scale > 1.001f) {
                                scale = 1f
                                offsetX = 0f
                                offsetY = 0f
                            } else {
                                scale = 2.5f
                                offsetX = 0f
                                offsetY = 0f
                            }
                        }
                    )
                }
        )

        if (controlsVisible) {
            Row(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Black.copy(alpha = 0.54f), Color.Transparent)
                        )
                    )
                    .padding(start = 14.dp, end = 18.dp, top = 16.dp, bottom = 30.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.14f))
                        .clickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center
                ) {
                    Text("‹", color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.Light)
                }
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(
                        title,
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        if (scale > 1.001f) "Масштаб ${(scale * 100).toInt()}%" else "Просмотр изображения",
                        color = Color.White.copy(alpha = 0.72f),
                        fontSize = 10.sp
                    )
                }
            }

            Text(
                if (scale > 1.001f) "Дважды нажмите, чтобы вернуть исходный размер" else "Сведите или разведите пальцы для изменения масштаба",
                color = Color.White.copy(alpha = 0.82f),
                fontSize = 11.sp,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 24.dp, vertical = 28.dp)
            )
        }
    }
}

@Composable
private fun ChatImagePreview(
    path: String,
    repository: SupabaseChatRepository,
    modifier: Modifier = Modifier
) {
    var bitmap by remember(path) { mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null) }
    var failed by remember(path) { mutableStateOf(false) }
    var showFullImage by remember(path) { mutableStateOf(false) }

    LaunchedEffect(path, repository) {
        bitmap = null
        failed = false
        runCatching {
            withContext(Dispatchers.IO) {
                val bytes = repository.downloadChatAttachment(path)
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
                var sampleSize = 1
                while (maxOf(bounds.outWidth / sampleSize, bounds.outHeight / sampleSize) > 1400) {
                    sampleSize *= 2
                }
                val options = BitmapFactory.Options().apply { inSampleSize = sampleSize }
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)?.asImageBitmap()
                    ?: throw IllegalStateException("Не удалось декодировать фото.")
            }
        }.onSuccess {
            bitmap = it
        }.onFailure {
            android.util.Log.w("ArturChatAttachments", "Image download failed", it)
            failed = true
        }
    }

    val loaded = bitmap
    if (loaded != null) {
        val aspectRatio = (loaded.width.toFloat() / loaded.height.toFloat()).coerceIn(0.62f, 1.55f)
        Image(
            bitmap = loaded,
            contentDescription = "Фотография",
            contentScale = ContentScale.Crop,
            modifier = modifier
                .heightIn(max = 360.dp)
                .aspectRatio(aspectRatio, matchHeightConstraintsFirst = aspectRatio < 1f)
                .clip(RoundedCornerShape(15.dp))
                .clickable { showFullImage = true }
        )
        if (showFullImage) {
            Dialog(
                onDismissRequest = { showFullImage = false },
                properties = DialogProperties(
                    usePlatformDefaultWidth = false,
                    decorFitsSystemWindows = false
                )
            ) {
                FullscreenChatImageViewer(
                    bitmap = loaded,
                    onDismiss = { showFullImage = false }
                )
            }
        }
    } else {
        Box(
            modifier
                .heightIn(min = 150.dp, max = 220.dp)
                .aspectRatio(1.15f, matchHeightConstraintsFirst = true)
                .clip(RoundedCornerShape(15.dp))
                .background(if (failed) Color(0xFFF5D2D0) else Color(0xFFE5EAF0)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                if (failed) "Фото недоступно" else "Загружаем фото…",
                color = Muted,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
private fun VoiceWaveformBars(
    waveform: List<Int>,
    progress: Float,
    modifier: Modifier = Modifier
) {
    val bars = remember(waveform) {
        if (waveform.size in 16..96) waveform.map { it.coerceIn(8, 100) }
        else List(VOICE_WAVEFORM_BAR_COUNT) { 8 }
    }
    val safeProgress = progress.coerceIn(0f, 1f)
    Canvas(modifier = modifier) {
        if (bars.isEmpty() || size.width <= 0f || size.height <= 0f) return@Canvas
        val step = size.width / bars.size
        val strokeWidth = (step * 0.58f).coerceIn(1.0.dp.toPx(), 1.7.dp.toPx())
        val minBarHeight = 2.4.dp.toPx()
        val maxBarHeight = size.height * 0.96f
        val playedColor = if (appOrangeChocolateTheme.value) Color(0xFFFF9B3D) else Color(0xFF5266DE)
        val futureColor = if (appOrangeChocolateTheme.value) Color(0xFF825638) else Color(0xFF9EAECE)
        bars.forEachIndexed { index, level ->
            val amplitude = ((level - 8) / 92f).coerceIn(0f, 1f)
            val barHeight = minBarHeight + amplitude * (maxBarHeight - minBarHeight)
            val x = step * (index + 0.5f)
            val top = (size.height - barHeight) / 2f
            drawLine(
                color = if ((index + 0.5f) / bars.size <= safeProgress) playedColor else futureColor,
                start = Offset(x, top),
                end = Offset(x, top + barHeight),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )
        }
    }
}

private object DecodedVoiceWaveformCache {
    private val values = object : LinkedHashMap<String, List<Int>>(64, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, List<Int>>?): Boolean =
            size > 128
    }

    @Synchronized
    fun get(path: String): List<Int>? = values[path]

    @Synchronized
    fun put(path: String, waveform: List<Int>) {
        values[path] = waveform.toList()
    }
}

private fun shouldRebuildVoiceWaveform(waveform: List<Int>): Boolean {
    if (waveform.size !in 16..96) return true
    val peak = waveform.maxOrNull() ?: 0
    val floor = waveform.minOrNull() ?: 0
    val range = peak - floor
    // 0.7.7/0.7.8 waveforms used absolute recorder peaks and usually have no
    // per-recording 0..100 envelope. Re-decode those once from the AAC source.
    return waveform.distinct().size < 16 ||
        range < 40 ||
        peak < 96 ||
        floor > 30
}

private const val MAX_CHAT_VIDEO_NOTE_BYTES = 10 * 1024 * 1024
private const val MAX_CHAT_VIDEO_NOTE_DURATION_MS = 60_000L
private const val CHAT_VIDEO_NOTE_TARGET_BYTES = 9_500_000L

private tailrec fun Context.findComponentActivity(): ComponentActivity? = when (this) {
    is ComponentActivity -> this
    is ContextWrapper -> baseContext.findComponentActivity()
    else -> null
}

@Composable
private fun ChatVideoNoteRecorderDialog(
    onDismiss: () -> Unit,
    onSend: (File, Long) -> Unit
) {
    val context = LocalContext.current
    val activity = remember(context) { context.findComponentActivity() }
    val previewView = remember(context) {
        PreviewView(context).apply {
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }
    val cameraProviderFuture = remember(context) {
        ProcessCameraProvider.getInstance(context)
    }

    var cameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }
    var videoCapture by remember { mutableStateOf<VideoCapture<Recorder>?>(null) }
    var activeRecording by remember { mutableStateOf<Recording?>(null) }
    var recordedFile by remember { mutableStateOf<File?>(null) }
    var isRecording by remember { mutableStateOf(false) }
    var isFinalizing by remember { mutableStateOf(false) }
    var isFrontCamera by remember { mutableStateOf(true) }
    var startedAt by remember { mutableStateOf(0L) }
    var durationMs by remember { mutableStateOf(0L) }
    var cameraReady by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var keepRecordedFile by remember { mutableStateOf(false) }
    val recorderDisposed = remember { java.util.concurrent.atomic.AtomicBoolean(false) }

    val latestProvider = rememberUpdatedState(cameraProvider)
    val latestRecording = rememberUpdatedState(activeRecording)
    val latestRecordedFile = rememberUpdatedState(recordedFile)
    val latestKeepFile = rememberUpdatedState(keepRecordedFile)

    DisposableEffect(Unit) {
        recorderDisposed.set(false)
        onDispose {
            recorderDisposed.set(true)
            runCatching { latestRecording.value?.stop() }
            runCatching { latestProvider.value?.unbindAll() }
            if (!latestKeepFile.value) latestRecordedFile.value?.delete()
        }
    }

    LaunchedEffect(activity, isFrontCamera, recordedFile == null) {
        if (recordedFile != null) {
            cameraProvider?.unbindAll()
            videoCapture = null
            cameraReady = false
            return@LaunchedEffect
        }
        if (activity == null) {
            errorMessage = "Не удалось открыть камеру в этом окне."
            return@LaunchedEffect
        }
        cameraReady = false
        errorMessage = null
        try {
            val provider = withContext(Dispatchers.IO) { cameraProviderFuture.get() }
            cameraProvider = provider
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }
            val recorder = Recorder.Builder()
                .setQualitySelector(
                    QualitySelector.from(
                        Quality.SD,
                        FallbackStrategy.lowerQualityOrHigherThan(Quality.SD)
                    )
                )
                .setTargetVideoEncodingBitRate(850_000)
                .build()
            val capture = VideoCapture.withOutput(recorder)
            val selector = if (isFrontCamera) {
                CameraSelector.DEFAULT_FRONT_CAMERA
            } else {
                CameraSelector.DEFAULT_BACK_CAMERA
            }
            provider.unbindAll()
            provider.bindToLifecycle(activity, selector, preview, capture)
            videoCapture = capture
            cameraReady = true
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            android.util.Log.e("ArturChatVideoNote", "Could not bind CameraX preview", error)
            errorMessage = error.message ?: "Не удалось запустить камеру."
        }
    }

    fun stopRecording() {
        if (!isRecording || isFinalizing) return
        isFinalizing = true
        runCatching { activeRecording?.stop() }
            .onFailure {
                isFinalizing = false
                errorMessage = it.message ?: "Не удалось завершить запись."
            }
    }

    fun startRecording() {
        if (isRecording || isFinalizing) return
        val capture = videoCapture
        if (capture == null || !cameraReady) {
            errorMessage = "Камера ещё не готова. Попробуйте ещё раз."
            return
        }
        recordedFile?.delete()
        recordedFile = null
        durationMs = 0L
        errorMessage = null
        val file = File(context.cacheDir, "video-note-${UUID.randomUUID()}.mp4")
        val outputOptions = FileOutputOptions.Builder(file)
            .setFileSizeLimit(CHAT_VIDEO_NOTE_TARGET_BYTES)
            .build()
        try {
            val recording = capture.output
                .prepareRecording(context, outputOptions)
                .withAudioEnabled()
                .start(ContextCompat.getMainExecutor(context)) { event ->
                    when (event) {
                        is VideoRecordEvent.Start -> {
                            startedAt = SystemClock.elapsedRealtime()
                            durationMs = 0L
                            isRecording = true
                            isFinalizing = false
                        }
                        is VideoRecordEvent.Status -> {
                            durationMs = (event.recordingStats.recordedDurationNanos / 1_000_000L)
                                .coerceAtMost(MAX_CHAT_VIDEO_NOTE_DURATION_MS)
                        }
                        is VideoRecordEvent.Finalize -> {
                            activeRecording = null
                            if (recorderDisposed.get()) {
                                file.delete()
                            } else {
                                isRecording = false
                                isFinalizing = false
                                durationMs = (event.recordingStats.recordedDurationNanos / 1_000_000L)
                                    .coerceAtMost(MAX_CHAT_VIDEO_NOTE_DURATION_MS)
                                val validFile = file.isFile &&
                                    file.length() in 1..MAX_CHAT_VIDEO_NOTE_BYTES.toLong() &&
                                    durationMs >= 1_000L
                                if (event.hasError() || !validFile) {
                                    file.delete()
                                    errorMessage = when {
                                        event.hasError() -> "Не удалось сохранить видеосообщение. Попробуйте ещё раз."
                                        durationMs < 1_000L -> "Запишите кружочек хотя бы одну секунду."
                                        else -> "Видео получилось слишком большим. Запишите его короче."
                                    }
                                } else {
                                    recordedFile = file
                                    cameraReady = false
                                    cameraProvider?.unbindAll()
                                    videoCapture = null
                                }
                            }
                        }
                    }
                }
            activeRecording = recording
            isRecording = true
            startedAt = SystemClock.elapsedRealtime()
        } catch (error: Exception) {
            file.delete()
            isRecording = false
            isFinalizing = false
            errorMessage = error.message ?: "Не удалось начать запись. Проверьте доступ к камере и микрофону."
        }
    }

    LaunchedEffect(isRecording, startedAt) {
        while (isRecording) {
            durationMs = (SystemClock.elapsedRealtime() - startedAt)
                .coerceAtLeast(0L)
                .coerceAtMost(MAX_CHAT_VIDEO_NOTE_DURATION_MS)
            if (durationMs >= MAX_CHAT_VIDEO_NOTE_DURATION_MS) {
                stopRecording()
                break
            }
            delay(100L)
        }
    }

    Dialog(
        onDismissRequest = {
            if (isRecording) activeRecording?.stop()
            onDismiss()
        },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color(0xFF080A10))
                .padding(horizontal = 24.dp, vertical = 22.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    "Видеосообщение",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(7.dp))
                Text(
                    when {
                        recordedFile != null -> "Кружочек готов к отправке"
                        isFinalizing -> "Сохраняем видео…"
                        isRecording -> "Идёт запись · ${formatVoiceDuration(durationMs)}"
                        else -> "Запишите короткое видео для чата"
                    },
                    color = Color.White.copy(alpha = 0.72f),
                    fontSize = 12.sp
                )
                Spacer(Modifier.height(28.dp))

                Box(
                    Modifier
                        .size(286.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF171B25)),
                    contentAlignment = Alignment.Center
                ) {
                    val finished = recordedFile
                    if (finished != null) {
                        VideoNoteLocalPreview(
                            file = finished,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        AndroidView(
                            factory = { previewView },
                            modifier = Modifier.fillMaxSize()
                        )
                        if (!cameraReady && errorMessage == null) {
                            androidx.compose.material3.CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(28.dp),
                                strokeWidth = 2.dp
                            )
                        }
                    }
                    if (isRecording) {
                        Surface(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(top = 17.dp),
                            color = Color.Black.copy(alpha = 0.54f),
                            shape = CircleShape
                        ) {
                            Row(
                                Modifier.padding(horizontal = 11.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    Modifier.size(7.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFFF5875))
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    formatVoiceDuration(durationMs),
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                if (errorMessage != null) {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        errorMessage.orEmpty(),
                        color = Color(0xFFFFA7B5),
                        fontSize = 11.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
                Spacer(Modifier.height(24.dp))

                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (recordedFile == null) {
                        TextButton(
                            onClick = { isFrontCamera = !isFrontCamera },
                            enabled = !isRecording && !isFinalizing && cameraReady
                        ) {
                            Text("↻ Камера", color = Color.White)
                        }
                        Spacer(Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (isRecording) stopRecording() else startRecording()
                            },
                            enabled = cameraReady && !isFinalizing,
                            shape = CircleShape,
                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                containerColor = if (isRecording) Color(0xFFDF5875) else Color(0xFF596BE8)
                            )
                        ) {
                            Text(
                                if (isRecording) "Остановить" else "● Записать",
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    } else {
                        TextButton(
                            onClick = {
                                recordedFile?.delete()
                                recordedFile = null
                                durationMs = 0L
                                errorMessage = null
                            }
                        ) {
                            Text("Перезаписать", color = Color.White)
                        }
                        Spacer(Modifier.width(8.dp))
                        Button(
                            onClick = {
                                val file = recordedFile ?: return@Button
                                keepRecordedFile = true
                                onSend(file, durationMs)
                            },
                            shape = CircleShape,
                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF596BE8)
                            )
                        ) {
                            Text("Отправить", color = Color.White, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                TextButton(
                    onClick = {
                        if (isRecording) activeRecording?.stop()
                        onDismiss()
                    },
                    enabled = !isFinalizing
                ) {
                    Text("Отмена", color = Color.White.copy(alpha = 0.68f))
                }
            }
        }
    }
}

@Composable
private fun VideoNoteLocalPreview(
    file: File,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val player = remember(file, context) { ExoPlayer.Builder(context).build() }
    DisposableEffect(player, file) {
        player.setMediaItem(MediaItem.fromUri(Uri.fromFile(file)))
        player.repeatMode = Player.REPEAT_MODE_ONE
        player.volume = 0f
        player.prepare()
        player.playWhenReady = true
        onDispose { runCatching { player.release() } }
    }
    AndroidView(
        factory = { viewContext ->
            (LayoutInflater.from(viewContext).inflate(R.layout.view_video_note_player, null) as PlayerView).apply {
                useController = false
                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                setShutterBackgroundColor(android.graphics.Color.BLACK)
                this.player = player
            }
        },
        update = { playerView ->
            if (playerView.player !== player) playerView.player = player
        },
        modifier = modifier.clip(CircleShape)
    )
}

@Composable
private fun ChatVideoNotePlayer(
    path: String,
    repository: SupabaseChatRepository,
    durationMs: Long,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var videoFile by remember(path) { mutableStateOf<File?>(null) }
    var failed by remember(path) { mutableStateOf(false) }
    var isLoading by remember(path) { mutableStateOf(true) }
    var isPlaying by remember(path) { mutableStateOf(false) }
    var soundEnabled by remember(path) { mutableStateOf(false) }
    val latestFile = rememberUpdatedState(videoFile)
    val exoPlayer = remember(path, context) { ExoPlayer.Builder(context).build() }

    LaunchedEffect(path, repository) {
        isLoading = true
        failed = false
        try {
            val bytes = withContext(Dispatchers.IO) {
                repository.downloadChatAttachment(path)
            }
            require(bytes.isNotEmpty() && bytes.size <= MAX_CHAT_VIDEO_NOTE_BYTES) {
                "Видеосообщение повреждено или превышает допустимый размер."
            }
            val localFile = withContext(Dispatchers.IO) {
                File.createTempFile("arturchat-video-note-", ".mp4", context.cacheDir)
                    .apply { writeBytes(bytes) }
            }
            videoFile = localFile
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            android.util.Log.w("ArturChatVideoNote", "Could not load video note", error)
            failed = true
        } finally {
            isLoading = false
        }
    }

    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }

            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                failed = true
                isPlaying = false
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
            runCatching { exoPlayer.release() }
            latestFile.value?.delete()
        }
    }

    LaunchedEffect(exoPlayer, videoFile) {
        val file = videoFile ?: return@LaunchedEffect
        runCatching {
            exoPlayer.setMediaItem(MediaItem.fromUri(Uri.fromFile(file)))
            exoPlayer.repeatMode = Player.REPEAT_MODE_ONE
            exoPlayer.volume = if (soundEnabled) 1f else 0f
            exoPlayer.prepare()
        }.onFailure {
            android.util.Log.w("ArturChatVideoNote", "Could not prepare video note player", it)
            failed = true
        }
    }

    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(Color(0xFF11131A))
    ) {
        val file = videoFile
        if (file != null && !failed) {
            AndroidView(
                factory = { viewContext ->
                    (LayoutInflater.from(viewContext).inflate(R.layout.view_video_note_player, null) as PlayerView).apply {
                        useController = false
                            resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                        setShutterBackgroundColor(android.graphics.Color.BLACK)
                        player = exoPlayer
                    }
                },
                update = { playerView ->
                    if (playerView.player !== exoPlayer) playerView.player = exoPlayer
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        if (isLoading || failed || !isPlaying) {
            Box(
                Modifier
                    .align(Alignment.Center)
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.48f)),
                contentAlignment = Alignment.Center
            ) {
                when {
                    isLoading -> androidx.compose.material3.CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    failed -> Text("!", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    else -> Text("▶", color = Color.White, fontSize = 19.sp)
                }
            }
        }

        if (file != null && !failed && !isLoading) {
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 13.dp)
                    .clip(CircleShape)
                    .clickable {
                        if (exoPlayer.isPlaying) {
                            soundEnabled = !soundEnabled
                            exoPlayer.volume = if (soundEnabled) 1f else 0f
                        } else {
                            soundEnabled = true
                            exoPlayer.volume = 1f
                            exoPlayer.play()
                        }
                    },
                color = Color.Black.copy(alpha = 0.42f),
                shape = CircleShape,
                shadowElevation = 0.dp
            ) {
                Text(
                    if (isPlaying && soundEnabled) "♫" else if (isPlaying) "◖" else formatVoiceDuration(durationMs),
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                )
            }
        }

        Box(
            Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .clickable(enabled = file != null && !failed && !isLoading) {
                    if (exoPlayer.isPlaying) {
                        soundEnabled = !soundEnabled
                        exoPlayer.volume = if (soundEnabled) 1f else 0f
                    } else {
                        soundEnabled = true
                        exoPlayer.volume = 1f
                        exoPlayer.play()
                    }
                }
        )
    }
}

@Composable
private fun ChatVoiceMessagePlayer(
    path: String,
    repository: SupabaseChatRepository,
    durationMs: Long,
    waveform: List<Int>
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val playerState = remember(path) { mutableStateOf<MediaPlayer?>(null) }
    val audioFileState = remember(path) { mutableStateOf<File?>(null) }
    val cachedWaveform = remember(path) { DecodedVoiceWaveformCache.get(path) }
    var displayWaveform by remember(path) { mutableStateOf(cachedWaveform ?: waveform) }
    var isLoading by remember(path) { mutableStateOf(false) }
    var isPrepared by remember(path) { mutableStateOf(false) }
    var isPlaying by remember(path) { mutableStateOf(false) }
    var playbackFailed by remember(path) { mutableStateOf(false) }
    var playbackPositionMs by remember(path) { mutableStateOf(0L) }
    val safeDurationMs = durationMs.coerceAtLeast(0L)

    LaunchedEffect(path, waveform) {
        val memoryCached = DecodedVoiceWaveformCache.get(path)
        if (memoryCached != null) {
            displayWaveform = memoryCached
        } else if (shouldRebuildVoiceWaveform(waveform)) {
            var temporaryFile: File? = null
            var keepFileForPlayback = false
            try {
                // Older messages carry a coarse legacy waveform. Rebuild it from their actual AAC.
                val audioBytes = withContext(Dispatchers.IO) {
                    repository.downloadChatAttachment(path)
                }
                temporaryFile = withContext(Dispatchers.IO) {
                    File.createTempFile("arturchat-waveform-", ".m4a", context.cacheDir)
                        .apply { writeBytes(audioBytes) }
                }
                val decoded = withContext(Dispatchers.IO) {
                    decodeVoiceWaveformFromFile(temporaryFile!!)
                }
                if (decoded != null) {
                    displayWaveform = decoded
                    DecodedVoiceWaveformCache.put(path, decoded)
                    if (audioFileState.value?.isFile != true) {
                        audioFileState.value = temporaryFile
                        keepFileForPlayback = true
                    }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                android.util.Log.w("ArturChatVoice", "Could not rebuild legacy waveform", error)
            } finally {
                if (!keepFileForPlayback) temporaryFile?.delete()
            }
        } else {
            DecodedVoiceWaveformCache.put(path, waveform)
        }
    }

    fun togglePlayback() {
        val currentPlayer = playerState.value
        if (currentPlayer != null && isPrepared) {
            runCatching {
                if (isPlaying) {
                    currentPlayer.pause()
                    isPlaying = false
                } else {
                    if (safeDurationMs > 0L && playbackPositionMs >= safeDurationMs) {
                        currentPlayer.seekTo(0)
                        playbackPositionMs = 0L
                    }
                    currentPlayer.start()
                    isPlaying = true
                }
            }.onFailure {
                playbackFailed = true
                isPlaying = false
            }
            return
        }
        if (isLoading) return
        playbackFailed = false
        isLoading = true
        scope.launch {
            var createdPlayer: MediaPlayer? = null
            try {
                val cachedAudioFile = audioFileState.value?.takeIf { it.isFile }
                val audioFile = cachedAudioFile ?: withContext(Dispatchers.IO) {
                    val audioBytes = repository.downloadChatAttachment(path)
                    File.createTempFile("arturchat-voice-", ".m4a", context.cacheDir).apply { writeBytes(audioBytes) }
                }
                if (audioFileState.value !== audioFile) {
                    audioFileState.value?.delete()
                    audioFileState.value = audioFile
                }
                val mediaPlayer = MediaPlayer()
                createdPlayer = mediaPlayer
                playerState.value = mediaPlayer
                mediaPlayer.setOnPreparedListener { preparedPlayer ->
                    isPrepared = true
                    isLoading = false
                    playbackPositionMs = 0L
                    runCatching { preparedPlayer.start(); isPlaying = true }.onFailure {
                        playbackFailed = true
                        isPlaying = false
                    }
                }
                mediaPlayer.setOnCompletionListener { completedPlayer ->
                    isPlaying = false
                    playbackPositionMs = 0L
                    runCatching { completedPlayer.seekTo(0) }
                }
                mediaPlayer.setOnErrorListener { failedPlayer, _, _ ->
                    isLoading = false
                    isPlaying = false
                    isPrepared = false
                    playbackFailed = true
                    runCatching { failedPlayer.release() }
                    if (playerState.value === failedPlayer) playerState.value = null
                    true
                }
                mediaPlayer.setDataSource(audioFile.absolutePath)
                mediaPlayer.prepareAsync()
            } catch (error: Exception) {
                runCatching { createdPlayer?.release() }
                playerState.value = null
                audioFileState.value?.delete()
                audioFileState.value = null
                isLoading = false
                playbackFailed = true
                android.util.Log.w("ArturChatVoice", "Voice playback failed", error)
            }
        }
    }

    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            playbackPositionMs = runCatching { playerState.value?.currentPosition?.toLong() ?: 0L }.getOrDefault(0L)
            delay(120L)
        }
    }
    DisposableEffect(path) {
        onDispose {
            runCatching { playerState.value?.release() }
            audioFileState.value?.delete()
        }
    }

    val progress = if (safeDurationMs > 0L) {
        (playbackPositionMs.toFloat() / safeDurationMs.toFloat()).coerceIn(0f, 1f)
    } else 0f

    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(34.dp).clip(CircleShape)
                    .background(Brush.linearGradient(listOf(Color(0xFF2CA8FF), Color(0xFF5266E8), Color(0xFF9362E8))))
                    .border(1.dp, Color.White.copy(alpha = 0.86f), CircleShape)
                    .clickable(enabled = !isLoading) { togglePlayback() },
                contentAlignment = Alignment.Center
            ) {
                if (isLoading) {
                    androidx.compose.material3.CircularProgressIndicator(
                        modifier = Modifier.size(14.dp), color = Color.White, strokeWidth = 1.8.dp
                    )
                } else ChatPlaybackGlyph(isPlaying)
            }
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                VoiceWaveformBars(
                    waveform = displayWaveform,
                    progress = progress,
                    modifier = Modifier.fillMaxWidth().height(22.dp)
                )
                Spacer(Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        formatVoiceDuration(playbackPositionMs),
                        color = InboxMuted,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        " / " + formatVoiceDuration(safeDurationMs),
                        color = InboxMuted.copy(alpha = 0.78f),
                        fontSize = 8.sp
                    )
                    if (isLoading) {
                        Spacer(Modifier.width(6.dp))
                        Text("Загрузка", color = InboxAccentStrong, fontSize = 8.sp)
                    }
                }
            }
        }
        if (playbackFailed) {
            Text("Не удалось воспроизвести аудио", color = Color(0xFFB85E79), fontSize = 8.sp)
        }
    }
}
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SelectionActionButton(
    icon: String,
    label: String,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 2.dp, vertical = 5.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(icon, color = color, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(2.dp))
        Text(
            label,
            color = color,
            fontSize = 9.sp,
            maxLines = 1,
            softWrap = false,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun ChatDateDivider(label: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 14.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        Surface(
            color = if (isDarkAppearance) PanelLight else Color(0xDDE7ECF8),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, ThemeBorder),
            shadowElevation = 0.dp
        ) {
            Text(
                text = label,
                color = if (isDarkAppearance) InboxSecondary else Color(0xFF65718A),
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.15.sp,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
            )
        }
    }
}

private fun formatChatDateDivider(
    date: java.time.LocalDate,
    today: java.time.LocalDate
): String {
    return when (date) {
        today -> "Сегодня"
        today.minusDays(1) -> "Вчера"
        else -> {
            val pattern = if (date.year == today.year) "d MMMM" else "d MMMM yyyy"
            java.time.format.DateTimeFormatter.ofPattern(pattern, Locale("ru")).format(date)
        }
    }
}

@Composable
private fun MessageBubble(
    message: ChatMessage,
    showTime: Boolean = true,
    separateFromPrevious: Boolean = false,
    sameSenderPrevious: Boolean = false,
    replyPreview: MessageReplyPreview? = null,
    attachmentPath: String? = null,
    attachmentRepository: SupabaseChatRepository? = null,
    onReply: (() -> Unit)? = null,
    onCopy: (() -> Unit)? = null,
    onForward: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
    onReplyPreviewClick: (() -> Unit)? = null,
    onEdit: (() -> Unit)? = null,
    reactions: List<MessageReactionCount> = emptyList(),
    onReaction: ((String?) -> Unit)? = null,
    selected: Boolean = false,
    onSelect: (() -> Unit)? = null,
    onToggleSelection: (() -> Unit)? = null
) {
    var showActions by remember(message.id) { mutableStateOf(false) }
    // Bounded swipe animation: the message follows the finger, then springs back.
    val replyDragOffset = remember(message.id) { Animatable(0f) }
    val replyAnimationScope = rememberCoroutineScope()
    val density = LocalDensity.current
    val maxReplyDragOffsetPx = with(density) { 72.dp.toPx() }
    val deliveryTicks = if (message.isMine && showTime) {
        when (message.deliveryStatus) {
            MessageDeliveryStatus.SENT -> 1 to Muted
            MessageDeliveryStatus.DELIVERED -> 2 to Muted
            MessageDeliveryStatus.READ -> 2 to if (appOrangeChocolateTheme.value) Color(0xFFFFB35C) else Color(0xFF5D6C84)
            null -> null
        }
    } else null
    val messageText = message.text
    val isVoiceAttachment = message.attachmentMimeType?.startsWith("audio/") == true && attachmentPath != null
    val isVideoNoteAttachment = message.attachmentMimeType == "video/mp4" && attachmentPath != null
    val isImageAttachment = message.attachmentMimeType?.startsWith("image/") == true && attachmentPath != null
    val hasImageCaption = isImageAttachment &&
        messageText.isNotBlank() && !messageText.equals("[Фото]", ignoreCase = true)
    val bubbleShape = RoundedCornerShape(
        topStart = 18.dp, topEnd = 18.dp,
        bottomStart = if (message.isMine) 18.dp else 5.dp,
        bottomEnd = if (message.isMine) 5.dp else 18.dp
    )

    Row(
        Modifier.fillMaxWidth().padding(
            top = when {
                separateFromPrevious -> 8.dp
                sameSenderPrevious -> 3.dp
                else -> 0.dp
            }
        ),
        horizontalArrangement = if (message.isMine) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom
    ) {
        Box(
            modifier = Modifier.fillMaxWidth()
                            .pointerInput(message.id, onReply) {
                if (onReply != null) {
                    var horizontalDragDistance = 0f
                    var currentDragOffset = 0f
                    val maxDragOffset = 72.dp.toPx()
                    val replyThreshold = 56.dp.toPx()

                    detectHorizontalDragGestures(
                        onDragStart = {
                            horizontalDragDistance = 0f
                            currentDragOffset = 0f
                            replyAnimationScope.launch { replyDragOffset.snapTo(0f) }
                        },
                        onHorizontalDrag = { change, dragAmount ->
                            horizontalDragDistance += dragAmount
                            if (horizontalDragDistance < 0f) {
                                change.consume()
                                currentDragOffset = (currentDragOffset + dragAmount)
                                    .coerceIn(-maxDragOffset, 0f)
                                replyAnimationScope.launch {
                                    replyDragOffset.snapTo(currentDragOffset)
                                }
                            }
                        },
                        onDragEnd = {
                            val shouldReply = horizontalDragDistance <= -replyThreshold
                            if (shouldReply) onReply.invoke()
                            horizontalDragDistance = 0f
                            currentDragOffset = 0f
                            replyAnimationScope.launch {
                                replyDragOffset.animateTo(
                                    targetValue = 0f,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioNoBouncy,
                                        stiffness = Spring.StiffnessMedium
                                    )
                                )
                            }
                        },
                        onDragCancel = {
                            horizontalDragDistance = 0f
                            currentDragOffset = 0f
                            replyAnimationScope.launch {
                                replyDragOffset.animateTo(
                                    targetValue = 0f,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioNoBouncy,
                                        stiffness = Spring.StiffnessMedium
                                    )
                                )
                            }
                        }
                    )
                }
            }
                .padding(horizontal = 16.dp),
            contentAlignment = if (message.isMine) Alignment.BottomEnd else Alignment.BottomStart
        ) {
            if (replyDragOffset.value < -8f) {
                val replyHintProgress = (-replyDragOffset.value / maxReplyDragOffsetPx).coerceIn(0f, 1f)
                Box(
                    modifier = Modifier.align(Alignment.CenterEnd)
                        .padding(end = 5.dp)
                        .size(28.dp)
                        .graphicsLayer {
                            alpha = replyHintProgress
                            val hintScale = 0.82f + replyHintProgress * 0.18f
                            scaleX = hintScale
                            scaleY = hintScale
                        }
                        .clip(CircleShape)
                        .background(ThemeSurfaceRaised),
                    contentAlignment = Alignment.Center
                ) {
                    Text("↩", color = AccentSlate, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
            }
            Surface(
                // Delivery ticks are inline with the final text line, so they cannot create a separate row.
                modifier = Modifier
                    .then(
                        when {
                            isVideoNoteAttachment -> Modifier.size(190.dp)
                            isVoiceAttachment -> Modifier.widthIn(min = 205.dp, max = 276.dp)
                            isImageAttachment -> Modifier.widthIn(max = 300.dp)
                            else -> Modifier.widthIn(max = 310.dp)
                        }
                    )
                    // The swipe listener lives on the full message row above.
                    .graphicsLayer {
                        translationX = replyDragOffset.value
                        val dragProgress = (-replyDragOffset.value / 72.dp.toPx()).coerceIn(0f, 1f)
                        val subtleScale = 1f - dragProgress * 0.012f
                        scaleX = subtleScale
                        scaleY = subtleScale
                    }
                    .combinedClickable(
                        enabled = onSelect != null || onToggleSelection != null || onReply != null || onCopy != null,
                        onClick = {
                            if (selected || onToggleSelection != null) {
                                onToggleSelection?.invoke()
                            } else {
                                showActions = true
                            }
                        },
                        onLongClick = {
                            // Long press starts message selection; taps are reserved for the context menu.
                            if (!selected) onSelect?.invoke()
                        }
                    ),
                color = when {
                    selected -> when { appOrangeChocolateTheme.value -> Color(0xFF89501E); appDarkTheme.value -> Color(0xFF40577D); else -> Color(0xFF879BCB) }
                    isImageAttachment || isVideoNoteAttachment -> Color.Transparent
                    appOrangeChocolateTheme.value && message.isMine -> Color(0xFF623315)
                    appOrangeChocolateTheme.value -> Color(0xFF32190D)
                    appDarkTheme.value && message.isMine -> Color(0xFF293A55)
                    appDarkTheme.value -> Color(0xFF1D2939)
                    message.isMine -> Color(0xE9E4EBFF)
                    else -> Color(0xEAFBFCFF)
                },
                border = if ((isImageAttachment || isVideoNoteAttachment) && !selected) null else BorderStroke(
                    if (selected) 1.4.dp else 1.dp,
                    when {
                        selected -> when { appOrangeChocolateTheme.value -> Color(0xFFC77A35); appDarkTheme.value -> Color(0xFF738DB9); else -> Color(0xFF607BB9) }
                        appOrangeChocolateTheme.value && message.isMine -> Color(0xFF9E5727)
                        appOrangeChocolateTheme.value -> Color(0xFF63371E)
                        appDarkTheme.value && message.isMine -> Color(0xFF536D99)
                        appDarkTheme.value -> Color(0xFF35445A)
                        message.isMine -> Color(0xFFBFCBFA).copy(alpha = 0.82f)
                        else -> Color.White.copy(alpha = 0.92f)
                    }
                ),
                shadowElevation = when {
                    selected -> 3.dp
                    isImageAttachment || isVideoNoteAttachment -> 0.dp
                    else -> 1.dp
                },
                shape = bubbleShape
            ) {
                Column(
                    modifier = if (isImageAttachment || isVideoNoteAttachment) Modifier else Modifier.padding(
                        start = if (isVoiceAttachment) 9.dp else 14.dp,
                        end = if (isVoiceAttachment) 9.dp else 14.dp,
                        top = if (isVoiceAttachment) 6.dp else 7.dp,
                        bottom = if (isVoiceAttachment) {
                            if (showTime) 2.dp else 4.dp
                        } else if (showTime) 5.dp else 8.dp
                    )
                ) {
                    if (message.forwardedFromName != null) {
                        Text(
                            "Переслано от: ${message.forwardedFromName}",
                            color = AccentSlate,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = if (isImageAttachment) Modifier.padding(start = 12.dp, end = 12.dp, top = 9.dp) else Modifier
                        )
                        if (!isImageAttachment) Spacer(Modifier.height(3.dp))
                    }
                    if (replyPreview != null && !message.isDeleted) {
                        Row(
                            Modifier.fillMaxWidth()
                                .then(if (isImageAttachment || isVideoNoteAttachment) Modifier.padding(start = 10.dp, end = 10.dp, top = 8.dp) else Modifier)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    when {
                                        appOrangeChocolateTheme.value && message.isMine -> Color(0xFF75411C)
                                        appOrangeChocolateTheme.value -> Color(0xFF4B2917)
                                        appDarkTheme.value && message.isMine -> Color(0xFF3A4D6C)
                                        appDarkTheme.value -> Color(0xFF2B394D)
                                        message.isMine -> Color(0xFFD9DFFF)
                                        else -> Color(0xFFE0E8F8)
                                    }
                                )
                                .clickable(enabled = onReplyPreviewClick != null) {
                                    onReplyPreviewClick?.invoke()
                                }
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(Modifier.width(3.dp).height(34.dp).clip(CircleShape).background(AccentSlate))
                            Spacer(Modifier.width(7.dp))
                            Column(Modifier.weight(1f)) {
                                Text(replyPreview.sender, color = AccentSlate, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                                Text(
                                    replyPreview.text,
                                    color = MainText,
                                    fontSize = 11.sp,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                    }
                    val isVoiceMessage = message.attachmentMimeType?.startsWith("audio/") == true
                    if (attachmentPath != null && attachmentRepository != null) {
                        when {
                            isVoiceMessage -> {
                                ChatVoiceMessagePlayer(
                                    path = attachmentPath,
                                    repository = attachmentRepository,
                                    durationMs = message.attachmentDurationMs ?: 0L,
                                    waveform = message.attachmentWaveform.orEmpty()
                                )
                                Spacer(Modifier.height(1.dp))
                            }
                            isVideoNoteAttachment -> {
                                Box(Modifier.size(190.dp)) {
                                    ChatVideoNotePlayer(
                                        path = attachmentPath,
                                        repository = attachmentRepository,
                                        durationMs = message.attachmentDurationMs ?: 0L,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    if (showTime) {
                                        ImageMessageMetadata(
                                            time = message.time,
                                            deliveryTicks = deliveryTicks,
                                            modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp)
                                        )
                                    }
                                }
                            }
                            else -> {
                                Box {
                                    ChatImagePreview(
                                        path = attachmentPath,
                                        repository = attachmentRepository,
                                        modifier = Modifier.widthIn(max = 300.dp)
                                    )
                                    if (showTime) {
                                        ImageMessageMetadata(
                                            time = message.time,
                                            deliveryTicks = deliveryTicks,
                                            modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp)
                                        )
                                    }
                                }
                            }
                        }
                        if (hasImageCaption) {
                            Text(
                                text = messageText,
                                color = MainText,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 9.dp)
                            )
                        }
                    } else if ((!isVoiceMessage && !isVideoNoteAttachment) || attachmentPath == null) {
                        Text(
                            text = messageText,
                            color = MainText,
                            fontSize = 14.sp
                        )
                    }
                    if (showTime && !isImageAttachment && !isVideoNoteAttachment) {
                        Row(
                            modifier = Modifier.align(Alignment.End).padding(top = if (isVoiceAttachment) 0.dp else 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            if (message.isEdited) {
                                Text(
                                    "изменено",
                                    color = Muted,
                                    fontSize = 8.sp
                                )
                            }
                            Text(
                                message.time,
                                color = Muted,
                                fontSize = 9.sp
                            )
                            deliveryTicks?.let { (count, tickColor) ->
                                Canvas(
                                    modifier = Modifier
                                        .padding(start = 1.dp, end = 1.dp)
                                        .size(width = if (count == 1) 8.dp else 13.dp, height = 9.dp)
                                ) {
                                    val unit = 4.dp.toPx()
                                    val arm = 2.2.dp.toPx()
                                    val rise = 6.2.dp.toPx()
                                    val stroke = 1.25.dp.toPx()
                                    repeat(count) { index ->
                                        val x = index * unit
                                        val joint = Offset(x + arm, size.height * 0.72f)
                                        drawLine(
                                            color = tickColor,
                                            start = Offset(x, size.height * 0.48f),
                                            end = joint,
                                            strokeWidth = stroke,
                                            cap = StrokeCap.Round
                                        )
                                        drawLine(
                                            color = tickColor,
                                            start = joint,
                                            end = Offset(x + rise, size.height * 0.2f),
                                            strokeWidth = stroke,
                                            cap = StrokeCap.Round
                                        )
                                    }
                                }
                            }
                        }
                    }
                    if (reactions.isNotEmpty()) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                            horizontalArrangement = if (message.isMine) Arrangement.End else Arrangement.Start,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            reactions.forEach { reaction ->
                                Surface(
                                    modifier = Modifier.padding(end = 4.dp).clickable(
                                        enabled = onReaction != null
                                    ) {
                                        onReaction?.invoke(
                                            if (reaction.selectedByMe) null else reaction.reaction
                                        )
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    color = when {
                                        appOrangeChocolateTheme.value && reaction.selectedByMe -> Color(0xFF633516)
                                        appOrangeChocolateTheme.value -> Color(0xFF3B2114)
                                        appDarkTheme.value && reaction.selectedByMe -> Color(0xFF34496A)
                                        appDarkTheme.value -> Color(0xFF273447)
                                        reaction.selectedByMe -> Color(0xFFDCE4FF)
                                        else -> Color(0xFFF2F4FA)
                                    },
                                    border = BorderStroke(
                                        1.dp,
                                        when {
                                            appOrangeChocolateTheme.value && reaction.selectedByMe -> Color(0xFFB36B2E)
                                            appOrangeChocolateTheme.value -> Color(0xFF674129)
                                            appDarkTheme.value && reaction.selectedByMe -> Color(0xFF627CA9)
                                            appDarkTheme.value -> Color(0xFF3B4A5F)
                                            reaction.selectedByMe -> Color(0xFF8EA2E8)
                                            else -> Color(0xFFE2E7F1)
                                        }
                                    ),
                                    shadowElevation = 0.dp
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                                    ) {
                                        Text(reaction.reaction, fontSize = 13.sp)
                                        Text(
                                            reaction.count.toString(),
                                            color = if (reaction.selectedByMe) AccentSlate else Muted,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            DropdownMenu(
                expanded = showActions,
                onDismissRequest = { showActions = false },
                // Keep the chat composer focus and current IME state unchanged when
                // the quick-reaction menu opens and closes.
                properties = PopupProperties(focusable = false),
                containerColor = PanelLight
            ) {
                if (onReaction != null && !message.isDeleted) {
                    Surface(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(24.dp),
                        color = when { appOrangeChocolateTheme.value -> Color(0xFF3D2414); appDarkTheme.value -> Color(0xFF263B35); else -> Color(0xFFE8F3E5) },
                        border = BorderStroke(1.dp, when { appOrangeChocolateTheme.value -> Color(0xFF75502D); appDarkTheme.value -> Color(0xFF3C6455); else -> Color.White.copy(alpha = 0.92f) }),
                        shadowElevation = 1.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            MESSAGE_REACTION_OPTIONS.forEach { emoji ->
                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .clickable {
                                            showActions = false
                                            val alreadySelected = reactions.any {
                                                it.reaction == emoji && it.selectedByMe
                                            }
                                            onReaction.invoke(if (alreadySelected) null else emoji)
                                        }
                                        .padding(horizontal = 3.dp, vertical = 5.dp)
                                ) {
                                    Text(emoji, fontSize = 20.sp)
                                }
                            }
                            Text("⌄", color = when { appOrangeChocolateTheme.value -> Color(0xFFFFB15C); appDarkTheme.value -> Color(0xFF9CC8B5); else -> Color(0xFF769173) }, fontSize = 16.sp)
                        }
                    }
                }
                if (onReply != null) {
                    DropdownMenuItem(
                        text = { Text("Ответить", color = MainText) },
                        leadingIcon = { Text("↩", color = MainText, fontSize = 18.sp) },
                        onClick = {
                            showActions = false
                            onReply()
                        }
                    )
                }
                if (onCopy != null) {
                    DropdownMenuItem(
                        text = { Text("Копировать", color = MainText) },
                        leadingIcon = { Text("▢", color = MainText, fontSize = 18.sp) },
                        onClick = {
                            showActions = false
                            onCopy()
                        }
                    )
                }
                if (onEdit != null) {
                    DropdownMenuItem(
                        text = { Text("Изменить", color = MainText) },
                        leadingIcon = { Text("✎", color = MainText, fontSize = 17.sp) },
                        onClick = {
                            showActions = false
                            onEdit()
                        }
                    )
                }
                if (onForward != null) {
                    DropdownMenuItem(
                        text = { Text("Переслать", color = MainText) },
                        leadingIcon = { Text("➦", color = MainText, fontSize = 18.sp) },
                        onClick = {
                            showActions = false
                            onForward()
                        }
                    )
                }
                if (onDelete != null) {
                    DropdownMenuItem(
                        text = { Text("Удалить", color = Color(0xFFB85E69)) },
                        leadingIcon = { Text("▤", color = Color(0xFFB85E69), fontSize = 18.sp) },
                        onClick = {
                            showActions = false
                            onDelete()
                        }
                    )
                }
                if (onSelect != null) {
                    DropdownMenuItem(
                        text = { Text("Выбрать", color = MainText) },
                        leadingIcon = { Text("☑", color = MainText, fontSize = 17.sp) },
                        onClick = {
                            showActions = false
                            onSelect()
                        }
                    )
                }
            }
        }
    }
}

private const val MAX_CHAT_VOICE_BYTES = 10 * 1024 * 1024
private const val MAX_CHAT_VOICE_DURATION_MS = 10 * 60 * 1000L

private class ChatVoiceRecorder(private val cacheDirectory: File) {
    private var recorder: MediaRecorder? = null
    private var outputFile: File? = null

    fun start() {
        check(recorder == null) { "Запись уже запущена." }
        val file = File(cacheDirectory, "voice-" + UUID.randomUUID().toString() + ".m4a")
        val activeRecorder = MediaRecorder()
        try {
            activeRecorder.setAudioSource(MediaRecorder.AudioSource.MIC)
            activeRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            activeRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            activeRecorder.setAudioEncodingBitRate(64_000)
            activeRecorder.setAudioSamplingRate(44_100)
            activeRecorder.setMaxFileSize(MAX_CHAT_VOICE_BYTES.toLong())
            activeRecorder.setOutputFile(file.absolutePath)
            activeRecorder.prepare()
            activeRecorder.start()
            recorder = activeRecorder
            outputFile = file
        } catch (error: Exception) {
            runCatching { activeRecorder.reset() }
            activeRecorder.release()
            file.delete()
            throw error
        }
    }

    fun readAmplitudeLevel(): Int {
        val activeRecorder = recorder ?: return 0
        // Keep the raw peak; per-recording normalization happens only when bars are built.
        return runCatching { activeRecorder.maxAmplitude.coerceIn(0, 32_767) }.getOrDefault(0)
    }

    fun stop(): File {
        val activeRecorder = recorder ?: throw IllegalStateException("Запись не запущена.")
        val completedFile = outputFile ?: throw IllegalStateException("Файл записи не найден.")
        recorder = null
        outputFile = null
        try {
            activeRecorder.stop()
            require(completedFile.isFile && completedFile.length() > 0L) {
                "Запись не содержит аудиоданных."
            }
            return completedFile
        } catch (error: Exception) {
            completedFile.delete()
            throw error
        } finally {
            runCatching { activeRecorder.reset() }
            activeRecorder.release()
        }
    }

    fun cancel() {
        val activeRecorder = recorder
        val partialFile = outputFile
        recorder = null
        outputFile = null
        if (activeRecorder != null) {
            runCatching { activeRecorder.stop() }
            runCatching { activeRecorder.reset() }
            activeRecorder.release()
        }
        partialFile?.delete()
    }
}

@Composable
private fun MinimizeCallButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        color = ThemeSurfaceRaised,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, ThemeBorder)
    ) {
        Text(
            "↓  Свернуть",
            color = AccentSlate,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 13.dp, vertical = 9.dp)
        )
    }
}

@Composable
private fun ActiveCallMiniBar(
    peerName: String,
    mediaType: String,
    status: String,
    duration: String?,
    onOpen: () -> Unit
) {
    val callType = if (mediaType == "video") "Видеозвонок" else "Аудиозвонок"
    val isConnected = duration != null || status == "Звонок активен"
    val stateTint = if (isConnected) InboxOnline else AccentSlate
    val cardShape = RoundedCornerShape(21.dp)
    val cardColors = when {
        appOrangeChocolateTheme.value -> listOf(Color(0xF02B160B), Color(0xF03A1D0D), Color(0xF02B160B))
        appDarkTheme.value -> listOf(Color(0xF2192231), Color(0xF2202B3A), Color(0xF2192231))
        else -> listOf(Color(0xFFF8FAFF), Color(0xFFECEBFF), Color(0xFFE5F1FF))
    }
    val brandColors = when {
        appOrangeChocolateTheme.value -> listOf(Color(0xFFFFC27A), Color(0xFFFF8D2A), Color(0xFFA84512))
        appDarkTheme.value -> listOf(Color(0xFF7188D8), Color(0xFFA7B6FF), Color(0xFF5267A8))
        else -> listOf(Color(0xFF38A6F6), Color(0xFF725FE6), Color(0xFF9460DC))
    }
    val cardBrush = Brush.linearGradient(cardColors)
    val brandBrush = Brush.linearGradient(brandColors)

    Surface(
        modifier = Modifier.fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 8.dp),
        color = Color.Transparent,
        shape = cardShape,
        border = BorderStroke(1.dp, ThemeBorder.copy(alpha = 0.92f)),
        shadowElevation = 4.dp
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
                .clip(cardShape)
                .background(cardBrush)
                .clickable(onClick = onOpen)
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(23.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(brandBrush),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "A",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-0.4).sp
                    )
                }
                Spacer(Modifier.width(7.dp))
                Text(
                    "A-CHAT",
                    color = AccentSlate,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.15.sp
                )
                Spacer(Modifier.weight(1f))
                Row(
                    modifier = Modifier.clip(CircleShape)
                        .background(stateTint.copy(alpha = 0.10f))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier.size(6.dp).background(stateTint, CircleShape)
                    )
                    Spacer(Modifier.width(5.dp))
                    Text(
                        if (isConnected) "ИДЁТ ЗВОНОК" else "СОЕДИНЕНИЕ",
                        color = stateTint,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.35.sp
                    )
                }
            }

            Spacer(Modifier.height(9.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(43.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        Modifier.fillMaxSize()
                            .background(
                                Brush.sweepGradient(brandColors + brandColors.first()),
                                CircleShape
                            )
                            .padding(2.dp)
                    ) {
                        Avatar(
                            peerName.take(2).uppercase(),
                            peerName.hashCode().ushr(1) % 4,
                            Modifier.fillMaxSize()
                        )
                    }
                    Box(
                        Modifier.align(Alignment.BottomEnd)
                            .size(19.dp)
                            .background(ThemeSurfaceRaised, CircleShape)
                            .padding(2.dp)
                            .background(brandBrush, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        ChatCallGlyph(
                            video = mediaType == "video",
                            tint = Color.White
                        )
                    }
                }

                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        peerName,
                        color = MainText,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        if (duration != null) "$callType  ·  $duration" else "$callType  ·  $status",
                        color = if (isConnected) InboxOnline else Muted,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(Modifier.width(8.dp))
                Row(
                    modifier = Modifier.clip(RoundedCornerShape(13.dp))
                        .background(brandBrush)
                        .padding(horizontal = 10.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Вернуться",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "›",
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun CallHistoryBubble(
    mediaType: String,
    status: String,
    durationSeconds: Int,
    isOutgoing: Boolean,
    time: String
) {
    val title = if (mediaType == "video") "Видеозвонок" else "Аудиозвонок"
    val duration = formatCallDurationSeconds(durationSeconds)
    val (subtitle, isMissed) = when (status) {
        "ended" -> (duration?.let { "Разговор · $it" } ?: "Разговор завершён") to false
        "declined" -> (if (isOutgoing) "Собеседник отклонил" else "Вы отклонили") to true
        "cancelled" -> (if (isOutgoing) "Нет ответа" else "Пропущенный вызов") to true
        else -> "Звонок завершён" to false
    }
    val accent = if (isMissed) InboxDanger else AccentSlate
    val cardBackground = when {
        appOrangeChocolateTheme.value && isMissed -> Color(0xFF421A13)
        appOrangeChocolateTheme.value -> Color(0xFF351A0C)
        appDarkTheme.value && isMissed -> Color(0xFF3A202A)
        appDarkTheme.value -> Color(0xFF172131)
        isMissed -> Color(0xFFFFF0F2)
        else -> Color(0xFFF0F3FF)
    }
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 5.dp),
        horizontalArrangement = if (isOutgoing) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            modifier = Modifier.widthIn(max = 270.dp),
            color = cardBackground,
            shape = RoundedCornerShape(17.dp),
            border = BorderStroke(1.dp, accent.copy(alpha = 0.13f))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 13.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier.size(36.dp).clip(CircleShape)
                        .background(accent.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        if (isOutgoing) "↗" else "↙",
                        color = accent,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(title, color = MainText, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(2.dp))
                    Text(
                        subtitle,
                        color = accent,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(Modifier.width(10.dp))
                Text(time, color = Muted, fontSize = 9.sp, maxLines = 1)
            }
        }
    }
}

private fun formatCallDurationSeconds(totalSeconds: Int): String? {
    if (totalSeconds <= 0) return null
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%02d:%02d".format(minutes, seconds)
    }
}

@Composable
private fun ActiveAudioCallScreen(
    peerName: String,
    status: String,
    duration: String?,
    error: String?,
    muted: Boolean,
    speakerEnabled: Boolean,
    onToggleMute: () -> Unit,
    onToggleSpeaker: () -> Unit,
    onMinimize: () -> Unit,
    onEnd: () -> Unit
) {
    val avatarRingColors = when {
        appOrangeChocolateTheme.value -> listOf(Color(0xFFFFD08A), Color(0xFFFF9B3D), Color(0xFFC75A19))
        appDarkTheme.value -> listOf(Color(0xFF7188D8), Color(0xFFA7B6FF), Color(0xFF5267A8))
        else -> listOf(Color(0xFF39A7FF), Color(0xFF5868E8), Color(0xFF9564E9))
    }
    val avatarHaloColor = when {
        appOrangeChocolateTheme.value -> Color(0xFFFF9B3D)
        appDarkTheme.value -> Color(0xFF8FA4FF)
        else -> Color(0xFF9EBBFF)
    }
    val callErrorBackground = when {
        appOrangeChocolateTheme.value -> Color(0xE53A1D0D)
        appDarkTheme.value -> Color(0xE52C202B)
        else -> Color(0xBFFFF0F2)
    }
    val callErrorBorder = when {
        appOrangeChocolateTheme.value -> Color(0x885E351D)
        appDarkTheme.value -> Color(0x887B4658)
        else -> Color(0x44D77782)
    }
    val callErrorText = when {
        appOrangeChocolateTheme.value -> Color(0xFFFFB59A)
        appDarkTheme.value -> Color(0xFFFFC3CE)
        else -> Color(0xFFA85F6E)
    }
    Column(
        modifier = Modifier.fillMaxSize()
            .arturChatGradientBackground()
            .padding(horizontal = 22.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "A-CHAT",
                color = AccentSlate,
                fontSize = 10.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 2.2.sp
            )
            MinimizeCallButton(onClick = onMinimize)
        }
        Spacer(Modifier.weight(0.72f))
        Box(
            modifier = Modifier.size(164.dp)
                .background(
                    Brush.radialGradient(
                        listOf(
                            avatarHaloColor.copy(alpha = 0.42f),
                            avatarHaloColor.copy(alpha = 0.16f),
                            Color.Transparent
                        )
                    ),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier.size(136.dp)
                    .background(
                        Brush.linearGradient(avatarRingColors),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Avatar(
                    peerName.take(2).uppercase(),
                    peerName.hashCode().ushr(1) % 4,
                    Modifier.size(122.dp)
                )
            }
        }
        Spacer(Modifier.height(22.dp))
        Text(
            peerName,
            color = MainText,
            fontSize = 25.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(12.dp))
        Surface(
            color = ThemeSurfaceRaised,
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, ThemeBorder)
        ) {
            Row(
                Modifier.padding(horizontal = 15.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier.size(7.dp).clip(CircleShape)
                        .background(if (error == null) InboxOnline else InboxDanger)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    status,
                    color = AccentSlate,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        if (duration != null) {
            Spacer(Modifier.height(9.dp))
            Text(
                duration,
                color = AccentSlate,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp
            )
        }
        if (error != null) {
            Spacer(Modifier.height(12.dp))
            Surface(
                modifier = Modifier.fillMaxWidth().heightIn(max = 190.dp),
                color = callErrorBackground,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, callErrorBorder)
            ) {
                Text(
                    error,
                    color = callErrorText,
                    fontSize = 10.sp,
                    lineHeight = 15.sp,
                    maxLines = 12,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 13.dp, vertical = 10.dp)
                )
            }
        }
        Spacer(Modifier.weight(1f))
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 13.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            CallControlButton(
                label = if (muted) "Микрофон выкл." else "Микрофон",
                active = !muted,
                labelColor = MainText,
                onClick = onToggleMute
            ) { tint -> ChatMicrophoneGlyph(tint = tint) }
            CallControlButton(
                label = if (speakerEnabled) "Динамик" else "Телефон",
                active = speakerEnabled,
                labelColor = MainText,
                onClick = onToggleSpeaker
            ) { tint -> CallSpeakerGlyph(tint = tint, enabled = speakerEnabled) }
            CallControlButton(
                label = "Завершить",
                destructive = true,
                labelColor = MainText,
                onClick = onEnd
            ) { tint ->
                Text("×", color = tint, fontSize = 31.sp, fontWeight = FontWeight.Light)
            }
        }
        Spacer(Modifier.height(5.dp))
        Text("Звонок один на один · WebRTC", color = Muted, fontSize = 9.sp)
    }
}

@Composable
private fun ActiveVideoCallScreen(
    peerName: String,
    status: String,
    duration: String?,
    error: String?,
    muted: Boolean,
    cameraEnabled: Boolean,
    client: WebRtcAudioCallClient,
    onToggleMute: () -> Unit,
    onToggleCamera: () -> Unit,
    onSwitchCamera: () -> Unit,
    onMinimize: () -> Unit,
    onEnd: () -> Unit
) {
    val remoteRenderer = client.remoteVideoRenderer
    val localRenderer = client.localVideoRenderer
    val callOverlayColor = ThemeSurfaceRaised.copy(alpha = 0.93f)
    val callOverlayBorder = ThemeBorder.copy(alpha = 0.90f)
    val callErrorBackground = when {
        appOrangeChocolateTheme.value -> Color(0xE53A1D0D)
        appDarkTheme.value -> Color(0xE52C202B)
        else -> Color(0xE5FFF0F2)
    }
    val callErrorBorder = when {
        appOrangeChocolateTheme.value -> Color(0x885E351D)
        appDarkTheme.value -> Color(0x887B4658)
        else -> Color(0x55D77782)
    }
    val callErrorText = when {
        appOrangeChocolateTheme.value -> Color(0xFFFFB59A)
        appDarkTheme.value -> Color(0xFFFFC3CE)
        else -> Color(0xFFA85F6E)
    }

    Box(Modifier.fillMaxSize().arturChatGradientBackground()) {
        if (remoteRenderer != null) {
            AndroidView(factory = { remoteRenderer }, modifier = Modifier.fillMaxSize())
        } else {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Подготавливаем видеосвязь…", color = MainText, fontSize = 14.sp)
            }
        }

        if (localRenderer != null) {
            AndroidView(
                factory = { localRenderer },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 12.dp, end = 12.dp)
                    .size(width = 104.dp, height = 146.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .border(1.dp, Color.White.copy(alpha = 0.52f), RoundedCornerShape(18.dp))
            )
        }

        MinimizeCallButton(
            onClick = onMinimize,
            modifier = Modifier.align(Alignment.TopStart)
                .padding(start = 14.dp, top = 12.dp)
        )

        Surface(
            modifier = Modifier.align(Alignment.TopStart)
                .padding(start = 14.dp, top = 62.dp, end = 126.dp)
                .widthIn(max = 230.dp),
            color = callOverlayColor,
            shape = RoundedCornerShape(18.dp),
            border = BorderStroke(1.dp, callOverlayBorder)
        ) {
            Column(Modifier.padding(horizontal = 13.dp, vertical = 10.dp)) {
                Text(
                    peerName,
                    color = MainText,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    status,
                    color = Muted,
                    fontSize = 10.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (duration != null) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        duration,
                        color = MainText,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp
                    )
                }
            }
        }

        if (error != null) {
            Surface(
                modifier = Modifier.align(Alignment.TopStart)
                    .padding(start = 14.dp, top = 78.dp, end = 126.dp)
                    .widthIn(max = 230.dp),
                color = callErrorBackground,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, callErrorBorder)
            ) {
                Text(
                    error,
                    color = callErrorText,
                    fontSize = 9.sp,
                    lineHeight = 13.sp,
                    maxLines = 5,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 11.dp, vertical = 9.dp)
                )
            }
        }

        Surface(
            modifier = Modifier.align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            color = callOverlayColor,
            shape = RoundedCornerShape(26.dp),
            border = BorderStroke(1.dp, callOverlayBorder),
            shadowElevation = 8.dp
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 5.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CallControlButton(
                    label = if (muted) "Микрофон выкл." else "Микрофон",
                    active = !muted,
                    onClick = onToggleMute
                ) { tint -> ChatMicrophoneGlyph(tint = tint) }
                CallControlButton(
                    label = if (cameraEnabled) "Камера" else "Камера выкл.",
                    active = cameraEnabled,
                    onClick = onToggleCamera
                ) { tint -> InboxCameraGlyph(tint = tint, iconSize = 24.dp) }
                CallControlButton(
                    label = "Сменить",
                    active = false,
                    onClick = onSwitchCamera
                ) { tint -> CallSwitchGlyph(tint = tint) }
                CallControlButton(
                    label = "Завершить",
                    destructive = true,
                    onClick = onEnd
                ) { tint ->
                    Text("×", color = tint, fontSize = 29.sp, fontWeight = FontWeight.Light)
                }
            }
        }
    }
}

@Composable
private fun CallControlButton(
    label: String,
    active: Boolean = false,
    destructive: Boolean = false,
    labelColor: Color = MainText,
    onClick: () -> Unit,
    content: @Composable (Color) -> Unit
) {
    val buttonColors = when {
        destructive -> when {
            appOrangeChocolateTheme.value -> listOf(Color(0xFFE97861), Color(0xFFB94734))
            appDarkTheme.value -> listOf(Color(0xFFC95E70), Color(0xFF943D55))
            else -> listOf(Color(0xFFFF7B91), Color(0xFFD94770))
        }
        active -> when {
            appOrangeChocolateTheme.value -> listOf(Color(0xFFFFC27A), Color(0xFFFF8D2A), Color(0xFFA84512))
            appDarkTheme.value -> listOf(Color(0xFF7188D8), Color(0xFFA7B6FF), Color(0xFF5267A8))
            else -> listOf(Color(0xFF35AEFF), Color(0xFF586BEA), Color(0xFF9262E8))
        }
        appOrangeChocolateTheme.value -> listOf(Color(0xFF52301D), Color(0xFF3A1D0D))
        appDarkTheme.value -> listOf(Color(0xFF303E56), Color(0xFF202B3A))
        else -> listOf(Color(0xFFFFFFFF), Color(0xFFE9EEFC))
    }
    val background = Brush.linearGradient(buttonColors)
    val tint = when {
        active && appOrangeChocolateTheme.value -> Color(0xFF3A1B0A)
        active || destructive -> Color.White
        else -> AccentSlate
    }

    Column(
        modifier = Modifier.width(70.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(58.dp)
                .clip(CircleShape)
                .background(background)
                .then(
                    if (active || destructive) Modifier
                    else Modifier.border(1.dp, ThemeBorder.copy(alpha = 0.88f), CircleShape)
                )
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            content(tint)
        }
        Spacer(Modifier.height(7.dp))
        Text(
            label,
            color = labelColor.copy(alpha = if (label.contains("выкл") || label == "Телефон") 0.78f else 0.94f),
            fontSize = 9.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

@Composable
private fun CallSpeakerGlyph(tint: Color, enabled: Boolean) {
    Canvas(Modifier.size(24.dp)) {
        val w = size.width
        val h = size.height
        val stroke = Stroke(width = 1.9.dp.toPx(), cap = StrokeCap.Round)
        val speaker = Path().apply {
            moveTo(w * 0.14f, h * 0.39f)
            lineTo(w * 0.34f, h * 0.39f)
            lineTo(w * 0.62f, h * 0.17f)
            lineTo(w * 0.62f, h * 0.83f)
            lineTo(w * 0.34f, h * 0.61f)
            lineTo(w * 0.14f, h * 0.61f)
            close()
        }
        drawPath(speaker, tint, style = stroke)
        if (enabled) {
            drawArc(
                tint,
                startAngle = -48f,
                sweepAngle = 96f,
                useCenter = false,
                topLeft = Offset(w * 0.42f, h * 0.23f),
                size = androidx.compose.ui.geometry.Size(w * 0.46f, h * 0.54f),
                style = stroke
            )
            drawArc(
                tint,
                startAngle = -48f,
                sweepAngle = 96f,
                useCenter = false,
                topLeft = Offset(w * 0.55f, h * 0.10f),
                size = androidx.compose.ui.geometry.Size(w * 0.42f, h * 0.80f),
                style = stroke
            )
        } else {
            drawLine(tint, Offset(w * 0.68f, h * 0.35f), Offset(w * 0.91f, h * 0.65f), strokeWidth = stroke.width, cap = StrokeCap.Round)
            drawLine(tint, Offset(w * 0.91f, h * 0.35f), Offset(w * 0.68f, h * 0.65f), strokeWidth = stroke.width, cap = StrokeCap.Round)
        }
    }
}

@Composable
private fun CallSwitchGlyph(tint: Color) {
    Canvas(Modifier.size(24.dp)) {
        val w = size.width
        val h = size.height
        val stroke = Stroke(width = 1.9.dp.toPx(), cap = StrokeCap.Round)
        drawArc(
            tint,
            startAngle = 205f,
            sweepAngle = 135f,
            useCenter = false,
            topLeft = Offset(w * 0.17f, h * 0.17f),
            size = androidx.compose.ui.geometry.Size(w * 0.66f, h * 0.66f),
            style = stroke
        )
        drawArc(
            tint,
            startAngle = 25f,
            sweepAngle = 135f,
            useCenter = false,
            topLeft = Offset(w * 0.17f, h * 0.17f),
            size = androidx.compose.ui.geometry.Size(w * 0.66f, h * 0.66f),
            style = stroke
        )
        val firstArrow = Path().apply {
            moveTo(w * 0.20f, h * 0.22f)
            lineTo(w * 0.39f, h * 0.22f)
            lineTo(w * 0.29f, h * 0.39f)
            close()
        }
        val secondArrow = Path().apply {
            moveTo(w * 0.80f, h * 0.78f)
            lineTo(w * 0.61f, h * 0.78f)
            lineTo(w * 0.71f, h * 0.61f)
            close()
        }
        drawPath(firstArrow, tint)
        drawPath(secondArrow, tint)
    }
}

private fun parseCallInstant(value: String?): java.time.Instant? {
    if (value.isNullOrBlank()) return null
    return runCatching { java.time.OffsetDateTime.parse(value).toInstant() }.getOrNull()
        ?: runCatching { java.time.Instant.parse(value) }.getOrNull()
}

private fun formatCallDateTime(value: String): String {
    val instant = parseCallInstant(value) ?: return value.substringAfter('T', value).take(5)
    val zone = java.time.ZoneId.systemDefault()
    val local = instant.atZone(zone)
    val today = java.time.LocalDate.now(zone)
    val dateLabel = when (local.toLocalDate()) {
        today -> "Сегодня"
        today.minusDays(1) -> "Вчера"
        else -> java.time.format.DateTimeFormatter.ofPattern("d MMM", java.util.Locale("ru"))
            .format(local)
    }
    return "$dateLabel · " + java.time.format.DateTimeFormatter.ofPattern("HH:mm").format(local)
}

private fun formatCallDuration(call: FaceVideoCall): String? {
    val start = parseCallInstant(call.answeredAt) ?: return null
    val end = parseCallInstant(call.endedAt)
        ?: if (call.status == "accepted") java.time.Instant.now() else return null
    val seconds = java.time.Duration.between(start, end).seconds.coerceAtLeast(0)
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    val remainder = seconds % 60
    return if (hours > 0) {
        "%d:%02d:%02d".format(hours, minutes, remainder)
    } else {
        "%02d:%02d".format(minutes, remainder)
    }
}

private enum class CallHistoryFilter {
    ALL,
    MISSED,
    INCOMING,
    OUTGOING
}

private fun callStatusLabel(call: FaceVideoCall, currentUserId: String): String {
    return when (call.status) {
        "ringing" -> {
            val age = parseCallInstant(call.createdAt)?.let {
                java.time.Duration.between(it, java.time.Instant.now()).seconds
            } ?: 0
            if (call.calleeId == currentUserId && age > 60) "Пропущен" else "Вызов"
        }
        "accepted" -> "На связи"
        "declined" -> "Отклонён"
        "cancelled" -> if (call.calleeId == currentUserId) "Пропущен" else "Отменён"
        "ended" -> "Завершён"
        else -> "Звонок"
    }
}

@Composable
private fun CallHistoryFilterTab(
    label: String,
    count: Int,
    selected: Boolean,
    modifier: Modifier = Modifier,
    countColor: Color = Muted,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            label,
            color = if (selected) AccentSlate else Muted,
            fontSize = 9.5.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            maxLines = 1,
            softWrap = false
        )
        Spacer(Modifier.height(3.dp))
        Text(
            count.toString(),
            color = if (selected) countColor else Muted.copy(alpha = 0.76f),
            fontSize = 9.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
        )
        Spacer(Modifier.height(5.dp))
        Box(
            Modifier
                .width(22.dp)
                .height(2.dp)
                .clip(CircleShape)
                .background(if (selected) AccentSlate else Color.Transparent)
        )
    }
}

@Composable
private fun CallsScreen(
    repository: SupabaseCallRepository?,
    cache: ArturChatLocalCache,
    userId: String?,
    onOpenProfile: (String) -> Unit
) {
    val cachedCalls = remember(cache, userId) { userId?.let(cache::readCalls).orEmpty() }
    val cachedProfiles = remember(cache, userId) { userId?.let(cache::readCallProfiles).orEmpty() }
    var calls by remember(userId, cache) { mutableStateOf(cachedCalls) }
    var profiles by remember(userId, cache) { mutableStateOf(cachedProfiles) }
    var isLoading by remember(userId, cache) {
        mutableStateOf(repository != null && userId != null && !cache.hasCallsSnapshot(userId))
    }
    var selectedFilter by remember(userId) { mutableStateOf(CallHistoryFilter.ALL) }
    var errorMessage by remember(userId) { mutableStateOf<String?>(null) }
    var refreshKey by remember(userId) { mutableStateOf(0) }

    LaunchedEffect(repository, userId, refreshKey, cache) {
        val currentRepository = repository
        val currentUserId = userId
        if (currentRepository == null || currentUserId == null) {
            calls = emptyList()
            profiles = emptyMap()
            isLoading = false
            errorMessage = "История звонков недоступна."
            return@LaunchedEffect
        }

        isLoading = calls.isEmpty()
        errorMessage = null
        runCatching {
            val loadedCalls = currentRepository.listMyCalls(currentUserId)
            val peerIds = loadedCalls.map { call ->
                if (call.callerId == currentUserId) call.calleeId else call.callerId
            }.toSet()
            val loadedProfiles = currentRepository.getProfiles(peerIds)
            calls = loadedCalls
            profiles = loadedProfiles
            withContext(Dispatchers.IO) {
                cache.saveCalls(currentUserId, loadedCalls)
                cache.saveCallProfiles(currentUserId, loadedProfiles.values)
            }
        }.onFailure {
            if (calls.isEmpty()) errorMessage = it.message ?: "Не удалось загрузить историю звонков."
            android.util.Log.w("ArturChatCalls", "Could not refresh call history; keeping local cache", it)
        }
        isLoading = false
    }

    val currentUserId = userId.orEmpty()
    val missedCount = calls.count { callStatusLabel(it, currentUserId) == "Пропущен" }
    val incomingCount = calls.count { it.calleeId == currentUserId }
    val outgoingCount = calls.count { it.callerId == currentUserId }
    val visibleCalls = remember(calls, userId, selectedFilter) {
        calls.filter { call ->
            when (selectedFilter) {
                CallHistoryFilter.ALL -> true
                CallHistoryFilter.MISSED -> callStatusLabel(call, currentUserId) == "Пропущен"
                CallHistoryFilter.INCOMING -> call.calleeId == currentUserId
                CallHistoryFilter.OUTGOING -> call.callerId == currentUserId
            }
        }
    }
    val filteredEmptyTitle = when (selectedFilter) {
        CallHistoryFilter.ALL -> "Пока нет звонков"
        CallHistoryFilter.MISSED -> "Пропущенных вызовов нет"
        CallHistoryFilter.INCOMING -> "Входящих звонков пока нет"
        CallHistoryFilter.OUTGOING -> "Исходящих звонков пока нет"
    }
    val filteredEmptyDescription = when (selectedFilter) {
        CallHistoryFilter.ALL -> "Исходящие и входящие аудио- и видеовызовы появятся здесь."
        CallHistoryFilter.MISSED -> "Здесь будут отображаться входящие звонки, на которые вы не ответили."
        CallHistoryFilter.INCOMING -> "Все принятые и пропущенные входящие звонки будут отображаться здесь."
        CallHistoryFilter.OUTGOING -> "Здесь отображаются звонки, которые вы совершали другим пользователям."
    }

    Column(
        Modifier.fillMaxSize().padding(horizontal = 18.dp, vertical = 18.dp)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text("A-CHAT", color = AccentSlate, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                Spacer(Modifier.height(4.dp))
                Text("Звонки", color = MainText, fontSize = 29.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp)
                Text("История аудио- и видеовызовов", color = Muted, fontSize = 11.sp)
            }
        }

        Spacer(Modifier.height(16.dp))
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 1.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CallHistoryFilterTab(
                label = "Все",
                count = calls.size,
                selected = selectedFilter == CallHistoryFilter.ALL,
                modifier = Modifier.weight(1f)
            ) { selectedFilter = CallHistoryFilter.ALL }
            CallHistoryFilterTab(
                label = "Пропущенные",
                count = missedCount,
                selected = selectedFilter == CallHistoryFilter.MISSED,
                countColor = if (missedCount > 0) Color(0xFFB85C6D) else Muted,
                modifier = Modifier.weight(1.35f)
            ) { selectedFilter = CallHistoryFilter.MISSED }
            CallHistoryFilterTab(
                label = "Входящие",
                count = incomingCount,
                selected = selectedFilter == CallHistoryFilter.INCOMING,
                modifier = Modifier.weight(1.1f)
            ) { selectedFilter = CallHistoryFilter.INCOMING }
            CallHistoryFilterTab(
                label = "Исходящие",
                count = outgoingCount,
                selected = selectedFilter == CallHistoryFilter.OUTGOING,
                modifier = Modifier.weight(1.15f)
            ) { selectedFilter = CallHistoryFilter.OUTGOING }
        }
        Spacer(Modifier.height(5.dp))
        Box(
            Modifier.fillMaxWidth().height(1.dp).background(
                Brush.horizontalGradient(
                    listOf(Color.Transparent, Color.White.copy(alpha = 0.80f), Color(0xFFD6DDF0).copy(alpha = 0.65f), Color.Transparent)
                )
            )
        )
        Spacer(Modifier.height(5.dp))

        when {
            isLoading -> {
                Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        androidx.compose.material3.CircularProgressIndicator(color = AccentSlate)
                        Spacer(Modifier.height(12.dp))
                        Text("Загружаем историю…", color = Muted, fontSize = 11.sp)
                    }
                }
            }
            selectedFilter != CallHistoryFilter.ALL && visibleCalls.isEmpty() && calls.isNotEmpty() -> {
                Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    Column(
                        Modifier.fillMaxWidth().padding(horizontal = 28.dp, vertical = 30.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            Modifier.size(68.dp)
                                .background(
                                    if (selectedFilter == CallHistoryFilter.MISSED) Color(0xFFFFE9EE) else Color(0xFFE8EDFA),
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                when (selectedFilter) {
                                    CallHistoryFilter.MISSED -> "↙"
                                    CallHistoryFilter.INCOMING -> "↙"
                                    CallHistoryFilter.OUTGOING -> "↗"
                                    CallHistoryFilter.ALL -> "☎"
                                },
                                color = if (selectedFilter == CallHistoryFilter.MISSED) Color(0xFFB85C6D) else AccentSlate,
                                fontSize = 30.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Spacer(Modifier.height(16.dp))
                        Text(filteredEmptyTitle, color = MainText, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(7.dp))
                        Text(
                            filteredEmptyDescription,
                            color = Muted,
                            fontSize = 11.sp,
                            lineHeight = 17.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
            calls.isEmpty() -> {
                Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    Column(
                        Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 30.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            Modifier.size(76.dp)
                                .background(
                                    Brush.linearGradient(listOf(Color(0xFF2CA8FF), Color(0xFF5266E8), Color(0xFF9362E8))),
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            ChatCallGlyph(video = false, tint = Color.White)
                        }
                        Spacer(Modifier.height(18.dp))
                        Text(filteredEmptyTitle, color = MainText, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(7.dp))
                        Text(
                            filteredEmptyDescription,
                            color = Muted,
                            fontSize = 11.sp,
                            lineHeight = 17.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        if (errorMessage != null) {
                            Spacer(Modifier.height(12.dp))
                            Text(errorMessage.orEmpty(), color = Color(0xFFA85F6E), fontSize = 10.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                            TextButton(onClick = { refreshKey++ }) { Text("Повторить", color = AccentSlate) }
                        }
                    }
                }
            }
            else -> {
                LazyColumn(
                    Modifier.fillMaxWidth().weight(1f),
                    contentPadding = PaddingValues(bottom = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    items(visibleCalls, key = { it.id }) { call ->
                        val currentUserId = userId.orEmpty()
                        val peerId = if (call.callerId == currentUserId) call.calleeId else call.callerId
                        val profile = profiles[peerId]
                        val title = profile?.let { it.displayName.ifBlank { it.username } } ?: "Собеседник"
                        val callIsVideo = call.mediaType == "video"
                        val isMissed = callStatusLabel(call, currentUserId) == "Пропущен"
                        val duration = formatCallDuration(call)
                        Column(
                            modifier = Modifier.fillMaxWidth()
                                .clickable { onOpenProfile(peerId) }
                                .padding(top = 10.dp, bottom = 9.dp)
                        ) {
                            Row(
                                Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(contentAlignment = Alignment.BottomEnd) {
                                    Avatar(
                                        title.trim().take(2).uppercase().ifBlank { "?" },
                                        peerId.hashCode().ushr(1) % 4,
                                        Modifier.size(48.dp),
                                        imageUrl = profile?.avatarUrl,
                                        imageCacheKey = peerId
                                    )
                                    Box(
                                        modifier = Modifier.size(20.dp).clip(CircleShape)
                                            .background(
                                                Brush.linearGradient(
                                                    if (appOrangeChocolateTheme.value) listOf(Color(0xFFFFB35C), Color(0xFFFF8B2A), Color(0xFFB84C12)) else listOf(Color(0xFF3CA8F4), Color(0xFF586BE4), Color(0xFF9364E6))
                                                )
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        ChatCallGlyph(video = callIsVideo, tint = Color.White)
                                    }
                                }
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(title, color = MainText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                                    Spacer(Modifier.height(3.dp))
                                    Text(
                                        (if (callIsVideo) "Видеозвонок" else "Аудиозвонок") +
                                            " · " + if (call.callerId == currentUserId) "Исходящий" else "Входящий",
                                        color = Muted,
                                        fontSize = 10.sp,
                                        maxLines = 1
                                    )
                                    Spacer(Modifier.height(5.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            Modifier.size(6.dp).clip(CircleShape).background(
                                                when {
                                                    isMissed || call.status == "declined" -> Color(0xFFD66C7D)
                                                    call.status == "accepted" -> Color(0xFF39A982)
                                                    else -> AccentSlate
                                                }
                                            )
                                        )
                                        Spacer(Modifier.width(5.dp))
                                        Text(
                                            callStatusLabel(call, currentUserId),
                                            color = if (isMissed || call.status == "declined") Color(0xFFB85C6D) else AccentSlate,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        if (duration != null) {
                                            Text("  ·  $duration", color = Muted, fontSize = 10.sp)
                                        }
                                    }
                                }
                                Spacer(Modifier.width(8.dp))
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(formatCallDateTime(call.createdAt), color = Muted, fontSize = 9.sp)
                                    Spacer(Modifier.height(5.dp))
                                    Text(if (call.callerId == currentUserId) "↗" else "↙", color = AccentSlate, fontSize = 17.sp, fontWeight = FontWeight.Medium)
                                }
                            }
                        }
                        Box(
                            Modifier.fillMaxWidth().height(1.dp).background(
                                Brush.horizontalGradient(
                                    if (appOrangeChocolateTheme.value) listOf(Color.Transparent, Color(0xFFFFB36B).copy(alpha = 0.48f), Color(0xFF8A4C27).copy(alpha = 0.38f), Color.Transparent) else listOf(Color.Transparent, Color.White.copy(alpha = 0.68f), Color(0xFFD6DDF0).copy(alpha = 0.52f), Color.Transparent)
                                )
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ContactsScreen(onOpenChat: (ChatSummary) -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(horizontal = 22.dp, vertical = 22.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("A-CHAT", color = AccentSlate, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
        Spacer(Modifier.height(5.dp))
        Text("Друзья", color = MainText, fontSize = 29.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(10.dp))
        Text(
            "Войдите в аккаунт A-Chat, чтобы отправлять и принимать заявки в друзья.",
            color = Muted, fontSize = 12.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

private enum class FriendsSection { FRIENDS, REQUESTS }

@Composable
private fun FriendsScreen(
    repository: SupabaseChatRepository,
    cache: ArturChatLocalCache,
    userId: String,
    onlineUserIds: Set<String>,
    onOpenChat: (FaceVideoChatSummary) -> Unit,
    onOpenProfile: (String) -> Unit
) {
    val cachedRelationships = remember(cache, userId) { cache.readFriendships(userId) }
    val cachedPeople = remember(cache, userId) { cache.readFriendProfiles(userId) }
    var relationships by remember(userId, cache) { mutableStateOf(cachedRelationships) }
    var people by remember(userId, cache) { mutableStateOf(cachedPeople) }
    var isLoading by remember(userId, cache) { mutableStateOf(!cache.hasFriendshipsSnapshot(userId)) }
    var refreshKey by remember(userId) { mutableStateOf(0) }
    var query by remember { mutableStateOf("") }
    var selectedSection by remember { mutableStateOf(FriendsSection.FRIENDS) }
    var showAddFriend by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var busyUserId by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(repository, userId, refreshKey, cache) {
        isLoading = relationships.isEmpty() && people.isEmpty()
        errorMessage = null
        runCatching {
            val loaded = repository.listMyFriendships(userId)
            val relatedIds = loaded.map { row ->
                if (row.userLow == userId) row.userHigh else row.userLow
            }.distinct()
            val loadedProfiles = repository.listProfiles(relatedIds).associateBy { it.id }
            relationships = loaded
            people = loadedProfiles
            withContext(Dispatchers.IO) {
                cache.saveFriendships(userId, loaded)
                cache.saveFriendProfiles(userId, loadedProfiles.values)
            }
        }.onFailure {
            if (relationships.isEmpty() && people.isEmpty()) {
                errorMessage = it.message ?: "Не удалось загрузить друзей."
            } else {
                android.util.Log.w("ArturChatFriends", "Could not refresh friends cache", it)
            }
        }
        isLoading = false
    }

    fun relatedUserId(row: com.facevideo.app.data.FaceVideoFriendship): String =
        if (row.userLow == userId) row.userHigh else row.userLow

    val friends = relationships.filter { it.status == "accepted" }
        .mapNotNull { row -> people[relatedUserId(row)] }
        .filter { profile ->
            query.isBlank() ||
                profile.displayName.contains(query.trim(), ignoreCase = true) ||
                profile.username.contains(query.trim().removePrefix("@"), ignoreCase = true)
        }
        .sortedBy { it.displayName.ifBlank { it.username }.lowercase() }

    val incoming = relationships.filter { it.status == "pending" && it.requestedBy != userId }
    val outgoing = relationships.filter { it.status == "pending" && it.requestedBy == userId }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp, vertical = 18.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("A-CHAT", color = AccentSlate, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                Spacer(Modifier.height(4.dp))
                Text("Друзья", color = MainText, fontSize = 29.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp)
                Text("Люди, с которыми вы взаимно подружились", color = Muted, fontSize = 11.sp)
            }
        }
        Spacer(Modifier.height(19.dp))
        Box(
            Modifier.fillMaxWidth().height(52.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(
                    Brush.horizontalGradient(
                        if (appOrangeChocolateTheme.value) {
                            listOf(Color(0xFFFFB45C), Color(0xFFFF922F), Color(0xFFC85A16))
                        } else {
                            listOf(Color(0xFF2CA8FF), Color(0xFF5266E8), Color(0xFF9362E8))
                        }
                    )
                )
                .clickable(onClick = { showAddFriend = true }),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = if (appOrangeChocolateTheme.value) Color(0x33FFF0D8) else Color(0x33FFFFFF),
                    shape = CircleShape,
                    modifier = Modifier.size(28.dp),
                    border = BorderStroke(1.dp, if (appOrangeChocolateTheme.value) Color(0x88FFE0B8) else Color.White.copy(alpha = 0.42f))
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("+", color = if (appOrangeChocolateTheme.value) Color(0xFF2B1509) else Color.White, fontSize = 19.sp, fontWeight = FontWeight.Medium)
                    }
                }
                Spacer(Modifier.width(10.dp))
                Text("Добавить друга", color = if (appOrangeChocolateTheme.value) Color(0xFF2B1509) else Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.height(15.dp))
        Surface(
            color = ThemeSurface,
            shape = RoundedCornerShape(18.dp),
            border = BorderStroke(1.dp, ThemeBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                Modifier.fillMaxWidth().padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf(
                    FriendsSection.FRIENDS to "Друзья · ${friends.size}",
                    FriendsSection.REQUESTS to "Заявки · ${incoming.size + outgoing.size}"
                ).forEach { (section, title) ->
                    val selected = selectedSection == section
                    Box(
                        Modifier.weight(1f).height(42.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .then(
                                if (selected) Modifier.background(
                                    Brush.horizontalGradient(
                                        if (appOrangeChocolateTheme.value) listOf(Color(0xFFFFB35B), Color(0xFFFF902F))
                                        else listOf(Color(0xFF5B6FE4), Color(0xFF8266E9))
                                    )
                                ) else Modifier
                            )
                            .clickable(interactionSource = null, indication = null) { selectedSection = section },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            title,
                            color = if (selected && appOrangeChocolateTheme.value) Color(0xFF2B1509) else if (selected) Color.White else MainText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(14.dp))

        if (isLoading) {
            Column(
                Modifier.fillMaxWidth().padding(vertical = 34.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                androidx.compose.material3.CircularProgressIndicator(color = AccentSlate)
                Spacer(Modifier.height(12.dp))
                Text("Обновляем список друзей…", color = Muted, fontSize = 11.sp)
            }
        } else if (errorMessage != null) {
            Text(errorMessage.orEmpty(), color = Color(0xFFA85F6E), fontSize = 12.sp)
            TextButton(onClick = { refreshKey++ }) { Text("Повторить", color = AccentSlate) }
        } else if (selectedSection == FriendsSection.FRIENDS) {
            SearchField(
                value = query,
                onValueChange = { query = it },
                compact = true,
                placeholder = "Поиск среди друзей"
            )
            Spacer(Modifier.height(12.dp))
            if (friends.isEmpty()) {
                Column(Modifier.fillMaxWidth().padding(vertical = 32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Avatar("♡", 2, Modifier.size(64.dp))
                    Spacer(Modifier.height(14.dp))
                    Text(if (query.isBlank()) "Пока нет друзей" else "Ничего не найдено", color = MainText, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        if (query.isBlank()) "Найди человека по псевдониму и отправь заявку. Он появится здесь после принятия."
                        else "Попробуй изменить поисковый запрос.",
                        color = Muted, fontSize = 11.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            } else {
                friends.forEach { friend ->
                    FriendDirectoryRow(
                        profile = friend,
                        status = if (friend.id in onlineUserIds) "В сети" else friend.statusText.ifBlank { "Не в сети" },
                        actionLabel = "Написать",
                        actionEnabled = busyUserId != friend.id,
                        onProfile = { onOpenProfile(friend.id) },
                        onAction = {
                            scope.launch {
                                busyUserId = friend.id
                                runCatching { repository.getOrCreateDirectChat(friend.id, userId) }
                                    .onSuccess { chat ->
                                        if (chat != null) onOpenChat(chat)
                                        else errorMessage = "Не удалось открыть переписку."
                                    }
                                    .onFailure { errorMessage = it.message ?: "Не удалось открыть переписку." }
                                busyUserId = null
                            }
                        }
                    )
                }
            }
        } else {
            Text("ВХОДЯЩИЕ ЗАЯВКИ", color = AccentSlate, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
            Spacer(Modifier.height(8.dp))
            if (incoming.isEmpty()) {
                Text("Новых заявок пока нет.", color = Muted, fontSize = 12.sp)
            } else {
                incoming.forEach { row ->
                    val personId = relatedUserId(row)
                    val person = people[personId]
                    if (person != null) {
                        FriendDirectoryRow(
                            profile = person,
                            status = "Хочет добавить вас в друзья",
                            actionLabel = "Принять",
                            actionEnabled = busyUserId != personId,
                            onProfile = { onOpenProfile(personId) },
                            onAction = {
                                scope.launch {
                                    busyUserId = personId
                                    runCatching { repository.respondToFriendRequest(personId, true) }
                                        .onSuccess { refreshKey++ }
                                        .onFailure { errorMessage = it.message ?: "Не удалось принять заявку." }
                                    busyUserId = null
                                }
                            },
                            secondaryActionLabel = "Отклонить",
                            onSecondaryAction = {
                                scope.launch {
                                    busyUserId = personId
                                    runCatching { repository.respondToFriendRequest(personId, false) }
                                        .onSuccess { refreshKey++ }
                                        .onFailure { errorMessage = it.message ?: "Не удалось отклонить заявку." }
                                    busyUserId = null
                                }
                            }
                        )
                    }
                }
            }
            Spacer(Modifier.height(18.dp))
            Text("ОТПРАВЛЕННЫЕ ЗАЯВКИ", color = AccentSlate, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
            Spacer(Modifier.height(8.dp))
            if (outgoing.isEmpty()) {
                Text("Вы пока никому не отправляли заявки.", color = Muted, fontSize = 12.sp)
            } else {
                outgoing.forEach { row ->
                    val personId = relatedUserId(row)
                    val person = people[personId]
                    if (person != null) {
                        FriendDirectoryRow(
                            profile = person,
                            status = "Ожидает подтверждения",
                            actionLabel = "Отменить",
                            actionEnabled = busyUserId != personId,
                            onProfile = { onOpenProfile(personId) },
                            onAction = {
                                scope.launch {
                                    busyUserId = personId
                                    runCatching { repository.cancelFriendRequest(personId) }
                                        .onSuccess { refreshKey++ }
                                        .onFailure { errorMessage = it.message ?: "Не удалось отменить заявку." }
                                    busyUserId = null
                                }
                            }
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
    }

    if (showAddFriend) {
        AddFriendDialog(
            repository = repository,
            currentUserId = userId,
            onDismiss = { showAddFriend = false },
            onChanged = {
                showAddFriend = false
                refreshKey++
            }
        )
    }
}

@Composable
private fun FriendDirectoryRow(
    profile: FaceVideoProfile,
    status: String,
    actionLabel: String,
    actionEnabled: Boolean,
    onProfile: () -> Unit,
    onAction: () -> Unit,
    secondaryActionLabel: String? = null,
    onSecondaryAction: (() -> Unit)? = null
) {
    Surface(
        color = ThemeSurfaceRaised,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, ThemeBorder),
        shadowElevation = 1.dp,
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Avatar(
                profile.displayName.ifBlank { profile.username }.take(1).uppercase(),
                profile.id.hashCode().ushr(1) % 4,
                Modifier.size(49.dp).clickable(onClick = onProfile),
                imageUrl = profile.avatarUrl,
                imageCacheKey = profile.id
            )
            Spacer(Modifier.width(11.dp))
            Column(Modifier.weight(1f).clickable(onClick = onProfile)) {
                Text(profile.displayName.ifBlank { profile.username }, color = MainText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                Text("@${profile.username}", color = Muted, fontSize = 10.sp, maxLines = 1)
                Text(status, color = AccentSlate, fontSize = 10.sp, maxLines = 1)
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Surface(
                    color = if (appOrangeChocolateTheme.value) Color(0xFFFFE1BC) else Color(0xFFE9EEFF),
                    shape = RoundedCornerShape(11.dp),
                    modifier = Modifier.clickable(enabled = actionEnabled, onClick = onAction)
                ) {
                    Text(
                        actionLabel,
                        color = if (appOrangeChocolateTheme.value) Color(0xFF9B4E17) else AccentSlate,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp)
                    )
                }
                if (secondaryActionLabel != null && onSecondaryAction != null) {
                    Surface(
                        color = if (appOrangeChocolateTheme.value) Color(0xFF482016) else Color(0xFFFFF0F2),
                        shape = RoundedCornerShape(11.dp),
                        modifier = Modifier.clickable(enabled = actionEnabled, onClick = onSecondaryAction)
                    ) {
                        Text(
                            secondaryActionLabel,
                            color = if (appOrangeChocolateTheme.value) Color(0xFFFFA18A) else Color(0xFFA85F6E),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AddFriendDialog(
    repository: SupabaseChatRepository,
    currentUserId: String,
    onDismiss: () -> Unit,
    onChanged: () -> Unit
) {
    var username by remember { mutableStateOf("") }
    var foundProfile by remember { mutableStateOf<FaceVideoProfile?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = PanelLight,
        title = { Text("Добавить друга", color = MainText, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text("Найди человека по уникальному псевдониму в A-Chat.", color = Muted, fontSize = 12.sp)
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = username,
                    onValueChange = {
                        username = it.lowercase().filter { ch -> ch in 'a'..'z' || ch in '0'..'9' || ch == '_' }.take(24)
                        foundProfile = null
                        message = null
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { Text("@", color = AccentSlate) },
                    label = { Text("Псевдоним пользователя") }
                )
                if (foundProfile != null) {
                    Spacer(Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Avatar(
                            foundProfile!!.displayName.ifBlank { foundProfile!!.username }.take(1).uppercase(),
                            2, Modifier.size(42.dp), imageUrl = foundProfile!!.avatarUrl,
                            imageCacheKey = foundProfile!!.id
                        )
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(foundProfile!!.displayName.ifBlank { foundProfile!!.username }, color = MainText, fontWeight = FontWeight.SemiBold)
                            Text("@${foundProfile!!.username}", color = Muted, fontSize = 11.sp)
                        }
                    }
                }
                if (message != null) {
                    Spacer(Modifier.height(10.dp))
                    Text(message.orEmpty(), color = if (message.orEmpty().contains("отправлена") || message.orEmpty().contains("друзья")) AccentSlate else Color(0xFFA85F6E), fontSize = 11.sp)
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = !busy && username.length >= 3,
                onClick = {
                    scope.launch {
                        busy = true
                        message = null
                        try {
                            val candidate = foundProfile
                            if (candidate == null) {
                                val found = repository.findUser(username)
                                when {
                                    found == null -> message = "Пользователь не найден."
                                    found.id == currentUserId -> message = "Нельзя добавить самого себя."
                                    else -> foundProfile = found
                                }
                            } else {
                                val result = repository.sendFriendRequest(candidate.id)
                                message = when (result) {
                                    "accepted" -> "Теперь вы друзья!"
                                    else -> "Заявка отправлена."
                                }
                                onChanged()
                            }
                        } catch (error: Exception) {
                            message = error.message ?: "Не удалось отправить заявку."
                        } finally {
                            busy = false
                        }
                    }
                }
            ) {
                Text(
                    when {
                        busy -> "Подождите…"
                        foundProfile != null -> "Отправить заявку"
                        else -> "Найти"
                    },
                    color = AccentSlate
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Закрыть", color = Muted) }
        }
    )
}

@Composable
private fun ProfileScreen(
    userEmail: String?,
    userId: String?,
    repository: SupabaseChatRepository?,
    cache: ArturChatLocalCache,
    onOpenFriends: () -> Unit,
    onOpenGroups: () -> Unit,
    onOpenStories: () -> Unit,
    onSignOut: () -> Unit
) {
    val cachedProfile = remember(cache, userId) { userId?.let { cache.readProfile(it, it) } }
    var profile by remember(userId, cache) { mutableStateOf(cachedProfile) }
    var username by remember(userId, cache) { mutableStateOf(cachedProfile?.username.orEmpty()) }
    var displayName by remember(userId, cache) { mutableStateOf(cachedProfile?.displayName.orEmpty()) }
    var statusText by remember(userId, cache) { mutableStateOf(cachedProfile?.statusText.orEmpty()) }
    var about by remember(userId, cache) { mutableStateOf(cachedProfile?.about.orEmpty()) }
    var isLoading by remember(userId) { mutableStateOf(cachedProfile == null) }
    var isSaving by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }
    var isEditing by remember(userId) { mutableStateOf(false) }
    var editSection by remember(userId) { mutableStateOf(0) }
    var showDiscardChanges by remember { mutableStateOf(false) }
    var showAvatarPreview by remember { mutableStateOf(false) }
    var showPrivacyInfo by remember { mutableStateOf(false) }
    var showAppearanceInfo by remember { mutableStateOf(false) }
    var showDevicesInfo by remember { mutableStateOf(false) }
    val cachedOwnStats = remember(cache, userId) { userId?.let { cache.readProfileStats(it, it) } }
    var friendCount by remember(userId, cache) { mutableStateOf(cachedOwnStats?.friendCount) }
    var storyCount by remember(userId, cache) { mutableStateOf(cachedOwnStats?.storyCount) }
    var groupCount by remember(userId, cache) { mutableStateOf(cachedOwnStats?.groupCount) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val profileScrollState = rememberScrollState()

    LaunchedEffect(repository, userId, cache) {
        val currentRepository = repository
        val currentUserId = userId
        if (currentRepository != null && currentUserId != null) {
            isLoading = profile == null
            val (profileResult, statsResult) = coroutineScope {
                val profileRequest = async { runCatching { currentRepository.getMyProfile(currentUserId) } }
                val statsRequest = async { runCatching { currentRepository.getProfileStats(currentUserId) } }
                profileRequest.await() to statsRequest.await()
            }
            profileResult.onSuccess { loaded ->
                profile = loaded
                if (loaded != null) cache.saveProfile(currentUserId, loaded)
                if (loaded != null) {
                    username = loaded.username
                    displayName = loaded.displayName
                    statusText = loaded.statusText
                    about = loaded.about
                }
                if (loaded == null && profile == null) errorMessage = "Профиль пока не найден."
            }.onFailure {
                errorMessage = it.message ?: "Не удалось обновить профиль."
                android.util.Log.w("ArturChatProfile", "Could not refresh own profile", it)
            }
            isLoading = false
            statsResult.onSuccess { stats ->
                friendCount = stats.friendCount
                storyCount = stats.storyCount
                groupCount = stats.groupCount
                withContext(Dispatchers.IO) {
                    cache.saveProfileStats(currentUserId, currentUserId, stats)
                }
            }.onFailure {
                android.util.Log.w("ArturChatProfile", "Could not refresh profile counters", it)
            }
        }
    }

    val hasChanges = profile?.let {
        username != it.username ||
            displayName != it.displayName ||
            statusText != it.statusText ||
            about != it.about
    } ?: false
    val validUsername = username.matches(Regex("^[a-z0-9_]{3,24}$"))
    val canSave = repository != null && userId != null && !isSaving && hasChanges && validUsername && displayName.trim().isNotEmpty()

    fun resetDraft() {
        username = profile?.username.orEmpty()
        displayName = profile?.displayName.orEmpty()
        statusText = profile?.statusText.orEmpty()
        about = profile?.about.orEmpty()
        errorMessage = null
        successMessage = null
    }

    fun closeEditor() {
        if (hasChanges) {
            showDiscardChanges = true
        } else {
            isEditing = false
            errorMessage = null
            successMessage = null
        }
    }

    BackHandler(enabled = isEditing) {
        closeEditor()
    }

    val avatarPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        val currentRepository = repository
        val currentUserId = userId
        val savedProfile = profile
        if (uri != null && currentRepository != null && currentUserId != null && savedProfile != null) {
            scope.launch {
                isSaving = true
                errorMessage = null
                successMessage = null
                runCatching {
                    val prepared = withContext(Dispatchers.IO) { prepareChatImage(context, uri) }
                    val url = currentRepository.uploadProfileAvatar(currentUserId, prepared.bytes)
                    // Avatar changes are independent of the unsaved text draft.
                    currentRepository.updateMyProfile(
                        currentUserId,
                        savedProfile.username,
                        savedProfile.displayName,
                        savedProfile.statusText,
                        savedProfile.about,
                        url
                    )
                }.onSuccess { updated ->
                    profile = updated
                    cache.saveProfile(currentUserId, updated)
                    username = updated.username
                    displayName = updated.displayName
                    statusText = updated.statusText
                    about = updated.about
                    successMessage = "Фотография профиля обновлена."
                }.onFailure {
                    errorMessage = it.message ?: "Не удалось обновить фотографию."
                }
                isSaving = false
            }
        }
    }

    Column(
        Modifier.fillMaxSize()
            .verticalScroll(profileScrollState)
            .padding(horizontal = 18.dp, vertical = 18.dp)
    ) {
        if (!isEditing) {
            fun startProfileEditor(section: Int = 0) {
                resetDraft()
                isEditing = true
                editSection = section
                errorMessage = null
                successMessage = null
            }

            fun openNotificationSettings() {
                val notificationIntent = Intent("android.settings.APP_NOTIFICATION_SETTINGS").apply {
                    putExtra("android.provider.extra.APP_PACKAGE", context.packageName)
                }
                runCatching { context.startActivity(notificationIntent) }
                    .onFailure {
                        val fallback = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.parse("package:${context.packageName}")
                        }
                        runCatching { context.startActivity(fallback) }
                    }
            }

            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Профиль",
                    color = MainText,
                    fontSize = 25.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.5).sp
                )
                Spacer(Modifier.weight(1f))
                Box(
                    modifier = Modifier.size(42.dp)
                        .clickable { scope.launch { profileScrollState.scrollTo(profileScrollState.maxValue) } },
                    contentAlignment = Alignment.Center
                ) {
                    ProfileMenuGlyph("settings", AccentSlate)
                }
            }

            Spacer(Modifier.height(18.dp))

            if (isLoading && profile == null) {
                Surface(
                    color = Color.Transparent,
                    shape = RoundedCornerShape(25.dp),
                    border = BorderStroke(0.dp, Color.Transparent),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        Modifier.fillMaxWidth().padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        androidx.compose.material3.CircularProgressIndicator(
                            modifier = Modifier.size(21.dp),
                            color = AccentSlate,
                            strokeWidth = 2.dp
                        )
                        Spacer(Modifier.width(12.dp))
                        Text("Загружаем профиль…", color = Muted, fontSize = 12.sp)
                    }
                }
            } else if (profile != null) {
                val p = profile!!
                Column(
                    Modifier.fillMaxWidth()
                        .padding(horizontal = 6.dp, vertical = 10.dp)
                ) {
                            Row(
                                Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    Modifier.size(78.dp)
                                        .background(
                                            Brush.sweepGradient(
                                                if (appOrangeChocolateTheme.value) {
                                                    listOf(
                                                        Color(0xFFFFD08A),
                                                        Color(0xFFFF9A3D),
                                                        Color(0xFFC75A19),
                                                        Color(0xFFFFD08A)
                                                    )
                                                } else {
                                                    listOf(
                                                        Color(0xFF45A9F5),
                                                        Color(0xFF7168E8),
                                                        Color(0xFFB56FD4),
                                                        Color(0xFF45A9F5)
                                                    )
                                                }
                                            ),
                                            CircleShape
                                        )
                                        .padding(3.dp)
                                ) {
                                    Avatar(
                                        displayName.ifBlank { username }.ifBlank { userEmail ?: "A" }
                                            .trim().take(1).uppercase(),
                                        1,
                                        Modifier.fillMaxSize(),
                                        imageUrl = p.avatarUrl,
                                        imageCacheKey = userId,
                                        onClick = { if (!p.avatarUrl.isNullOrBlank()) showAvatarPreview = true }
                                    )
                                }
                                Spacer(Modifier.width(14.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        displayName.ifBlank { "Ваше имя" },
                                        color = MainText,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(Modifier.height(3.dp))
                                    Text(
                                        "@${username.ifBlank { "username" }}",
                                        color = Muted,
                                        fontSize = 11.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(Modifier.height(7.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            Modifier.size(7.dp)
                                                .background(Color(0xFF31BD72), CircleShape)
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            "В сети",
                                            color = if (appOrangeChocolateTheme.value) Color(0xFFD8B89F) else Color(0xFF4E607D),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                    if (statusText.isNotBlank()) {
                                        Spacer(Modifier.height(5.dp))
                                        Text(
                                            statusText,
                                            color = if (appOrangeChocolateTheme.value) Color(0xFFFFB15C) else Color(0xFF6678A8),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Medium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                                Spacer(Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier.size(39.dp)
                                        .clickable { startProfileEditor(0) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    ProfileMenuGlyph("edit", AccentSlate)
                                }
                            }

                            Spacer(Modifier.height(17.dp))
                            Box(
                                Modifier.fillMaxWidth().height(1.dp)
                                    .background(if (appOrangeChocolateTheme.value) Color(0xFF89532F) else Color.White.copy(alpha = 0.78f))
                            )
                            Spacer(Modifier.height(13.dp))
                            Row(
                                Modifier.fillMaxWidth().padding(horizontal = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                ProfileStat("друзья", friendCount?.toString() ?: "—", Modifier.weight(1f), onOpenFriends)
                                Box(Modifier.width(1.dp).height(38.dp).background(if (appOrangeChocolateTheme.value) Color(0xFF704426) else Color(0x6695A5C6)))
                                ProfileStat(
                                    "истории",
                                    storyCount?.toString() ?: "—",
                                    Modifier.weight(1f),
                                    onOpenStories,
                                    enabled = (storyCount ?: 0) > 0
                                )
                                Box(Modifier.width(1.dp).height(38.dp).background(if (appOrangeChocolateTheme.value) Color(0xFF704426) else Color(0x6695A5C6)))
                                ProfileStat("группы", groupCount?.toString() ?: "—", Modifier.weight(1f), onOpenGroups)
                            }
                        }
            } else {
                Surface(
                    color = Color.Transparent,
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(0.dp, Color.Transparent),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(18.dp)) {
                        Text(
                            errorMessage ?: "Личный профиль доступен после входа в аккаунт A-Chat.",
                            color = Muted,
                            fontSize = 12.sp
                        )
                        if (!userEmail.isNullOrBlank()) {
                            TextButton(onClick = onSignOut) { Text("Выйти из аккаунта") }
                        }
                    }
                }
            }

            Spacer(Modifier.height(22.dp))
            Text("Аккаунт", color = MainText, fontSize = 19.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(10.dp))
            Surface(
                color = Color.Transparent,
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(0.dp, Color.Transparent),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    ProfileMenuRow(
                        iconKind = "person",
                        title = "Личные данные",
                        subtitle = "Имя, фото и описание",
                        iconBackground = if (appOrangeChocolateTheme.value) Color(0xFFFFE5C7) else Color(0xFFE2F0FF),
                        iconTint = if (appOrangeChocolateTheme.value) Color(0xFF9A4F17) else Color(0xFF4B78D6),
                        showDivider = true,
                        onClick = { startProfileEditor(0) }
                    )
                    ProfileMenuRow(
                        iconKind = "shield",
                        title = "Приватность",
                        subtitle = "Кто видит вашу активность",
                        iconBackground = if (appOrangeChocolateTheme.value) Color(0xFFF1DFC9) else Color(0xFFECE8FF),
                        iconTint = if (appOrangeChocolateTheme.value) Color(0xFF8D5728) else Color(0xFF735BDA),
                        onClick = { showPrivacyInfo = true }
                    )
                }
            }

            Spacer(Modifier.height(22.dp))
            Text("Настройки", color = MainText, fontSize = 19.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(10.dp))
            Surface(
                color = Color.Transparent,
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(0.dp, Color.Transparent),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    ProfileMenuRow(
                        iconKind = "bell",
                        title = "Уведомления",
                        subtitle = "Звуки и сообщения",
                        iconBackground = if (appOrangeChocolateTheme.value) Color(0xFFFFE0B8) else Color(0xFFF7E4F8),
                        iconTint = if (appOrangeChocolateTheme.value) Color(0xFFAE5B1C) else Color(0xFF9A61C8),
                        showDivider = true,
                        onClick = { openNotificationSettings() }
                    )
                    ProfileMenuRow(
                        iconKind = "appearance",
                        title = "Оформление",
                        subtitle = when { appOrangeChocolateTheme.value -> "Orange & Chocolate"; appDarkTheme.value -> "Чёрная тема"; else -> "Светлая тема · по умолчанию" },
                        iconBackground = if (appOrangeChocolateTheme.value) Color(0xFFFFE9D2) else Color(0xFFE4EDFF),
                        iconTint = if (appOrangeChocolateTheme.value) Color(0xFF9A4F17) else Color(0xFF4F70CC),
                        showDivider = true,
                        onClick = { showAppearanceInfo = true }
                    )
                    ProfileMenuRow(
                        iconKind = "device",
                        title = "Устройства",
                        subtitle = "Активные сеансы",
                        iconBackground = Color(0xFFECE8FF),
                        iconTint = Color(0xFF735BDA),
                        onClick = { showDevicesInfo = true }
                    )
                }
            }

            Spacer(Modifier.height(18.dp))
            Surface(
                color = Color.Transparent,
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(0.dp, Color.Transparent),
                modifier = Modifier.fillMaxWidth()
            ) {
                ProfileMenuRow(
                    iconKind = "support",
                    title = "Помощь и поддержка",
                    subtitle = null,
                    iconBackground = if (appOrangeChocolateTheme.value) Color(0xFFFFE5C7) else Color(0xFFE2F0FF),
                    iconTint = if (appOrangeChocolateTheme.value) Color(0xFF9A4F17) else Color(0xFF4B78D6),
                    onClick = {
                        runCatching {
                            context.startActivity(
                                Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/arturwarlord/face_video/issues"))
                            )
                        }.onFailure {
                            Toast.makeText(context, "Не удалось открыть поддержку", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }

            Spacer(Modifier.height(19.dp))
        } else {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "‹",
                    color = AccentSlate,
                    fontSize = 29.sp,
                    modifier = Modifier.clickable { closeEditor() }.padding(end = 12.dp)
                )
                Column(Modifier.weight(1f)) {
                    Text("РЕДАКТИРОВАНИЕ", color = AccentSlate, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.4.sp)
                    Text("Профиль", color = MainText, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                }
                if (hasChanges) {
                    Surface(color = Color(0xFFFFF1DA), shape = RoundedCornerShape(20.dp)) {
                        Text(
                            "Не сохранено",
                            color = Color(0xFF9A7137),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                        )
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
            Text("Разделите настройки профиля по категориям", color = Muted, fontSize = 11.sp)
            Spacer(Modifier.height(17.dp))

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                ProfileEditorTab("Данные", "01", editSection == 0, Modifier.weight(1f)) { editSection = 0 }
                ProfileEditorTab("О себе", "02", editSection == 1, Modifier.weight(1f)) { editSection = 1 }
                ProfileEditorTab("Фото", "03", editSection == 2, Modifier.weight(1f)) { editSection = 2 }
            }

            Spacer(Modifier.height(19.dp))

            when (editSection) {
                0 -> {
                    Text("ОСНОВНАЯ ИНФОРМАЦИЯ", color = AccentSlate, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
                    Spacer(Modifier.height(5.dp))
                    Text("Как вас будут видеть другие пользователи", color = Muted, fontSize = 11.sp)
                    Spacer(Modifier.height(17.dp))
                    ProfileInputField(
                        label = "Имя",
                        value = displayName,
                        onValueChange = {
                            displayName = it.take(48)
                            successMessage = null
                        },
                        placeholder = "Как к вам обращаться",
                        maxLength = 48
                    )
                    Spacer(Modifier.height(15.dp))
                    ProfileInputField(
                        label = "Уникальный псевдоним",
                        value = username,
                        onValueChange = {
                            username = it.lowercase().filter { ch -> ch in 'a'..'z' || ch in '0'..'9' || ch == '_' }.take(24)
                            errorMessage = null
                            successMessage = null
                        },
                        placeholder = "например, artur_01",
                        prefix = "@",
                        maxLength = 24,
                        footnote = "3–24 символа · латинские буквы, цифры и _",
                        isInvalid = username.isNotEmpty() && !validUsername
                    )
                    Spacer(Modifier.height(15.dp))
                    ProfileReadOnlyRow(
                        title = "Электронная почта",
                        value = userEmail.orEmpty(),
                        note = "Используется для входа в A-Chat"
                    )
                }
                1 -> {
                    Text("ВАШЕ ПРИСУТСТВИЕ", color = AccentSlate, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
                    Spacer(Modifier.height(5.dp))
                    Text("Коротко расскажите, чем хотите поделиться", color = Muted, fontSize = 11.sp)
                    Spacer(Modifier.height(17.dp))
                    ProfileInputField(
                        label = "Статус",
                        value = statusText,
                        onValueChange = {
                            statusText = it.take(80)
                            successMessage = null
                        },
                        placeholder = "Например: На работе · Не беспокоить",
                        maxLength = 80,
                        footnote = "Короткая фраза, которую увидят ваши собеседники"
                    )
                    Spacer(Modifier.height(17.dp))
                    ProfileInputField(
                        label = "О себе",
                        value = about,
                        onValueChange = {
                            about = it.take(500)
                            successMessage = null
                        },
                        placeholder = "Расскажите немного о себе…",
                        maxLength = 500,
                        multiline = true,
                        footnote = "До 500 символов"
                    )
                    Spacer(Modifier.height(13.dp))
                    Surface(
                        color = Color.Transparent,
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(0.dp, Color.Transparent),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "Совет: оставляйте только ту информацию, которой готовы поделиться с друзьями.",
                            color = Muted,
                            fontSize = 10.sp,
                            lineHeight = 15.sp,
                            modifier = Modifier.padding(13.dp)
                        )
                    }
                }
                else -> {
                    Text("ФОТО ПРОФИЛЯ", color = AccentSlate, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
                    Spacer(Modifier.height(5.dp))
                    Text("Изображение, по которому вас узнают друзья", color = Muted, fontSize = 11.sp)
                    Spacer(Modifier.height(23.dp))
                    Column(
                        Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Avatar(
                            displayName.ifBlank { username }.ifBlank { userEmail ?: "A" }.take(1).uppercase(),
                            1,
                            Modifier.size(136.dp),
                            highlighted = true,
                            imageUrl = profile?.avatarUrl,
                            imageCacheKey = userId,
                            onClick = { if (!profile?.avatarUrl.isNullOrBlank()) showAvatarPreview = true }
                        )
                        Spacer(Modifier.height(17.dp))
                        Text(
                            displayName.ifBlank { "Ваш профиль" },
                            color = MainText,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            username.takeIf { it.isNotBlank() }?.let { "@$it" } ?: "Псевдоним не указан",
                            color = Muted,
                            fontSize = 12.sp
                        )
                        Spacer(Modifier.height(20.dp))
                        Surface(
                            shape = RoundedCornerShape(17.dp),
                            color = Color.Transparent,
                            shadowElevation = 3.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                Modifier.fillMaxWidth()
                                    .background(
                                        Brush.horizontalGradient(
                                            if (appOrangeChocolateTheme.value) {
                                                listOf(Color(0xFFFFB45C), Color(0xFFFF922F), Color(0xFFC85A16))
                                            } else {
                                                listOf(Color(0xFF26A9F5), Color(0xFF5364E8), Color(0xFF9160EA))
                                            }
                                        )
                                    )
                                    .clickable(enabled = !isSaving) { avatarPicker.launch("image/*") }
                                    .padding(horizontal = 16.dp, vertical = 15.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("＋", color = if (appOrangeChocolateTheme.value) Color(0xFF2B1509) else Color.White, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    if (isSaving) "Обновляем фото…" else "Выбрать фотографию",
                                    color = if (appOrangeChocolateTheme.value) Color(0xFF2B1509) else Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Spacer(Modifier.height(11.dp))
                        Text(
                            "Поддерживаются изображения из галереи. Фото обновится сразу после загрузки.",
                            color = Muted,
                            fontSize = 10.sp,
                            lineHeight = 15.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }

            if (errorMessage != null) {
                Spacer(Modifier.height(14.dp))
                ProfileFeedbackMessage(errorMessage.orEmpty(), isSuccess = false)
            }
            if (successMessage != null) {
                Spacer(Modifier.height(10.dp))
                ProfileFeedbackMessage(successMessage.orEmpty(), isSuccess = true)
            }

            Spacer(Modifier.height(20.dp))
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = Color.Transparent,
                shadowElevation = if (canSave) 5.dp else 0.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    Modifier.fillMaxWidth()
                        .background(
                            if (canSave) {
                                Brush.horizontalGradient(
                                    if (appOrangeChocolateTheme.value) {
                                        listOf(Color(0xFFFFB45C), Color(0xFFFF922F), Color(0xFFC85A16))
                                    } else {
                                        listOf(Color(0xFF26A9F5), Color(0xFF5364E8), Color(0xFF9160EA))
                                    }
                                )
                            } else {
                                Brush.horizontalGradient(
                                    if (appOrangeChocolateTheme.value) listOf(Color(0xFF5A3923), Color(0xFF402516))
                                    else listOf(Color(0xFFD7DEED), Color(0xFFC8D2E7))
                                )
                            }
                        )
                        .clickable(enabled = canSave && repository != null && userId != null) {
                            val currentRepository = repository
                            val currentUserId = userId
                            if (currentRepository != null && currentUserId != null) {
                                scope.launch {
                                    isSaving = true
                                    errorMessage = null
                                    successMessage = null
                                    runCatching {
                                        currentRepository.updateMyProfile(
                                            currentUserId,
                                            username,
                                            displayName,
                                            statusText,
                                            about
                                        )
                                    }.onSuccess { updated ->
                                        profile = updated
                                        cache.saveProfile(currentUserId, updated)
                                        username = updated.username
                                        displayName = updated.displayName
                                        statusText = updated.statusText
                                        about = updated.about
                                        successMessage = "Изменения сохранены и доступны собеседникам."
                                        isEditing = false
                                    }.onFailure { failure ->
                                        errorMessage = when {
                                            failure.message.orEmpty().contains("duplicate", true) ||
                                                failure.message.orEmpty().contains("unique", true) ->
                                                "Этот псевдоним уже занят. Выбери другой."
                                            else -> failure.message ?: "Не удалось сохранить изменения."
                                        }
                                    }
                                    isSaving = false
                                }
                            }
                        }
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isSaving) {
                        androidx.compose.material3.CircularProgressIndicator(
                            Modifier.size(19.dp),
                            color = if (canSave && appOrangeChocolateTheme.value) Color(0xFF2B1509) else if (canSave) Color.White else Muted,
                            strokeWidth = 2.dp
                        )
                        Spacer(Modifier.width(9.dp))
                    }
                    Text(
                        when {
                            isSaving -> "Сохраняем…"
                            canSave -> "Сохранить изменения"
                            hasChanges && !validUsername -> "Проверьте псевдоним"
                            hasChanges && displayName.isBlank() -> "Укажите имя"
                            else -> "Все изменения сохранены"
                        },
                        color = when {
                            canSave && appOrangeChocolateTheme.value -> Color(0xFF2B1509)
                            canSave -> Color.White
                            appOrangeChocolateTheme.value -> Color(0xFFC9A990)
                            else -> Color(0xFF73809A)
                        },
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Text(
                "Изменения профиля синхронизируются с вашим аккаунтом A-Chat.",
                color = Muted,
                fontSize = 10.sp,
                lineHeight = 14.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 9.dp, bottom = 15.dp)
            )
        }
        Spacer(Modifier.height(20.dp))
    }

    if (showPrivacyInfo) {
        AlertDialog(
            onDismissRequest = { showPrivacyInfo = false },
            title = { Text("Приватность") },
            text = {
                Text("Здесь будут собраны настройки видимости активности и список заблокированных пользователей. Отдельный пункт для заблокированных на главной странице профиля не нужен.")
            },
            confirmButton = {
                TextButton(onClick = { showPrivacyInfo = false }) { Text("Понятно") }
            }
        )
    }

    if (showAppearanceInfo) {
        AlertDialog(
            onDismissRequest = { showAppearanceInfo = false },
            containerColor = Panel,
            title = { Text("Оформление", color = MainText, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Выбери тему приложения. Настройка сохранится на этом устройстве.",
                        color = Muted,
                        fontSize = 12.sp
                    )
                    ThemeOptionRow(
                        title = "Светлая",
                        subtitle = "Фирменные пастельные цвета",
                        selected = !appDarkTheme.value && !appOrangeChocolateTheme.value,
                        onClick = {
                            appDarkTheme.value = false
                            appOrangeChocolateTheme.value = false
                            context.getSharedPreferences(ARTURCHAT_THEME_PREFERENCES, 0)
                                .edit()
                                .putBoolean(ARTURCHAT_THEME_DARK_KEY, false)
                                .putBoolean(ARTURCHAT_THEME_ORANGE_KEY, false)
                                .apply()
                        }
                    )
                    ThemeOptionRow(
                        title = "Чёрная",
                        subtitle = "Графитовый фон и холодные акценты",
                        selected = appDarkTheme.value,
                        onClick = {
                            appDarkTheme.value = true
                            appOrangeChocolateTheme.value = false
                            context.getSharedPreferences(ARTURCHAT_THEME_PREFERENCES, 0)
                                .edit()
                                .putBoolean(ARTURCHAT_THEME_DARK_KEY, true)
                                .putBoolean(ARTURCHAT_THEME_ORANGE_KEY, false)
                                .apply()
                        }
                    )
                    ThemeOptionRow(
                        title = "Orange & Chocolate",
                        subtitle = "Шоколадный фон и янтарные акценты",
                        selected = appOrangeChocolateTheme.value,
                        onClick = {
                            appDarkTheme.value = false
                            appOrangeChocolateTheme.value = true
                            context.getSharedPreferences(ARTURCHAT_THEME_PREFERENCES, 0)
                                .edit()
                                .putBoolean(ARTURCHAT_THEME_DARK_KEY, false)
                                .putBoolean(ARTURCHAT_THEME_ORANGE_KEY, true)
                                .apply()
                        }
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showAppearanceInfo = false }) {
                    Text("Готово", color = AccentSlate, fontWeight = FontWeight.SemiBold)
                }
            }
        )
    }

    if (showDevicesInfo) {
        AlertDialog(
            onDismissRequest = { showDevicesInfo = false },
            title = { Text("Устройства") },
            text = { Text("Управление активными сеансами пока не подключено. Мы добавим этот раздел отдельно, чтобы здесь отображались реальные подключения, а не условный список.") },
            confirmButton = {
                TextButton(onClick = { showDevicesInfo = false }) { Text("Понятно") }
            }
        )
    }

    if (showAvatarPreview && !profile?.avatarUrl.isNullOrBlank() && userId != null) {
        FullscreenAvatarDialog(
            imageUrl = profile!!.avatarUrl!!,
            imageCacheKey = userId,
            title = displayName.ifBlank { username }.ifBlank { "Фото профиля" },
            onDismiss = { showAvatarPreview = false }
        )
    }

    if (showDiscardChanges) {
        AlertDialog(
            onDismissRequest = { showDiscardChanges = false },
            title = { Text("Отменить изменения?") },
            text = { Text("Несохранённые данные будут сброшены.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDiscardChanges = false
                        resetDraft()
                        isEditing = false
                    }
                ) {
                    Text("Не сохранять", color = Color(0xFFA85F6E))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardChanges = false }) {
                    Text("Продолжить")
                }
            }
        )
    }
}


@Composable
private fun ProfileStat(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    Column(
        modifier.clickable(enabled = enabled, onClick = onClick).padding(vertical = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(value, color = MainText, fontSize = 21.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(2.dp))
        Text(label, color = Muted, fontSize = 11.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun ProfileMenuRow(
    iconKind: String,
    title: String,
    subtitle: String?,
    iconBackground: Color,
    iconTint: Color,
    showDivider: Boolean = false,
    onClick: () -> Unit
) {
    Column {
        Row(
            Modifier.fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 15.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier.size(43.dp)
                    .clip(RoundedCornerShape(15.dp))
                    .background(iconBackground),
                contentAlignment = Alignment.Center
            ) {
                ProfileMenuGlyph(iconKind, iconTint)
            }
            Spacer(Modifier.width(13.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    title,
                    color = MainText,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!subtitle.isNullOrBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        subtitle,
                        color = Muted,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
            Text("›", color = Muted, fontSize = 25.sp, fontWeight = FontWeight.Light)
        }
        if (showDivider) {
            Box(
                Modifier.fillMaxWidth().padding(start = 71.dp, end = 15.dp)
                    .height(1.dp).background(if (appOrangeChocolateTheme.value) Color(0xFF704426) else Color(0xFFE4E8F1))
            )
        }
    }
}

@Composable
private fun ProfileMenuGlyph(kind: String, tint: Color) {
    Canvas(Modifier.size(23.dp)) {
        val w = size.width
        val h = size.height
        val line = Stroke(width = 1.9.dp.toPx(), cap = StrokeCap.Round)
        when (kind) {
            "person" -> {
                drawCircle(tint, radius = w * 0.17f, center = Offset(w * 0.5f, h * 0.29f), style = line)
                val shoulders = Path().apply {
                    moveTo(w * 0.17f, h * 0.88f)
                    cubicTo(w * 0.19f, h * 0.64f, w * 0.32f, h * 0.55f, w * 0.5f, h * 0.55f)
                    cubicTo(w * 0.68f, h * 0.55f, w * 0.81f, h * 0.64f, w * 0.83f, h * 0.88f)
                }
                drawPath(shoulders, tint, style = line)
            }
            "shield" -> {
                val shield = Path().apply {
                    moveTo(w * 0.5f, h * 0.08f)
                    lineTo(w * 0.84f, h * 0.22f)
                    lineTo(w * 0.80f, h * 0.57f)
                    cubicTo(w * 0.77f, h * 0.73f, w * 0.63f, h * 0.86f, w * 0.5f, h * 0.93f)
                    cubicTo(w * 0.37f, h * 0.86f, w * 0.23f, h * 0.73f, w * 0.20f, h * 0.57f)
                    lineTo(w * 0.16f, h * 0.22f)
                    close()
                }
                drawPath(shield, tint, style = line)
                drawLine(tint, Offset(w * 0.35f, h * 0.50f), Offset(w * 0.46f, h * 0.61f), line.width, cap = StrokeCap.Round)
                drawLine(tint, Offset(w * 0.46f, h * 0.61f), Offset(w * 0.68f, h * 0.37f), line.width, cap = StrokeCap.Round)
            }
            "bell" -> {
                val bell = Path().apply {
                    moveTo(w * 0.5f, h * 0.13f)
                    cubicTo(w * 0.31f, h * 0.13f, w * 0.25f, h * 0.30f, w * 0.25f, h * 0.48f)
                    lineTo(w * 0.25f, h * 0.66f)
                    cubicTo(w * 0.25f, h * 0.75f, w * 0.19f, h * 0.79f, w * 0.15f, h * 0.82f)
                    lineTo(w * 0.85f, h * 0.82f)
                    cubicTo(w * 0.81f, h * 0.79f, w * 0.75f, h * 0.75f, w * 0.75f, h * 0.66f)
                    lineTo(w * 0.75f, h * 0.48f)
                    cubicTo(w * 0.75f, h * 0.30f, w * 0.69f, h * 0.13f, w * 0.5f, h * 0.13f)
                }
                drawPath(bell, tint, style = line)
                drawArc(tint, 200f, 140f, false, Offset(w * 0.40f, h * 0.78f), androidx.compose.ui.geometry.Size(w * 0.20f, h * 0.18f), style = line)
                drawLine(tint, Offset(w * 0.42f, h * 0.94f), Offset(w * 0.58f, h * 0.94f), line.width, cap = StrokeCap.Round)
            }
            "appearance" -> {
                val center = Offset(w * 0.5f, h * 0.5f)
                val radius = w * 0.35f
                val lightDarkSplit = Path().apply {
                    moveTo(w * 0.5f, h * 0.15f)
                    cubicTo(w * 0.31f, h * 0.15f, w * 0.15f, h * 0.31f, w * 0.15f, h * 0.5f)
                    cubicTo(w * 0.15f, h * 0.69f, w * 0.31f, h * 0.85f, w * 0.5f, h * 0.85f)
                    close()
                }
                drawPath(lightDarkSplit, tint)
                drawCircle(tint, radius = radius, center = center, style = line)
                drawLine(tint, Offset(w * 0.5f, h * 0.15f), Offset(w * 0.5f, h * 0.85f), line.width, cap = StrokeCap.Round)
            }
            "settings" -> {
                val center = Offset(w * 0.5f, h * 0.5f)
                val radii = floatArrayOf(0.33f, 0.33f, 0.42f, 0.42f, 0.42f, 0.33f, 0.33f, 0.28f)
                val gear = Path().apply {
                    repeat(48) { index ->
                        val angle = Math.toRadians((index * 7.5f - 90f).toDouble())
                        val radius = w * radii[index % radii.size]
                        val point = Offset(
                            center.x + kotlin.math.cos(angle).toFloat() * radius,
                            center.y + kotlin.math.sin(angle).toFloat() * radius
                        )
                        if (index == 0) moveTo(point.x, point.y) else lineTo(point.x, point.y)
                    }
                    close()
                }
                drawPath(gear, tint, style = line)
                drawCircle(tint, radius = w * 0.115f, center = center, style = line)
            }
            "edit" -> {
                val pencil = Path().apply {
                    moveTo(w * 0.24f, h * 0.73f)
                    lineTo(w * 0.67f, h * 0.30f)
                    lineTo(w * 0.79f, h * 0.42f)
                    lineTo(w * 0.36f, h * 0.85f)
                    lineTo(w * 0.20f, h * 0.89f)
                    close()
                }
                drawPath(pencil, tint, style = line)
                drawLine(tint, Offset(w * 0.60f, h * 0.36f), Offset(w * 0.72f, h * 0.48f), line.width, cap = StrokeCap.Round)
            }
            "device" -> {
                drawRoundRect(
                    tint,
                    topLeft = Offset(w * 0.24f, h * 0.07f),
                    size = androidx.compose.ui.geometry.Size(w * 0.52f, h * 0.86f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.09f, w * 0.09f),
                    style = line
                )
                drawLine(tint, Offset(w * 0.42f, h * 0.81f), Offset(w * 0.58f, h * 0.81f), line.width, cap = StrokeCap.Round)
            }
            "support" -> {
                drawCircle(tint, radius = w * 0.39f, center = Offset(w * 0.5f, h * 0.5f), style = line)
                drawArc(tint, 190f, 160f, false, Offset(w * 0.24f, h * 0.24f), androidx.compose.ui.geometry.Size(w * 0.52f, h * 0.52f), style = line)
                drawLine(tint, Offset(w * 0.5f, h * 0.39f), Offset(w * 0.5f, h * 0.58f), line.width, cap = StrokeCap.Round)
                drawCircle(tint, radius = w * 0.035f, center = Offset(w * 0.5f, h * 0.71f))
            }
        }
    }
}


@Composable
private fun ThemeOptionRow(
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        color = if (selected) PanelLight else ThemeSurface,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, if (selected) AccentSlate.copy(alpha = 0.55f) else ThemeBorder)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            androidx.compose.material3.RadioButton(
                selected = selected,
                onClick = onClick,
                colors = androidx.compose.material3.RadioButtonDefaults.colors(
                    selectedColor = AccentSlate,
                    unselectedColor = Muted
                )
            )
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text(title, color = MainText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(2.dp))
                Text(subtitle, color = Muted, fontSize = 10.sp)
            }
        }
    }
}

@Composable
private fun ProfileEditorTab(
    title: String,
    number: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(15.dp),
        color = if (selected) Color.Transparent else ThemeSurface,
        border = BorderStroke(
            1.dp,
            if (selected) AccentSlate.copy(alpha = 0.72f) else ThemeBorder
        ),
        shadowElevation = if (selected) 3.dp else 0.dp
    ) {
        Column(
            Modifier.fillMaxWidth()
                .then(
                    if (selected) Modifier.background(
                        Brush.horizontalGradient(
                            if (appOrangeChocolateTheme.value) listOf(Color(0xFFFFB45C), Color(0xFFFF922F), Color(0xFFC85A16))
                            else listOf(Color(0xFF28A8F3), Color(0xFF5364E8), Color(0xFF9160EA))
                        )
                    ) else Modifier
                )
                .padding(horizontal = 6.dp, vertical = 11.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                number,
                color = if (selected && appOrangeChocolateTheme.value) Color(0xFF4A2811) else if (selected) Color.White.copy(alpha = 0.82f) else AccentSlate.copy(alpha = 0.72f),
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp
            )
            Spacer(Modifier.height(3.dp))
            Text(
                title,
                color = if (selected && appOrangeChocolateTheme.value) Color(0xFF2B1509) else if (selected) Color.White else MainText,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun ProfileReadOnlyRow(
    title: String,
    value: String,
    note: String
) {
    Surface(
        color = ThemeSurface,
        shape = RoundedCornerShape(17.dp),
        border = BorderStroke(1.dp, ThemeBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(14.dp)) {
            Text(title.uppercase(), color = AccentSlate, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Spacer(Modifier.height(5.dp))
            Text(value.ifBlank { "Не указана" }, color = MainText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(3.dp))
            Text(note, color = Muted, fontSize = 10.sp)
        }
    }
}

@Composable
private fun ProfileFeedbackMessage(
    message: String,
    isSuccess: Boolean
) {
    Surface(
        color = if (appOrangeChocolateTheme.value) {
            if (isSuccess) Color(0xFF1D3B2C) else Color(0xFF482016)
        } else if (isSuccess) Color(0xFFEAF7F1).copy(alpha = 0.94f) else Color(0xFFFFF0F1).copy(alpha = 0.94f),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(
            1.dp,
            if (isSuccess) Color(0xFFC9E8D9) else Color(0xFFF0D0D3)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            message,
            color = if (isSuccess) Color(0xFF39795E) else Color(0xFFA85F6E),
            fontSize = 11.sp,
            lineHeight = 15.sp,
            modifier = Modifier.padding(horizontal = 13.dp, vertical = 11.dp)
        )
    }
}

@Composable
private fun ProfileInputField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    prefix: String? = null,
    maxLength: Int? = null,
    footnote: String? = null,
    isInvalid: Boolean = false,
    multiline: Boolean = false
) {
    Column(modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth()
                .padding(horizontal = 2.dp)
                .padding(bottom = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                label.uppercase(),
                color = if (isInvalid) Color(0xFFA85F6E) else AccentSlate,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.15.sp,
                modifier = Modifier.weight(1f)
            )
            if (maxLength != null) {
                Text(
                    "${value.length}/$maxLength",
                    color = if (isInvalid) Color(0xFFA85F6E) else Muted,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
        Surface(
            color = Color.Transparent,
            shape = RoundedCornerShape(18.dp),
            border = BorderStroke(
                1.2.dp,
                if (isInvalid) Color(0xFFE2BFC1) else ThemeBorder
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                Modifier.fillMaxWidth()
                    .background(
                        Brush.linearGradient(
                            when { appOrangeChocolateTheme.value -> listOf(Color(0xFF4A2511), Color(0xFF35180B), Color(0xFF4A2511)); appDarkTheme.value -> listOf(Color(0xFF202B3A), Color(0xFF1A2432), Color(0xFF202B3A)); else -> listOf(
                                Color.White.copy(alpha = 0.62f),
                                Color(0xFFE8EDFF).copy(alpha = 0.54f),
                                Color.White.copy(alpha = 0.70f)
                            )
                        }
                        )
                    )
                    .padding(
                        horizontal = 15.dp,
                        vertical = if (multiline) 13.dp else 16.dp
                    ),
                verticalAlignment = if (multiline) Alignment.Top else Alignment.CenterVertically
            ) {
                if (prefix != null) {
                    Text(
                        prefix,
                        color = AccentSlate,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(end = 8.dp, top = if (multiline) 1.dp else 0.dp)
                    )
                }
                Box(Modifier.weight(1f)) {
                    if (value.isEmpty()) {
                        Text(
                            placeholder,
                            color = (if (appOrangeChocolateTheme.value) Color(0xFFC9A990) else Color(0xFF7E8BAA)).copy(alpha = 0.86f),
                            fontSize = 14.sp,
                            lineHeight = 20.sp
                        )
                    }
                    BasicTextField(
                        value = value,
                        onValueChange = onValueChange,
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = !multiline,
                        minLines = if (multiline) 3 else 1,
                        maxLines = if (multiline) 5 else 1,
                        textStyle = TextStyle(
                            color = MainText,
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        keyboardOptions = KeyboardOptions(
                            capitalization = if (multiline) KeyboardCapitalization.Sentences
                                else KeyboardCapitalization.Words
                        ),
                        cursorBrush = androidx.compose.ui.graphics.SolidColor(AccentSlate)
                    )
                }
            }
        }
        if (footnote != null) {
            Text(
                footnote,
                color = if (isInvalid) Color(0xFFA85F6E) else Muted,
                fontSize = 10.sp,
                lineHeight = 14.sp,
                modifier = Modifier.padding(start = 3.dp, top = 6.dp)
            )
        }
    }
}

@Composable
private fun UserProfileScreen(
    userId: String,
    currentUserId: String,
    repository: SupabaseChatRepository,
    cache: ArturChatLocalCache,
    onlineUserIds: Set<String>,
    onBack: () -> Unit,
    onOpenStories: () -> Unit,
    onOpenChat: (FaceVideoChatSummary) -> Unit
) {
    val cachedProfile = remember(cache, userId, currentUserId) { cache.readProfile(currentUserId, userId) }
    val cachedFriendship = remember(cache, userId, currentUserId) { cache.readFriendship(currentUserId, userId) }
    val cachedProfileStats = remember(cache, userId, currentUserId) { cache.readProfileStats(currentUserId, userId) }
    var profile by remember(userId, currentUserId, cache) { mutableStateOf(cachedProfile) }
    var friendship by remember(userId, currentUserId, cache) { mutableStateOf(cachedFriendship) }
    var profileStats by remember(userId, currentUserId, cache) { mutableStateOf(cachedProfileStats) }
    var isLoading by remember(userId) { mutableStateOf(cachedProfile == null) }
    var isBusy by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showRemoveFriend by remember { mutableStateOf(false) }
    var showAvatarPreview by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    suspend fun refreshProfileStats() {
        runCatching { repository.getProfileStats(userId) }
            .onSuccess {
                profileStats = it
                withContext(Dispatchers.IO) {
                    cache.saveProfileStats(currentUserId, userId, it)
                }
            }
            .onFailure {
                android.util.Log.w("ArturChatProfile", "Could not load public profile counters", it)
            }
    }

    LaunchedEffect(repository, userId, currentUserId, cache) {
        isLoading = profile == null
        val (profileResult, friendshipResult, statsResult) = coroutineScope {
            val profileRequest = async { runCatching { repository.getProfile(userId) } }
            val friendshipRequest = async { runCatching { repository.getFriendshipWith(currentUserId, userId) } }
            val statsRequest = async { runCatching { repository.getProfileStats(userId) } }
            Triple(profileRequest.await(), friendshipRequest.await(), statsRequest.await())
        }
        profileResult.onSuccess { loadedProfile ->
            if (loadedProfile != null) {
                profile = loadedProfile
                withContext(Dispatchers.IO) {
                    cache.saveProfile(currentUserId, loadedProfile)
                }
            }
        }.onFailure {
            errorMessage = it.message ?: "Не удалось загрузить профиль."
            android.util.Log.w("ArturChatProfile", "Could not refresh user profile; keeping local cache", it)
        }
        friendshipResult.onSuccess { loadedFriendship ->
            friendship = loadedFriendship
            withContext(Dispatchers.IO) {
                cache.saveFriendship(currentUserId, userId, loadedFriendship)
            }
        }.onFailure {
            errorMessage = it.message ?: "Не удалось обновить статус дружбы."
            android.util.Log.w("ArturChatProfile", "Could not refresh friendship state", it)
        }
        statsResult.onSuccess { stats ->
            profileStats = stats
            withContext(Dispatchers.IO) {
                cache.saveProfileStats(currentUserId, userId, stats)
            }
        }.onFailure {
            android.util.Log.w("ArturChatProfile", "Could not load public profile counters", it)
        }
        isLoading = false
    }

    fun refreshFriendship() {
        scope.launch {
            isBusy = true
            runCatching { repository.getFriendshipWith(currentUserId, userId) }
                .onSuccess {
                    friendship = it
                    withContext(Dispatchers.IO) {
                        cache.saveFriendship(currentUserId, userId, it)
                    }
                    refreshProfileStats()
                }
                .onFailure { errorMessage = it.message ?: "Не удалось обновить статус дружбы." }
            isBusy = false
        }
    }

    Column(Modifier.fillMaxSize().arturChatGradientBackground().verticalScroll(rememberScrollState()).padding(20.dp)) {
        Text("‹ Назад", color = AccentSlate, fontSize = 16.sp, modifier = Modifier.clickable(onClick = onBack).padding(vertical = 8.dp))
        if (isLoading && profile == null) {
            Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                androidx.compose.material3.CircularProgressIndicator(color = AccentSlate)
            }
        } else if (profile != null) {
            val p = profile!!
            val acceptedFriend = friendship?.status == "accepted"
            val canOpenStories = acceptedFriend && (profileStats?.storyCount ?: 0) > 0
            Column(Modifier.fillMaxWidth()) {
                Spacer(Modifier.height(20.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier.size(112.dp)
                            .background(
                                Brush.sweepGradient(
                                    listOf(
                                        if (appOrangeChocolateTheme.value) Color(0xFFFFC27A) else Color(0xFF5DAAF1),
                                        if (appOrangeChocolateTheme.value) Color(0xFFFF8B2B) else Color(0xFF8269E8),
                                        if (appOrangeChocolateTheme.value) Color(0xFFB84C12) else Color(0xFFCE79B6),
                                        Color(0xFF5DAAF1)
                                    )
                                ),
                                CircleShape
                            )
                            .padding(4.dp)
                    ) {
                        Avatar(
                            p.displayName.ifBlank { p.username }.take(1).uppercase(),
                            2,
                            Modifier.fillMaxSize(),
                            imageUrl = p.avatarUrl,
                            imageCacheKey = p.id,
                            onClick = { if (!p.avatarUrl.isNullOrBlank()) showAvatarPreview = true }
                        )
                    }
                    Spacer(Modifier.width(13.dp))
                    Row(
                        Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                profileStats?.friendCount?.toString() ?: "—",
                                color = MainText,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text("друзья", color = Muted, fontSize = 9.sp, maxLines = 1)
                        }
                        Column(
                            Modifier.weight(1f).clickable(
                                enabled = canOpenStories,
                                onClick = onOpenStories
                            ),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                profileStats?.storyCount?.toString() ?: "—",
                                color = MainText,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text("истории", color = Muted, fontSize = 9.sp, maxLines = 1)
                        }
                        Column(
                            Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                profileStats?.groupCount?.toString() ?: "—",
                                color = MainText,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text("группы", color = Muted, fontSize = 9.sp, maxLines = 1)
                        }
                    }
                }
                Spacer(Modifier.height(18.dp))
                Column(Modifier.fillMaxWidth()) {
                    Text(
                        p.displayName.ifBlank { p.username },
                        color = MainText,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text("@${p.username}", color = Muted, fontSize = 11.sp)
                    Spacer(Modifier.height(7.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier.size(7.dp).background(
                                if (userId in onlineUserIds) Color(0xFF41B88A) else Color(0xFF9AA4B6),
                                CircleShape
                            )
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            if (userId in onlineUserIds) "В сети" else "Не в сети",
                            color = if (userId in onlineUserIds) AccentSlate else Muted,
                            fontSize = 10.sp
                        )
                    }
                    if (p.statusText.isNotBlank()) {
                        Spacer(Modifier.height(11.dp))
                        Surface(color = PaletteBlush, shape = RoundedCornerShape(18.dp)) {
                            Text(
                                p.statusText,
                                color = MainText,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                fontSize = 12.sp
                            )
                        }
                    }
                }
                Spacer(Modifier.height(18.dp))

                when {
                    userId == currentUserId -> {
                        Text("Это ваш профиль.", color = Muted, fontSize = 12.sp)
                    }
                    friendship?.status == "accepted" -> {
                        Button(
                            onClick = {
                                scope.launch {
                                    isBusy = true
                                    runCatching { repository.getOrCreateDirectChat(userId, currentUserId) }
                                        .onSuccess { chat ->
                                            if (chat != null) onOpenChat(chat)
                                            else errorMessage = "Не удалось открыть переписку."
                                        }
                                        .onFailure { errorMessage = it.message ?: "Не удалось открыть переписку." }
                                    isBusy = false
                                }
                            },
                            enabled = !isBusy,
                            modifier = Modifier.fillMaxWidth()
                        ) { Text(if (isBusy) "Открываем…" else "Написать") }
                        TextButton(onClick = { showRemoveFriend = true }, enabled = !isBusy) {
                            Text("Удалить из друзей", color = Color(0xFFA85F6E), fontSize = 11.sp)
                        }
                    }
                    friendship?.status == "pending" && friendship?.requestedBy == currentUserId -> {
                        Surface(color = PanelLight, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
                            Text("Заявка отправлена", color = AccentSlate, fontSize = 13.sp, modifier = Modifier.padding(14.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                        }
                        TextButton(
                            onClick = {
                                scope.launch {
                                    isBusy = true
                                    runCatching { repository.cancelFriendRequest(userId) }
                                        .onSuccess { friendship = null }
                                        .onFailure { errorMessage = it.message ?: "Не удалось отменить заявку." }
                                    isBusy = false
                                }
                            },
                            enabled = !isBusy
                        ) { Text("Отменить заявку", color = Muted, fontSize = 11.sp) }
                    }
                    friendship?.status == "pending" -> {
                        Text("Этот пользователь отправил вам заявку в друзья.", color = Muted, fontSize = 12.sp)
                        Button(
                            onClick = {
                                scope.launch {
                                    isBusy = true
                                    runCatching { repository.respondToFriendRequest(userId, true) }
                                        .onSuccess { refreshFriendship() }
                                        .onFailure { errorMessage = it.message ?: "Не удалось принять заявку." }
                                    isBusy = false
                                }
                            },
                            enabled = !isBusy,
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Принять заявку") }
                        TextButton(
                            onClick = {
                                scope.launch {
                                    isBusy = true
                                    runCatching { repository.respondToFriendRequest(userId, false) }
                                        .onSuccess { friendship = null }
                                        .onFailure { errorMessage = it.message ?: "Не удалось отклонить заявку." }
                                    isBusy = false
                                }
                            },
                            enabled = !isBusy
                        ) { Text("Отклонить", color = Color(0xFFA85F6E)) }
                    }
                    else -> {
                        Button(
                            onClick = {
                                scope.launch {
                                    isBusy = true
                                    runCatching { repository.sendFriendRequest(userId) }
                                        .onSuccess { refreshFriendship() }
                                        .onFailure { errorMessage = it.message ?: "Не удалось отправить заявку." }
                                    isBusy = false
                                }
                            },
                            enabled = !isBusy,
                            modifier = Modifier.fillMaxWidth()
                        ) { Text(if (isBusy) "Отправляем…" else "＋ Добавить в друзья") }
                    }
                }

                if (errorMessage != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(errorMessage.orEmpty(), color = Color(0xFFA85F6E), fontSize = 11.sp)
                }

                Spacer(Modifier.height(20.dp))
                Surface(color = Panel, shape = RoundedCornerShape(18.dp), border = BorderStroke(1.dp, PaletteMist), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text("О СЕБЕ", color = AccentSlate, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.3.sp)
                        Spacer(Modifier.height(8.dp))
                        Text(p.about.ifBlank { "Пользователь пока ничего не рассказал о себе." }, color = MainText, fontSize = 14.sp)
                    }
                }
            }
        } else {
            Text(errorMessage ?: "Не удалось загрузить профиль пользователя.", color = Muted)
        }
    }

    if (showAvatarPreview && !profile?.avatarUrl.isNullOrBlank()) {
        FullscreenAvatarDialog(
            imageUrl = profile!!.avatarUrl!!,
            imageCacheKey = profile!!.id,
            title = profile!!.displayName.ifBlank { profile!!.username },
            onDismiss = { showAvatarPreview = false }
        )
    }

    if (showRemoveFriend) {
        AlertDialog(
            onDismissRequest = { showRemoveFriend = false },
            title = { Text("Удалить из друзей?") },
            text = { Text("Вы больше не будете отображаться друг у друга в списке друзей.") },
            confirmButton = {
                TextButton(onClick = {
                    showRemoveFriend = false
                    scope.launch {
                        isBusy = true
                        runCatching { repository.removeFriend(userId) }
                            .onSuccess {
                                friendship = null
                                refreshProfileStats()
                            }
                            .onFailure { errorMessage = it.message ?: "Не удалось удалить друга." }
                        isBusy = false
                    }
                }) { Text("Удалить", color = Color(0xFFA85F6E)) }
            },
            dismissButton = { TextButton(onClick = { showRemoveFriend = false }) { Text("Отмена") } }
        )
    }
}

@Composable
private fun ChatChromeActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    filled: Boolean = false,
    transparent: Boolean = false,
    size: Dp = 40.dp,
    content: @Composable () -> Unit
) {
    val gradient = Brush.linearGradient(
        if (appOrangeChocolateTheme.value) {
            listOf(Color(0xFFFFB45C), Color(0xFFFF8F2D), Color(0xFFC85A16))
        } else {
            listOf(Color(0xFF28A9F5), Color(0xFF5266E8), Color(0xFF9462E8))
        }
    )
    Box(
        modifier = modifier.size(size)
            .clip(RoundedCornerShape(15.dp))
            .then(
                when {
                    filled -> Modifier.background(
                        if (enabled) gradient else Brush.linearGradient(
                            listOf(Color(0xFFB7C4DF), Color(0xFF9EACC9))
                        )
                    )
                    transparent -> Modifier
                    else -> Modifier.background(ThemeSurface.copy(alpha = if (isDarkAppearance) 0.46f else 0.30f))
                }
            )
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Box(Modifier.alpha(if (enabled) 1f else 0.42f)) { content() }
    }
}

@Composable
private fun InboxHeaderActionButton(
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    // A slim gradient rim and a frosted inner disc make each header action
    // read as a deliberate icon control rather than a plain white circle.
    Box(
        modifier = Modifier
            .size(46.dp)
            .background(
                Brush.linearGradient(
                    if (appOrangeChocolateTheme.value) {
                        listOf(
                            Color(0xFFFFC47B).copy(alpha = 0.92f),
                            Color(0xFFFF9234).copy(alpha = 0.98f),
                            Color(0xFFB9561D).copy(alpha = 0.92f)
                        )
                    } else {
                        listOf(
                            Color(0xFF9EADFF).copy(alpha = 0.82f),
                            Color(0xFFB7D9FF).copy(alpha = 0.96f),
                            Color(0xFFC7B5FF).copy(alpha = 0.88f)
                        )
                    }
                ),
                CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier.size(42.dp).clickable(onClick = onClick),
            color = if (appOrangeChocolateTheme.value) Color(0xF32D170C) else Color(0xF7FFFFFF),
            shape = CircleShape,
            border = BorderStroke(1.dp, if (appOrangeChocolateTheme.value) Color(0xFF704329) else Color.White.copy(alpha = 0.98f)),
            shadowElevation = 2.dp
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            if (appOrangeChocolateTheme.value) listOf(Color(0xFF3B1E10), Color(0xFF2B1409)) else listOf(Color.White, Color(0xFFF0F3FF))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                content()
            }
        }
    }
}

@Composable
private fun ChatHeaderIconButton(
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier.size(42.dp).clickable(
            interactionSource = null,
            indication = null,
            onClick = onClick
        ),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

@Composable
private fun InboxBackGlyph() {
    Canvas(Modifier.size(27.dp)) {
        val line = Stroke(width = 2.25.dp.toPx(), cap = StrokeCap.Round)
        val arrow = Path().apply {
            moveTo(size.width * 0.79f, size.height * 0.50f)
            lineTo(size.width * 0.23f, size.height * 0.50f)
            moveTo(size.width * 0.23f, size.height * 0.50f)
            lineTo(size.width * 0.54f, size.height * 0.20f)
            moveTo(size.width * 0.23f, size.height * 0.50f)
            lineTo(size.width * 0.54f, size.height * 0.80f)
        }
        drawPath(arrow, InboxAccentStrong, style = line)
    }
}

@Composable
private fun ChatSendGlyph() {
    Canvas(Modifier.size(22.dp)) {
        val line = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
        val path = Path().apply {
            moveTo(size.width * 0.12f, size.height * 0.48f)
            lineTo(size.width * 0.88f, size.height * 0.12f)
            lineTo(size.width * 0.62f, size.height * 0.89f)
            lineTo(size.width * 0.46f, size.height * 0.57f)
            close()
            moveTo(size.width * 0.46f, size.height * 0.57f)
            lineTo(size.width * 0.88f, size.height * 0.12f)
        }
        drawPath(path, Color.White, style = line)
    }
}

@Composable
private fun ChatScheduleGlyph() {
    Canvas(Modifier.size(19.dp)) {
        val line = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round)
        val center = Offset(size.width * 0.5f, size.height * 0.5f)
        drawCircle(InboxAccentStrong, radius = size.minDimension * 0.39f, center = center, style = line)
        drawLine(
            InboxAccentStrong,
            Offset(center.x, center.y),
            Offset(center.x, size.height * 0.27f),
            strokeWidth = line.width,
            cap = StrokeCap.Round
        )
        drawLine(
            InboxAccentStrong,
            Offset(center.x, center.y),
            Offset(size.width * 0.69f, center.y * 1.08f),
            strokeWidth = line.width,
            cap = StrokeCap.Round
        )
        drawArc(
            color = InboxAccentStrong,
            startAngle = 218f,
            sweepAngle = 95f,
            useCenter = false,
            topLeft = Offset(size.width * 0.03f, size.height * 0.03f),
            size = androidx.compose.ui.geometry.Size(size.width * 0.94f, size.height * 0.94f),
            style = Stroke(width = 1.2.dp.toPx(), cap = StrokeCap.Round)
        )
    }
}


@Composable
private fun ChatVideoNoteGlyph(tint: Color = InboxAccentStrong) {
    ArturChatTypeGlyph(
        type = ArturChatIconType.VIDEO_NOTE,
        tint = tint,
        modifier = Modifier.size(23.dp)
    )
}

@Composable
private fun ChatMicrophoneGlyph(tint: Color = InboxAccentStrong) {
    Canvas(Modifier.size(22.dp)) {
        val w = size.width
        val h = size.height
        val line = Stroke(width = 1.9.dp.toPx(), cap = StrokeCap.Round)
        val capsule = Path().apply {
            moveTo(w * 0.50f, h * 0.10f)
            cubicTo(w * 0.36f, h * 0.10f, w * 0.30f, h * 0.19f, w * 0.30f, h * 0.30f)
            lineTo(w * 0.30f, h * 0.53f)
            cubicTo(w * 0.30f, h * 0.66f, w * 0.38f, h * 0.74f, w * 0.50f, h * 0.74f)
            cubicTo(w * 0.62f, h * 0.74f, w * 0.70f, h * 0.66f, w * 0.70f, h * 0.53f)
            lineTo(w * 0.70f, h * 0.30f)
            cubicTo(w * 0.70f, h * 0.19f, w * 0.64f, h * 0.10f, w * 0.50f, h * 0.10f)
        }
        val cradle = Path().apply {
            moveTo(w * 0.17f, h * 0.47f)
            cubicTo(w * 0.17f, h * 0.72f, w * 0.31f, h * 0.86f, w * 0.50f, h * 0.86f)
            cubicTo(w * 0.69f, h * 0.86f, w * 0.83f, h * 0.72f, w * 0.83f, h * 0.47f)
            moveTo(w * 0.50f, h * 0.86f)
            lineTo(w * 0.50f, h * 0.98f)
            moveTo(w * 0.36f, h * 0.98f)
            lineTo(w * 0.64f, h * 0.98f)
        }
        drawPath(capsule, tint, style = line)
        drawPath(cradle, tint, style = line)
    }
}

@Composable
private fun ChatPlaybackGlyph(isPlaying: Boolean) {
    Canvas(Modifier.size(15.dp)) {
        if (isPlaying) {
            val width = 2.4.dp.toPx()
            drawLine(Color.White, Offset(size.width * 0.34f, size.height * 0.22f), Offset(size.width * 0.34f, size.height * 0.78f), width, cap = StrokeCap.Round)
            drawLine(Color.White, Offset(size.width * 0.68f, size.height * 0.22f), Offset(size.width * 0.68f, size.height * 0.78f), width, cap = StrokeCap.Round)
        } else {
            val play = Path().apply {
                moveTo(size.width * 0.27f, size.height * 0.14f)
                lineTo(size.width * 0.80f, size.height * 0.50f)
                lineTo(size.width * 0.27f, size.height * 0.86f)
                close()
            }
            drawPath(play, Color.White)
        }
    }
}

@Composable
private fun ChatNotificationGlyph(
    muted: Boolean,
    tint: Color = InboxAccentStrong
) {
    Canvas(Modifier.size(21.dp)) {
        val w = size.width
        val h = size.height
        val stroke = Stroke(width = 1.9.dp.toPx(), cap = StrokeCap.Round)

        val bell = Path().apply {
            moveTo(w * 0.50f, h * 0.12f)
            cubicTo(w * 0.33f, h * 0.12f, w * 0.27f, h * 0.27f, w * 0.27f, h * 0.45f)
            lineTo(w * 0.27f, h * 0.63f)
            cubicTo(w * 0.27f, h * 0.72f, w * 0.22f, h * 0.77f, w * 0.17f, h * 0.80f)
            lineTo(w * 0.83f, h * 0.80f)
            cubicTo(w * 0.78f, h * 0.77f, w * 0.73f, h * 0.72f, w * 0.73f, h * 0.63f)
            lineTo(w * 0.73f, h * 0.45f)
            cubicTo(w * 0.73f, h * 0.27f, w * 0.67f, h * 0.12f, w * 0.50f, h * 0.12f)
            close()
        }
        drawPath(bell, tint, style = stroke)
        drawLine(
            color = tint,
            start = Offset(w * 0.40f, h * 0.87f),
            end = Offset(w * 0.60f, h * 0.87f),
            strokeWidth = stroke.width,
            cap = StrokeCap.Round
        )
        drawArc(
            color = tint,
            startAngle = 205f,
            sweepAngle = 130f,
            useCenter = false,
            topLeft = Offset(w * 0.42f, h * 0.02f),
            size = androidx.compose.ui.geometry.Size(w * 0.16f, h * 0.13f),
            style = stroke
        )

        if (muted) {
            drawLine(
                color = tint,
                start = Offset(w * 0.16f, h * 0.18f),
                end = Offset(w * 0.84f, h * 0.82f),
                strokeWidth = 2.0.dp.toPx(),
                cap = StrokeCap.Round
            )
        }
    }
}

@Composable
private fun ChatCallGlyph(video: Boolean, tint: Color = InboxAccentStrong) {
    Canvas(Modifier.size(if (video) 21.dp else 22.dp)) {
        val w = size.width
        val h = size.height
        val line = Stroke(width = 1.9.dp.toPx(), cap = StrokeCap.Round)
        if (video) {
            val cameraBody = Path().apply {
                moveTo(w * 0.13f, h * 0.28f)
                cubicTo(w * 0.13f, h * 0.20f, w * 0.20f, h * 0.15f, w * 0.29f, h * 0.15f)
                lineTo(w * 0.56f, h * 0.15f)
                cubicTo(w * 0.65f, h * 0.15f, w * 0.70f, h * 0.21f, w * 0.70f, h * 0.30f)
                lineTo(w * 0.70f, h * 0.70f)
                cubicTo(w * 0.70f, h * 0.79f, w * 0.64f, h * 0.85f, w * 0.55f, h * 0.85f)
                lineTo(w * 0.29f, h * 0.85f)
                cubicTo(w * 0.20f, h * 0.85f, w * 0.13f, h * 0.79f, w * 0.13f, h * 0.70f)
                close()
            }
            val lens = Path().apply {
                moveTo(w * 0.72f, h * 0.39f)
                lineTo(w * 0.91f, h * 0.27f)
                lineTo(w * 0.91f, h * 0.73f)
                lineTo(w * 0.72f, h * 0.61f)
                close()
            }
            drawPath(cameraBody, tint, style = line)
            drawPath(lens, tint, style = line)
        } else {
            val handset = Path().apply {
                moveTo(w * 0.31f, h * 0.13f)
                cubicTo(w * 0.23f, h * 0.13f, w * 0.17f, h * 0.22f, w * 0.20f, h * 0.33f)
                cubicTo(w * 0.25f, h * 0.61f, w * 0.43f, h * 0.77f, w * 0.69f, h * 0.83f)
                cubicTo(w * 0.79f, h * 0.85f, w * 0.86f, h * 0.78f, w * 0.85f, h * 0.68f)
                lineTo(w * 0.82f, h * 0.56f)
                cubicTo(w * 0.81f, h * 0.51f, w * 0.76f, h * 0.49f, w * 0.70f, h * 0.52f)
                lineTo(w * 0.60f, h * 0.59f)
                cubicTo(w * 0.49f, h * 0.53f, w * 0.42f, h * 0.46f, w * 0.37f, h * 0.35f)
                lineTo(w * 0.43f, h * 0.24f)
                cubicTo(w * 0.46f, h * 0.19f, w * 0.41f, h * 0.14f, w * 0.36f, h * 0.13f)
                close()
            }
            drawPath(handset, tint, style = line)
        }
    }
}

@Composable
private fun InboxSearchGlyph(iconSize: Dp = 21.dp) {
    Canvas(Modifier.size(iconSize)) {
        val stroke = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
        drawCircle(
            color = InboxAccentStrong,
            radius = size.minDimension * 0.31f,
            center = Offset(size.width * 0.42f, size.height * 0.42f),
            style = stroke
        )
        drawLine(
            color = InboxAccentStrong,
            start = Offset(size.width * 0.64f, size.height * 0.64f),
            end = Offset(size.width * 0.91f, size.height * 0.91f),
            strokeWidth = stroke.width,
            cap = StrokeCap.Round
        )
    }
}

@Composable
private fun InboxCameraGlyph(
    tint: Color = InboxAccentStrong,
    iconSize: Dp = 28.dp
) {
    Canvas(Modifier.size(iconSize)) {
        val stroke = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
        val body = Path().apply {
            moveTo(size.width * 0.16f, size.height * 0.34f)
            cubicTo(size.width * 0.16f, size.height * 0.27f, size.width * 0.22f, size.height * 0.23f, size.width * 0.30f, size.height * 0.23f)
            lineTo(size.width * 0.41f, size.height * 0.23f)
            lineTo(size.width * 0.49f, size.height * 0.13f)
            lineTo(size.width * 0.68f, size.height * 0.13f)
            lineTo(size.width * 0.76f, size.height * 0.23f)
            lineTo(size.width * 0.79f, size.height * 0.23f)
            cubicTo(size.width * 0.87f, size.height * 0.23f, size.width * 0.90f, size.height * 0.29f, size.width * 0.90f, size.height * 0.37f)
            lineTo(size.width * 0.90f, size.height * 0.72f)
            cubicTo(size.width * 0.90f, size.height * 0.80f, size.width * 0.84f, size.height * 0.85f, size.width * 0.76f, size.height * 0.85f)
            lineTo(size.width * 0.27f, size.height * 0.85f)
            cubicTo(size.width * 0.19f, size.height * 0.85f, size.width * 0.15f, size.height * 0.79f, size.width * 0.15f, size.height * 0.71f)
            close()
        }
        drawPath(body, tint, style = stroke)
        drawCircle(
            tint,
            radius = size.width * 0.15f,
            center = Offset(size.width * 0.53f, size.height * 0.53f),
            style = stroke
        )
    }
}

@Composable
private fun NewChatGlyph() {
    Canvas(Modifier.size(20.dp)) {
        val line = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round)
        val bubble = Path().apply {
            moveTo(size.width * 0.18f, size.height * 0.18f)
            cubicTo(size.width * 0.18f, size.height * 0.10f, size.width * 0.27f, size.height * 0.08f, size.width * 0.36f, size.height * 0.08f)
            lineTo(size.width * 0.72f, size.height * 0.08f)
            cubicTo(size.width * 0.82f, size.height * 0.08f, size.width * 0.88f, size.height * 0.16f, size.width * 0.88f, size.height * 0.26f)
            lineTo(size.width * 0.88f, size.height * 0.61f)
            cubicTo(size.width * 0.88f, size.height * 0.70f, size.width * 0.80f, size.height * 0.77f, size.width * 0.70f, size.height * 0.77f)
            lineTo(size.width * 0.41f, size.height * 0.77f)
            lineTo(size.width * 0.20f, size.height * 0.92f)
            lineTo(size.width * 0.25f, size.height * 0.75f)
            cubicTo(size.width * 0.20f, size.height * 0.70f, size.width * 0.18f, size.height * 0.65f, size.width * 0.18f, size.height * 0.58f)
            close()
        }
        drawPath(bubble, Color.White, style = line)
        drawLine(Color.White, Offset(size.width * 0.55f, size.height * 0.30f), Offset(size.width * 0.55f, size.height * 0.57f), strokeWidth = line.width, cap = StrokeCap.Round)
        drawLine(Color.White, Offset(size.width * 0.42f, size.height * 0.435f), Offset(size.width * 0.68f, size.height * 0.435f), strokeWidth = line.width, cap = StrokeCap.Round)
    }
}

@Composable
private fun InboxDeleteGlyph() {
    Canvas(Modifier.size(19.dp)) {
        val w = size.width
        val h = size.height
        val stroke = Stroke(width = 1.75.dp.toPx(), cap = StrokeCap.Round)
        drawLine(Color.White, Offset(w * 0.17f, h * 0.27f), Offset(w * 0.83f, h * 0.27f), stroke.width, cap = StrokeCap.Round)
        drawLine(Color.White, Offset(w * 0.38f, h * 0.15f), Offset(w * 0.62f, h * 0.15f), stroke.width, cap = StrokeCap.Round)
        drawLine(Color.White, Offset(w * 0.27f, h * 0.32f), Offset(w * 0.32f, h * 0.84f), stroke.width, cap = StrokeCap.Round)
        drawLine(Color.White, Offset(w * 0.73f, h * 0.32f), Offset(w * 0.68f, h * 0.84f), stroke.width, cap = StrokeCap.Round)
        drawLine(Color.White, Offset(w * 0.32f, h * 0.84f), Offset(w * 0.68f, h * 0.84f), stroke.width, cap = StrokeCap.Round)
        drawLine(Color.White, Offset(w * 0.44f, h * 0.43f), Offset(w * 0.45f, h * 0.72f), stroke.width * 0.8f, cap = StrokeCap.Round)
        drawLine(Color.White, Offset(w * 0.56f, h * 0.43f), Offset(w * 0.55f, h * 0.72f), stroke.width * 0.8f, cap = StrokeCap.Round)
    }
}

@Composable
private fun InboxMoreGlyph() {
    Canvas(Modifier.size(18.dp)) {
        val dotRadius = 1.55.dp.toPx()
        listOf(0.24f, 0.50f, 0.76f).forEach { y ->
            drawCircle(InboxMuted, radius = dotRadius, center = Offset(size.width * 0.5f, size.height * y))
        }
    }
}

@Composable
private fun InboxNavigationGlyph(tab: MainTab, selected: Boolean) {
    val tint = if (selected) InboxAccent else InboxMuted
    Canvas(Modifier.size(22.dp)) {
        val w = size.width
        val h = size.height
        val line = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round)
        when (tab) {
            MainTab.CHATS -> {
                val bubble = Path().apply {
                    moveTo(w * 0.18f, h * 0.22f)
                    cubicTo(w * 0.18f, h * 0.13f, w * 0.26f, h * 0.10f, w * 0.36f, h * 0.10f)
                    lineTo(w * 0.72f, h * 0.10f)
                    cubicTo(w * 0.82f, h * 0.10f, w * 0.88f, h * 0.17f, w * 0.88f, h * 0.27f)
                    lineTo(w * 0.88f, h * 0.62f)
                    cubicTo(w * 0.88f, h * 0.72f, w * 0.80f, h * 0.78f, w * 0.70f, h * 0.78f)
                    lineTo(w * 0.42f, h * 0.78f)
                    lineTo(w * 0.22f, h * 0.92f)
                    lineTo(w * 0.26f, h * 0.75f)
                    cubicTo(w * 0.20f, h * 0.70f, w * 0.18f, h * 0.65f, w * 0.18f, h * 0.58f)
                    close()
                }
                drawPath(bubble, tint, style = line)
                listOf(0.39f, 0.53f, 0.67f).forEach { x -> drawCircle(tint, radius = 1.0.dp.toPx(), center = Offset(w * x, h * 0.45f)) }
            }
            MainTab.GROUPS -> {
                val rearBubble = Path().apply {
                    moveTo(w * 0.22f, h * 0.13f)
                    lineTo(w * 0.72f, h * 0.13f)
                    cubicTo(w * 0.82f, h * 0.13f, w * 0.88f, h * 0.20f, w * 0.88f, h * 0.30f)
                    lineTo(w * 0.88f, h * 0.56f)
                    cubicTo(w * 0.88f, h * 0.66f, w * 0.80f, h * 0.72f, w * 0.70f, h * 0.72f)
                    lineTo(w * 0.60f, h * 0.72f)
                    lineTo(w * 0.50f, h * 0.86f)
                    lineTo(w * 0.47f, h * 0.72f)
                    lineTo(w * 0.31f, h * 0.72f)
                    cubicTo(w * 0.21f, h * 0.72f, w * 0.14f, h * 0.65f, w * 0.14f, h * 0.55f)
                    lineTo(w * 0.14f, h * 0.31f)
                    cubicTo(w * 0.14f, h * 0.20f, w * 0.14f, h * 0.13f, w * 0.22f, h * 0.13f)
                    close()
                }
                drawPath(rearBubble, tint, style = line)
                val frontBubble = Path().apply {
                    moveTo(w * 0.34f, h * 0.34f)
                    lineTo(w * 0.79f, h * 0.34f)
                    cubicTo(w * 0.87f, h * 0.34f, w * 0.93f, h * 0.41f, w * 0.93f, h * 0.49f)
                    lineTo(w * 0.93f, h * 0.70f)
                    cubicTo(w * 0.93f, h * 0.79f, w * 0.87f, h * 0.85f, w * 0.78f, h * 0.85f)
                    lineTo(w * 0.64f, h * 0.85f)
                    lineTo(w * 0.55f, h * 0.96f)
                    lineTo(w * 0.53f, h * 0.85f)
                    lineTo(w * 0.38f, h * 0.85f)
                    cubicTo(w * 0.29f, h * 0.85f, w * 0.23f, h * 0.78f, w * 0.23f, h * 0.70f)
                }
                drawPath(frontBubble, tint, style = line)
            }
            MainTab.CALLS -> {
                val handset = Path().apply {
                    moveTo(w * 0.31f, h * 0.14f)
                    cubicTo(w * 0.23f, h * 0.14f, w * 0.18f, h * 0.22f, w * 0.20f, h * 0.32f)
                    cubicTo(w * 0.25f, h * 0.59f, w * 0.43f, h * 0.76f, w * 0.69f, h * 0.82f)
                    cubicTo(w * 0.79f, h * 0.84f, w * 0.86f, h * 0.77f, w * 0.85f, h * 0.68f)
                    lineTo(w * 0.82f, h * 0.56f)
                    cubicTo(w * 0.81f, h * 0.51f, w * 0.76f, h * 0.49f, w * 0.71f, h * 0.52f)
                    lineTo(w * 0.60f, h * 0.59f)
                    cubicTo(w * 0.49f, h * 0.53f, w * 0.42f, h * 0.46f, w * 0.37f, h * 0.35f)
                    lineTo(w * 0.43f, h * 0.24f)
                    cubicTo(w * 0.46f, h * 0.19f, w * 0.43f, h * 0.15f, w * 0.38f, h * 0.14f)
                    close()
                }
                drawPath(handset, tint, style = line)
            }
            MainTab.CONTACTS -> {
                drawCircle(tint, radius = w * 0.105f, center = Offset(w * 0.50f, h * 0.28f), style = line)
                drawCircle(tint, radius = w * 0.085f, center = Offset(w * 0.20f, h * 0.41f), style = line)
                drawCircle(tint, radius = w * 0.085f, center = Offset(w * 0.80f, h * 0.41f), style = line)
                val centerShoulders = Path().apply {
                    moveTo(w * 0.22f, h * 0.88f)
                    cubicTo(w * 0.24f, h * 0.68f, w * 0.36f, h * 0.58f, w * 0.50f, h * 0.58f)
                    cubicTo(w * 0.65f, h * 0.58f, w * 0.77f, h * 0.68f, w * 0.79f, h * 0.88f)
                }
                drawPath(centerShoulders, tint, style = line)
                val leftShoulder = Path().apply {
                    moveTo(w * 0.05f, h * 0.82f)
                    cubicTo(w * 0.06f, h * 0.66f, w * 0.13f, h * 0.59f, w * 0.25f, h * 0.59f)
                }
                drawPath(leftShoulder, tint, style = line)
                val rightShoulder = Path().apply {
                    moveTo(w * 0.75f, h * 0.59f)
                    cubicTo(w * 0.87f, h * 0.59f, w * 0.94f, h * 0.66f, w * 0.95f, h * 0.82f)
                }
                drawPath(rightShoulder, tint, style = line)
            }
            MainTab.PROFILE -> {
                drawCircle(tint, radius = w * 0.16f, center = Offset(w * 0.50f, h * 0.29f), style = line)
                val shoulders = Path().apply {
                    moveTo(w * 0.15f, h * 0.91f)
                    cubicTo(w * 0.17f, h * 0.65f, w * 0.32f, h * 0.56f, w * 0.50f, h * 0.56f)
                    cubicTo(w * 0.68f, h * 0.56f, w * 0.83f, h * 0.65f, w * 0.85f, h * 0.91f)
                }
                drawPath(shoulders, tint, style = line)
            }
        }
    }
}

@Composable
private fun BottomNavigation(
    selectedTab: MainTab,
    unreadChatsCount: Int,
    onSelect: (MainTab) -> Unit
) {
    Surface(
        color = Color.Transparent,
        shape = RoundedCornerShape(28.dp),
        shadowElevation = 0.dp,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Row(
            Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(
                    Brush.verticalGradient(
                        when {
                            appOrangeChocolateTheme.value -> listOf(Color(0xF2381A0A), Color(0xF12A1207), Color(0xF2381A0A))
                            appDarkTheme.value -> listOf(Color(0xF21B2534), Color(0xED141D2A), Color(0xF21B2534))
                            else -> listOf(Color(0xEFFFFFFF), Color(0xD8E8EEFC), Color(0xDDE3EAF8))
                        }
                    )
                )
                .padding(top = 8.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            MainTab.values().forEach { tab ->
                val selected = tab == selectedTab
                Column(
                    Modifier.weight(1f)
                        .clickable(interactionSource = null, indication = null) { onSelect(tab) }
                        .padding(vertical = 2.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier.size(width = 42.dp, height = 28.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        InboxNavigationGlyph(tab = tab, selected = selected)
                        if (tab == MainTab.CHATS) {
                            formatUnreadBadge(unreadChatsCount)?.let { badgeText ->
                                Surface(
                                    modifier = Modifier.align(Alignment.TopEnd)
                                        .height(18.dp).widthIn(min = 18.dp),
                                    color = InboxAccent,
                                    shape = CircleShape
                                ) {
                                    Box(
                                        modifier = Modifier.height(18.dp).padding(horizontal = 3.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            badgeText,
                                            color = Color.White,
                                            fontSize = 8.sp,
                                            lineHeight = 8.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        tab.title,
                        color = if (selected) InboxAccentStrong else InboxMuted,
                        fontSize = 10.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }
    }
}
