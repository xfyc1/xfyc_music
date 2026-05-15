package com.xfyc.music.ui.player

import android.content.ComponentName
import android.content.Context
import androidx.core.content.ContextCompat
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xfyc.music.data.cover.CoverLoader
import com.xfyc.music.data.local.entity.SongEntity
import com.xfyc.music.data.lyric.LyricLoader
import com.xfyc.music.data.repository.SettingsRepository
import com.xfyc.music.data.repository.SongRepository
import com.xfyc.music.domain.model.PlayMode
import com.xfyc.music.domain.model.Song
import com.xfyc.music.domain.model.toDomainModel
import com.xfyc.music.player.MusicService
import com.xfyc.music.player.PlayQueue
import com.xfyc.music.player.PlayerController
import com.google.common.util.concurrent.ListenableFuture
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val playerController: PlayerController,
    val playQueue: PlayQueue,
    private val songRepository: SongRepository,
    private val settingsRepository: SettingsRepository,
    private val lyricLoader: LyricLoader,
    private val coverLoader: CoverLoader
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

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var mediaController: MediaController? = null

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
                playSongIds(listOf(song.id), 0)
            } else {
                val songId = songRepository.upsertRemoteSong(song, inLibrary = false)
                playSongIds(listOf(songId), 0)
            }
        }
    }

    fun playQueue(songs: List<Song>, startIndex: Int = 0) {
        viewModelScope.launch {
            val ids = songs.mapNotNull { song ->
                if (song.isLocal) {
                    song.id
                } else {
                    songRepository.upsertRemoteSong(song, inLibrary = false)
                }
            }
            if (ids.isNotEmpty()) {
                playSongIds(ids, startIndex)
            }
        }
    }

    fun playSongIds(songIds: List<Long>, startIndex: Int = 0) {
        if (songIds.isEmpty()) return
        connectPlaybackService()
        playQueue.setQueue(songIds, startIndex.coerceIn(songIds.indices))
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
        mediaController?.release()
        super.onCleared()
    }

    private fun connectPlaybackService() {
        if (mediaController != null || controllerFuture != null) return

        val token = SessionToken(context, ComponentName(context, MusicService::class.java))
        val future = MediaController.Builder(context, token).buildAsync()
        controllerFuture = future
        future.addListener(
            {
                mediaController = future.get()
                controllerFuture = null
            },
            ContextCompat.getMainExecutor(context)
        )
    }
}
