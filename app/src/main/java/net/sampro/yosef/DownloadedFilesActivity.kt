package net.sampro.yosef

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.sampro.yosef.adapters.FilesDownloadsAdapter
import net.sampro.yosef.databinding.ActivityDownloadedFilesBinding
import net.sampro.yosef.models.FileModel
import net.sampro.yosef.utils.Constants.FILE_OUT_PUT_DIR
import java.io.File

class DownloadedFilesActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDownloadedFilesBinding

    private lateinit var filesDownloadedList: MutableList<FileModel>
    private lateinit var filesAdapter: FilesDownloadsAdapter
    private lateinit var dirLocation: File

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDownloadedFilesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar.apply {
            title = "Downloads"
        }

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        dirLocation = File(applicationContext.filesDir, FILE_OUT_PUT_DIR)

        filesDownloadedList = mutableListOf()

        setUpRecyclerView()

        filesAdapter.setOnItemClickListener(object : FilesDownloadsAdapter.OnItemClickListener {
            override fun onOpenClick(position: Int) {
                val fileToOpen = File(dirLocation, filesDownloadedList[position].name.toString())
                var fileUri = Uri.fromFile(fileToOpen)

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    fileUri = FileProvider.getUriForFile(this@DownloadedFilesActivity, "${BuildConfig.APPLICATION_ID}.provider", fileToOpen)
                }

                grantUriPermission(packageName, fileUri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION )

                val contentResolver = this@DownloadedFilesActivity.contentResolver
                val mimeType = contentResolver.getType(fileUri)
                val actionView = Intent(Intent.ACTION_VIEW)
                actionView.setDataAndType(fileUri, mimeType)
                actionView.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION)

                try {
                    startActivity(actionView)
                } catch (ex: ActivityNotFoundException) {
                    Toast.makeText(this@DownloadedFilesActivity, "No App found to open this file type.", Toast.LENGTH_SHORT).show()
                    Log.e("DOWNLOADED_ACTIVITY", "${ex.stackTrace}")
                }

//                val actionView = Intent(Intent.ACTION_VIEW, fileUri)
//                val chooser = Intent.createChooser(actionView, "Choose an app to open file.")
//                val resoInfoList = packageManager.queryIntentActivities(chooser, PackageManager.MATCH_DEFAULT_ONLY)
//                for (resolveInfo in resoInfoList) {
//                    val packageName = resolveInfo.activityInfo.packageName
//                    grantUriPermission(packageName, fileUri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION )
//                }
//
//                chooser.resolveActivity(packageManager)?.run {
//                    startActivity(chooser)
//                }
//                Toast.makeText(this@DownloadedFilesActivity, "File MimeType = $mimeType", Toast.LENGTH_SHORT).show()
            }

            override fun onDeleteClick(position: Int) {

                MaterialAlertDialogBuilder(this@DownloadedFilesActivity)
                    .setTitle("Conformation")
                    .setMessage("Are you sure you want to delete this file?")
                    .setNegativeButton("NO") { dialog, _ ->
                        dialog.dismiss()
                    }
                    .setPositiveButton("Yes") { dialog, _ ->
                        val fileToDelete = File(dirLocation, filesDownloadedList[position].name.toString())
                        if (fileToDelete.exists()) {
                            val isDeleteSuccess = fileToDelete.delete()
                            if (isDeleteSuccess) {
                                filesDownloadedList.removeAt(position)
                                filesAdapter.differ.submitList(filesDownloadedList.toMutableList())
                                Toast.makeText(this@DownloadedFilesActivity, "File Successfully Deleted.", Toast.LENGTH_SHORT).show()
                            }
                        }
                        dialog.dismiss()
                    }
                    .show()

                //Toast.makeText(this@DownloadedFilesActivity, "${filesDownloadedList[position]} FILE DELETE = $fileToDelete", Toast.LENGTH_LONG).show()
                //Log.i("FILE_INFO", "${filesDownloadedList[position]} FILE DELETE = $fileToDelete File Exist = ${fileToDelete.exists()} DIR Location = $dirLocation")
            }

        })

        binding.swipeRefreshLayoutDownloads.setOnRefreshListener {
            lifecycleScope.launch(Dispatchers.IO) {
                getDownloadedFiles()
            }
        }

        lifecycleScope.launch(Dispatchers.IO) {
            getDownloadedFiles()
        }
    }

    @SuppressLint("SetTextI18n")
    private suspend fun getDownloadedFiles() {
        withContext(Dispatchers.Main) {
            if (!binding.swipeRefreshLayoutDownloads.isRefreshing) {
                binding.textViewStatus.text = "Loading..."
                binding.clLoadingDataTransfer.visibility = View.VISIBLE
            }
        }
        val fileDir = File(applicationContext.filesDir, FILE_OUT_PUT_DIR)

        if (fileDir.exists()) {
            val files = fileDir.listFiles()
            var id = 0
            val fileList = files?.filter { it.canRead() && it.isFile }?.map {
                id++
                val name = it.name
                val uri = it.toUri()
                FileModel(id, name, uri)
            }
            if (fileList!!.isNotEmpty()) {
                filesDownloadedList = fileList.toMutableList()
            }
        }
        withContext(Dispatchers.Main) {
            filesAdapter.differ.submitList(filesDownloadedList.toMutableList())
            binding.clLoadingDataTransfer.visibility = View.GONE
            binding.swipeRefreshLayoutDownloads.isRefreshing = false
        }
    }

    private fun setUpRecyclerView() {
        filesAdapter = FilesDownloadsAdapter()
        binding.rvDownloadFileList.apply {
            adapter = filesAdapter
            layoutManager = LinearLayoutManager(this@DownloadedFilesActivity)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
            }
        }

        return super.onOptionsItemSelected(item)
    }
}