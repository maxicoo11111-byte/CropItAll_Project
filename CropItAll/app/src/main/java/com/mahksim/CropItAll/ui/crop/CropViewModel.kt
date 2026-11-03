package com.mahksim.CropItAll.ui.crop

import android.graphics.RectF
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mahksim.CropItAll.data.FileRepository
import com.mahksim.CropItAll.data.FileItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * ViewModel для экрана обрезки.
 */
class CropViewModel(
    private val fileRepository: FileRepository
) : ViewModel() {

    /**
     * Состояние экрана обрезки.
     */
    private val _uiState = MutableStateFlow(CropUiState())
    val uiState: StateFlow<CropUiState> = _uiState.asStateFlow()

    /**
     * Список файлов для обработки.
     */
    private var files: List<FileItem> = emptyList()

    /**
     * Индекс текущего файла.
     */
    private var currentIndex: Int = 0

    /**
     * Сохранённая рамка обрезки.
     */
    private var savedCropRect: RectF? = null

    /**
     * Инициализация ViewModel.
     *
     * @param files Список файлов для обработки
     */
    fun init(files: List<FileItem>, startIndex: Int = 0) {
        this.files = files
        this.currentIndex = startIndex
        this.savedCropRect = null
        updateState()
    }

    /**
     * Сохраняет текущую рамку обрезки.
     *
     * @param rectF Прямоугольник рамки
     */
    fun saveCropRect(rectF: RectF) {
        savedCropRect = rectF
        // TODO: Сохранить в DataStore через SettingsRepository
    }

    /**
     * Восстанавливает сохранённую рамку обрезки.
     *
     * @return Сохранённая рамка или null
     */
    fun restoreCropRect(): RectF? {
        // TODO: Загрузить из DataStore через SettingsRepository
        return savedCropRect
    }

    /**
     * Проверяет, есть ли выделенная рамка.
     *
     * @return true если рамка выделена
     */
    fun hasSelectedCropRect(): Boolean {
        return savedCropRect != null
    }

    /**
     * Устанавливает текущий индекс файла.
     *
     * @param index Индекс файла
     */
    fun setCurrentIndex(index: Int) {
        currentIndex = index
        updateState()
    }

    /**
     * Возвращает URI текущего файла.
     *
     * @return URI текущего файла или null
     */
    fun getCurrentFileUri(): android.net.Uri? {
        return files.getOrNull(currentIndex)?.uri
    }

    /**
     * Возвращает имя текущего файла.
     *
     * @return Имя файла или null
     */
    fun getCurrentFileName(): String? {
        return files.getOrNull(currentIndex)?.name
    }

    /**
     * Возвращает текущий индекс.
     *
     * @return Текущий индекс
     */
    fun getCurrentIndex(): Int = currentIndex

    /**
     * Возвращает общее количество файлов.
     *
     * @return Общее количество файлов
     */
    fun getTotalFiles(): Int = files.size

    /**
     * Проверяет, есть ли следующий файл.
     *
     * @return true если следующий файл существует
     */
    fun hasNext(): Boolean = currentIndex < files.size - 1

    /**
     * Проверяет, есть ли предыдущий файл.
     *
     * @return true если предыдущий файл существует
     */
    fun hasPrevious(): Boolean = currentIndex > 0

    /**
     * Возвращает следующий файл.
     *
     * @return URI следующего файла или null
     */
    fun getNextFileUri(): android.net.Uri? {
        if (currentIndex < files.size - 1) {
            return files[currentIndex + 1].uri
        }
        return null
    }

    /**
     * Возвращает предыдущий файл.
     *
     * @return URI предыдущего файла или null
     */
    fun getPreviousFileUri(): android.net.Uri? {
        if (currentIndex > 0) {
            return files[currentIndex - 1].uri
        }
        return null
    }

    /**
     * Обновляет состояние UI.
     */
    private fun updateState() {
        _uiState.value = CropUiState(
            currentFileIndex = currentIndex,
            totalFiles = files.size,
            hasSelectedCropRect = savedCropRect != null
        )
    }

    /**
     * Очищает сохранённую рамку.
     */
    fun clearSelectedCropRect() {
        savedCropRect = null
        updateState()
    }
}

/**
 * Состояние экрана обрезки.
 *
 * @property currentFileIndex Текущий индекс файла
 * @property totalFiles Общее количество файлов
 * @property hasSelectedCropRect Есть ли выделенная рамка
 */
data class CropUiState(
    val currentFileIndex: Int = 0,
    val totalFiles: Int = 0,
    val hasSelectedCropRect: Boolean = false
)