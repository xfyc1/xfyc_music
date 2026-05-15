package com.xfyc.music.domain.model

data class Song(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val duration: Long,
    val sourceType: String,
    val sourceId: String? = null,
    val filePath: String? = null,
    val playUrl: String? = null,
    val coverUrl: String? = null,
    val lyricUrl: String? = null,
    val format: String,
    val bitrate: Int,
    val fileSize: Long,
    val isFavorite: Boolean = false,
    val lastPlayedAt: Long? = null
) {
    val durationFormatted: String
        get() {
            val totalSeconds = duration / 1000
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            return "%d:%02d".format(minutes, seconds)
        }

    val isLocal: Boolean get() = sourceType == "local"
}
