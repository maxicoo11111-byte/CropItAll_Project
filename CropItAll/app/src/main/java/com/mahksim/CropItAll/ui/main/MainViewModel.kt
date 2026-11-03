package com.mahksim.CropItAll.ui.main

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mahksim.CropItAll.data.FileRepository
import com.mahksim.CropItAll.data.SettingsRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel(
    private val fileRepository: FileRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private val _sourceFolderUri = MutableStateFlow<Uri?>(null)
    val sourceFolderUri: StateFlow<Uri?> = _sourceFolderUri.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _navigationToCrop = MutableSharedFlow<Unit>()
    val navigationToCrop: SharedFlow<Unit> = _navigationToCrop.asSharedFlow()

    private val _emptyFolderEvent = MutableSharedFlow<Unit>()
    val emptyFolderEvent: SharedFlow<Unit> = _emptyFolderEvent.asSharedFlow()

    private val _binCount = MutableStateFlow(0)
    val binCount: StateFlow<Int> = _binCount.asStateFlow()

    init {
        viewModelScope.launch {
            loadSavedFolder()
        }
    }

    private suspend fun loadSavedFolder() {
        try {
            val uri = settingsRepository.getSourceFolderUri()
            _sourceFolderUri.value = uri
            if (uri != null) {
                checkBin()
            }
        } catch (e: Exception) {
            android.util.Log.e("CropItAll", "loadSavedFolder error: ${e.message}", e)
        }
    }

    fun onFolderSelected(uri: Uri?) {
        viewModelScope.launch {
            if (uri != null) {
                settingsRepository.saveSourceFolderUri(uri)
                _sourceFolderUri.value = uri
                checkBin()
            }
        }
    }

    fun checkBin() {
        val uri = _sourceFolderUri.value ?: return
        viewModelScope.launch {
            _binCount.value = fileRepository.getBinFilesCount(uri, "Корзина")
        }
    }

    fun clearBin() {
        val uri = _sourceFolderUri.value ?: return
        viewModelScope.launch {
            if (fileRepository.emptyBin(uri, "Корзина")) {
                _binCount.value = 0
            }
        }
    }

    fun scanFolder() {
        val uri = _sourceFolderUri.value ?: return
        viewModelScope.launch {
            _isScanning.value = true
            try {
                val files = fileRepository.scanFolder(uri)
                if (files.isEmpty()) {
                    _emptyFolderEvent.emit(Unit)
                } else {
                    _navigationToCrop.emit(Unit)
                }
            } catch (e: Exception) {
                _emptyFolderEvent.emit(Unit)
            } finally {
                _isScanning.value = false
            }
        }
    }

    fun saveWebinarName(name: String) {
        viewModelScope.launch { settingsRepository.saveBaseName(name) }
    }

    fun saveMode(deleteSources: Boolean) {
        viewModelScope.launch { settingsRepository.saveDeleteOriginalMode(deleteSources) }
    }

    suspend fun isTrainingCompleted(): Boolean {
        return try { settingsRepository.isTrainingCompleted() } catch (e: Exception) { false }
    }

    fun markTrainingCompleted() {
        viewModelScope.launch { settingsRepository.saveTrainingCompleted(true) }
    }

    suspend fun loadSavedWebinarName(): String {
        return try { settingsRepository.getBaseName() } catch (e: Exception) { "" }
    }

    suspend fun loadSavedMode(): Boolean {
        return try { settingsRepository.getDeleteOriginalMode() } catch (e: Exception) { false }
    }

    // НОВЫЙ МЕТОД: загрузка журнала последней операции
    suspend fun loadLastOperationSummary(): String? {
        return try { settingsRepository.getLastOperationSummary() } catch (e: Exception) { null }
    }

    companion object {
        fun Factory(context: android.content.Context): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
                        val fileRepository = com.mahksim.CropItAll.data.RepositoryProvider.fileRepository
                        val settingsRepository = com.mahksim.CropItAll.data.RepositoryProvider.settingsRepository
                        return MainViewModel(fileRepository, settingsRepository) as T
                    }
                    throw IllegalArgumentException("Unknown ViewModel class")
                }
            }
        }
    }
}

data class MainUiState(
    val folderUri: android.net.Uri? = null,
    val baseName: String = "",
    val deleteOriginalMode: Boolean = true,
    val isFirstLaunch: Boolean = true,
    val trainingCompleted: Boolean = false,
    val currentStep: Int = 1
)