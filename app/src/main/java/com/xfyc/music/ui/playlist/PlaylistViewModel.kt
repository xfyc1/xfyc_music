package com.xfyc.music.ui.playlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xfyc.music.data.repository.PlaylistRepository
import com.xfyc.music.data.repository.SongRepository
import com.xfyc.music.domain.model.Playlist
import com.xfyc.music.domain.model.Song
import com.xfyc.music.domain.model.toDomainModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlaylistViewModel @Inject constructor(
    private val playlistRepository: PlaylistRepository,
    private val songRepository: SongRepository
) : ViewModel() {

    private val _playlists = MutableStateFlow<List<Playlist>>(emptyList())
    val playlists: StateFlow<List<Playlist>> = _playlists.asStateFlow()

    private val _playlistSongs = MutableStateFlow<List<Song>>(emptyList())
    val playlistSongs: StateFlow<List<Song>> = _playlistSongs.asStateFlow()

    private val _currentPlaylist = MutableStateFlow<Playlist?>(null)
    val currentPlaylist: StateFlow<Playlist?> = _currentPlaylist.asStateFlow()

    private val _allSongs = MutableStateFlow<List<Song>>(emptyList())
    val allSongs: StateFlow<List<Song>> = _allSongs.asStateFlow()

    init {
        viewModelScope.launch {
            playlistRepository.getAllPlaylists().collect { entities ->
                _playlists.value = entities.map { it.toDomainModel() }
            }
        }
        viewModelScope.launch {
            songRepository.getAllSongs().collect { entities ->
                _allSongs.value = entities.map { it.toDomainModel() }
            }
        }
    }

    fun createPlaylist(name: String, description: String = "") {
        viewModelScope.launch {
            playlistRepository.createPlaylist(name, description)
        }
    }

    fun deletePlaylist(id: Long) {
        viewModelScope.launch {
            playlistRepository.deletePlaylist(id)
        }
    }

    fun loadPlaylistSongs(playlistId: Long) {
        viewModelScope.launch {
            playlistRepository.getPlaylistById(playlistId)?.let {
                _currentPlaylist.value = it.toDomainModel()
            }
        }
        viewModelScope.launch {
            playlistRepository.getSongsInPlaylist(playlistId).collect { entities ->
                _playlistSongs.value = entities.map { it.toDomainModel() }
            }
        }
    }

    fun addSongToPlaylist(playlistId: Long, songId: Long) {
        viewModelScope.launch {
            playlistRepository.addSongToPlaylist(playlistId, songId)
        }
    }

    fun removeSongFromPlaylist(playlistId: Long, songId: Long) {
        viewModelScope.launch {
            playlistRepository.removeSongFromPlaylist(playlistId, songId)
        }
    }

    fun addSongsToPlaylist(playlistId: Long, songIds: List<Long>) {
        viewModelScope.launch {
            playlistRepository.addSongsToPlaylist(playlistId, songIds)
        }
    }
}
