package com.xfyc.music.data.source

import com.xfyc.music.data.local.entity.MusicSourceEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject
import javax.inject.Singleton

data class LxMusicConfig(
    val name: String,
    val description: String,
    val version: String,
    val author: String,
    val apiUrl: String,
    val apiKey: String,
    val sources: Map<String, List<String>> // sourceId -> qualities
)

@Singleton
class LxMusicImporter @Inject constructor(
    private val okHttpClient: OkHttpClient
) {

    private val sourceNameMap = mapOf(
        "kw" to "酷我音乐",
        "kg" to "酷狗音乐",
        "tx" to "天行音乐",
        "wy" to "网易云音乐",
        "mg" to "咪咕音乐",
    )

    suspend fun importFromUrl(url: String): Result<List<MusicSourceEntity>> {
        return withContext(Dispatchers.IO) {
            try {
                val jsContent = fetchJsFile(url)
                val config = parseJsConfig(jsContent)
                val entities = config.sources.map { (sourceId, qualities) ->
                    createMusicSourceEntity(config, sourceId, qualities)
                }
                Result.success(entities)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    private fun fetchJsFile(url: String): String {
        val request = Request.Builder().url(url).get().build()
        val response = okHttpClient.newCall(request).execute()
        if (!response.isSuccessful) {
            throw Exception("HTTP ${response.code}：${response.message}")
        }
        return response.body?.string() ?: throw Exception("响应内容为空")
    }

    fun parseJsConfig(jsContent: String): LxMusicConfig {
        val name = Regex("""@name\s+(.+?)(?:\n|$)""").find(jsContent)?.groupValues?.get(1)?.trim() ?: ""
        val description = Regex("""@description\s+(.+?)(?:\n|$)""").find(jsContent)?.groupValues?.get(1)?.trim() ?: ""
        val author = Regex("""@author\s+(.+?)(?:\n|$)""").find(jsContent)?.groupValues?.get(1)?.trim() ?: ""
        val version = Regex("""@version\s+v?([\d.]+)""").find(jsContent)?.groupValues?.get(1)?.trim() ?: ""

        val apiUrl = Regex("""API_URL\s*=\s*['"](.+?)['"]""").find(jsContent)?.groupValues?.get(1) ?: ""
        val apiKey = Regex("""API_KEY\s*=\s*['"](.+?)['"]""").find(jsContent)?.groupValues?.get(1) ?: ""

        if (apiUrl.isBlank()) {
            throw Exception("未找到 API_URL 配置")
        }

        // Parse MUSIC_QUALITY object to get source IDs
        val sources = mutableMapOf<String, List<String>>()
        val qualityRegex = Regex("""(\w+)\s*:\s*\[([^\]]+)\]""")

        // Find the MUSIC_QUALITY block
        val musicQualityBlock = Regex(
            """MUSIC_QUALITY\s*=\s*\{([^}]+)\}""",
            RegexOption.DOT_MATCHES_ALL
        ).find(jsContent)?.groupValues?.get(1) ?: ""

        qualityRegex.findAll(musicQualityBlock).forEach { match ->
            val sourceId = match.groupValues[1].trim()
            val qualitiesStr = match.groupValues[2].trim()
            val qualities = Regex("""'([^']+)'""").findAll(qualitiesStr).map { it.groupValues[1] }.toList()
            sources[sourceId] = qualities
        }

        if (sources.isEmpty()) {
            // Fallback: try to parse MUSIC_SOURCE array
            val musicSourceRegex = Regex("""MUSIC_SOURCE\s*=\s*\[([^\]]+)\]""")
            val musicSourceBlock = musicSourceRegex.find(jsContent)?.groupValues?.get(1) ?: ""
            val sourceIds = Regex("""['"]([^'"]+)['"]""").findAll(musicSourceBlock).map { it.groupValues[1] }.toList()
            sourceIds.forEach { sources[it] = listOf("128k") }
        }

        return LxMusicConfig(
            name = name,
            description = description,
            version = version,
            author = author,
            apiUrl = apiUrl,
            apiKey = apiKey,
            sources = sources
        )
    }

    private fun createMusicSourceEntity(
        config: LxMusicConfig,
        sourceId: String,
        qualities: List<String>
    ): MusicSourceEntity {
        val displayName = sourceNameMap[sourceId] ?: sourceId.uppercase()
        val bestQuality = qualities.lastOrNull() ?: "320k"
        val allQualities = qualities.joinToString(",")
        val baseUrl = config.apiUrl.trimEnd('/')

        return MusicSourceEntity(
            name = "$displayName (LX)",
            type = "lx_music",
            baseUrl = config.apiUrl,
            authType = "api_key",
            authConfig = """{"key": "${config.apiKey}", "headerName": "X-Request-Key"}""",
            searchUrl = "$baseUrl/search?keyword={query}&source=$sourceId&page={page}",
            songDetailUrl = null,
            playUrl = "$baseUrl/url/$sourceId/{songId}/$bestQuality",
            coverUrl = null,
            lyricUrl = null,
            configJson = """{"sourceId":"$sourceId","qualities":"$allQualities","quality":"$bestQuality","importer":"lx_music","importVersion":"${config.version}","importUrl":"${config.apiUrl}"}""",
            enabled = true,
            status = "idle"
        )
    }
}
