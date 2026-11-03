package com.mahksim.CropItAll.data

/**
 * Данные о файле.
 *
 * @property uri URI файла через SAF
 * @property name Имя файла
 * @property size Размер файла в байтах
 * @property lastModified Время последнего изменения в миллисекундах
 */
data class FileItem(
    val uri: android.net.Uri,
    val name: String,
    val size: Long,
    val lastModified: Long
)