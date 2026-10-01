package com.example.ui.dashboard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.DownloadEntity
import com.example.data.DownloadRepository
import com.example.download.DownloadController
import com.example.model.MediaFormat
import com.example.model.MediaType
import com.example.model.ParsedMedia
import com.example.parser.MediaParserEngine
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DashboardUiState(
    val allDownloadsList: List<DownloadEntity> = emptyList(),
    val downloads: List<DownloadEntity> = emptyList(),
    val activeFilter: String = "ALL", // "ALL", "VIDEO", "AUDIO"
    val inputUrl: String = "",
    val isParsing: Boolean = false,
    val parsedMedia: ParsedMedia? = null,
    val parseError: String? = null,
    val selectedFormat: MediaFormat? = null,
    val isDownloadInitiated: Boolean = false
)

class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = DownloadRepository(AppDatabase.getInstance(application).downloadDao())
    val downloadController = DownloadController(application)

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.allDownloads.collect { allItems ->
                _uiState.update { current ->
                    current.copy(
                        allDownloadsList = allItems,
                        downloads = filterDownloads(allItems, current.activeFilter)
                    )
                }
            }
        }
    }

    private fun filterDownloads(list: List<DownloadEntity>, filter: String): List<DownloadEntity> {
        return when (filter) {
            "VIDEO" -> list.filter { it.mediaType == "VIDEO" }
            "AUDIO" -> list.filter { it.mediaType == "AUDIO" }
            else -> list
        }
    }

    fun onUrlInputChanged(newUrl: String) {
        _uiState.update { it.copy(inputUrl = newUrl, parseError = null) }
    }

    fun parseCurrentUrl() {
        val url = _uiState.value.inputUrl.trim()
        if (url.isEmpty()) return

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isParsing = true,
                    parseError = null,
                    parsedMedia = null,
                    selectedFormat = null,
                    isDownloadInitiated = false
                )
            }

            val result = MediaParserEngine.parse(url)
            result.onSuccess { media ->
                val hasVideo = media.availableFormats.any { it.mediaType == MediaType.VIDEO }
                val defaultFormat = if (hasVideo) {
                    media.availableFormats.firstOrNull { it.mediaType == MediaType.VIDEO }
                } else {
                    media.availableFormats.firstOrNull { it.mediaType == MediaType.AUDIO }
                }
                _uiState.update {
                    it.copy(
                        isParsing = false,
                        parsedMedia = media,
                        selectedFormat = defaultFormat
                    )
                }
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        isParsing = false,
                        parseError = err.localizedMessage ?: "Failed to parse video stream"
                    )
                }
            }
        }
    }

    fun selectFormat(format: MediaFormat) {
        _uiState.update { it.copy(selectedFormat = format) }
    }

    fun startDownload() {
        val media = _uiState.value.parsedMedia ?: return
        val format = _uiState.value.selectedFormat ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isDownloadInitiated = true) }
            downloadController.startDownload(media, format)
            _uiState.update { it.copy(inputUrl = "") }
            delay(1200)
            _uiState.update {
                it.copy(
                    parsedMedia = null,
                    selectedFormat = null,
                    isDownloadInitiated = false
                )
            }
        }
    }

    fun dismissParseDialog() {
        _uiState.update {
            it.copy(
                parsedMedia = null,
                parseError = null,
                selectedFormat = null,
                isDownloadInitiated = false
            )
        }
    }

    fun setFilter(filter: String) {
        _uiState.update { current ->
            current.copy(
                activeFilter = filter,
                downloads = filterDownloads(current.allDownloadsList, filter)
            )
        }
    }

    fun deleteDownload(entity: DownloadEntity) {
        viewModelScope.launch {
            downloadController.deleteFile(entity)
        }
    }

    fun openFile(entity: DownloadEntity) {
        val mime = if (entity.mediaType == "AUDIO") "audio/*" else "video/*"
        downloadController.openDownloadedFile(entity.localFilePath, mime)
    }

    fun shareFile(entity: DownloadEntity) {
        val mime = if (entity.mediaType == "AUDIO") "audio/*" else "video/*"
        downloadController.shareDownloadedFile(entity.localFilePath, mime)
    }
}
