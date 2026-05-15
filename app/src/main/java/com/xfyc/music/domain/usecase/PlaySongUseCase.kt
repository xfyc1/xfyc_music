package com.xfyc.music.domain.usecase

import com.xfyc.music.data.repository.SongRepository
import javax.inject.Inject

class PlaySongUseCase @Inject constructor(
    private val songRepository: SongRepository
) {
    suspend operator fun invoke(songId: Long) {
        songRepository.setLastPlayed(songId)
    }
}
