package com.xfyc.music.data.local.dao

import androidx.room.*
import com.xfyc.music.data.local.entity.MusicSourceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MusicSourceDao {

    @Query("SELECT * FROM music_sources ORDER BY name ASC")
    fun getAllSources(): Flow<List<MusicSourceEntity>>

    @Query("SELECT * FROM music_sources WHERE enabled = 1 ORDER BY name ASC")
    fun getEnabledSources(): Flow<List<MusicSourceEntity>>

    @Query("SELECT * FROM music_sources WHERE id = :id")
    suspend fun getSourceById(id: Long): MusicSourceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSource(source: MusicSourceEntity): Long

    @Update
    suspend fun updateSource(source: MusicSourceEntity)

    @Query("DELETE FROM music_sources WHERE id = :id")
    suspend fun deleteSource(id: Long)

    @Query("UPDATE music_sources SET enabled = :enabled, updatedAt = :updatedAt WHERE id = :id")
    suspend fun setEnabled(id: Long, enabled: Boolean, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE music_sources SET status = :status, errorMessage = :error, updatedAt = :updatedAt WHERE id = :id")
    suspend fun setStatus(id: Long, status: String, error: String? = null, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE music_sources SET lastSyncAt = :timestamp WHERE id = :id")
    suspend fun setLastSync(id: Long, timestamp: Long = System.currentTimeMillis())
}
