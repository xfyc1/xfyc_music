package com.xfyc.music.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "music_sources")
data class MusicSourceEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val type: String, // "url", "json_config", "custom_api", "subsonic", "navidrome"
    val baseUrl: String,
    val authType: String = "none", // "none", "token", "api_key", "basic_auth", "cookie", "custom_header"
    val authConfig: String? = null, // encrypted JSON
    val searchUrl: String? = null,
    val songDetailUrl: String? = null,
    val playUrl: String? = null,
    val coverUrl: String? = null,
    val lyricUrl: String? = null,
    val enabled: Boolean = true,
    val allowCache: Boolean = true,
    val lastSyncAt: Long? = null,
    val status: String = "idle", // "idle", "syncing", "error", "offline"
    val errorMessage: String? = null,
    val configJson: String? = null, // stored JSON config for json_config type
    val timeoutSeconds: Int = 30,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
