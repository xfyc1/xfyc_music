package com.xfyc.music.data.local.dao

import androidx.room.*
import com.xfyc.music.data.local.entity.PlaylistEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaylistDao {

    @Query("SELECT * FROM playlists ORDER BY updatedAt DESC")
    fun getAllPlaylists(): Flow<List<PlaylistEntity>>

    @Query("SELECT * FROM playlists WHERE id = :id")
    suspend fun getPlaylistById(id: Long): PlaylistEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: PlaylistEntity): Long

    @Update
    suspend fun updatePlaylist(playlist: PlaylistEntity)

    @Query("DELETE FROM playlists WHERE id = :id")
    suspend fun deletePlaylist(id: Long)

    @Query("UPDATE playlists SET songCount = (SELECT COUNT(*) FROM playlist_items WHERE playlistId = :playlistId), updatedAt = :updatedAt WHERE id = :playlistId")
    suspend fun updateSongCount(playlistId: Long, updatedAt: Long = System.currentTimeMillis())
}
