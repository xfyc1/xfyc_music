package com.xfyc.music.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.xfyc.music.domain.model.PlayMode
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val DEFAULT_PLAY_MODE = stringPreferencesKey("default_play_mode")
        val DEFAULT_VOLUME = floatPreferencesKey("default_volume")
        val SCAN_ON_STARTUP = booleanPreferencesKey("scan_on_startup")
        val CACHE_SIZE_LIMIT = intPreferencesKey("cache_size_limit_mb")
        val AUTO_PAUSE_HEADSET = booleanPreferencesKey("auto_pause_headset")
        val LAST_PLAYED_SONG_ID = longPreferencesKey("last_played_song_id")
        val LAST_PLAY_POSITION = longPreferencesKey("last_play_position")
        val PLAY_QUEUE_IDS = stringPreferencesKey("play_queue_ids")
        val THEME_MODE = stringPreferencesKey("theme_mode")
    }

    val defaultPlayMode: Flow<PlayMode> = context.dataStore.data.map { prefs ->
        val modeName = prefs[Keys.DEFAULT_PLAY_MODE] ?: PlayMode.SEQUENTIAL.name
        try { PlayMode.valueOf(modeName) } catch (_: Exception) { PlayMode.SEQUENTIAL }
    }

    val scanOnStartup: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.SCAN_ON_STARTUP] ?: false
    }

    val autoPauseHeadset: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.AUTO_PAUSE_HEADSET] ?: true
    }

    val lastPlayedSongId: Flow<Long?> = context.dataStore.data.map { prefs ->
        prefs[Keys.LAST_PLAYED_SONG_ID]
    }

    val lastPlayPosition: Flow<Long> = context.dataStore.data.map { prefs ->
        prefs[Keys.LAST_PLAY_POSITION] ?: 0L
    }

    val playQueueIds: Flow<List<Long>> = context.dataStore.data.map { prefs ->
        prefs[Keys.PLAY_QUEUE_IDS]?.split(",")?.mapNotNull { it.toLongOrNull() } ?: emptyList()
    }

    val themeMode: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[Keys.THEME_MODE] ?: "system"
    }

    suspend fun setDefaultPlayMode(mode: PlayMode) {
        context.dataStore.edit { prefs ->
            prefs[Keys.DEFAULT_PLAY_MODE] = mode.name
        }
    }

    suspend fun setScanOnStartup(scan: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.SCAN_ON_STARTUP] = scan
        }
    }

    suspend fun setAutoPauseHeadset(pause: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.AUTO_PAUSE_HEADSET] = pause
        }
    }

    suspend fun saveLastPlayed(songId: Long) {
        context.dataStore.edit { prefs ->
            prefs[Keys.LAST_PLAYED_SONG_ID] = songId
        }
    }

    suspend fun saveLastPlayPosition(positionMs: Long) {
        context.dataStore.edit { prefs ->
            prefs[Keys.LAST_PLAY_POSITION] = positionMs
        }
    }

    suspend fun savePlayQueueIds(ids: List<Long>) {
        context.dataStore.edit { prefs ->
            prefs[Keys.PLAY_QUEUE_IDS] = ids.joinToString(",")
        }
    }

    suspend fun setThemeMode(mode: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.THEME_MODE] = mode
        }
    }
}
