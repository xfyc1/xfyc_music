package com.xfyc.music.data.scanner

import android.media.MediaMetadataRetriever
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MetadataExtractor @Inject constructor() {

    fun extractBitrate(filePath: String): Int {
        return try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(filePath)
            val bitrate = retriever.extractMetadata(
                MediaMetadataRetriever.METADATA_KEY_BITRATE
            )?.toIntOrNull() ?: 0
            retriever.release()
            bitrate / 1000 // Convert to kbps
        } catch (e: Exception) {
            0
        }
    }

    fun extractEmbeddedCover(filePath: String): ByteArray? {
        return try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(filePath)
            val picture = retriever.embeddedPicture
            retriever.release()
            picture
        } catch (e: Exception) {
            null
        }
    }

    fun extractMetadata(filePath: String): Map<String, String> {
        val result = mutableMapOf<String, String>()
        try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(filePath)
            val keys = listOf(
                MediaMetadataRetriever.METADATA_KEY_TITLE to "title",
                MediaMetadataRetriever.METADATA_KEY_ARTIST to "artist",
                MediaMetadataRetriever.METADATA_KEY_ALBUM to "album",
                MediaMetadataRetriever.METADATA_KEY_DURATION to "duration",
                MediaMetadataRetriever.METADATA_KEY_BITRATE to "bitrate",
                MediaMetadataRetriever.METADATA_KEY_GENRE to "genre",
                MediaMetadataRetriever.METADATA_KEY_YEAR to "year",
                MediaMetadataRetriever.METADATA_KEY_NUM_TRACKS to "trackNumber"
            )
            keys.forEach { (key, label) ->
                retriever.extractMetadata(key)?.let { value ->
                    result[label] = value
                }
            }
            retriever.release()
        } catch (_: Exception) { }
        return result
    }
}
