package com.xfyc.music.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.xfyc.music.data.local.dao.MusicSourceDao
import com.xfyc.music.data.local.dao.PlaylistDao
import com.xfyc.music.data.local.dao.PlaylistItemDao
import com.xfyc.music.data.local.dao.SongDao
import com.xfyc.music.data.local.entity.MusicSourceEntity
import com.xfyc.music.data.local.entity.PlaylistEntity
import com.xfyc.music.data.local.entity.PlaylistItemEntity
import com.xfyc.music.data.local.entity.SongEntity

@Database(
    entities = [
        SongEntity::class,
        PlaylistEntity::class,
        PlaylistItemEntity::class,
        MusicSourceEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun songDao(): SongDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun playlistItemDao(): PlaylistItemDao
    abstract fun musicSourceDao(): MusicSourceDao
}
