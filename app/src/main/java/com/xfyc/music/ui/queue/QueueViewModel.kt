package com.xfyc.music.ui.queue

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xfyc.music.data.repository.SongRepository
import com.xfyc.music.domain.model.Song
import com.xfyc.music.domain.model.toDomainModel
import com.xfyc.music.player.PlayQueue
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class QueueViewModel @Inject constructor(
    private val playQueue: PlayQueue,
    private val songRepository: SongRepository
) : ViewModel() {

    private val _queueSongs = MutableStateFlow<List<Song>>(emptyList())
    val queueSongs: StateFlow<List<Song>> = _queueSongs.asStateFlow()

    val currentIndex: StateFlow<Int> = playQueue.currentIndex

    init {
        viewModelScope.launch {
            playQueue.songs.collect { songIds ->
                val songs = songIds.mapNotNull { id ->
                    songRepository.getSongById(id)?.toDomainModel()
                }
                _queueSongs.value = songs
            }
        }
    }

    fun removeFromQueue(index: Int) {
        playQueue.removeAt(index)
    }

    fun moveItem(fromIndex: Int, toIndex: Int) {
        playQueue.move(fromIndex, toIndex)
    }

    fun skipTo(index: Int) {
        playQueue.skipTo(index)
    }

    fun clearQueue() {
        playQueue.clear()
    }
}
