package com.xfyc.music.ui.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xfyc.music.data.cover.CoverLoader
import com.xfyc.music.data.local.entity.SongEntity
import com.xfyc.music.data.lyric.LyricLoader
import com.xfyc.music.data.repository.MusicSourceRepository
import com.xfyc.music.data.repository.SettingsRepository
import com.xfyc.music.data.repository.SongRepository
import com.xfyc.music.domain.model.PlayMode
import com.xfyc.music.domain.model.Song
import com.xfyc.music.domain.model.toDomainModel
import com.xfyc.music.player.PlayQueue
import com.xfyc.music.player.PlayerController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val playerController: PlayerController,
    val playQueue: PlayQueue,
    private val songRepository: SongRepository,
    private val settingsRepository: SettingsRepository,
    private val lyricLoader: LyricLoader,
    private val coverLoader: CoverLoader,
    private val musicSourceRepository: MusicSourceRepository
) : ViewModel() {

    val isPlaying: StateFlow<Boolean> = playerController.isPlaying

    private val _currentSong = MutableStateFlow<Song?>(null)
    val currentSong: StateFlow<Song?> = _currentSong.asStateFlow()

    val playMode: StateFlow<PlayMode> = playQueue.playMode

    val currentIndex: StateFlow<Int> = playQueue.currentIndex

    val queueSize: StateFlow<Int> = playQueue.songs.map { it.size }
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition.asStateFlow()

    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration.asStateFlow()

    private val _lyrics = MutableStateFlow<List<LyricLoader.LyricLine>?>(null)
    val lyrics: StateFlow<List<LyricLoader.LyricLine>?> = _lyrics.asStateFlow()

    private val _currentLyricIndex = MutableStateFlow(-1)
    val currentLyricIndex: StateFlow<Int> = _currentLyricIndex.asStateFlow()

    private val _coverData = MutableStateFlow<ByteArray?>(null)
    val coverData: StateFlow<ByteArray?> = _coverData.asStateFlow()

    init {
        viewModelScope.launch {
            playerController.currentSongId.collect { songId ->
                songId?.let {
                    val entity = songRepository.getSongById(it)
                    _currentSong.value = entity?.toDomainModel()
                    // Load lyrics and cover
                    loadLyricsAndCover(entity)
                }
            }
        }
        viewModelScope.launch {
            while (true) {
                _currentPosition.value = playerController.currentPosition
                _duration.value = playerController.duration
                // Update current lyric line
                val lrc = _lyrics.value
                if (lrc != null) {
                    _currentLyricIndex.value = lyricLoader.findCurrentLine(lrc, _currentPosition.value)
                }
                kotlinx.coroutines.delay(300)
            }
        }
    }

    private fun loadLyricsAndCover(entity: SongEntity?) {
        viewModelScope.launch {
            val song = entity ?: return@launch
            val lrc = lyricLoader.loadLyric(song.lyricUrl, song.filePath)
            _lyrics.value = lrc
        }
        viewModelScope.launch {
            val song = entity ?: return@launch
            val cover = coverLoader.loadCover(song.coverUrl, song.filePath)
            _coverData.value = cover
        }
    }

    fun togglePlayPause() {
        playerController.togglePlayPause()
    }

    fun skipToNext() {
        val hasNext = playerController.skipToNext()
        if (!hasNext) playerController.pause()
    }

    fun skipToPrevious() {
        playerController.skipToPrevious()
    }

    fun seekTo(positionMs: Long) {
        playerController.seekTo(positionMs)
    }

    fun seekToProgress(progress: Float) {
        playerController.seekToProgress(progress)
    }

    fun cyclePlayMode() {
        playQueue.cyclePlayMode()
    }

    fun toggleFavorite() {
        viewModelScope.launch {
            val song = _currentSong.value ?: return@launch
            songRepository.setFavorite(song.id, !song.isFavorite)
        }
    }

    fun playSong(song: Song) {
        viewModelScope.launch {
            if (song.isLocal) {
                songRepository.setLastPlayed(song.id)
                val entity = songRepository.getSongById(song.id) ?: return@launch
                playerController.playSong(entity)
                _currentSong.value = song
            } else {
                // Remote song - resolve play URL from source
                val sourceId = song.sourceId
                if (sourceId != null) {
                    val playUrlResult = musicSourceRepository.getPlayUrl(
                        // Try to extract the source database ID from the song
                        // For remote songs from search, sourceId is the remote song ID
                        sourceId = 0, // placeholder — will need proper source tracking
                        songRemoteId = sourceId
                    )
                    playUrlResult.onSuccess { url ->
                        val playableSong = song.copy(playUrl = url)
                        playerController.playRemoteSong(playableSong)
                        _currentSong.value = playableSong
                    }
                } else {
                    // Direct URL play
                    playerController.playRemoteSong(song)
                    _currentSong.value = song
                }
            }
        }
    }

    fun playQueue(songs: List<Song>, startIndex: Int = 0) {
        playQueue.setQueue(songs.map { it.id }, startIndex)
    }

    fun addToQueue(song: Song) {
        playQueue.addToQueue(song.id)
    }

    fun addToNext(song: Song) {
        playQueue.addToNext(song.id)
    }

    override fun onCleared() {
        viewModelScope.launch {
            playerController.currentSongId.value?.let {
                settingsRepository.saveLastPlayed(it)
            }
            settingsRepository.saveLastPlayPosition(playerController.currentPosition)
            settingsRepository.savePlayQueueIds(playQueue.getSongIds())
        }
        super.onCleared()
    }
}
