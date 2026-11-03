package com.mahksim.CropItAll.data

import android.net.Uri

/**
 * Интерфейс репозитория настроек через DataStore.
 */
interface SettingsRepository {

    /**
     * Сохраняет URI исходной папки.
     *
     * @param treeUri URI дерева папки
     */
    suspend fun saveSourceFolderUri(treeUri: Uri)

    /**
     * Получает сохранённый URI исходной папки.
     *
     * @return URI дерева папки или null
     */
    suspend fun getSourceFolderUri(): Uri?

    /**
     * Сохраняет название вебинара.
     *
     * @param baseName Название вебинара
     */
    suspend fun saveBaseName(baseName: String)

    /**
     * Получает сохранённое название вебинара.
     *
     * @return Название вебинара или пустая строка
     */
    suspend fun getBaseName(): String

    /**
     * Сохраняет режим удаления файлов.
     *
     * @param deleteOriginal true если удалять исходники, false если перемещать в корзину
     */
    suspend fun saveDeleteOriginalMode(deleteOriginal: Boolean)

    /**
     * Получает режим удаления файлов.
     *
     * @return true если удалять исходники, false если перемещать в корзину
     */
    suspend fun getDeleteOriginalMode(): Boolean

    /**
     * Сохраняет флаг первого запуска.
     *
     * @param firstLaunch true если первый запуск
     */
    suspend fun saveFirstLaunch(firstLaunch: Boolean)

    /**
     * Получает флаг первого запуска.
     *
     * @return true если первый запуск
     */
    suspend fun isFirstLaunch(): Boolean

    /**
     * Сохраняет состояние обучения.
     *
     * @param trainingCompleted true если обучение завершено
     */
    suspend fun saveTrainingCompleted(trainingCompleted: Boolean)

    /**
     * Получает состояние обучения.
     *
     * @return true если обучение завершено
     */
    suspend fun isTrainingCompleted(): Boolean

    /**
     * Сохраняет URI сохранённой рамки обрезки.
     *
     * @param rectUri URI сохранённой рамки
     */
    suspend fun saveCropRect(rect: String)

    /**
     * Получает URI сохранённой рамки обрезки.
     *
     * @return URI сохранённой рамки или null
     */
    suspend fun getCropRect(): String?

    /**
     * Очищает URI исходной папки.
     */
    suspend fun clearSourceFolderUri()

    /**
     * Очищает все сохранённые данные.
     */
    suspend fun clearAll()
    suspend fun saveLastOperationSummary(summary: String)
    suspend fun getLastOperationSummary(): String?
}