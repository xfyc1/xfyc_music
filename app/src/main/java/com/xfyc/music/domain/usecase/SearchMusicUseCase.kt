package com.xfyc.music.domain.usecase

import com.xfyc.music.data.repository.SearchRepository
import com.xfyc.music.domain.model.SearchResult
import javax.inject.Inject

class SearchMusicUseCase @Inject constructor(
    private val searchRepository: SearchRepository
) {
    suspend operator fun invoke(query: String): List<SearchResult> {
        if (query.isBlank()) return emptyList()
        return searchRepository.searchAll(query)
    }
}
