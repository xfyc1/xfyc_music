package com.xfyc.music.di

import com.xfyc.music.data.source.GenericApiAdapter
import com.xfyc.music.data.source.MusicSourceAdapter
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {

    @Binds
    @Singleton
    abstract fun bindMusicSourceAdapter(
        adapter: GenericApiAdapter
    ): MusicSourceAdapter
}
