package com.xfyc.music.domain.model

import com.xfyc.music.data.local.entity.MusicSourceEntity
import com.xfyc.music.data.local.entity.PlaylistEntity
import com.xfyc.music.data.local.entity.SongEntity

fun SongEntity.toDomainModel(): Song = Song(
    id = id,
    title = title,
    artist = artist,
    album = album,
    duration = duration,
    sourceType = sourceType,
    sourceId = sourceId,
    filePath = filePath,
    playUrl = playUrl,
    coverUrl = coverUrl,
    lyricUrl = lyricUrl,
    format = format,
    bitrate = bitrate,
    fileSize = fileSize,
    isFavorite = isFavorite,
    inLibrary = inLibrary,
    lastPlayedAt = lastPlayedAt
)

fun PlaylistEntity.toDomainModel(): Playlist = Playlist(
    id = id,
    name = name,
    description = description,
    coverUrl = coverUrl,
    songCount = songCount
)

fun MusicSourceEntity.toDomainModel(): MusicSource = MusicSource(
    id = id,
    name = name,
    type = type,
    baseUrl = baseUrl,
    authType = authType,
    authConfig = authConfig,
    searchUrl = searchUrl,
    songDetailUrl = songDetailUrl,
    playUrl = playUrl,
    coverUrl = coverUrl,
    lyricUrl = lyricUrl,
    enabled = enabled,
    allowCache = allowCache,
    lastSyncAt = lastSyncAt,
    status = status,
    errorMessage = errorMessage,
    configJson = configJson,
    timeoutSeconds = timeoutSeconds
)
