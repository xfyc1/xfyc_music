package com.xfyc.music.data.cache

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CacheManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val cacheRoot = File(context.cacheDir, "music_cache")
    val audioCacheDir = File(cacheRoot, "audio")
    val coverCacheDir = File(cacheRoot, "cover")
    val lyricCacheDir = File(cacheRoot, "lyric")
    val searchCacheDir = File(cacheRoot, "search")

    init {
        listOf(audioCacheDir, coverCacheDir, lyricCacheDir, searchCacheDir).forEach {
            if (!it.exists()) it.mkdirs()
        }
    }

    fun getCachedCover(url: String): File? {
        val file = File(coverCacheDir, url.toMD5())
        return if (file.exists()) file else null
    }

    fun saveCover(url: String, data: ByteArray): File {
        val file = File(coverCacheDir, url.toMD5())
        file.writeBytes(data)
        return file
    }

    fun getCachedLyric(url: String): File? {
        val file = File(lyricCacheDir, url.toMD5())
        return if (file.exists()) file else null
    }

    fun saveLyric(url: String, content: String): File {
        val file = File(lyricCacheDir, url.toMD5())
        file.writeText(content)
        return file
    }

    fun getCachedAudio(url: String): File? {
        val file = File(audioCacheDir, url.toMD5())
        return if (file.exists()) file else null
    }

    fun saveAudio(url: String, data: ByteArray): File {
        val file = File(audioCacheDir, url.toMD5())
        file.writeBytes(data)
        return file
    }

    fun getCachedSearch(query: String, sourceId: Long): String? {
        val file = File(searchCacheDir, "${sourceId}_${query.toMD5()}")
        return if (file.exists() && System.currentTimeMillis() - file.lastModified() < 5 * 60 * 1000) {
            file.readText()
        } else null
    }

    fun saveSearch(query: String, sourceId: Long, result: String) {
        val file = File(searchCacheDir, "${sourceId}_${query.toMD5()}")
        file.writeText(result)
    }

    fun clearCache() {
        listOf(audioCacheDir, coverCacheDir, lyricCacheDir, searchCacheDir).forEach { dir ->
            dir.listFiles()?.forEach { it.delete() }
        }
    }

    fun getCacheSize(): Long {
        return cacheRoot.walkTopDown().filter { it.isFile }.sumOf { it.length() }
    }

    fun enforceSizeLimit(maxSizeMB: Int = 500) {
        val maxBytes = maxSizeMB * 1024L * 1024L
        var currentSize = getCacheSize()
        if (currentSize <= maxBytes) return

        val allFiles = cacheRoot.walkTopDown()
            .filter { it.isFile }
            .sortedBy { it.lastModified() }
            .toList()

        for (file in allFiles) {
            if (currentSize <= maxBytes * 0.8) break
            currentSize -= file.length()
            file.delete()
        }
    }

    companion object {
        private fun String.toMD5(): String {
            val digest = java.security.MessageDigest.getInstance("MD5")
            return digest.digest(this.toByteArray()).joinToString("") { "%02x".format(it) }
        }
    }
}
