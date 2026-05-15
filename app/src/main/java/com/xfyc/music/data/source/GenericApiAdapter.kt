package com.xfyc.music.data.source

import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.xfyc.music.data.cache.CacheManager
import com.xfyc.music.data.local.entity.MusicSourceEntity
import com.xfyc.music.domain.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GenericApiAdapter @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val gson: Gson,
    private val cacheManager: CacheManager
) : MusicSourceAdapter {

    override suspend fun testConnection(source: MusicSourceEntity): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                val client = buildClient(source)
                val request = Request.Builder()
                    .url(source.baseUrl)
                    .apply { addAuthHeaders(source) }
                    .get()
                    .build()
                val response = client.newCall(request).execute()
                Result.success(response.isSuccessful)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun search(
        source: MusicSourceEntity,
        query: String,
        page: Int
    ): Result<List<Song>> {
        return withContext(Dispatchers.IO) {
            try {
                // LX Music sources use direct music service API calls
                if (source.type == "lx_music") {
                    val sourceId = extractSourceId(source)
                    val response = when (sourceId) {
                        "tx" -> searchQQMusic(query, page)
                        "wy" -> searchNetEaseMusic(query, page)
                        "kw" -> searchKuwoMusic(query, page)
                        "kg" -> searchKugouMusic(query, page)
                        else -> return@withContext Result.failure(Exception("暂不支持该音源搜索: $sourceId"))
                    }
                    val songs = parseLxMusicSearchResponse(source, response, sourceId)
                    return@withContext Result.success(songs)
                }

                val searchUrl = source.searchUrl
                    ?: return@withContext Result.failure(Exception("搜索 URL 未配置"))

                val url = searchUrl
                    .replace("{query}", java.net.URLEncoder.encode(query, "UTF-8"))
                    .replace("{page}", page.toString())
                    .replace("{baseUrl}", source.baseUrl.trimEnd('/'))

                val response = executeRequest(source, url)
                val songs = parseSearchResponse(source, response, query)
                Result.success(songs)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun getSongDetail(source: MusicSourceEntity, songId: String): Result<Song> {
        return withContext(Dispatchers.IO) {
            try {
                val detailUrl = source.songDetailUrl
                    ?: return@withContext Result.failure(Exception("详情 URL 未配置"))

                val url = detailUrl
                    .replace("{songId}", songId)
                    .replace("{baseUrl}", source.baseUrl.trimEnd('/'))

                val response = executeRequest(source, url)
                val song = parseSongDetail(source, response, songId)
                Result.success(song)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun getPlayUrl(source: MusicSourceEntity, songId: String): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val playUrl = source.playUrl
                    ?: return@withContext Result.failure(Exception("播放 URL 未配置"))

                val url = playUrl
                    .replace("{songId}", songId)
                    .replace("{baseUrl}", source.baseUrl.trimEnd('/'))

                // Try to extract URL from JSON response first
                val response = executeRequest(source, url)
                val json = JsonParser.parseString(response)
                if (json.isJsonObject) {
                    val obj = json.asJsonObject
                    // Direct URL field
                    val urlField = obj.get("url") ?: obj.get("playUrl") ?: obj.get("src")
                    if (urlField != null) return@withContext Result.success(urlField.asString)
                    // LX Music style: data field might be a URL string or an object with url
                    val dataField = obj.get("data")
                    if (dataField != null) {
                        if (dataField.isJsonPrimitive) {
                            return@withContext Result.success(dataField.asString)
                        }
                        if (dataField.isJsonObject) {
                            val dataObj = dataField.asJsonObject
                            val nestedUrl = dataObj.get("url") ?: dataObj.get("playUrl")
                                ?: dataObj.get("src")
                            if (nestedUrl != null) return@withContext Result.success(nestedUrl.asString)
                        }
                    }
                }
                // Otherwise return the request URL itself
                Result.success(url)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun getCoverUrl(source: MusicSourceEntity, songId: String): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val coverUrl = source.coverUrl
                    ?: return@withContext Result.failure(Exception("封面 URL 未配置"))

                val url = coverUrl
                    .replace("{songId}", songId)
                    .replace("{baseUrl}", source.baseUrl.trimEnd('/'))

                val response = executeRequest(source, url)
                // Try to extract URL from JSON response
                val json = JsonParser.parseString(response)
                if (json.isJsonObject) {
                    val obj = json.asJsonObject
                    val urlField = obj.get("url") ?: obj.get("coverUrl") ?: obj.get("cover")
                    if (urlField != null) return@withContext Result.success(urlField.asString)
                }
                Result.success(url)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun getLyricUrl(source: MusicSourceEntity, songId: String): Result<String?> {
        return withContext(Dispatchers.IO) {
            try {
                val lyricUrl = source.lyricUrl ?: return@withContext Result.success(null)
                val url = lyricUrl
                    .replace("{songId}", songId)
                    .replace("{baseUrl}", source.baseUrl.trimEnd('/'))

                val response = executeRequest(source, url)
                val json = JsonParser.parseString(response)
                if (json.isJsonObject) {
                    val obj = json.asJsonObject
                    val urlField = obj.get("url") ?: obj.get("lyricUrl") ?: obj.get("lyric")
                    if (urlField != null) return@withContext Result.success(urlField.asString)
                }
                // Response might be the lyric content itself
                if (response.trimStart().startsWith("[") || response.trimStart().startsWith("{")) {
                    return@withContext Result.success(null)
                }
                Result.success(url)
            } catch (e: Exception) {
                Result.success(null) // Lyrics are optional
            }
        }
    }

    private fun executeRequest(source: MusicSourceEntity, url: String): String {
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

    private fun parseSearchResponse(source: MusicSourceEntity, json: String, query: String): List<Song> {
        val root = JsonParser.parseString(json)
        val items: JsonArray? = when {
            root.isJsonObject -> {
                val obj = root.asJsonObject
                // Try direct array fields first
                obj.getAsJsonArray("data") ?: obj.getAsJsonArray("result")
                ?: obj.getAsJsonArray("results") ?: obj.getAsJsonArray("songs")
                ?: obj.getAsJsonArray("items") ?: obj.getAsJsonArray("list")
                // LX Music style: data is an object containing a list
                ?: obj.getAsJsonObject("data")?.let { dataObj ->
                    dataObj.getAsJsonArray("list") ?: dataObj.getAsJsonArray("songs")
                    ?: dataObj.getAsJsonArray("items") ?: dataObj.getAsJsonArray("results")
                }
            }
            root.isJsonArray -> root.asJsonArray
            else -> return emptyList()
        }

        return items?.mapNotNull { element ->
            parseSongFromJson(source, element.asJsonObject, source.name)
        } ?: emptyList()
    }

    private fun parseSongDetail(source: MusicSourceEntity, json: String, songId: String): Song {
        val root = JsonParser.parseString(json)
        val obj = when {
            root.isJsonObject -> root.asJsonObject
            else -> throw Exception("无效的响应格式")
        }
        val data = obj.getAsJsonObject("data") ?: obj
        return parseSongFromJson(source, data, source.name)
            .copy(sourceId = songId)
    }

    private fun parseSongFromJson(source: MusicSourceEntity, obj: JsonObject, sourceName: String): Song {
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

    private fun buildClient(source: MusicSourceEntity): OkHttpClient {
        return okHttpClient.newBuilder()
            .connectTimeout(source.timeoutSeconds.toLong(), TimeUnit.SECONDS)
            .readTimeout(source.timeoutSeconds.toLong(), TimeUnit.SECONDS)
            .build()
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

    private fun JsonObject.getString(key: String): String? {
        return try { get(key)?.asString } catch (_: Exception) { null }
    }

    private fun JsonObject.getLong(key: String): Long? {
        return try { get(key)?.asLong } catch (_: Exception) { null }
    }

    private fun JsonObject?.getAsJsonObject(key: String): JsonObject? {
        return try { this?.get(key)?.asJsonObject } catch (_: Exception) { null }
    }

    private fun JsonObject?.getAsJsonArray(key: String): JsonArray? {
        return try { this?.get(key)?.asJsonArray } catch (_: Exception) { null }
    }

    // --- LX Music direct music service search ---

    private fun extractSourceId(source: MusicSourceEntity): String {
        return try {
            val config = source.configJson ?: return ""
            JsonParser.parseString(config).asJsonObject.getString("sourceId") ?: ""
        } catch (_: Exception) { "" }
    }

    private fun searchQQMusic(query: String, page: Int): String {
        val body = """{"req_1":{"method":"DoSearchForQQMusicDesktop","module":"music.search.SearchCgiService","param":{"num_per_page":20,"page_num":${page - 1},"query":"$query","search_type":0}}}"""
        val request = Request.Builder()
            .url("https://u.y.qq.com/cgi-bin/musicu.fcg")
            .post(body.toRequestBody("application/json".toMediaType()))
            .header("Referer", "https://y.qq.com")
            .build()
        val response = okHttpClient.newCall(request).execute()
        if (!response.isSuccessful) throw Exception("QQ音乐搜索失败: ${response.code}")
        return response.body?.string() ?: throw Exception("QQ音乐搜索响应为空")
    }

    private fun searchNetEaseMusic(query: String, page: Int): String {
        val body = """{"s":"$query","type":1,"limit":20,"offset":${(page - 1) * 20},"total":true}"""
        val request = Request.Builder()
            .url("https://music.163.com/api/search/get")
            .post(body.toRequestBody("application/x-www-form-urlencoded".toMediaType()))
            .header("Referer", "https://music.163.com")
            .build()
        val response = okHttpClient.newCall(request).execute()
        if (!response.isSuccessful) throw Exception("网易云搜索失败: ${response.code}")
        return response.body?.string() ?: throw Exception("网易云搜索响应为空")
    }

    private fun searchKuwoMusic(query: String, page: Int): String {
        val encodedQuery = java.net.URLEncoder.encode(query, "UTF-8")
        val request = Request.Builder()
            .url("https://search.kuwo.cn/r.s?all=$encodedQuery&pn=${page - 1}&rn=20&ft=music&rformat=json&encoding=utf8")
            .get()
            .header("Referer", "https://www.kuwo.cn")
            .build()
        val response = okHttpClient.newCall(request).execute()
        if (!response.isSuccessful) throw Exception("酷我搜索失败: ${response.code}")
        return response.body?.string() ?: throw Exception("酷我搜索响应为空")
    }

    private fun searchKugouMusic(query: String, page: Int): String {
        val encodedQuery = java.net.URLEncoder.encode(query, "UTF-8")
        val request = Request.Builder()
            .url("https://songsearch.kugou.com/song_search_v2?keyword=$encodedQuery&page=$page&pagesize=20")
            .get()
            .header("Referer", "https://www.kugou.com")
            .build()
        val response = okHttpClient.newCall(request).execute()
        if (!response.isSuccessful) throw Exception("酷狗搜索失败: ${response.code}")
        return response.body?.string() ?: throw Exception("酷狗搜索响应为空")
    }

    private fun parseLxMusicSearchResponse(source: MusicSourceEntity, json: String, sourceId: String): List<Song> {
        val root = JsonParser.parseString(json)
        val items: JsonArray? = when (sourceId) {
            "tx" -> {
                root.asJsonObject
                    ?.getAsJsonObject("req_1")
                    ?.getAsJsonObject("data")
                    ?.getAsJsonObject("body")
                    ?.getAsJsonObject("song")
                    ?.getAsJsonArray("list")
            }
            "wy" -> {
                root.asJsonObject
                    ?.getAsJsonObject("result")
                    ?.getAsJsonArray("songs")
            }
            "kw" -> {
                root.asJsonObject
                    ?.getAsJsonArray("abslist")
            }
            "kg" -> {
                root.asJsonObject
                    ?.getAsJsonObject("data")
                    ?.getAsJsonArray("lists")
            }
            else -> null
        }

        return items?.mapNotNull { element ->
            val obj = element.asJsonObject
            when (sourceId) {
                "tx" -> {
                    val id = obj.getString("id") ?: obj.getString("songmid") ?: ""
                    val title = obj.getString("title") ?: obj.getString("name") ?: "未知歌曲"
                    val artist = obj.getAsJsonArray("singer")?.firstOrNull()?.asJsonObject?.getString("name")
                        ?: "未知艺术家"
                    Song(
                        id = 0, title = title, artist = artist,
                        album = obj.getAsJsonObject("album")?.getString("title") ?: "",
                        duration = (obj.getLong("interval") ?: 0L) * 1000,
                        sourceType = "remote", sourceId = id,
                        coverUrl = obj.getAsJsonObject("album")?.getString("mid")?.let {
                            "https://y.gtimg.cn/music/photo_new/T002R800x800M000$it.jpg"
                        },
                        format = "MP3", bitrate = 0, fileSize = 0
                    )
                }
                "wy" -> {
                    Song(
                        id = 0, title = obj.getString("name") ?: "未知歌曲",
                        artist = obj.getAsJsonArray("artists")?.firstOrNull()?.asJsonObject?.getString("name") ?: "未知艺术家",
                        album = obj.getAsJsonObject("album")?.getString("name") ?: "",
                        duration = (obj.getLong("duration") ?: 0L),
                        sourceType = "remote", sourceId = obj.getString("id") ?: "",
                        coverUrl = obj.getAsJsonObject("album")?.getString("picUrl"),
                        format = "MP3", bitrate = 0, fileSize = 0
                    )
                }
                "kw" -> {
                    Song(
                        id = 0, title = obj.getString("SONGNAME") ?: obj.getString("NAME") ?: "未知歌曲",
                        artist = obj.getString("ARTIST") ?: obj.getString("SINGER") ?: "未知艺术家",
                        album = obj.getString("ALBUM") ?: "",
                        duration = (obj.getLong("DURATION") ?: 0L) * 1000,
                        sourceType = "remote", sourceId = obj.getString("MUSICRID")?.replace("MUSIC_", "") ?: "",
                        format = "MP3", bitrate = 0, fileSize = 0
                    )
                }
                "kg" -> {
                    Song(
                        id = 0, title = obj.getString("SongName") ?: obj.getString("songname") ?: "未知歌曲",
                        artist = obj.getString("SingerName") ?: obj.getString("singername") ?: "未知艺术家",
                        album = obj.getString("AlbumName") ?: "",
                        duration = (obj.getLong("Duration") ?: 0L) * 1000,
                        sourceType = "remote", sourceId = obj.getString("SQFileHash") ?: obj.getString("FileHash") ?: "",
                        coverUrl = obj.getString("AlbumID")?.let { "https://imge.kugou.com/stdmusic/$it.jpg" },
                        format = "MP3", bitrate = 0, fileSize = 0
                    )
                }
                else -> null
            }
        } ?: emptyList()
    }
}
