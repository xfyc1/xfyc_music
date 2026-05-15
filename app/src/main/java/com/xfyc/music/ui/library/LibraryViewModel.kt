package com.xfyc.music.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xfyc.music.data.repository.PlaylistRepository
import com.xfyc.music.data.repository.SongRepository
import com.xfyc.music.domain.model.Playlist
import com.xfyc.music.domain.model.Song
import com.xfyc.music.domain.model.toDomainModel
import com.xfyc.music.domain.usecase.ScanMusicUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val songRepository: SongRepository,
    private val playlistRepository: PlaylistRepository,
    private val scanMusicUseCase: ScanMusicUseCase
) : ViewModel() {

    private val _allSongs = MutableStateFlow<List<Song>>(emptyList())
    val allSongs: StateFlow<List<Song>> = _allSongs.asStateFlow()

    private val _artists = MutableStateFlow<List<String>>(emptyList())
    val artists: StateFlow<List<String>> = _artists.asStateFlow()

    private val _albums = MutableStateFlow<List<String>>(emptyList())
    val albums: StateFlow<List<String>> = _albums.asStateFlow()

    private val _playlists = MutableStateFlow<List<Playlist>>(emptyList())
    val playlists: StateFlow<List<Playlist>> = _playlists.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _scanResult = MutableStateFlow<String?>(null)
    val scanResult: StateFlow<String?> = _scanResult.asStateFlow()

    private val _selectedTab = MutableStateFlow(0)
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    init {
        loadData()
    }

    fun setTab(tab: Int) {
        _selectedTab.value = tab
    }

    fun scanMusic() {
        viewModelScope.launch {
            _isScanning.value = true
            val result = scanMusicUseCase()
            result.onSuccess { count ->
                _scanResult.value = "Found $count songs"
                loadData()
            }.onFailure { e ->
                _scanResult.value = "Scan failed: ${e.message}"
            }
            _isScanning.value = false
        }
    }

    fun clearScanResult() {
        _scanResult.value = null
    }

    fun toggleFavorite(songId: Long, currentFavorite: Boolean) {
        viewModelScope.launch {
            songRepository.setFavorite(songId, !currentFavorite)
        }
    }

    private fun loadData() {
        viewModelScope.launch {
            songRepository.getAllSongs().collect { entities ->
                _allSongs.value = entities.map { it.toDomainModel() }
            }
        }
        viewModelScope.launch {
            songRepository.getArtists().collect { _artists.value = it }
        }
        viewModelScope.launch {
            songRepository.getAlbums().collect { _albums.value = it }
        }
        viewModelScope.launch {
            playlistRepository.getAllPlaylists().collect { entities ->
                _playlists.value = entities.map { it.toDomainModel() }
            }
        }
    }
}
