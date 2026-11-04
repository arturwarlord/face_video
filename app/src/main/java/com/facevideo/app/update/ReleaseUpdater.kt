package com.facevideo.app.update

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.facevideo.app.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

internal data class AvailableRelease(
    val versionName: String,
    val notes: String,
    val apkUrl: String,
    val checksumUrl: String
)

internal object ReleaseUpdater {
    private const val LATEST_RELEASE_URL =
        "https://api.github.com/repos/arturwarlord/face_video/releases/latest"
    private const val APK_ASSET_NAME = "ArturChat.apk"
    private const val CHECKSUM_ASSET_NAME = "ArturChat.apk.sha256"

    suspend fun findUpdate(): AvailableRelease? = withContext(Dispatchers.IO) {
        val response = getText(LATEST_RELEASE_URL)
        val release = JSONObject(response)
        val version = release.optString("tag_name").removePrefix("v").trim()
        if (version.isBlank() || !isNewerVersion(version, BuildConfig.VERSION_NAME)) {
            return@withContext null
        }

        val assets = release.optJSONArray("assets") ?: return@withContext null
        var apkUrl: String? = null
        var checksumUrl: String? = null
        for (index in 0 until assets.length()) {
            val asset = assets.optJSONObject(index) ?: continue
            when (asset.optString("name")) {
                APK_ASSET_NAME -> apkUrl = asset.optString("browser_download_url")
                CHECKSUM_ASSET_NAME -> checksumUrl = asset.optString("browser_download_url")
            }
        }

        if (apkUrl.isNullOrBlank() || checksumUrl.isNullOrBlank()) return@withContext null
        AvailableRelease(
            versionName = version,
            notes = release.optString("body").take(1200),
            apkUrl = apkUrl,
            checksumUrl = checksumUrl
        )
    }

    suspend fun downloadAndInstall(
        context: Context,
        release: AvailableRelease,
        onProgress: (Int) -> Unit
    ) = withContext(Dispatchers.IO) {
        val expectedHash = getText(release.checksumUrl)
            .trim()
            .substringBefore(' ')
            .lowercase()
        if (!expectedHash.matches(Regex("^[a-f0-9]{64}$"))) {
            throw IOException("Некорректная контрольная сумма обновления.")
        }

        val apkFile = File(context.cacheDir, "arturchat-update.apk")
        downloadFile(release.apkUrl, apkFile, onProgress)

        val actualHash = sha256(apkFile)
        if (!MessageDigest.isEqual(
                expectedHash.toByteArray(Charsets.US_ASCII),
                actualHash.toByteArray(Charsets.US_ASCII)
            )
        ) {
            apkFile.delete()
            throw IOException("Проверка целостности APK не пройдена. Обновление отменено.")
        }

        installApk(context, apkFile)
    }

    fun openInstallPermissionSettings(context: Context) {
        val intent = Intent(
            Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
            Uri.parse("package:${context.packageName}")
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    private fun isNewerVersion(remote: String, current: String): Boolean {
        fun parts(value: String): List<Int> = value.substringBefore('-')
            .split('.')
            .map { it.toIntOrNull() ?: 0 }

        val left = parts(remote)
        val right = parts(current)
        for (index in 0 until maxOf(left.size, right.size)) {
            val a = left.getOrElse(index) { 0 }
            val b = right.getOrElse(index) { 0 }
            if (a != b) return a > b
        }
        return false
    }

    private fun getText(address: String): String {
        val connection = (URL(address).openConnection() as HttpURLConnection).apply {
            connectTimeout = 15_000
            readTimeout = 30_000
            requestMethod = "GET"
            setRequestProperty("User-Agent", "ArturChat-Android")
            setRequestProperty("Accept", "application/vnd.github+json")
            setRequestProperty("X-GitHub-Api-Version", "2022-11-28")
        }
        try {
            val status = connection.responseCode
            if (status !in 200..299) throw IOException("Сервер обновлений ответил: HTTP $status")
            return connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }

    private suspend fun downloadFile(
        address: String,
        destination: File,
        onProgress: (Int) -> Unit
    ) {
        withContext(Dispatchers.IO) {
            val connection = (URL(address).openConnection() as HttpURLConnection).apply {
                connectTimeout = 15_000
                readTimeout = 60_000
                requestMethod = "GET"
                setRequestProperty("User-Agent", "ArturChat-Android")
            }
            try {
                val status = connection.responseCode
                if (status !in 200..299) throw IOException("Не удалось скачать APK: HTTP $status")
                val total = connection.contentLengthLong
                var received = 0L
                connection.inputStream.use { input ->
                    FileOutputStream(destination).use { output ->
                        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                        while (true) {
                            val count = input.read(buffer)
                            if (count < 0) break
                            output.write(buffer, 0, count)
                            received += count
                            if (total > 0L) {
                                val percent = ((received * 100L) / total).toInt().coerceIn(0, 100)
                                withContext(Dispatchers.Main) { onProgress(percent) }
                            }
                        }
                        output.fd.sync()
                    }
                }
                if (!destination.isFile || destination.length() < 1_000_000L) {
                    throw IOException("Загруженный APK выглядит неполным.")
                }
            } catch (error: Exception) {
                destination.delete()
                throw error
            } finally {
                connection.disconnect()
            }
        }
    }

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().buffered().use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                digest.update(buffer, 0, count)
            }
        }
        return digest.digest().joinToString("") { byte -> "%02x".format(byte) }
    }

