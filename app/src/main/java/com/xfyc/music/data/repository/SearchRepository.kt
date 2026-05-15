package com.xfyc.music.data.repository

import com.xfyc.music.data.local.dao.SongDao
import com.xfyc.music.domain.model.SearchResult
import com.xfyc.music.domain.model.Song
import com.xfyc.music.domain.model.toDomainModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SearchRepository @Inject constructor(
    private val songDao: SongDao,
    private val musicSourceRepository: MusicSourceRepository
) {

    fun searchLocal(query: String): Flow<List<Song>> {
        return songDao.searchSongs(query).map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    suspend fun searchAll(query: String): List<SearchResult> {
        val results = mutableListOf<SearchResult>()

        // Local search
        val localSongs = searchLocal(query).first()
        if (localSongs.isNotEmpty()) {
            results.add(SearchResult(
                songs = localSongs,
                sourceName = "本地",
                sourceType = "local",
                totalCount = localSongs.size
            ))
        }

        // Remote sources search
        val enabledSources = musicSourceRepository.getEnabledSources().let { flow ->
            var list = emptyList<com.xfyc.music.data.local.entity.MusicSourceEntity>()
            kotlinx.coroutines.runBlocking { flow.collect { list = it; return@collect } }
            list
        }

        for (source in enabledSources) {
            try {
                val result = musicSourceRepository.searchSource(source.id, query)
                result.onSuccess { songs ->
                    if (songs.isNotEmpty()) {
                        results.add(SearchResult(
                            songs = songs,
                            sourceName = source.name,
                            sourceType = source.type,
                            totalCount = songs.size
                        ))
                    }
                }
            } catch (_: Exception) { }
        }

        return results
    }
}
