package com.xfyc.music.data.local.dao

import androidx.room.*
import com.xfyc.music.data.local.entity.PlaylistItemEntity
import com.xfyc.music.data.local.entity.SongEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaylistItemDao {

    @Query("""
        SELECT s.* FROM songs s
        INNER JOIN playlist_items pi ON s.id = pi.songId
        WHERE pi.playlistId = :playlistId
        ORDER BY pi.sortOrder ASC
    """)
    fun getSongsInPlaylist(playlistId: Long): Flow<List<SongEntity>>

    @Query("SELECT * FROM playlist_items WHERE playlistId = :playlistId ORDER BY sortOrder ASC")
    suspend fun getItemsByPlaylist(playlistId: Long): List<PlaylistItemEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertItem(item: PlaylistItemEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertItems(items: List<PlaylistItemEntity>)

    @Update
    suspend fun updateItem(item: PlaylistItemEntity)

    @Query("DELETE FROM playlist_items WHERE id = :id")
    suspend fun deleteItem(id: Long)

    @Query("DELETE FROM playlist_items WHERE playlistId = :playlistId AND songId = :songId")
    suspend fun deleteByPlaylistAndSong(playlistId: Long, songId: Long)

    @Query("DELETE FROM playlist_items WHERE playlistId = :playlistId")
    suspend fun deleteAllInPlaylist(playlistId: Long)

    @Query("SELECT MAX(sortOrder) FROM playlist_items WHERE playlistId = :playlistId")
    suspend fun getMaxSortOrder(playlistId: Long): Int?

    @Query("SELECT COUNT(*) FROM playlist_items WHERE playlistId = :playlistId AND songId = :songId")
    suspend fun existsInPlaylist(playlistId: Long, songId: Long): Int
}
