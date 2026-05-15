package com.xfyc.music.data.scanner

import android.content.Context
import android.media.MediaMetadataRetriever
import android.provider.MediaStore
import com.xfyc.music.data.local.entity.SongEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaScanner @Inject constructor(
    @ApplicationContext private val context: Context,
    private val metadataExtractor: MetadataExtractor
) {

    data class ScanResult(
        val songs: List<SongEntity>,
        val totalFound: Int,
        val durationMs: Long
    )

    suspend fun scanMediaStore(): ScanResult {
        val startTime = System.currentTimeMillis()
        val songs = mutableListOf<SongEntity>()

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.MIME_TYPE
        )

        val selection = "${MediaStore.Audio.Media.IS_MUSIC} = 1"
        val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"

        context.contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            null,
            sortOrder
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val dataCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
            val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
            val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE)

            while (cursor.moveToNext()) {
                val filePath = cursor.getString(dataCol) ?: continue
                val mimeType = cursor.getString(mimeCol) ?: ""
                val format = getFormatFromMime(mimeType)

                if (format == null) continue

                val title = cursor.getString(titleCol)
                    ?.takeIf { it.isNotBlank() }
                    ?: filePath.substringAfterLast("/").substringBeforeLast(".")

                val artist = cursor.getString(artistCol)
                    ?.takeIf { it.isNotBlank() && it != "<unknown>" }
                    ?: "未知艺术家"

                val album = cursor.getString(albumCol)
                    ?.takeIf { it.isNotBlank() && it != "<unknown>" }
                    ?: "未知专辑"

                val bitrate = metadataExtractor.extractBitrate(filePath)

                songs.add(
                    SongEntity(
                        title = title,
                        artist = artist,
                        album = album,
                        duration = cursor.getLong(durationCol),
                        sourceType = "local",
                        filePath = filePath,
                        format = format,
                        bitrate = bitrate,
                        fileSize = cursor.getLong(sizeCol)
                    )
                )
            }
        }

        return ScanResult(
            songs = songs,
            totalFound = songs.size,
            durationMs = System.currentTimeMillis() - startTime
        )
    }

    private fun getFormatFromMime(mimeType: String): String? = when {
        mimeType.contains("mp3") || mimeType.contains("mpeg") -> "MP3"
        mimeType.contains("flac") -> "FLAC"
        mimeType.contains("wav") -> "WAV"
        mimeType.contains("aac") || mimeType.contains("mp4") -> "AAC"
        mimeType.contains("ogg") || mimeType.contains("vorbis") -> "OGG"
        else -> null
    }
}
