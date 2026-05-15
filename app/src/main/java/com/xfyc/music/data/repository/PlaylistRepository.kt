package com.xfyc.music.data.repository

import com.xfyc.music.data.local.dao.PlaylistDao
import com.xfyc.music.data.local.dao.PlaylistItemDao
import com.xfyc.music.data.local.entity.PlaylistEntity
import com.xfyc.music.data.local.entity.PlaylistItemEntity
import com.xfyc.music.data.local.entity.SongEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlaylistRepository @Inject constructor(
    private val playlistDao: PlaylistDao,
    private val playlistItemDao: PlaylistItemDao
) {

    fun getAllPlaylists(): Flow<List<PlaylistEntity>> = playlistDao.getAllPlaylists()

    suspend fun getPlaylistById(id: Long): PlaylistEntity? = playlistDao.getPlaylistById(id)

    fun getSongsInPlaylist(playlistId: Long): Flow<List<SongEntity>> =
        playlistItemDao.getSongsInPlaylist(playlistId)

    suspend fun createPlaylist(name: String, description: String = ""): Long {
        val playlist = PlaylistEntity(name = name, description = description)
        return playlistDao.insertPlaylist(playlist)
    }

    suspend fun updatePlaylist(playlist: PlaylistEntity) = playlistDao.updatePlaylist(playlist)

    suspend fun deletePlaylist(id: Long) {
        playlistItemDao.deleteAllInPlaylist(id)
        playlistDao.deletePlaylist(id)
    }

    suspend fun addSongToPlaylist(playlistId: Long, songId: Long) {
        if (playlistItemDao.existsInPlaylist(playlistId, songId) > 0) return
        val maxOrder = playlistItemDao.getMaxSortOrder(playlistId) ?: -1
        val item = PlaylistItemEntity(
            playlistId = playlistId,
            songId = songId,
            sortOrder = maxOrder + 1
        )
        playlistItemDao.insertItem(item)
        playlistDao.updateSongCount(playlistId)
    }

    suspend fun removeSongFromPlaylist(playlistId: Long, songId: Long) {
        playlistItemDao.deleteByPlaylistAndSong(playlistId, songId)
        playlistDao.updateSongCount(playlistId)
    }

    suspend fun reorderItem(itemId: Long, newSortOrder: Int) {
        val item = playlistItemDao.getItemsByPlaylist(0).find { it.id == itemId }
            ?: return
        playlistItemDao.updateItem(item.copy(sortOrder = newSortOrder))
    }

    suspend fun addSongsToPlaylist(playlistId: Long, songIds: List<Long>) {
        val maxOrder = playlistItemDao.getMaxSortOrder(playlistId) ?: -1
        val items = songIds.mapIndexed { index, songId ->
            PlaylistItemEntity(
                playlistId = playlistId,
                songId = songId,
                sortOrder = maxOrder + 1 + index
            )
        }
        playlistItemDao.insertItems(items)
        playlistDao.updateSongCount(playlistId)
    }
}