    private fun installApk(context: Context, apkFile: File) {
        if (!context.packageManager.canRequestPackageInstalls()) {
            throw InstallPermissionRequiredException()
        }

        val installer = context.packageManager.packageInstaller
        val parameters = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL)
        parameters.setAppPackageName(context.packageName)
        parameters.setSize(apkFile.length())
        val sessionId = installer.createSession(parameters)

        try {
            installer.openSession(sessionId).use { session ->
                apkFile.inputStream().use { input ->
                    session.openWrite("ArturChat.apk", 0, apkFile.length()).use { output ->
                        input.copyTo(output)
                        session.fsync(output)
                    }
                }
                val callback = Intent(context, UpdateInstallReceiver::class.java)
                    .setAction(UpdateInstallReceiver.ACTION_INSTALL_STATUS)
                val pendingIntentFlags = PendingIntent.FLAG_UPDATE_CURRENT or
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0
                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    sessionId,
                    callback,
                    pendingIntentFlags
                )
                session.commit(pendingIntent.intentSender)
            }
        } catch (error: Exception) {
            runCatching { installer.abandonSession(sessionId) }
            throw error
        }
    }
}

internal class InstallPermissionRequiredException : IOException(
    "Нужно разрешить ArturChat устанавливать обновления из этого источника."
)

private const val ARTURCHAT_OFFICIAL_SITE = "https://arturwarlord.github.io/face_video/"

