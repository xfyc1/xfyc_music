package com.xfyc.music.data.lyric

import com.xfyc.music.data.cache.CacheManager
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LyricLoader @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val cacheManager: CacheManager
) {

    data class LyricLine(
        val timeMs: Long,
        val text: String
    )

    suspend fun loadLyric(lyricUrl: String?, localFilePath: String?): List<LyricLine>? {
        // Try online URL first
        lyricUrl?.let { url ->
            try {
                val cached = cacheManager.getCachedLyric(url)
                val content = if (cached != null) {
                    cached.readText()
                } else {
                    val response = okHttpClient.newCall(Request.Builder().url(url).build()).execute()
                    if (response.isSuccessful) {
                        val body = response.body?.string() ?: return@let null
                        cacheManager.saveLyric(url, body)
                        body
                    } else null
                }
                return parseLrc(content)
            } catch (_: Exception) { }
        }

        // Try local LRC file
        localFilePath?.let { path ->
            val lrcPath = path.replaceAfterLast('.', "lrc")
            val lrcFile = File(lrcPath)
            if (lrcFile.exists()) {
                return parseLrc(lrcFile.readText())
            }
        }

        return null
    }

    fun parseLrc(content: String?): List<LyricLine>? {
        if (content == null) return null
        val lines = mutableListOf<LyricLine>()
        val regex = Regex("""\[(\d{2}):(\d{2})(?:\.(\d{2,3}))?\](.*)""")

        for (line in content.lines()) {
            val match = regex.find(line.trim()) ?: continue
            val min = match.groupValues[1].toIntOrNull() ?: continue
            val sec = match.groupValues[2].toIntOrNull() ?: continue
            val msStr = match.groupValues.getOrNull(3)?.takeIf { it.isNotEmpty() }
            val ms = if (msStr != null && msStr.length == 2) {
                (msStr.toIntOrNull() ?: 0) * 10
            } else {
                msStr?.toIntOrNull() ?: 0
            }
            val text = match.groupValues[4].trim()
            if (text.isNotEmpty()) {
                lines.add(LyricLine(min * 60000L + sec * 1000L + ms, text))
            }
        }

        return if (lines.isEmpty()) null else lines.sortedBy { it.timeMs }
    }

    fun findCurrentLine(lyrics: List<LyricLine>, positionMs: Long): Int {
        var result = -1
        for (i in lyrics.indices) {
            if (lyrics[i].timeMs <= positionMs) {
                result = i
            } else {
                break
            }
        }
        return result
    }
}
