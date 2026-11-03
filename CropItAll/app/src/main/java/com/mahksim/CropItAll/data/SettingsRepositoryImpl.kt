package com.mahksim.CropItAll.data

import android.content.Context
import android.net.Uri
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * Extension property для DataStore.
 * ВАЖНО: объявлено на верхнем уровне файла (вне класса) — так требует Kotlin.
 */
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/**
 * Реализация репозитория настроек через DataStore.
 */
class SettingsRepositoryImpl(
    private val context: Context
) : SettingsRepository {

    // Ключи настроек
    private val SOURCE_FOLDER_URI_KEY = stringPreferencesKey("source_folder_uri")
    private val BASE_NAME_KEY = stringPreferencesKey("base_name")
    private val DELETE_ORIGINAL_MODE_KEY = booleanPreferencesKey("delete_original_mode")
    private val FIRST_LAUNCH_KEY = booleanPreferencesKey("first_launch")
    private val TRAINING_COMPLETED_KEY = booleanPreferencesKey("training_completed")
    // Убедитесь, что ключ объявлен как String
    private val CROP_RECT_KEY = stringPreferencesKey("crop_rect")
    private val LAST_OP_SUMMARY_KEY = stringPreferencesKey("last_operation_summary")

    override suspend fun saveLastOperationSummary(summary: String) {
        context.dataStore.edit { preferences ->
            preferences[LAST_OP_SUMMARY_KEY] = summary
        }
    }

    override suspend fun getLastOperationSummary(): String? {
        return context.dataStore.data.map { preferences ->
            preferences[LAST_OP_SUMMARY_KEY]
        }.first()
    }

    // === Чтение/запись URI исходной папки ===

    override suspend fun saveSourceFolderUri(treeUri: Uri) {
        context.dataStore.edit { preferences ->
            preferences[SOURCE_FOLDER_URI_KEY] = treeUri.toString()
        }
    }

    override suspend fun getSourceFolderUri(): Uri? {
        return context.dataStore.data.map { preferences ->
            preferences[SOURCE_FOLDER_URI_KEY]?.let { Uri.parse(it) }
        }.first()
    }

    override suspend fun clearSourceFolderUri() {
        context.dataStore.edit { preferences ->
            preferences.remove(SOURCE_FOLDER_URI_KEY)
        }
    }

    // === Чтение/запись названия вебинара ===

    override suspend fun saveBaseName(baseName: String) {
        context.dataStore.edit { preferences ->
            preferences[BASE_NAME_KEY] = baseName
        }
    }

    override suspend fun getBaseName(): String {
        return context.dataStore.data.map { preferences ->
            preferences[BASE_NAME_KEY] ?: ""
        }.first()
    }

    // === Чтение/запись режима удаления ===

    override suspend fun saveDeleteOriginalMode(deleteOriginal: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[DELETE_ORIGINAL_MODE_KEY] = deleteOriginal
        }
    }

    override suspend fun getDeleteOriginalMode(): Boolean {
        return context.dataStore.data.map { preferences ->
            preferences[DELETE_ORIGINAL_MODE_KEY] ?: true
        }.first()
    }

    // === Чтение/запись флага первого запуска ===

    override suspend fun saveFirstLaunch(firstLaunch: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[FIRST_LAUNCH_KEY] = firstLaunch
        }
    }

    override suspend fun isFirstLaunch(): Boolean {
        return context.dataStore.data.map { preferences ->
            preferences[FIRST_LAUNCH_KEY] ?: true
        }.first()
    }

    // === Чтение/запись флага завершения обучения ===

    override suspend fun saveTrainingCompleted(trainingCompleted: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[TRAINING_COMPLETED_KEY] = trainingCompleted
        }
    }

    override suspend fun isTrainingCompleted(): Boolean {
        return context.dataStore.data.map { preferences ->
            preferences[TRAINING_COMPLETED_KEY] ?: false
        }.first()
    }

    // === Чтение/запись прямоугольника обрезки ===

    override suspend fun saveCropRect(rect: String) {
        context.dataStore.edit { preferences ->
            preferences[CROP_RECT_KEY] = rect
        }
    }

    override suspend fun getCropRect(): String? {
        return context.dataStore.data.map { preferences ->
            preferences[CROP_RECT_KEY]
        }.first()
    }

    // === Очистка ===

    override suspend fun clearAll() {
        context.dataStore.edit { preferences ->
            preferences.clear()
        }
    }
}