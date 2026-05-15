package com.xfyc.music.player

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.xfyc.music.data.local.entity.SongEntity
import com.xfyc.music.domain.model.PlayMode
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlayerController @Inject constructor(
    @ApplicationContext private val context: Context,
    private val playQueue: PlayQueue,
    private val audioFocusHandler: AudioFocusHandler
) {
    val exoPlayer: ExoPlayer = ExoPlayer.Builder(context).build()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentSongId = MutableStateFlow<Long?>(null)
    val currentSongId: StateFlow<Long?> = _currentSongId.asStateFlow()

    init {
        audioFocusHandler.attach(exoPlayer)
        audioFocusHandler.setCallbacks(
            onPause = { pause() },
            onPlay = { play() }
        )

        exoPlayer.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _isPlaying.value = isPlaying
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                when (playbackState) {
                    Player.STATE_ENDED -> {
                        if (playQueue.playMode.value == PlayMode.SINGLE_LOOP) {
                            exoPlayer.seekTo(0)
                            exoPlayer.play()
                        } else {
                            onSongEnded()
                        }
                    }
                    Player.STATE_READY -> {
                        if (exoPlayer.playWhenReady) {
                            _isPlaying.value = true
                        }
                    }
                }
            }
        })
    }

    fun playSong(song: SongEntity) {
        val mediaItem = buildMediaItem(song)
        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.prepare()
        play()
        _currentSongId.value = song.id
    }

    fun playRemoteSong(song: com.xfyc.music.domain.model.Song) {
        val mediaItem = MediaItem.Builder()
            .setMediaId(song.sourceId ?: song.id.toString())
            .setUri(song.playUrl ?: "")
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(song.title)
                    .setArtist(song.artist)
                    .setAlbumTitle(song.album)
                    .build()
            )
            .build()
        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.prepare()
        play()
        _currentSongId.value = song.id
    }

    fun play() {
        if (audioFocusHandler.requestFocus()) {
            exoPlayer.playWhenReady = true
            audioFocusHandler.registerNoisyReceiver()
        }
    }

    fun pause() {
        exoPlayer.playWhenReady = false
        _isPlaying.value = false
        audioFocusHandler.unregisterNoisyReceiver()
    }

    fun togglePlayPause() {
        if (exoPlayer.playWhenReady) {
            pause()
        } else {
            play()
        }
    }

    fun seekTo(positionMs: Long) {
        exoPlayer.seekTo(positionMs)
    }

    fun seekToProgress(progress: Float) {
        val duration = exoPlayer.duration
        if (duration > 0) {
            exoPlayer.seekTo((duration * progress).toLong())
        }
    }

    fun skipToNext(): Boolean {
        val nextIndex = playQueue.skipToNext()
        return if (nextIndex >= 0) {
            // Signal caller to load song at nextIndex
            true
        } else {
            false
        }
    }

    fun skipToPrevious(): Boolean {
        // If more than 3 seconds in, restart current song
        if (exoPlayer.currentPosition > 3000) {
            exoPlayer.seekTo(0)
            return true
        }
        val prevIndex = playQueue.skipToPrevious()
        return if (prevIndex >= 0) {
            true
        } else {
            exoPlayer.seekTo(0)
            true
        }
    }

    val duration: Long get() = exoPlayer.duration

    val currentPosition: Long get() = exoPlayer.currentPosition

    fun release() {
        audioFocusHandler.unregisterNoisyReceiver()
        audioFocusHandler.abandonFocus()
        exoPlayer.release()
    }

    private fun onSongEnded() {
        val hasNext = skipToNext()
        if (!hasNext) {
            pause()
            exoPlayer.seekTo(0)
        }
        // Caller should observe currentIndex changes and load next song
    }

    private fun buildMediaItem(song: SongEntity): MediaItem {
        val path = song.filePath
        return if (path != null) {
            MediaItem.Builder()
                .setMediaId(song.id.toString())
                .setUri("file://$path")
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(song.title)
                        .setArtist(song.artist)
                        .setAlbumTitle(song.album)
                        .build()
                )
                .build()
        } else {
            MediaItem.Builder()
                .setMediaId(song.id.toString())
                .setUri(song.playUrl!!)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(song.title)
                        .setArtist(song.artist)
                        .setAlbumTitle(song.album)
                        .build()
                )
                .build()
        }
    }
}
