package com.xfyc.music.data.source

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.xfyc.music.data.local.entity.MusicSourceEntity
import com.xfyc.music.domain.model.Song
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SourceHttpClient @Inject constructor(
    private val okHttpClient: OkHttpClient
) {

    fun buildClient(source: MusicSourceEntity): OkHttpClient {
        return okHttpClient.newBuilder()
            .connectTimeout(source.timeoutSeconds.toLong(), TimeUnit.SECONDS)
            .readTimeout(source.timeoutSeconds.toLong(), TimeUnit.SECONDS)
            .build()
    }

    fun executeGetRequest(source: MusicSourceEntity, url: String): String {
        val client = buildClient(source)
        val request = Request.Builder()
            .url(url)
            .apply { addAuthHeaders(source) }
            .get()
            .build()
        val response = client.newCall(request).execute()
        if (!response.isSuccessful) {
            throw Exception("HTTP ${response.code}：${response.message}")
        }
        return response.body?.string() ?: throw Exception("响应内容为空")
    }

    fun executePostRequest(
        source: MusicSourceEntity?,
        url: String,
        body: String,
        contentType: okhttp3.MediaType,
        extraHeaders: Map<String, String> = emptyMap()
    ): String {
        val client = if (source != null) buildClient(source) else okHttpClient
        val requestBuilder = Request.Builder()
            .url(url)
            .post(body.toRequestBody(contentType))
        if (source != null) {
            requestBuilder.apply { addAuthHeaders(source) }
        }
        extraHeaders.forEach { (k, v) -> requestBuilder.header(k, v) }
        val response = client.newCall(requestBuilder.build()).execute()
        if (!response.isSuccessful) {
            throw Exception("HTTP ${response.code}：${response.message}")
        }
        return response.body?.string() ?: throw Exception("响应内容为空")
    }

    private fun Request.Builder.addAuthHeaders(source: MusicSourceEntity) {
        val config = source.authConfig ?: return
        try {
            val authJson = JsonParser.parseString(config).asJsonObject
            when (source.authType) {
                "token" -> {
                    val token = authJson.getString("token") ?: return
                    header("Authorization", "Bearer $token")
                }
                "api_key" -> {
                    val key = authJson.getString("key") ?: return
                    val headerName = authJson.getString("headerName") ?: "X-Api-Key"
                    header(headerName, key)
                }
                "basic_auth" -> {
                    val username = authJson.getString("username") ?: return
                    val password = authJson.getString("password") ?: return
                    val credentials = "$username:$password"
                    val encoded = android.util.Base64.encodeToString(
                        credentials.toByteArray(), android.util.Base64.NO_WRAP
                    )
                    header("Authorization", "Basic $encoded")
                }
                "cookie" -> {
                    val cookie = authJson.getString("cookie") ?: return
                    header("Cookie", cookie)
                }
                "custom_header" -> {
                    val headerName = authJson.getString("headerName") ?: return
                    val headerValue = authJson.getString("headerValue") ?: return
                    header(headerName, headerValue)
                }
            }
        } catch (_: Exception) { }
    }

    fun parseGenericSongFromJson(obj: JsonObject, sourceName: String): Song {
        val id = obj.getString("id") ?: obj.getString("songId")
            ?: obj.getString("hash") ?: obj.getString("songmid") ?: ""
        val title = obj.getString("title") ?: obj.getString("name")
            ?: obj.getString("songName") ?: "未知歌曲"
        val artist = obj.getString("artist") ?: obj.getString("singer")
            ?: obj.getString("author") ?: obj.getString("artists") ?: "未知艺术家"
        val album = obj.getString("album") ?: obj.getString("albumName")
            ?: obj.getString("albumname") ?: ""
        val duration = obj.getLong("duration") ?: obj.getLong("durationMs")
            ?: obj.getLong("interval") ?: 0L

        val cover = obj.getString("cover") ?: obj.getString("coverUrl")
            ?: obj.getString("picUrl") ?: obj.getString("imageUrl")
            ?: obj.getString("img") ?: obj.getString("imgUrl")

        val lyric = obj.getString("lyric") ?: obj.getString("lyricUrl")
            ?: obj.getString("lrcUrl") ?: obj.getString("lrc")
        val playUrl = obj.getString("playUrl") ?: obj.getString("url")
            ?: obj.getString("src") ?: obj.getString("musicUrl")

        return Song(
            id = 0,
            title = title,
            artist = artist,
            album = album,
            duration = duration,
            sourceType = "remote",
            sourceId = id.takeIf { it.isNotBlank() },
            playUrl = playUrl,
            coverUrl = cover,
            lyricUrl = lyric,
            format = "MP3",
            bitrate = 0,
            fileSize = 0
        )
    }
}

fun JsonObject.getString(key: String): String? {
    return try { get(key)?.asString } catch (_: Exception) { null }
}

fun JsonObject.getLong(key: String): Long? {
    return try { get(key)?.asLong } catch (_: Exception) { null }
}

fun JsonObject?.getAsJsonObject(key: String): JsonObject? {
    return try { this?.get(key)?.asJsonObject } catch (_: Exception) { null }
}

fun JsonObject?.getAsJsonArray(key: String): com.google.gson.JsonArray? {
    return try { this?.get(key)?.asJsonArray } catch (_: Exception) { null }
}
