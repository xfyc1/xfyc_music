package com.xfyc.music.domain.model

data class Playlist(
    val id: Long,
    val name: String,
    val description: String = "",
    val coverUrl: String? = null,
    val songCount: Int = 0
)
