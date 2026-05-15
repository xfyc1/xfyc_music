package com.xfyc.music.data.repository

import com.xfyc.music.data.local.dao.MusicSourceDao
import com.xfyc.music.data.local.entity.MusicSourceEntity
import com.xfyc.music.data.source.MusicSourceAdapter
import com.xfyc.music.domain.model.Song
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MusicSourceRepository @Inject constructor(
    private val musicSourceDao: MusicSourceDao,
    private val sourceAdapter: MusicSourceAdapter
) {

    fun getAllSources(): Flow<List<MusicSourceEntity>> = musicSourceDao.getAllSources()

    fun getEnabledSources(): Flow<List<MusicSourceEntity>> = musicSourceDao.getEnabledSources()

    suspend fun getSourceById(id: Long): MusicSourceEntity? = musicSourceDao.getSourceById(id)

    suspend fun addSource(source: MusicSourceEntity): Long = musicSourceDao.insertSource(source)

    suspend fun updateSource(source: MusicSourceEntity) = musicSourceDao.updateSource(source)

    suspend fun deleteSource(id: Long) = musicSourceDao.deleteSource(id)

    suspend fun setEnabled(id: Long, enabled: Boolean) = musicSourceDao.setEnabled(id, enabled)

    suspend fun testConnection(sourceId: Long): Result<Boolean> {
        val source = musicSourceDao.getSourceById(sourceId) ?: return Result.failure(Exception("Source not found"))
        return try {
            val result = sourceAdapter.testConnection(source)
            if (result.isSuccess && result.getOrNull() == true) {
                musicSourceDao.setStatus(sourceId, "idle")
                musicSourceDao.setLastSync(sourceId)
            } else {
                val error = result.exceptionOrNull()?.message ?: "Connection failed"
                musicSourceDao.setStatus(sourceId, "error", error)
            }
            result
        } catch (e: Exception) {
            musicSourceDao.setStatus(sourceId, "error", e.message)
            Result.failure(e)
        }
    }

    suspend fun searchSource(sourceId: Long, query: String, page: Int = 1): Result<List<Song>> {
        val source = musicSourceDao.getSourceById(sourceId)
            ?: return Result.failure(Exception("Source not found"))
        if (!source.enabled) return Result.failure(Exception("Source is disabled"))

        musicSourceDao.setStatus(sourceId, "syncing")
        val result = sourceAdapter.search(source, query, page)
        result.onFailure {
            musicSourceDao.setStatus(sourceId, "error", it.message)
        }.onSuccess {
            musicSourceDao.setStatus(sourceId, "idle")
            musicSourceDao.setLastSync(sourceId)
        }
        return result
    }

    suspend fun getPlayUrl(sourceId: Long, songRemoteId: String): Result<String> {
        val source = musicSourceDao.getSourceById(sourceId)
            ?: return Result.failure(Exception("Source not found"))
        return sourceAdapter.getPlayUrl(source, songRemoteId)
    }

    suspend fun getCoverUrl(sourceId: Long, songRemoteId: String): Result<String> {
        val source = musicSourceDao.getSourceById(sourceId)
            ?: return Result.failure(Exception("Source not found"))
        return sourceAdapter.getCoverUrl(source, songRemoteId)
    }

    suspend fun getLyricUrl(sourceId: Long, songRemoteId: String): Result<String?> {
        val source = musicSourceDao.getSourceById(sourceId)
            ?: return Result.failure(Exception("Source not found"))
        return sourceAdapter.getLyricUrl(source, songRemoteId)
    }

    suspend fun searchAllSources(query: String): List<Result<List<Song>>> {
        val sources = musicSourceDao.getEnabledSources().let { flow ->
            var list = emptyList<MusicSourceEntity>()
            flow.collect { list = it; return@collect } // trick to get first value
            list
        }
        // Actually use a proper approach:
        return emptyList()
    }
}
