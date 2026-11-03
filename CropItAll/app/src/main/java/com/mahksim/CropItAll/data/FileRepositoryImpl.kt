package com.mahksim.CropItAll.data

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.provider.DocumentsContract
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Реализация репозитория файлов через SAF.
 */
class FileRepositoryImpl(
    private val context: Context
) : FileRepository {

    override var cachedFiles: List<FileItem> = emptyList()
        private set

    override suspend fun scanFolder(treeUri: Uri): List<FileItem> = withContext(Dispatchers.IO) {
        try {
            val imageExtensions = setOf(".jpg", ".jpeg", ".png")
            val files = mutableListOf<FileItem>()

            val childUri = DocumentsContract.buildChildDocumentsUriUsingTree(
                treeUri,
                DocumentsContract.getTreeDocumentId(treeUri)
            )

            val projection = arrayOf(
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                DocumentsContract.Document.COLUMN_SIZE,
                DocumentsContract.Document.COLUMN_LAST_MODIFIED,
                DocumentsContract.Document.COLUMN_MIME_TYPE
            )
            val sort = "${DocumentsContract.Document.COLUMN_LAST_MODIFIED} ASC"

            context.contentResolver.query(childUri, projection, null, null, sort)?.use { cursor ->
                while (cursor.moveToNext()) {
                    val documentId = cursor.getString(0)
                    val name = cursor.getString(1)
                    val size = cursor.getLong(2)
                    val lastModified = cursor.getLong(3)
                    val mimeType = cursor.getString(4)

                    val isImage = mimeType?.startsWith("image/") == true
                    val hasImageExtension = imageExtensions.any { name.lowercase().endsWith(it) }

                    if (isImage || hasImageExtension) {
                        val fileUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, documentId)
                        files.add(FileItem(fileUri, name, size, lastModified))
                    }
                }
            }
            cachedFiles = files
            files
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun createFolder(treeUri: Uri, folderName: String): Uri? = withContext(Dispatchers.IO) {
        try {
            val parentDocumentId = DocumentsContract.getTreeDocumentId(treeUri)
            val parentUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, parentDocumentId)

            val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, parentDocumentId)
            val cursor = context.contentResolver.query(
                childrenUri,
                arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID, DocumentsContract.Document.COLUMN_DISPLAY_NAME, DocumentsContract.Document.COLUMN_MIME_TYPE),
                null, null, null
            )
            cursor?.use {
                while (it.moveToNext()) {
                    val name = it.getString(1)
                    val mime = it.getString(2)
                    if (name == folderName && mime == DocumentsContract.Document.MIME_TYPE_DIR) {
                        val id = it.getString(0)
                        return@withContext DocumentsContract.buildDocumentUriUsingTree(treeUri, id)
                    }
                }
            }

            DocumentsContract.createDocument(
                context.contentResolver,
                parentUri,
                DocumentsContract.Document.MIME_TYPE_DIR,
                folderName
            )
        } catch (e: Exception) {
            android.util.Log.e("CropItAll", "createFolder error", e)
            null
        }
    }

    override suspend fun saveImage(folderUri: Uri, fileName: String, bitmap: Bitmap): Uri? = withContext(Dispatchers.IO) {
        try {
            val fileUri = DocumentsContract.createDocument(
                context.contentResolver,
                folderUri,
                "image/jpeg",
                fileName
            )
            if (fileUri != null) {
                context.contentResolver.openOutputStream(fileUri)?.use { out ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
                }
                fileUri
            } else null
        } catch (e: Exception) {
            android.util.Log.e("CropItAll", "saveImage error", e)
            null
        }
    }

    override suspend fun getFileSize(uri: Uri): Long = withContext(Dispatchers.IO) {
        try {
            val projection = arrayOf(DocumentsContract.Document.COLUMN_SIZE)
            context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) return@withContext cursor.getLong(0)
            }
            -1L
        } catch (e: Exception) { -1L }
    }

    override suspend fun deleteFile(uri: Uri): Boolean = withContext(Dispatchers.IO) {
        try { DocumentsContract.deleteDocument(context.contentResolver, uri) } catch (e: Exception) { false }
    }

    override suspend fun moveToBin(sourceUri: Uri, sourceName: String, binFolderUri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            val newUri = DocumentsContract.createDocument(
                context.contentResolver,
                binFolderUri,
                "image/jpeg",
                sourceName
            )
            if (newUri != null) {
                context.contentResolver.openInputStream(sourceUri)?.use { input ->
                    context.contentResolver.openOutputStream(newUri)?.use { output ->
                        input.copyTo(output)
                    }
                }
                DocumentsContract.deleteDocument(context.contentResolver, sourceUri)
                true
            } else false
        } catch (e: Exception) {
            android.util.Log.e("CropItAll", "moveToBin error: ${e.message}", e)
            false
        }
    }

    override fun removeFileFromCache(fileUri: Uri) {
        cachedFiles = cachedFiles.filter { it.uri != fileUri }
    }

    override suspend fun getBinFilesCount(treeUri: Uri, binName: String): Int = withContext(Dispatchers.IO) {
        try {
            val parentDocumentId = DocumentsContract.getTreeDocumentId(treeUri)
            val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, parentDocumentId)

            var binUri: Uri? = null
            context.contentResolver.query(
                childrenUri,
                arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID, DocumentsContract.Document.COLUMN_DISPLAY_NAME, DocumentsContract.Document.COLUMN_MIME_TYPE),
                null, null, null
            )?.use { cursor ->
                while (cursor.moveToNext()) {
                    val name = cursor.getString(1)
                    val mime = cursor.getString(2)
                    if (name == binName && mime == DocumentsContract.Document.MIME_TYPE_DIR) {
                        val id = cursor.getString(0)
                        binUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, id)
                        break
                    }
                }
            }

            if (binUri == null) return@withContext 0

            val binChildrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(binUri!!, DocumentsContract.getDocumentId(binUri!!))
            var count = 0
            context.contentResolver.query(binChildrenUri, arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID), null, null, null)?.use { cursor ->
                count = cursor.count
            }
            count
        } catch (e: Exception) {
            0
        }
    }

    override suspend fun emptyBin(treeUri: Uri, binName: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val parentDocumentId = DocumentsContract.getTreeDocumentId(treeUri)
            val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, parentDocumentId)

            var binUri: Uri? = null
            context.contentResolver.query(
                childrenUri,
                arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID, DocumentsContract.Document.COLUMN_DISPLAY_NAME, DocumentsContract.Document.COLUMN_MIME_TYPE),
                null, null, null
            )?.use { cursor ->
                while (cursor.moveToNext()) {
                    val name = cursor.getString(1)
                    val mime = cursor.getString(2)
                    if (name == binName && mime == DocumentsContract.Document.MIME_TYPE_DIR) {
                        val id = cursor.getString(0)
                        binUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, id)
                        break
                    }
                }
            }

            if (binUri != null) {
                // Удаляем папку рекурсивно (SAF удаляет папку и ее содержимое)
                DocumentsContract.deleteDocument(context.contentResolver, binUri!!)
                true
            } else true
        } catch (e: Exception) {
            android.util.Log.e("CropItAll", "emptyBin error: ${e.message}", e)
            false
        }
    }
}