@Composable
fun ArturChatUpdateHost(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val pendingConfirmationIntent by UpdateInstallCoordinator.pendingConfirmationIntent.collectAsState()
    var availableRelease by remember { mutableStateOf<AvailableRelease?>(null) }
    var isDownloading by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf(0) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var installerMessage by remember { mutableStateOf<String?>(null) }
    var installerMessageAt by remember { mutableStateOf(0L) }
    var installSubmitted by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        availableRelease = runCatching { ReleaseUpdater.findUpdate() }.getOrNull()
    }

    content()

    val release = availableRelease
    LaunchedEffect(release?.versionName) {
        if (release != null) {
            val preferences = context.getSharedPreferences(
                UpdateInstallReceiver.PREFERENCES,
                Context.MODE_PRIVATE
            )
            while (true) {
                val updatedAt = preferences.getLong(
                    UpdateInstallReceiver.KEY_INSTALL_UPDATED_AT,
                    0L
                )
                if (updatedAt > installerMessageAt) {
                    installerMessageAt = updatedAt
                    installerMessage = preferences.getString(
                        UpdateInstallReceiver.KEY_INSTALL_MESSAGE,
                        null
                    )
                    when (preferences.getString(UpdateInstallReceiver.KEY_INSTALL_STATE, "")) {
                        "success" -> {
                            availableRelease = null
                            installSubmitted = false
                        }
                        "failure" -> installSubmitted = false
                    }
                }
                delay(500)
            }
        }
    }
    if (release != null) {
        val updateHighlights = remember(release.notes) {
            release.notes.lineSequence()
                .map { it.trim() }
                .filter { it.startsWith("- ") || it.startsWith("• ") }
                .map {
                    it.removePrefix("- ")
                        .removePrefix("• ")
                        .replace(Regex("""\\*\\*(.*?)\\*\\*"""), "$1")
                        .trim()
                }
                .filterNot {
                    it.startsWith("Следите за новостями", ignoreCase = true) ||
                        it.startsWith("Разработчик:", ignoreCase = true) ||
                        it.startsWith("Бета-тест:", ignoreCase = true)
                }
                .take(4)
                .map { if (it.length > 100) it.take(97).trimEnd() + "…" else it }
                .toList()
                .ifEmpty { listOf("Новые функции и улучшения") }
        }
        AlertDialog(
            onDismissRequest = { if (!isDownloading) availableRelease = null },
            title = {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("Доступно обновление", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text("Версия ${release.versionName}", color = Color(0xFF858397), fontSize = 12.sp)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text("Что добавлено:", fontWeight = FontWeight.SemiBold, fontSize = 11.sp, color = Color(0xFF858397))
                    updateHighlights.forEach { item ->
                        Text("• $item", fontSize = 12.sp, color = Color(0xFF343246))
                    }
                    TextButton(
                        onClick = {
                            runCatching {
                                context.startActivity(
                                    Intent(Intent.ACTION_VIEW, Uri.parse(ARTURCHAT_OFFICIAL_SITE))
                                )
                            }.onFailure {
                                errorMessage = "Не удалось открыть сайт."
                            }
                        },
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                    ) {
                        Text("Наш сайт ↗", fontSize = 12.sp)
                    }
                    Text("Разработчик: Artur Ai", color = Color(0xFF858397), fontSize = 10.sp)
                    Text("Бета-тест: Rasul S, Radmir A", color = Color(0xFF858397), fontSize = 10.sp)

                    if (isDownloading) {
                        Spacer(Modifier.height(4.dp))
                        CircularProgressIndicator(progress = { progress / 100f })
                        Text("Загрузка: $progress%", fontSize = 11.sp)
                    }
                    if (errorMessage != null) {
                        Text(errorMessage.orEmpty(), fontSize = 11.sp, color = Color(0xFFC97888))
                    }
                    if (pendingConfirmationIntent != null) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Файл готов. Открой установщик Android.",
                            fontWeight = FontWeight.Medium,
                            fontSize = 11.sp
                        )
                    } else if (installSubmitted && installerMessage == null) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "APK передан Android. Ожидаем подтверждение.",
                            fontWeight = FontWeight.Medium,
                            fontSize = 11.sp
                        )
                    }
                    if (installerMessage != null) {
                        Spacer(Modifier.height(4.dp))
                        Text(installerMessage.orEmpty(), fontSize = 11.sp)
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = !isDownloading && (!installSubmitted || pendingConfirmationIntent != null),
                    onClick = {
                        val confirmation = pendingConfirmationIntent
                        if (confirmation != null) {
                            try {
                                // This runs from a real user tap while ArturChat is in the foreground.
                                context.startActivity(Intent(confirmation))
                            } catch (error: Exception) {
                                errorMessage = "Не удалось открыть системный установщик: ${error.message ?: "неизвестная ошибка"}"
                            }
                        } else if (!installSubmitted) {
                            scope.launch {
                                if (!context.packageManager.canRequestPackageInstalls()) {
                                    errorMessage = "Разреши установку для ArturChat в системных настройках, вернись в приложение и нажми «Обновить» ещё раз."
                                    ReleaseUpdater.openInstallPermissionSettings(context)
                                    return@launch
                                }
                                isDownloading = true
                                errorMessage = null
                                installerMessage = null
                                installSubmitted = false
                                progress = 0
                                context.getSharedPreferences(
                                    UpdateInstallReceiver.PREFERENCES,
                                    Context.MODE_PRIVATE
                                ).edit()
                                    .remove(UpdateInstallReceiver.KEY_INSTALL_STATE)
                                    .remove(UpdateInstallReceiver.KEY_INSTALL_MESSAGE)
                                    .putLong(UpdateInstallReceiver.KEY_INSTALL_UPDATED_AT, 0L)
                                    .apply()
                                try {
                                    ReleaseUpdater.downloadAndInstall(context, release) { progress = it }
                                    installSubmitted = true
                                } catch (error: InstallPermissionRequiredException) {
                                    errorMessage = error.message
                                } catch (error: Exception) {
                                    errorMessage = error.message ?: "Не удалось установить обновление."
                                } finally {
                                    isDownloading = false
                                }
                            }
                        }
                    }
                ) {
                    Text(
                        when {
                            isDownloading -> "Загрузка..."
                            pendingConfirmationIntent != null -> "Открыть установщик Android"
                            installSubmitted -> "Ожидаем Android..."
                            else -> "Обновить"
                        },
                        fontSize = 12.sp
                    )
                }
            },
            dismissButton = {
                TextButton(
                    enabled = !isDownloading,
                    onClick = { availableRelease = null }
                ) { Text("Позже", fontSize = 12.sp) }
            }
        )
    }
}
