package com.xfyc.music.data.source

import com.xfyc.music.data.local.entity.MusicSourceEntity
import com.xfyc.music.domain.model.Song

interface MusicSourceAdapter {
    suspend fun testConnection(source: MusicSourceEntity): Result<Boolean>
    suspend fun search(source: MusicSourceEntity, query: String, page: Int = 1): Result<List<Song>>
    suspend fun getSongDetail(source: MusicSourceEntity, songId: String): Result<Song>
    suspend fun getPlayUrl(source: MusicSourceEntity, songId: String): Result<String>
    suspend fun getCoverUrl(source: MusicSourceEntity, songId: String): Result<String>
    suspend fun getLyricUrl(source: MusicSourceEntity, songId: String): Result<String?>
}
