package com.xfyc.music.domain.model

data class SearchResult(
    val songs: List<Song>,
    val sourceName: String,
    val sourceType: String, // "local" or the music source type
    val totalCount: Int
)
