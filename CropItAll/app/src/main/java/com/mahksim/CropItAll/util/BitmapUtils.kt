package com.mahksim.CropItAll.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Rect
import android.net.Uri

/**
 * Утилиты для работы с Bitmap.
 */
object BitmapUtils {

    /**
     * Загружает оригинальный Bitmap из Uri и обрезает его по заданному Rect.
     */
    fun cropBitmap(context: Context, uri: Uri, cropRect: Rect): Bitmap? {
        return try {
            // 1. Загружаем оригинал
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = false }
            val originalBitmap = context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, options)
            } ?: return null

            // 2. Защита от выхода за границы (на случай багов с координатами)
            val safeLeft = cropRect.left.coerceIn(0, originalBitmap.width - 1)
            val safeTop = cropRect.top.coerceIn(0, originalBitmap.height - 1)
            val safeRight = cropRect.right.coerceIn(safeLeft + 1, originalBitmap.width)
            val safeBottom = cropRect.bottom.coerceIn(safeTop + 1, originalBitmap.height)

            val safeWidth = safeRight - safeLeft
            val safeHeight = safeBottom - safeTop

            // 3. Вырезаем
            val croppedBitmap = Bitmap.createBitmap(
                originalBitmap,
                safeLeft,
                safeTop,
                safeWidth,
                safeHeight
            )

            // Освобождаем память, если это разные объекты
            if (croppedBitmap != originalBitmap) {
                originalBitmap.recycle()
            }

            croppedBitmap
        } catch (e: Exception) {
            android.util.Log.e("CropItAll", "cropBitmap error: ${e.message}", e)
            null
        }
    }
}