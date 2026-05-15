package com.xfyc.music.di

import com.xfyc.music.data.repository.SettingsRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideSettingsRepository(settingsRepository: SettingsRepository): SettingsRepository {
        return settingsRepository
    }
}
