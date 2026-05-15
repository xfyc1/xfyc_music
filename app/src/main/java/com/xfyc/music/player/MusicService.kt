package com.xfyc.music.player

import android.content.Intent
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.xfyc.music.data.local.entity.SongEntity
import com.xfyc.music.data.repository.MusicSourceRepository
import com.xfyc.music.data.repository.SettingsRepository
import com.xfyc.music.data.repository.SongRepository
import com.xfyc.music.domain.model.PlayMode
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MusicService : MediaSessionService() {

    @Inject lateinit var playerController: PlayerController
    @Inject lateinit var playQueue: PlayQueue
    @Inject lateinit var songRepository: SongRepository
    @Inject lateinit var musicSourceRepository: MusicSourceRepository
    @Inject lateinit var settingsRepository: SettingsRepository

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var mediaSession: MediaSession? = null
    private var previousIndex = -1

    override fun onCreate() {
        super.onCreate()

        val player = playerController.exoPlayer

        mediaSession = MediaSession.Builder(this, player)
            .setCallback(MediaSessionCallback())
            .build()

        // Observe queue changes and load the current song
        serviceScope.launch {
            playQueue.currentIndex.collect { index ->
                if (index >= 0 && index != previousIndex) {
                    previousIndex = index
                    loadAndPlaySongAtIndex(index)
                }
            }
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        if (!playerController.exoPlayer.playWhenReady) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        mediaSession?.release()
        playerController.release()
        super.onDestroy()
    }

    private fun loadAndPlaySongAtIndex(index: Int) {
        serviceScope.launch {
            val songId = playQueue.songs.value.getOrNull(index) ?: return@launch
            val song = songRepository.getSongById(songId) ?: return@launch
            val playableSong = resolvePlayableSong(song) ?: return@launch
            playerController.playSong(playableSong)
            updateMetadata(playableSong)
        }
    }

    private suspend fun resolvePlayableSong(song: SongEntity): SongEntity? {
        if (song.sourceType != "remote" || !song.playUrl.isNullOrBlank()) return song

        val sourceInfo = musicSourceRepository.decodeRemoteSourceId(song.sourceId) ?: return null
        val (sourceId, remoteSongId) = sourceInfo
        val playUrl = musicSourceRepository.getPlayUrl(sourceId, remoteSongId).getOrNull()
            ?: return null
        val updatedSong = song.copy(playUrl = playUrl, updatedAt = System.currentTimeMillis())
        songRepository.updateSong(updatedSong)
        return updatedSong
    }

    private fun updateMetadata(song: com.xfyc.music.data.local.entity.SongEntity) {
        // Metadata is set via the MediaItem on the player — no need to set separately
        // The MediaSession reads metadata from the player's current MediaItem
    }

    private inner class MediaSessionCallback : MediaSession.Callback {

        override fun onPostConnect(
            session: MediaSession,
            controller: MediaSession.ControllerInfo
        ) {
            serviceScope.launch {
                try {
                    val savedSongId = settingsRepository.lastPlayedSongId.first()
                    val savedPosition = settingsRepository.lastPlayPosition.first()
                    val savedQueue = settingsRepository.playQueueIds.first()
                    val savedMode = settingsRepository.defaultPlayMode.first()

                    if (savedQueue.isNotEmpty()) {
                        playQueue.setQueue(savedQueue)
                        playQueue.setPlayMode(savedMode)
                        previousIndex = -1 // force reload on first index emission

                        savedSongId?.let { id ->
                            val idx = savedQueue.indexOf(id)
                            if (idx >= 0) {
                                playQueue.skipTo(idx)
                                val song = songRepository.getSongById(id)
                                if (song != null) {
                                    playerController.playSong(song)
                                    updateMetadata(song)
                                    if (savedPosition > 0) {
                                        playerController.seekTo(savedPosition)
                                    }
                                }
                            }
                        }
                    }
                } catch (_: Exception) { }
            }
        }
    }
}
