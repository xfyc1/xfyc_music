package com.xfyc.music.data.source

import com.xfyc.music.data.local.entity.MusicSourceEntity
import com.xfyc.music.domain.model.Song
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MusicSourceAdapterRouter @Inject constructor(
    adapters: Set<@JvmSuppressWildcards MusicSourceAdapter>,
    private val fallback: GenericApiAdapter
) : MusicSourceAdapter {

    override val supportedTypes: Set<String> =
        adapters.flatMap { it.supportedTypes }.toSet()

    private val adapterMap: Map<String, MusicSourceAdapter> =
        adapters.flatMap { adapter ->
            adapter.supportedTypes.map { it to adapter }
        }.toMap()

    private fun resolve(source: MusicSourceEntity): MusicSourceAdapter =
        adapterMap[source.type] ?: fallback

    override suspend fun testConnection(source: MusicSourceEntity): Result<Boolean> =
        resolve(source).testConnection(source)

    override suspend fun search(source: MusicSourceEntity, query: String, page: Int): Result<List<Song>> =
        resolve(source).search(source, query, page)

    override suspend fun getSongDetail(source: MusicSourceEntity, songId: String): Result<Song> =
        resolve(source).getSongDetail(source, songId)

    override suspend fun getPlayUrl(source: MusicSourceEntity, songId: String): Result<String> =
        resolve(source).getPlayUrl(source, songId)

    override suspend fun getCoverUrl(source: MusicSourceEntity, songId: String): Result<String> =
        resolve(source).getCoverUrl(source, songId)

    override suspend fun getLyricUrl(source: MusicSourceEntity, songId: String): Result<String?> =
        resolve(source).getLyricUrl(source, songId)
}
