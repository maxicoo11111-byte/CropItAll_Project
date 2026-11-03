package com.mahksim.CropItAll

import android.app.Application
import com.mahksim.CropItAll.data.RepositoryProvider

/**
 * Application-класс для инициализации синглтона репозиториев.
 */
class CropItAllApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        RepositoryProvider.init(this)
    }
}