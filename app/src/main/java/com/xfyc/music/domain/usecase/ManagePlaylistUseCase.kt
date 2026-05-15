package com.xfyc.music.domain.usecase

import com.xfyc.music.data.repository.PlaylistRepository
import javax.inject.Inject

class ManagePlaylistUseCase @Inject constructor(
    private val playlistRepository: PlaylistRepository
) {
    suspend fun createPlaylist(name: String, description: String = ""): Long {
        return playlistRepository.createPlaylist(name, description)
    }

    suspend fun deletePlaylist(id: Long) {
        playlistRepository.deletePlaylist(id)
    }

    suspend fun addSong(playlistId: Long, songId: Long) {
        playlistRepository.addSongToPlaylist(playlistId, songId)
    }

    suspend fun removeSong(playlistId: Long, songId: Long) {
        playlistRepository.removeSongFromPlaylist(playlistId, songId)
    }

    suspend fun addSongs(playlistId: Long, songIds: List<Long>) {
        playlistRepository.addSongsToPlaylist(playlistId, songIds)
    }
}
