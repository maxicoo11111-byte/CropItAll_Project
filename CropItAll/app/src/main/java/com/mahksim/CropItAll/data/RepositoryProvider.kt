package com.mahksim.CropItAll.data

import android.content.Context

/**
 * Синглтон для доступа к репозиториям.
 * Инициализируется в CropItAllApplication.
 */
object RepositoryProvider {
    lateinit var fileRepository: FileRepository
        private set

    lateinit var settingsRepository: SettingsRepository
        private set

    fun init(context: Context) {
        if (!::fileRepository.isInitialized) {
            fileRepository = FileRepositoryImpl(context.applicationContext)
        }
        if (!::settingsRepository.isInitialized) {
            settingsRepository = SettingsRepositoryImpl(context.applicationContext)
        }
    }
}