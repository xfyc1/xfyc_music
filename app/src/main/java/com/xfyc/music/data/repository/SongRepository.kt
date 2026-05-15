package com.xfyc.music.data.repository

import com.xfyc.music.data.local.dao.SongDao
import com.xfyc.music.data.local.entity.SongEntity
import com.xfyc.music.data.scanner.MediaScanner
import com.xfyc.music.domain.model.Song
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

    suspend fun getRemoteSongBySourceId(sourceId: String): SongEntity? =
        songDao.getRemoteSongBySourceId(sourceId)

    suspend fun getSongCount(): Int = songDao.getSongCount()

    suspend fun setFavorite(id: Long, favorite: Boolean) = songDao.setFavorite(id, favorite)

    suspend fun setLastPlayed(id: Long) = songDao.setLastPlayed(id)

    suspend fun deleteSong(id: Long) = songDao.deleteSong(id)

    suspend fun updateSong(song: SongEntity) = songDao.updateSong(song)

    suspend fun upsertRemoteSong(song: Song, inLibrary: Boolean = false): Long {
        val sourceId = song.sourceId ?: return songDao.insertSong(song.toRemoteEntity(inLibrary))
        val existing = songDao.getRemoteSongBySourceId(sourceId)
        if (existing != null) {
            songDao.updateSong(
                song.toRemoteEntity(inLibrary).copy(
                    id = existing.id,
                    isFavorite = existing.isFavorite,
                    lastPlayedAt = existing.lastPlayedAt,
                    inLibrary = existing.inLibrary || inLibrary,
                    createdAt = existing.createdAt
                )
            )
            return existing.id
        }
        return songDao.insertSong(song.toRemoteEntity(inLibrary))
    }

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

    private fun Song.toRemoteEntity(inLibrary: Boolean = this.inLibrary): SongEntity = SongEntity(
        title = title,
        artist = artist,
        album = album,
        duration = duration,
        sourceType = "remote",
        sourceId = sourceId,
        playUrl = playUrl,
        coverUrl = coverUrl,
        lyricUrl = lyricUrl,
        format = format,
        bitrate = bitrate,
        fileSize = fileSize,
        isFavorite = isFavorite,
        inLibrary = inLibrary,
        lastPlayedAt = lastPlayedAt
    )
}
