package com.xfyc.music.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "songs",
    indices = [
        Index(value = ["filePath"], unique = true),
        Index(value = ["title"]),
        Index(value = ["artist"]),
        Index(value = ["album"])
    ]
)
data class SongEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val artist: String,
    val album: String,
    val duration: Long,
    val sourceType: String, // "local" or "remote"
    val sourceId: String? = null,
    val filePath: String? = null,
    val playUrl: String? = null,
    val coverUrl: String? = null,
    val lyricUrl: String? = null,
    val format: String,
    val bitrate: Int,
    val fileSize: Long,
    val isFavorite: Boolean = false,
    val lastPlayedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
