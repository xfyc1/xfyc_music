package com.xfyc.music.di

import android.content.Context
import androidx.room.Room
import com.xfyc.music.data.local.AppDatabase
import com.xfyc.music.data.local.dao.MusicSourceDao
import com.xfyc.music.data.local.dao.PlaylistDao
import com.xfyc.music.data.local.dao.PlaylistItemDao
import com.xfyc.music.data.local.dao.SongDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.io.File
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "xfyc_music.db"
        ).build()
    }

    @Provides
    fun provideSongDao(database: AppDatabase): SongDao = database.songDao()

    @Provides
    fun providePlaylistDao(database: AppDatabase): PlaylistDao = database.playlistDao()

    @Provides
    fun providePlaylistItemDao(database: AppDatabase): PlaylistItemDao = database.playlistItemDao()

    @Provides
    fun provideMusicSourceDao(database: AppDatabase): MusicSourceDao = database.musicSourceDao()

    @Provides
    @Singleton
    fun provideCacheDir(@ApplicationContext context: Context): File {
        return context.cacheDir
    }
}
