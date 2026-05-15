package com.xfyc.music.player

import com.xfyc.music.domain.model.PlayMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlayQueue @Inject constructor() {

    private val _songs = MutableStateFlow<List<Long>>(emptyList())
    val songs: StateFlow<List<Long>> = _songs.asStateFlow()

    private val _currentIndex = MutableStateFlow(-1)
    val currentIndex: StateFlow<Int> = _currentIndex.asStateFlow()

    private val _playMode = MutableStateFlow(PlayMode.SEQUENTIAL)
    val playMode: StateFlow<PlayMode> = _playMode.asStateFlow()

    private val shuffleOrder = mutableListOf<Int>()

    val currentSongId: Long?
        get() = _songs.value.getOrNull(_currentIndex.value)

    val size: Int get() = _songs.value.size

    val isEmpty: Boolean get() = _songs.value.isEmpty()

    val isLastSong: Boolean
        get() {
            if (_playMode.value == PlayMode.SHUFFLE) {
                return shuffleOrder.isEmpty() || _currentIndex.value == shuffleOrder.last()
            }
            return _currentIndex.value >= _songs.value.size - 1
        }

    fun setQueue(songIds: List<Long>, startIndex: Int = 0) {
        _songs.value = songIds.toList()
        _currentIndex.value = startIndex.coerceIn(0, (songIds.size - 1).coerceAtLeast(0))
        if (_playMode.value == PlayMode.SHUFFLE) {
            generateShuffleOrder()
        }
    }

    fun addToNext(songId: Long) {
        val list = _songs.value.toMutableList()
        val insertAt = _currentIndex.value + 1
        list.add(insertAt, songId)
        _songs.value = list
    }

    fun addToQueue(songId: Long) {
        _songs.value = _songs.value + songId
    }

    fun addToQueue(songIds: List<Long>) {
        _songs.value = _songs.value + songIds
    }

    fun removeAt(index: Int) {
        val list = _songs.value.toMutableList()
        if (index in list.indices) {
            list.removeAt(index)
            _songs.value = list
            when {
                index < _currentIndex.value -> _currentIndex.value -= 1
                index == _currentIndex.value -> {
                    if (_currentIndex.value >= list.size) {
                        _currentIndex.value = list.size - 1
                    }
                }
            }
        }
    }

    fun removeSong(songId: Long) {
        val index = _songs.value.indexOf(songId)
        if (index >= 0) removeAt(index)
    }

    fun move(fromIndex: Int, toIndex: Int) {
        val list = _songs.value.toMutableList()
        if (fromIndex in list.indices && toIndex in list.indices) {
            val item = list.removeAt(fromIndex)
            list.add(toIndex, item)
            _songs.value = list

            // Adjust current index
            if (_currentIndex.value == fromIndex) {
                _currentIndex.value = toIndex
            } else if (fromIndex < _currentIndex.value && toIndex >= _currentIndex.value) {
                _currentIndex.value -= 1
            } else if (fromIndex > _currentIndex.value && toIndex <= _currentIndex.value) {
                _currentIndex.value += 1
            }
        }
    }

    fun clear() {
        _songs.value = emptyList()
        _currentIndex.value = -1
    }

    fun skipToNext(): Int {
        val size = _songs.value.size
        if (size == 0) return -1

        val nextIndex = when (_playMode.value) {
            PlayMode.SINGLE_LOOP -> _currentIndex.value
            PlayMode.SEQUENTIAL -> {
                val next = _currentIndex.value + 1
                if (next >= size) -1 else next
            }
            PlayMode.LIST_LOOP -> (_currentIndex.value + 1) % size
            PlayMode.SHUFFLE -> {
                val currentShuffleIdx = shuffleOrder.indexOf(_currentIndex.value)
                if (currentShuffleIdx < shuffleOrder.size - 1) {
                    shuffleOrder[currentShuffleIdx + 1]
                } else {
                    generateShuffleOrder()
                    shuffleOrder.firstOrNull() ?: -1
                }
            }
        }

        if (nextIndex >= 0) {
            _currentIndex.value = nextIndex
        }
        return nextIndex
    }

    fun skipToPrevious(): Int {
        val size = _songs.value.size
        if (size == 0) return -1

        val prevIndex = when (_playMode.value) {
            PlayMode.SINGLE_LOOP -> _currentIndex.value
            PlayMode.SEQUENTIAL -> {
                val prev = _currentIndex.value - 1
                if (prev < 0) -1 else prev
            }
            PlayMode.LIST_LOOP -> (_currentIndex.value - 1 + size) % size
            PlayMode.SHUFFLE -> {
                val currentShuffleIdx = shuffleOrder.indexOf(_currentIndex.value)
                if (currentShuffleIdx > 0) {
                    shuffleOrder[currentShuffleIdx - 1]
                } else {
                    -1
                }
            }
        }

        if (prevIndex >= 0) {
            _currentIndex.value = prevIndex
        }
        return prevIndex
    }

    fun skipTo(index: Int) {
        if (index in _songs.value.indices) {
            _currentIndex.value = index
        }
    }

    fun setPlayMode(mode: PlayMode) {
        _playMode.value = mode
        if (mode == PlayMode.SHUFFLE) {
            generateShuffleOrder()
        }
    }

    fun cyclePlayMode(): PlayMode {
        val newMode = when (_playMode.value) {
            PlayMode.SEQUENTIAL -> PlayMode.SINGLE_LOOP
            PlayMode.SINGLE_LOOP -> PlayMode.LIST_LOOP
            PlayMode.LIST_LOOP -> PlayMode.SHUFFLE
            PlayMode.SHUFFLE -> PlayMode.SEQUENTIAL
        }
        setPlayMode(newMode)
        return newMode
    }

    fun getSongIds(): List<Long> = _songs.value.toList()

    private fun generateShuffleOrder() {
        val indices = _songs.value.indices.toMutableList()
        indices.shuffle()
        // Ensure current playing song stays first in shuffle
        val current = _currentIndex.value
        if (current in indices) {
            indices.remove(current)
            shuffleOrder.clear()
            shuffleOrder.add(current)
            shuffleOrder.addAll(indices)
        } else {
            shuffleOrder.clear()
            shuffleOrder.addAll(indices)
        }
    }
}
