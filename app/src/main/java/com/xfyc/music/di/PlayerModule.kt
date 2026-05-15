package com.xfyc.music.di

import com.xfyc.music.player.AudioFocusHandler
import com.xfyc.music.player.PlayQueue
import com.xfyc.music.player.PlayerController
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object PlayerModule {

    @Provides
    @Singleton
    fun providePlayQueue(): PlayQueue = PlayQueue()

    @Provides
    @Singleton
    fun providePlayerController(playerController: PlayerController): PlayerController = playerController

    @Provides
    @Singleton
    fun provideAudioFocusHandler(audioFocusHandler: AudioFocusHandler): AudioFocusHandler = audioFocusHandler
}
