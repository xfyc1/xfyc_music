package com.xfyc.music.domain.model

data class MusicSource(
    val id: Long,
    val name: String,
    val type: String,
    val baseUrl: String,
    val authType: String = "none",
    val authConfig: String? = null,
    val searchUrl: String? = null,
    val songDetailUrl: String? = null,
    val playUrl: String? = null,
    val coverUrl: String? = null,
    val lyricUrl: String? = null,
    val enabled: Boolean = true,
    val allowCache: Boolean = true,
    val lastSyncAt: Long? = null,
    val status: String = "idle",
    val errorMessage: String? = null,
    val configJson: String? = null,
    val timeoutSeconds: Int = 30
)
