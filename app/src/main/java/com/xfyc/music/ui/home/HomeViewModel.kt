package com.xfyc.music.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xfyc.music.data.repository.SongRepository
import com.xfyc.music.domain.model.Song
import com.xfyc.music.domain.model.toDomainModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val songRepository: SongRepository
) : ViewModel() {

    private val _recentSongs = MutableStateFlow<List<Song>>(emptyList())
    val recentSongs: StateFlow<List<Song>> = _recentSongs.asStateFlow()

    private val _favoriteSongs = MutableStateFlow<List<Song>>(emptyList())
    val favoriteSongs: StateFlow<List<Song>> = _favoriteSongs.asStateFlow()

    private val _songCount = MutableStateFlow(0)
    val songCount: StateFlow<Int> = _songCount.asStateFlow()

    init {
        viewModelScope.launch {
            songRepository.getRecentSongs(10).collect { entities ->
                _recentSongs.value = entities.map { it.toDomainModel() }
            }
        }
        viewModelScope.launch {
            songRepository.getFavoriteSongs().collect { entities ->
                _favoriteSongs.value = entities.map { it.toDomainModel() }
            }
        }
        viewModelScope.launch {
            _songCount.value = songRepository.getSongCount()
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _songCount.value = songRepository.getSongCount()
        }
    }
}
