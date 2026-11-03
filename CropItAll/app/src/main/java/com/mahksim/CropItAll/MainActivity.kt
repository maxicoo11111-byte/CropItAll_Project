package com.mahksim.CropItAll

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.mahksim.CropItAll.ui.main.MainViewModel
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private val viewModel: MainViewModel by viewModels {
        MainViewModel.Factory(applicationContext)
    }

    private var isBinDialogShowing = false

    private val folderPickerLauncher =
        registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri: Uri? ->
            if (uri != null) {
                try {
                    contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    )
                } catch (e: Exception) { }
                viewModel.onFolderSelected(uri)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(0, systemBars.top, 0, systemBars.bottom)
            insets
        }

        findViewById<View>(R.id.btnSelectFolder).setOnClickListener {
            folderPickerLauncher.launch(null)
        }

        lifecycleScope.launch {
            viewModel.sourceFolderUri.collect { uri ->
                updateFrameButtonState()
            }
        }

        lifecycleScope.launch {
            viewModel.navigationToCrop.collect {
                val name = findViewById<EditText>(R.id.etWebinarName).text.toString()
                val intent = Intent(this@MainActivity, CropActivity::class.java)
                intent.putExtra("webinarName", name)
                startActivity(intent)
                viewModel.markTrainingCompleted()
            }
        }

        lifecycleScope.launch {
            viewModel.emptyFolderEvent.collect {
                Toast.makeText(this@MainActivity, R.string.empty_folder, Toast.LENGTH_SHORT).show()
            }
        }

        lifecycleScope.launch {
            viewModel.isScanning.collect { isScanning ->
                findViewById<View>(R.id.progressBar).visibility = if (isScanning) View.VISIBLE else View.GONE
                findViewById<View>(R.id.tvScanning).visibility = if (isScanning) View.VISIBLE else View.GONE
                updateFrameButtonState()
            }
        }

        lifecycleScope.launch {
            viewModel.binCount.collect { count ->
                if (count > 0 && !isBinDialogShowing) {
                    showBinCleanupDialog(count)
                }
            }
        }

        findViewById<EditText>(R.id.etWebinarName).addTextChangedListener(
            object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    updateFrameButtonState()
                }
                override fun afterTextChanged(s: Editable?) {}
            }
        )

        findViewById<View>(R.id.btnFrame).setOnClickListener {
            val name = findViewById<EditText>(R.id.etWebinarName).text.toString()
            viewModel.saveWebinarName(name)
            val deleteSources = findViewById<RadioButton>(R.id.rbDeleteSources).isChecked
            viewModel.saveMode(deleteSources)
            viewModel.scanFolder()
        }

        findViewById<RadioGroup>(R.id.rgMode).setOnCheckedChangeListener { _, checkedId ->
            val deleteSources = checkedId == R.id.rbDeleteSources
            viewModel.saveMode(deleteSources)
        }

        lifecycleScope.launch {
            val trainingCompleted = viewModel.isTrainingCompleted()
            if (!trainingCompleted) {
                findViewById<View>(R.id.tvInfo1).visibility = View.VISIBLE
                findViewById<View>(R.id.tvHintSelect).visibility = View.VISIBLE
                findViewById<View>(R.id.tvHintFrame).visibility = View.VISIBLE
            }
            val savedName = viewModel.loadSavedWebinarName()
            if (savedName.isNotEmpty()) {
                findViewById<EditText>(R.id.etWebinarName).setText(savedName)
            }
            val savedMode = viewModel.loadSavedMode()
            if (savedMode) {
                findViewById<RadioButton>(R.id.rbDeleteSources).isChecked = true
            } else {
                findViewById<RadioButton>(R.id.rbMoveToBin).isChecked = true
            }
        }
    }

    // НОВЫЙ МЕТОД: обновляет журнал (поле И2) при возвращении с экрана обрезки
    override fun onResume() {
        super.onResume()
        lifecycleScope.launch {
            val summary = viewModel.loadLastOperationSummary()
            val tvInfo2 = findViewById<TextView>(R.id.tvInfo2)
            if (!summary.isNullOrEmpty()) {
                tvInfo2.text = summary
                tvInfo2.visibility = View.VISIBLE
            } else {
                tvInfo2.visibility = View.GONE
            }
        }
    }

    private fun updateFrameButtonState() {
        val hasUri = viewModel.sourceFolderUri.value != null
        val hasName = findViewById<EditText>(R.id.etWebinarName).text.isNotEmpty()
        val isScanning = viewModel.isScanning.value
        findViewById<View>(R.id.btnFrame).isEnabled = hasUri && hasName && !isScanning

        val stepText = when {
            !hasUri -> getString(R.string.info_step_1)
            !hasName -> getString(R.string.info_step_2)
            else -> getString(R.string.info_step_3)
        }
        findViewById<TextView>(R.id.tvInfo1).text = stepText
    }

    private fun showBinCleanupDialog(count: Int) {
        isBinDialogShowing = true
        AlertDialog.Builder(this)
            .setTitle(R.string.bin_cleanup_title)
            .setMessage(getString(R.string.bin_cleanup_msg, count))
            .setPositiveButton(R.string.yes) { _, _ ->
                viewModel.clearBin()
                isBinDialogShowing = false
            }
            .setNegativeButton(R.string.no) { _, _ ->
                isBinDialogShowing = false
            }
            .setOnCancelListener {
                isBinDialogShowing = false
            }
            .show()
    }
}