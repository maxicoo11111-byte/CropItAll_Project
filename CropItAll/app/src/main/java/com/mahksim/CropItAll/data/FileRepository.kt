package com.mahksim.CropItAll.data

import android.graphics.Bitmap
import android.net.Uri

/**
 * Интерфейс репозитория для работы с файлами через SAF.
 */
interface FileRepository {

    val cachedFiles: List<FileItem>

    suspend fun scanFolder(treeUri: Uri): List<FileItem>

    suspend fun getFileSize(uri: Uri): Long

    suspend fun deleteFile(uri: Uri): Boolean

    suspend fun moveToBin(sourceUri: Uri, sourceName: String, binFolderUri: Uri): Boolean

    suspend fun createFolder(treeUri: Uri, folderName: String): Uri?

    suspend fun saveImage(folderUri: Uri, fileName: String, bitmap: Bitmap): Uri?

    fun removeFileFromCache(fileUri: Uri)

    /** Возвращает количество файлов в папке Корзина. */
    suspend fun getBinFilesCount(treeUri: Uri, binName: String): Int

    /** Удаляет папку Корзина целиком. */
    suspend fun emptyBin(treeUri: Uri, binName: String): Boolean
}