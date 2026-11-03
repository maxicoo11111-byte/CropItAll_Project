package com.mahksim.CropItAll.util

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract

/**
 * Утилиты для работы с именами файлов.
 */
object FileUtils {

    /**
     * Вычисляет следующий свободный индекс для файла формата "001.jpg".
     * Читает содержимое папки и ищет максимальный номер.
     */
    fun getNextFileIndex(context: Context, treeUri: Uri): Int {
        val regex = Regex("""^(\d{3})""")
        var maxIndex = 0

        try {
            // Безопасно получаем ID документа: если это documentUri - getDocumentId, иначе getTreeDocumentId
            val documentId = try {
                DocumentsContract.getDocumentId(treeUri)
            } catch (e: Exception) {
                DocumentsContract.getTreeDocumentId(treeUri)
            }

            val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(
                treeUri,
                documentId
            )
            val projection = arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME)

            context.contentResolver.query(childrenUri, projection, null, null, null)?.use { cursor ->
                while (cursor.moveToNext()) {
                    val name = cursor.getString(0) ?: continue
                    val match = regex.find(name)
                    if (match != null) {
                        val index = match.groupValues[1].toIntOrNull() ?: 0
                        if (index > maxIndex) {
                            maxIndex = index
                        }
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("CropItAll", "getNextFileIndex error: ${e.message}", e)
        }
        return maxIndex + 1
    }

    /** Форматирует байты в КБ или МБ для отчета. */
    fun formatSize(bytes: Long): String {
        val absBytes = Math.abs(bytes)
        if (absBytes < 1024) return "$absBytes B"
        val kb = absBytes / 1024.0
        if (kb < 1024) return String.format(java.util.Locale.US, "%.1f KB", kb)
        val mb = kb / 1024.0
        return String.format(java.util.Locale.US, "%.1f MB", mb)
    }
}