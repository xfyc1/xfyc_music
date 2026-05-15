package com.xfyc.music.ui.source

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xfyc.music.data.local.entity.MusicSourceEntity
import com.xfyc.music.data.repository.MusicSourceRepository
import com.xfyc.music.domain.model.MusicSource
import com.xfyc.music.domain.model.toDomainModel
import com.xfyc.music.domain.usecase.MusicSourceUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MusicSourceViewModel @Inject constructor(
    private val musicSourceRepository: MusicSourceRepository,
    private val musicSourceUseCase: MusicSourceUseCase
) : ViewModel() {

    private val _sources = MutableStateFlow<List<MusicSource>>(emptyList())
    val sources: StateFlow<List<MusicSource>> = _sources.asStateFlow()

    private val _isTesting = MutableStateFlow<MutableMap<Long, Boolean>>(mutableMapOf())
    val isTesting: StateFlow<Map<Long, Boolean>> = _isTesting.asStateFlow()

    private val _testResult = MutableStateFlow<Pair<Long, String>?>(null)
    val testResult: StateFlow<Pair<Long, String>?> = _testResult.asStateFlow()

    init {
        viewModelScope.launch {
            musicSourceRepository.getAllSources().collect { entities ->
                _sources.value = entities.map { it.toDomainModel() }
            }
        }
    }

    fun addUrlSource(
        name: String,
        baseUrl: String,
        searchPath: String,
        playPath: String,
        coverPath: String,
        lyricPath: String,
        authType: String,
        authConfig: String?
    ) {
        viewModelScope.launch {
            val source = MusicSourceEntity(
                name = name,
                type = "url",
                baseUrl = baseUrl,
                authType = authType,
                authConfig = authConfig,
                searchUrl = buildUrl(baseUrl, searchPath),
                playUrl = buildUrl(baseUrl, playPath),
                coverUrl = buildCoverPath(baseUrl, coverPath),
                lyricUrl = buildCoverPath(baseUrl, lyricPath)
            )
            musicSourceUseCase.addSource(source)
        }
    }

    fun addJsonConfigSource(name: String, jsonConfig: String) {
        viewModelScope.launch {
            val source = MusicSourceEntity(
                name = name,
                type = "json_config",
                baseUrl = "",
                configJson = jsonConfig
            )
            musicSourceUseCase.addSource(source)
        }
    }

    fun deleteSource(id: Long) {
        viewModelScope.launch {
            musicSourceUseCase.deleteSource(id)
        }
    }

    fun toggleEnabled(id: Long, enabled: Boolean) {
        viewModelScope.launch {
            musicSourceUseCase.setEnabled(id, !enabled)
        }
    }

    fun testConnection(id: Long) {
        viewModelScope.launch {
            val current = _isTesting.value.toMutableMap()
            current[id] = true
            _isTesting.value = current

            val result = musicSourceUseCase.testConnection(id)
            val msg = result.fold(
                onSuccess = { "Connection successful" },
                onFailure = { "Failed: ${it.message}" }
            )
            _testResult.value = Pair(id, msg)

            current[id] = false
            _isTesting.value = current
        }
    }

    fun clearTestResult() {
        _testResult.value = null
    }

    private fun buildUrl(baseUrl: String, path: String): String? {
        if (path.isBlank()) return null
        val trimmedBase = baseUrl.trimEnd('/')
        return if (path.startsWith("http")) path else "$trimmedBase/${path.trimStart('/')}"
    }

    private fun buildCoverPath(baseUrl: String, path: String): String? {
        if (path.isBlank()) return null
        return buildUrl(baseUrl, path)
    }
}
