package com.xfyc.music.data.source

import com.google.gson.JsonArray
import com.google.gson.JsonParser
import com.xfyc.music.data.local.entity.MusicSourceEntity
import com.xfyc.music.domain.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LxMusicAdapter @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val sourceHttpClient: SourceHttpClient
) : MusicSourceAdapter {

    override val supportedTypes: Set<String> = setOf("lx_music")

    override suspend fun testConnection(source: MusicSourceEntity): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                val client = okHttpClient.newBuilder()
                    .apply { connectTimeout(source.timeoutSeconds.toLong(), java.util.concurrent.TimeUnit.SECONDS)
                        readTimeout(source.timeoutSeconds.toLong(), java.util.concurrent.TimeUnit.SECONDS) }
                    .build()
                val request = Request.Builder()
                    .url(source.baseUrl)
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
                val sourceId = extractSourceId(source)
                val response = when (sourceId) {
                    "tx" -> searchQQMusic(query, page)
                    "wy" -> searchNetEaseMusic(query, page)
                    "kw" -> searchKuwoMusic(query, page)
                    "kg" -> searchKugouMusic(query, page)
                    else -> return@withContext Result.failure(Exception("暂不支持该音源搜索: $sourceId"))
                }
                val songs = parseLxMusicSearchResponse(response, sourceId)
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
                val response = sourceHttpClient.executeGetRequest(source, url)
                val root = JsonParser.parseString(response)
                val obj = if (root.isJsonObject) root.asJsonObject else throw Exception("无效的响应格式")
                val data = obj.getAsJsonObject("data") ?: obj
                Result.success(sourceHttpClient.parseGenericSongFromJson(data, source.name).copy(sourceId = songId))
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
                val response = sourceHttpClient.executeGetRequest(source, url)
                val json = JsonParser.parseString(response)
                if (json.isJsonObject) {
                    val obj = json.asJsonObject
                    val urlField = obj.get("url") ?: obj.get("playUrl") ?: obj.get("src")
                    if (urlField != null) return@withContext Result.success(urlField.asString)
                    val dataField = obj.get("data")
                    if (dataField != null) {
                        if (dataField.isJsonPrimitive) return@withContext Result.success(dataField.asString)
                        if (dataField.isJsonObject) {
                            val dataObj = dataField.asJsonObject
                            val nestedUrl = dataObj.get("url") ?: dataObj.get("playUrl") ?: dataObj.get("src")
                            if (nestedUrl != null) return@withContext Result.success(nestedUrl.asString)
                        }
                    }
                }
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
                val response = sourceHttpClient.executeGetRequest(source, url)
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
                val response = sourceHttpClient.executeGetRequest(source, url)
                val json = JsonParser.parseString(response)
                if (json.isJsonObject) {
                    val obj = json.asJsonObject
                    val urlField = obj.get("url") ?: obj.get("lyricUrl") ?: obj.get("lyric")
                    if (urlField != null) return@withContext Result.success(urlField.asString)
                }
                if (response.trimStart().startsWith("[") || response.trimStart().startsWith("{")) {
                    return@withContext Result.success(null)
                }
                Result.success(url)
            } catch (e: Exception) {
                Result.success(null)
            }
        }
    }

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

    private fun parseLxMusicSearchResponse(json: String, sourceId: String): List<Song> {
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
