package com.xfyc.music.data.local.dao

import androidx.room.*
import com.xfyc.music.data.local.entity.SongEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SongDao {

    @Query("SELECT * FROM songs WHERE inLibrary = 1 ORDER BY title ASC")
    fun getAllSongs(): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs WHERE id = :id")
    suspend fun getSongById(id: Long): SongEntity?

    @Query("SELECT * FROM songs WHERE filePath = :path LIMIT 1")
    suspend fun getSongByPath(path: String): SongEntity?

    @Query("SELECT * FROM songs WHERE sourceType = 'remote' AND sourceId = :sourceId LIMIT 1")
    suspend fun getRemoteSongBySourceId(sourceId: String): SongEntity?

    @Query("SELECT * FROM songs WHERE isFavorite = 1 ORDER BY title ASC")
    fun getFavoriteSongs(): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs WHERE lastPlayedAt IS NOT NULL ORDER BY lastPlayedAt DESC LIMIT :limit")
    fun getRecentSongs(limit: Int = 20): Flow<List<SongEntity>>

    @Query("SELECT DISTINCT artist FROM songs WHERE inLibrary = 1 AND artist != '' ORDER BY artist ASC")
    fun getArtists(): Flow<List<String>>

    @Query("SELECT DISTINCT album FROM songs WHERE inLibrary = 1 AND album != '' ORDER BY album ASC")
    fun getAlbums(): Flow<List<String>>

    @Query("SELECT * FROM songs WHERE inLibrary = 1 AND artist = :artist ORDER BY album, title ASC")
    fun getSongsByArtist(artist: String): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs WHERE inLibrary = 1 AND album = :album ORDER BY title ASC")
    fun getSongsByAlbum(album: String): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs WHERE inLibrary = 1 AND (title LIKE '%' || :query || '%' OR artist LIKE '%' || :query || '%' OR album LIKE '%' || :query || '%')")
    fun searchSongs(query: String): Flow<List<SongEntity>>

    @Query("SELECT COUNT(*) FROM songs WHERE inLibrary = 1")
    suspend fun getSongCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSong(song: SongEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSongs(songs: List<SongEntity>): List<Long>

    @Update
    suspend fun updateSong(song: SongEntity)

    @Query("UPDATE songs SET isFavorite = :favorite, inLibrary = 1, updatedAt = :updatedAt WHERE id = :id")
    suspend fun setFavorite(id: Long, favorite: Boolean, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE songs SET lastPlayedAt = :timestamp WHERE id = :id")
    suspend fun setLastPlayed(id: Long, timestamp: Long = System.currentTimeMillis())

    @Query("DELETE FROM songs WHERE id = :id")
    suspend fun deleteSong(id: Long)

    @Query("SELECT COUNT(*) FROM songs WHERE filePath = :path")
    suspend fun countByPath(path: String): Int
}
