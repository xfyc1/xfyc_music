package com.xfyc.music.data.cover

import android.graphics.BitmapFactory
import com.xfyc.music.data.cache.CacheManager
import com.xfyc.music.data.scanner.MetadataExtractor
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CoverLoader @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val cacheManager: CacheManager,
    private val metadataExtractor: MetadataExtractor
) {

    suspend fun loadCover(
        coverUrl: String?,
        localFilePath: String?
    ): ByteArray? {
        // Try embedded cover first (local files)
        localFilePath?.let { path ->
            metadataExtractor.extractEmbeddedCover(path)?.let { return it }
        }

        // Try same-directory cover images
        localFilePath?.let { path ->
            val dir = File(path).parentFile ?: return@let
            val coverNames = listOf("cover.jpg", "cover.png", "folder.jpg", "album.jpg", "albumart.jpg")
            for (name in coverNames) {
                val coverFile = File(dir, name)
                if (coverFile.exists()) {
                    return coverFile.readBytes()
                }
            }
        }

        // Try online cover URL
        coverUrl?.let { url ->
            try {
                val cached = cacheManager.getCachedCover(url)
                if (cached != null) return cached.readBytes()

                val response = okHttpClient.newCall(Request.Builder().url(url).build()).execute()
                if (response.isSuccessful) {
                    val data = response.body?.bytes() ?: return@let null
                    cacheManager.saveCover(url, data)
                    return data
                }
            } catch (_: Exception) { }
        }

        return null
    }
}
