package com.xfyc.music.data.source

import com.google.gson.JsonArray
import com.google.gson.JsonParser
import com.xfyc.music.data.local.entity.MusicSourceEntity
import com.xfyc.music.domain.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GenericApiAdapter @Inject constructor(
    private val sourceHttpClient: SourceHttpClient
) : MusicSourceAdapter {

    override val supportedTypes: Set<String> = setOf("url", "custom_api", "json_config")

    override suspend fun testConnection(source: MusicSourceEntity): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                val response = sourceHttpClient.executeGetRequest(source, source.baseUrl)
                Result.success(response.isNotEmpty())
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
                val searchUrl = source.searchUrl
                    ?: return@withContext Result.failure(Exception("搜索 URL 未配置"))

                val url = searchUrl
                    .replace("{query}", java.net.URLEncoder.encode(query, "UTF-8"))
                    .replace("{page}", page.toString())
                    .replace("{baseUrl}", source.baseUrl.trimEnd('/'))

                val response = sourceHttpClient.executeGetRequest(source, url)
                val songs = parseSearchResponse(source, response)
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
                val song = sourceHttpClient.parseGenericSongFromJson(data, source.name)
                    .copy(sourceId = songId)
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

    private fun parseSearchResponse(source: MusicSourceEntity, json: String): List<Song> {
        val root = JsonParser.parseString(json)
        val items: JsonArray? = when {
            root.isJsonObject -> {
                val obj = root.asJsonObject
                obj.getAsJsonArray("data") ?: obj.getAsJsonArray("result")
                ?: obj.getAsJsonArray("results") ?: obj.getAsJsonArray("songs")
                ?: obj.getAsJsonArray("items") ?: obj.getAsJsonArray("list")
                ?: obj.getAsJsonObject("data")?.let { dataObj ->
                    dataObj.getAsJsonArray("list") ?: dataObj.getAsJsonArray("songs")
                    ?: dataObj.getAsJsonArray("items") ?: dataObj.getAsJsonArray("results")
                }
            }
            root.isJsonArray -> root.asJsonArray
            else -> return emptyList()
        }

        return items?.mapNotNull { element ->
            sourceHttpClient.parseGenericSongFromJson(element.asJsonObject, source.name)
        } ?: emptyList()
    }
}
