package com.xfyc.music.domain.usecase

import com.xfyc.music.data.repository.SongRepository
import javax.inject.Inject

class ScanMusicUseCase @Inject constructor(
    private val songRepository: SongRepository
) {
    suspend operator fun invoke(): Result<Int> {
        return try {
            val result = songRepository.scanAndImport()
            Result.success(result.totalFound)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
