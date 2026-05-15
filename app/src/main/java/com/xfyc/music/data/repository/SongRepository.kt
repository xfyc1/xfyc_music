package com.xfyc.music.data.repository

import com.xfyc.music.data.local.dao.SongDao
import com.xfyc.music.data.local.entity.SongEntity
import com.xfyc.music.data.scanner.MediaScanner
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SongRepository @Inject constructor(
    private val songDao: SongDao,
    private val mediaScanner: MediaScanner
) {

    fun getAllSongs(): Flow<List<SongEntity>> = songDao.getAllSongs()

    fun getFavoriteSongs(): Flow<List<SongEntity>> = songDao.getFavoriteSongs()

    fun getRecentSongs(limit: Int = 20): Flow<List<SongEntity>> = songDao.getRecentSongs(limit)

    fun getArtists(): Flow<List<String>> = songDao.getArtists()

    fun getAlbums(): Flow<List<String>> = songDao.getAlbums()

    fun getSongsByArtist(artist: String): Flow<List<SongEntity>> = songDao.getSongsByArtist(artist)

    fun getSongsByAlbum(album: String): Flow<List<SongEntity>> = songDao.getSongsByAlbum(album)

    fun searchSongs(query: String): Flow<List<SongEntity>> = songDao.searchSongs(query)

    suspend fun getSongById(id: Long): SongEntity? = songDao.getSongById(id)

    suspend fun getSongCount(): Int = songDao.getSongCount()

    suspend fun setFavorite(id: Long, favorite: Boolean) = songDao.setFavorite(id, favorite)

    suspend fun setLastPlayed(id: Long) = songDao.setLastPlayed(id)

    suspend fun deleteSong(id: Long) = songDao.deleteSong(id)

    suspend fun scanAndImport(): MediaScanner.ScanResult {
        val result = mediaScanner.scanMediaStore()
        if (result.songs.isNotEmpty()) {
            songDao.insertSongs(result.songs)
        }
        return result
    }

    suspend fun isDuplicate(filePath: String): Boolean {
        return songDao.countByPath(filePath) > 0
    }
}
