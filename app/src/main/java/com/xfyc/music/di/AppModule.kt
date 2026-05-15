package com.xfyc.music.di

import com.xfyc.music.data.source.GenericApiAdapter
import com.xfyc.music.data.source.LxMusicAdapter
import com.xfyc.music.data.source.MusicSourceAdapter
import com.xfyc.music.data.source.MusicSourceAdapterRouter
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {

    @Binds
    @Singleton
    abstract fun bindMusicSourceAdapter(
        router: MusicSourceAdapterRouter
    ): MusicSourceAdapter

    @Binds
    @IntoSet
    abstract fun bindGenericApiAdapter(
        adapter: GenericApiAdapter
    ): MusicSourceAdapter

    @Binds
    @IntoSet
    abstract fun bindLxMusicAdapter(
        adapter: LxMusicAdapter
    ): MusicSourceAdapter
}
