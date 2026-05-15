package com.xfyc.music.domain.usecase

import com.xfyc.music.data.local.entity.MusicSourceEntity
import com.xfyc.music.data.repository.MusicSourceRepository
import javax.inject.Inject

class MusicSourceUseCase @Inject constructor(
    private val musicSourceRepository: MusicSourceRepository
) {
    suspend fun addSource(source: MusicSourceEntity): Long {
        return musicSourceRepository.addSource(source)
    }

    suspend fun updateSource(source: MusicSourceEntity) {
        musicSourceRepository.updateSource(source)
    }

    suspend fun deleteSource(id: Long) {
        musicSourceRepository.deleteSource(id)
    }

    suspend fun setEnabled(id: Long, enabled: Boolean) {
        musicSourceRepository.setEnabled(id, enabled)
    }

    suspend fun testConnection(id: Long): Result<Boolean> {
        return musicSourceRepository.testConnection(id)
    }
}
