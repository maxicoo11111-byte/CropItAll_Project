package com.mahksim.CropItAll

import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import com.mahksim.CropItAll.data.FileItem
import com.mahksim.CropItAll.data.RepositoryProvider
import com.mahksim.CropItAll.ui.crop.CropOverlayView
import com.mahksim.CropItAll.ui.crop.ImagePagerAdapter
import com.mahksim.CropItAll.util.BitmapUtils
import com.mahksim.CropItAll.util.FileUtils
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CropActivity : AppCompatActivity() {

    private lateinit var viewPager: ViewPager2
    private lateinit var cropOverlay: CropOverlayView
    private lateinit var adapter: ImagePagerAdapter
    private var webinarName: String = ""
    private var currentBitmapRect: android.graphics.Rect? = null
    private var isRestored = false
    private var isProcessing = false
    private lateinit var filesList: MutableList<FileItem>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_crop)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        webinarName = intent.getStringExtra("webinarName") ?: "Вебинар"
        filesList = RepositoryProvider.fileRepository.cachedFiles.toMutableList()

        if (filesList.isEmpty()) {
            finish()
            return
        }

        viewPager = findViewById(R.id.viewPager)
        cropOverlay = findViewById(R.id.cropOverlay)

        val firstUri = filesList[0].uri
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        try {
            contentResolver.openInputStream(firstUri)?.use {
                BitmapFactory.decodeStream(it, null, options)
            }
            cropOverlay.setBitmapSize(options.outWidth, options.outHeight)
        } catch (e: Exception) {
            android.util.Log.e("CropItAll", "Failed to get bitmap size", e)
        }

        cropOverlay.setOnCropRectChangedListener(object : CropOverlayView.OnCropRectChangedListener {
            override fun onCropRectChanged(viewRect: android.graphics.RectF, bitmapRect: android.graphics.Rect) {
                currentBitmapRect = bitmapRect
                if (isRestored) {
                    val rectStr = "${viewRect.left},${viewRect.top},${viewRect.right},${viewRect.bottom}"
                    lifecycleScope.launch {
                        RepositoryProvider.settingsRepository.saveCropRect(rectStr)
                    }
                }
            }
        })

        lifecycleScope.launch {
            val savedRectStr = RepositoryProvider.settingsRepository.getCropRect()
            if (savedRectStr != null) {
                val parts = savedRectStr.split(",")
                if (parts.size == 4) {
                    cropOverlay.setInitialRect(
                        parts[0].toFloat(), parts[1].toFloat(),
                        parts[2].toFloat(), parts[3].toFloat()
                    )
                    isRestored = true
                }
            }
            if (!isRestored) {
                cropOverlay.calculateDefaultRect()
                isRestored = true
            }
        }

        adapter = ImagePagerAdapter(
            context = this,
            files = filesList,
            scope = lifecycleScope,
            onFirstImageLoaded = { findViewById<View>(R.id.progressBar).visibility = View.GONE }
        )
        viewPager.adapter = adapter

        findViewById<View>(R.id.btnCropIt).visibility = View.VISIBLE
        findViewById<View>(R.id.btnCropAll).visibility = View.VISIBLE

        setupNavigation(filesList.size)

        findViewById<View>(R.id.btnDone).setOnClickListener { showExitDialog() }

        findViewById<View>(R.id.btnCropIt).setOnClickListener {
            if (isProcessing) return@setOnClickListener
            cropCurrentImage()
        }

        findViewById<View>(R.id.btnCropAll).setOnClickListener {
            if (isProcessing) return@setOnClickListener
            showCropAllDialog()
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() { showExitDialog() }
        })
    }

    private fun cropCurrentImage() {
        val rect = currentBitmapRect ?: return
        val currentPosition = viewPager.currentItem

        if (currentPosition >= filesList.size) return
        val currentFile = filesList[currentPosition]

        isProcessing = true
        lifecycleScope.launch {
            try {
                val sourceFolderUri = RepositoryProvider.settingsRepository.getSourceFolderUri() ?: return@launch
                val targetFolderUri = RepositoryProvider.fileRepository.createFolder(sourceFolderUri, webinarName) ?: return@launch

                val origSize = RepositoryProvider.fileRepository.getFileSize(currentFile.uri)

                val croppedBitmap = BitmapUtils.cropBitmap(this@CropActivity, currentFile.uri, rect)
                if (croppedBitmap != null) {
                    val nextIndex = FileUtils.getNextFileIndex(this@CropActivity, targetFolderUri)
                    val fileName = String.format(java.util.Locale.US, "%03d.jpg", nextIndex)

                    val savedUri = RepositoryProvider.fileRepository.saveImage(targetFolderUri, fileName, croppedBitmap)
                    croppedBitmap.recycle()

                    if (savedUri != null) {
                        val cropSize = RepositoryProvider.fileRepository.getFileSize(savedUri)
                        val saved = if (origSize > 0 && cropSize > 0) origSize - cropSize else 0L

                        // СОХРАНЯЕМ ЖУРНАЛ
                        val date = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date())
                        val summary = getString(R.string.journal_format, date, webinarName, 1, 0, FileUtils.formatSize(saved))
                        RepositoryProvider.settingsRepository.saveLastOperationSummary(summary)

                        RepositoryProvider.fileRepository.removeFileFromCache(currentFile.uri)
                        adapter.removeItem(currentPosition)

                        val deleteSources = RepositoryProvider.settingsRepository.getDeleteOriginalMode()
                        if (deleteSources) {
                            RepositoryProvider.fileRepository.deleteFile(currentFile.uri)
                        } else {
                            val binFolder = RepositoryProvider.fileRepository.createFolder(sourceFolderUri, "Корзина")
                            if (binFolder != null) {
                                RepositoryProvider.fileRepository.moveToBin(currentFile.uri, currentFile.name, binFolder)
                            }
                        }

                        Toast.makeText(this@CropActivity, "Сохранено: $fileName", Toast.LENGTH_SHORT).show()

                        if (adapter.itemCount == 0) {
                            Toast.makeText(this@CropActivity, "Все файлы обработаны", Toast.LENGTH_SHORT).show()
                            finish()
                        }
                    }
                }
            } finally {
                isProcessing = false
            }
        }
    }

    private fun showCropAllDialog() {
        AlertDialog.Builder(this)
            .setTitle(R.string.crop_all_confirm_title)
            .setMessage(getString(R.string.crop_all_confirm_msg, webinarName))
            .setPositiveButton(R.string.yes) { _, _ -> checkDeleteWarningAndProcess() }
            .setNegativeButton(R.string.no, null)
            .show()
    }

    private fun checkDeleteWarningAndProcess() {
        lifecycleScope.launch {
            val deleteSources = RepositoryProvider.settingsRepository.getDeleteOriginalMode()
            if (deleteSources) {
                AlertDialog.Builder(this@CropActivity)
                    .setTitle(R.string.delete_warning_title)
                    .setMessage(R.string.delete_warning_msg)
                    .setPositiveButton(R.string.yes) { _, _ -> processAllImages() }
                    .setNegativeButton(R.string.no, null)
                    .show()
            } else {
                processAllImages()
            }
        }
    }

    private fun processAllImages() {
        val startIndex = viewPager.currentItem
        val total = filesList.size - startIndex
        if (total <= 0) return

        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_progress, null)
        val tvProgress = dialogView.findViewById<TextView>(R.id.tvProgressText)
        val pbProgress = dialogView.findViewById<ProgressBar>(R.id.pbProgress)
        pbProgress.max = total

        val dialog = AlertDialog.Builder(this)
            .setTitle(R.string.processing_title)
            .setView(dialogView)
            .setCancelable(false)
            .create()
        dialog.show()

        isProcessing = true

        lifecycleScope.launch {
            val sourceFolderUri = RepositoryProvider.settingsRepository.getSourceFolderUri()
            val targetFolderUri = sourceFolderUri?.let { RepositoryProvider.fileRepository.createFolder(it, webinarName) }
            val deleteSources = RepositoryProvider.settingsRepository.getDeleteOriginalMode()
            val rect = currentBitmapRect

            if (targetFolderUri == null || rect == null) {
                dialog.dismiss()
                isProcessing = false
                return@launch
            }

            var processedCount = 0
            var croppedCount = 0
            var skippedCount = 0
            var originalSize = 0L
            var croppedSize = 0L

            val binFolderUri = if (!deleteSources) {
                sourceFolderUri?.let { RepositoryProvider.fileRepository.createFolder(it, "Корзина") }
            } else null

            val filesToProcess = filesList.subList(startIndex, filesList.size).toList()

            for (file in filesToProcess) {
                tvProgress.text = getString(R.string.processing_x_of_y, processedCount + 1, total)
                pbProgress.progress = processedCount + 1

                originalSize += RepositoryProvider.fileRepository.getFileSize(file.uri)

                val croppedBitmap = BitmapUtils.cropBitmap(this@CropActivity, file.uri, rect)
                if (croppedBitmap != null) {
                    val nextIndex = FileUtils.getNextFileIndex(this@CropActivity, targetFolderUri)
                    val fileName = String.format(java.util.Locale.US, "%03d.jpg", nextIndex)
                    val saved = RepositoryProvider.fileRepository.saveImage(targetFolderUri, fileName, croppedBitmap)
                    croppedBitmap.recycle()

                    if (saved != null) {
                        croppedCount++
                        val actualSize = RepositoryProvider.fileRepository.getFileSize(saved)
                        if (actualSize > 0) croppedSize += actualSize

                        RepositoryProvider.fileRepository.removeFileFromCache(file.uri)

                        if (deleteSources) {
                            RepositoryProvider.fileRepository.deleteFile(file.uri)
                        } else if (binFolderUri != null) {
                            RepositoryProvider.fileRepository.moveToBin(file.uri, file.name, binFolderUri)
                        }
                    } else {
                        skippedCount++
                    }
                } else {
                    skippedCount++
                }
                processedCount++
            }

            // СОХРАНЯЕМ ЖУРНАЛ
            val date = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date())
            val freed = originalSize - croppedSize
            val summary = getString(R.string.journal_format, date, webinarName, croppedCount, skippedCount, FileUtils.formatSize(freed))
            RepositoryProvider.settingsRepository.saveLastOperationSummary(summary)

            filesList.clear()
            adapter.notifyDataSetChanged()

            dialog.dismiss()
            isProcessing = false
            showReportDialog(croppedCount, total, skippedCount, originalSize, croppedSize, deleteSources)
        }
    }

    private fun showReportDialog(cropped: Int, total: Int, skipped: Int, origSize: Long, cropSize: Long, deleted: Boolean) {
        val freed = origSize - cropSize
        val message = buildString {
            append(getString(R.string.report_cropped, cropped, total))
            append("\n")
            append(getString(R.string.report_skipped, skipped))
            append("\n\n")
            append(getString(R.string.report_sources_size, FileUtils.formatSize(origSize)))
            append("\n")
            append(getString(R.string.report_cropped_size, FileUtils.formatSize(cropSize)))
            append("\n\n")
            if (deleted) {
                append(getString(R.string.report_freed, FileUtils.formatSize(freed)))
            } else {
                append(getString(R.string.report_freed_bin, FileUtils.formatSize(freed)))
            }
        }
        AlertDialog.Builder(this)
            .setTitle(R.string.report_title)
            .setMessage(message)
            .setPositiveButton(android.R.string.ok) { _, _ -> finish() }
            .show()
    }

    private fun setupNavigation(totalCount: Int) {
        val btnFirst = findViewById<View>(R.id.btnFirst)
        val btnPrev = findViewById<View>(R.id.btnPrev)
        val btnNext = findViewById<View>(R.id.btnNext)
        val btnLast = findViewById<View>(R.id.btnLast)

        btnFirst.setOnClickListener { viewPager.setCurrentItem(0, true) }
        btnPrev.setOnClickListener { if (viewPager.currentItem > 0) viewPager.setCurrentItem(viewPager.currentItem - 1, true) }
        btnNext.setOnClickListener { if (viewPager.currentItem < totalCount - 1) viewPager.setCurrentItem(viewPager.currentItem + 1, true) }
        btnLast.setOnClickListener { viewPager.setCurrentItem(totalCount - 1, true) }

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                btnFirst.isEnabled = position > 0
                btnPrev.isEnabled = position > 0
                btnNext.isEnabled = position < filesList.size - 1
                btnLast.isEnabled = position < filesList.size - 1
            }
        })
    }

    private fun showExitDialog() {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.exit_crop_title, webinarName))
            .setMessage(R.string.exit_crop_message)
            .setPositiveButton(R.string.exit) { _, _ -> finish() }
            .setNegativeButton(R.string.new_webinar) { _, _ -> showNewWebinarDialog() }
            .show()
    }

    private fun showNewWebinarDialog() {
        val input = EditText(this).apply {
            setText(webinarName)
            setSelection(text.length)
            val padding = (16 * resources.displayMetrics.density).toInt()
            setPadding(padding, padding, padding, padding)
            hint = getString(R.string.webinar_name_hint)
        }

        val container = FrameLayout(this).apply {
            val padding = (24 * resources.displayMetrics.density).toInt()
            setPadding(padding, 0, padding, 0)
            addView(input)
        }

        AlertDialog.Builder(this)
            .setTitle(R.string.new_webinar_title)
            .setMessage(R.string.new_webinar_msg)
            .setView(container)
            .setPositiveButton(R.string.start) { _, _ ->
                val newName = input.text.toString().trim()
                if (newName.isEmpty()) {
                    Toast.makeText(this, R.string.enter_webinar_name, Toast.LENGTH_SHORT).show()
                } else {
                    webinarName = newName
                    lifecycleScope.launch {
                        RepositoryProvider.settingsRepository.saveBaseName(newName)
                    }
                    Toast.makeText(this, "Вебинар изменён на: $newName", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton(R.string.no, null)
            .show()
    }
}