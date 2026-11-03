package com.mahksim.CropItAll.ui.crop

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.mahksim.CropItAll.R
import com.mahksim.CropItAll.data.FileItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Адаптер для ViewPager2 — показывает изображения.
 * Поддерживает удаление элементов.
 */
class ImagePagerAdapter(
    private val context: Context,
    val files: MutableList<FileItem>, // MutableList для удаления
    private val scope: CoroutineScope,
    private val onFirstImageLoaded: () -> Unit
) : RecyclerView.Adapter<ImagePagerAdapter.ImageViewHolder>() {

    private var firstImageLoaded = false
    private val reqWidth: Int = context.resources.displayMetrics.widthPixels
    private val reqHeight: Int = context.resources.displayMetrics.heightPixels

    class ImageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imageView: ImageView = view.findViewById(R.id.ivImage)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val view = LayoutInflater.from(context)
            .inflate(R.layout.item_image, parent, false)
        return ImageViewHolder(view)
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        val file = files[position]
        loadBitmap(file.uri, holder.imageView, position)
    }

    override fun getItemCount(): Int = files.size

    /**
     * Удаляет файл из списка по позиции.
     */
    fun removeItem(position: Int) {
        if (position in files.indices) {
            files.removeAt(position)
            notifyItemRemoved(position)
            notifyItemRangeChanged(position, files.size - position)
        }
    }

    private fun loadBitmap(uri: Uri, imageView: ImageView, position: Int) {
        scope.launch {
            val bitmap = withContext(Dispatchers.IO) {
                decodeSampledBitmap(uri, reqWidth, reqHeight)
            }
            if (bitmap != null) {
                imageView.setImageBitmap(bitmap)
            } else {
                imageView.setImageResource(android.R.drawable.ic_menu_gallery)
            }
            if (position == 0 && !firstImageLoaded) {
                firstImageLoaded = true
                onFirstImageLoaded()
            }
        }
    }

    private fun decodeSampledBitmap(uri: Uri, reqW: Int, reqH: Int): Bitmap? {
        return try {
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, options)
            }
            options.inSampleSize = calculateInSampleSize(options, reqW, reqH)
            options.inJustDecodeBounds = false
            context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, options)
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqW: Int, reqH: Int): Int {
        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1
        if (height > reqH || width > reqW) {
            val halfHeight = height / 2
            val halfWidth = width / 2
            while ((halfHeight / inSampleSize) >= reqH && (halfWidth / inSampleSize) >= reqW) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }
}