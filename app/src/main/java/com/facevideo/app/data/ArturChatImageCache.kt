package com.facevideo.app.data

import android.content.Context
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap

/**
 * Private on-device image cache for public profile avatars and friend-only stories.
 * Cached bytes are shown immediately; remote refreshes run on Dispatchers.IO from
 * Compose callers. Story entries are namespaced by the signed-in viewer.
 */
class ArturChatImageCache(context: Context) {
    private val appContext = context.applicationContext
    private val root = File(appContext.cacheDir, "arturchat_media_v1")
    private val preferences = appContext.getSharedPreferences(
        "arturchat_media_index_v1",
        Context.MODE_PRIVATE
    )

    fun readBytes(kind: String, ownerId: String, mediaId: String): ByteArray? {
        val file = mediaFile(kind, ownerId, mediaId)
        if (!file.isFile) return null
        file.setLastModified(System.currentTimeMillis())
        return runCatching { file.readBytes() }.getOrNull()
    }

    fun shouldRefresh(
        kind: String,
        ownerId: String,
        mediaId: String,
        sourceUrl: String,
        freshnessMillis: Long,
        refreshWhenSourceChanges: Boolean
    ): Boolean {
        val file = mediaFile(kind, ownerId, mediaId)
        if (!file.isFile) return true
        val id = entryId(kind, ownerId, mediaId)
        val lastFetched = preferences.getLong("$id.fetched", 0L)
        val sourceChanged = refreshWhenSourceChanges &&
            preferences.getString("$id.source", null) != sourceUrl
        return sourceChanged || System.currentTimeMillis() - lastFetched >= freshnessMillis
    }

    /**
     * Downloads once per media key, reuses fresh disk entries, and limits the
     * cache to 64 MiB. Call from a background dispatcher.
     */
    fun fetchAndStore(
        kind: String,
        ownerId: String,
        mediaId: String,
        sourceUrl: String,
        freshnessMillis: Long,
        refreshWhenSourceChanges: Boolean
    ): ByteArray? {
        if (sourceUrl.isBlank()) return readBytes(kind, ownerId, mediaId)
        val identity = "$kind|$ownerId|$mediaId"
        val lock = downloadLocks.computeIfAbsent(identity) { Any() }
        return synchronized(lock) {
            if (!shouldRefresh(
                    kind, ownerId, mediaId, sourceUrl,
                    freshnessMillis, refreshWhenSourceChanges
                )
            ) {
                return@synchronized readBytes(kind, ownerId, mediaId)
            }

            val connection = URL(sourceUrl).openConnection().apply {
                connectTimeout = CONNECT_TIMEOUT_MS
                readTimeout = READ_TIMEOUT_MS
                useCaches = true
            }
            val bytes = connection.getInputStream().use { input ->
                val output = ByteArrayOutputStream()
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                var total = 0
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    total += count
                    require(total <= MAX_IMAGE_BYTES) { "Изображение превышает допустимый размер кеша." }
                    output.write(buffer, 0, count)
                }
                output.toByteArray()
            }
            writeBytes(
                kind = kind,
                ownerId = ownerId,
                mediaId = mediaId,
                bytes = bytes,
                sourceUrl = if (refreshWhenSourceChanges) sourceUrl else null
            )
            bytes
        }
    }

    /** Seeds a just-published story from the bytes already uploaded by this device. */
    fun writeBytes(
        kind: String,
        ownerId: String,
        mediaId: String,
        bytes: ByteArray,
        sourceUrl: String? = null
    ) {
        if (bytes.isEmpty() || bytes.size > MAX_IMAGE_BYTES) return
        val file = mediaFile(kind, ownerId, mediaId)
        file.parentFile?.mkdirs()
        val temporary = File(file.parentFile, "${file.name}.tmp")
        FileOutputStream(temporary).use { stream ->
            stream.write(bytes)
            stream.fd.sync()
        }
        if (!temporary.renameTo(file)) {
            temporary.copyTo(file, overwrite = true)
            temporary.delete()
        }

        val id = entryId(kind, ownerId, mediaId)
        preferences.edit()
            .putString("$id.kind", kind)
            .putString("$id.owner", ownerId)
            .putString("$id.media", mediaId)
            .putLong("$id.fetched", System.currentTimeMillis())
            .apply {
                if (sourceUrl == null) remove("$id.source")
                else putString("$id.source", sourceUrl)
            }
            .apply()
        trimCache()
    }

    fun remove(kind: String, ownerId: String, mediaId: String) {
        val id = entryId(kind, ownerId, mediaId)
        mediaFile(kind, ownerId, mediaId).delete()
        preferences.edit()
            .remove("$id.kind")
            .remove("$id.owner")
            .remove("$id.media")
            .remove("$id.source")
            .remove("$id.fetched")
            .apply()
    }

    /** Remove expired or no-longer-visible private stories after a successful server refresh. */
    fun pruneStories(viewerUserId: String, visibleStoryIds: Set<String>) {
        val editor = preferences.edit()
        preferences.all.keys.filter { it.endsWith(".kind") }.forEach { kindKey ->
            val id = kindKey.removeSuffix(".kind")
            val kind = preferences.getString("$id.kind", null)
            val owner = preferences.getString("$id.owner", null)
            val mediaId = preferences.getString("$id.media", null)
            if (kind == "stories" && owner == viewerUserId && mediaId != null && mediaId !in visibleStoryIds) {
                mediaFile(kind, owner, mediaId).delete()
                editor.remove("$id.kind")
                    .remove("$id.owner")
                    .remove("$id.media")
                    .remove("$id.source")
                    .remove("$id.fetched")
            }
        }
        editor.apply()
    }

    private fun mediaFile(kind: String, ownerId: String, mediaId: String): File {
        val safeKind = kind.replace(Regex("[^A-Za-z0-9_-]"), "_")
        return File(File(root, safeKind), "${entryId(kind, ownerId, mediaId)}.bin")
    }

    private fun entryId(kind: String, ownerId: String, mediaId: String): String =
        sha256("$kind|$ownerId|$mediaId")

    private fun sha256(value: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(value.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }

    private fun trimCache() {
        val files = root.walkTopDown().filter { it.isFile && !it.name.endsWith(".tmp") }
            .toList().sortedBy { it.lastModified() }
        var total = files.sumOf { it.length() }
        if (total <= MAX_CACHE_BYTES) return
        val editor = preferences.edit()
        for (file in files) {
            if (total <= MAX_CACHE_BYTES) break
            val size = file.length()
            val id = file.nameWithoutExtension
            file.delete()
            total -= size
            editor.remove("$id.kind")
                .remove("$id.owner")
                .remove("$id.media")
                .remove("$id.source")
                .remove("$id.fetched")
        }
        editor.apply()
    }

    companion object {
        private const val CONNECT_TIMEOUT_MS = 8_000
        private const val READ_TIMEOUT_MS = 12_000
        private const val MAX_IMAGE_BYTES = 12 * 1024 * 1024
        private const val MAX_CACHE_BYTES = 64L * 1024L * 1024L
        private val downloadLocks = ConcurrentHashMap<String, Any>()
    }
}
