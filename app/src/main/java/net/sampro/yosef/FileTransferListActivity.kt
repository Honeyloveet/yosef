package net.sampro.yosef

import android.annotation.SuppressLint
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.sampro.yosef.adapters.FileTransferListAdapter
import net.sampro.yosef.databinding.ActivityFileTransferListBinding
import net.sampro.yosef.models.ReceivedFileGet
import net.sampro.yosef.utils.Constants.FILE_OUT_PUT_DIR
import net.sampro.yosef.utils.LoggedInUser
import java.io.File

class FileTransferListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFileTransferListBinding

    private lateinit var dbReference: FirebaseDatabase

    private lateinit var storage: FirebaseStorage

    private lateinit var filesList: MutableList<ReceivedFileGet>
    private lateinit var filesAdapter: FileTransferListAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFileTransferListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.apply {
            title = "File Transfers"
        }

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        storage = FirebaseStorage.getInstance()

        filesList = mutableListOf()
        dbReference = FirebaseDatabase.getInstance()

        setupRecyclerView()

        binding.swipeRefreshLayoutFileTrfList.setOnRefreshListener {
            filesList.clear()
            lifecycleScope.launch(Dispatchers.IO) {
                getFiles()
            }
        }

        filesAdapter.setOnItemClickListener(object : FileTransferListAdapter.OnItemClickListener {
            override fun onDownloadClick(position: Int) {

                val outPutDir = File(applicationContext.filesDir, FILE_OUT_PUT_DIR)

                if (!outPutDir.exists()) {
                    outPutDir.mkdir()
                }

                val outPutFile = File(outPutDir, filesList[position].file_name!!)

                if (!outPutFile.exists()) {
                    lifecycleScope.launch(Dispatchers.IO) {
                        downloadFile(position, outPutFile)
                    }
                } else {
                    Toast.makeText(this@FileTransferListActivity, "File already exists.", Toast.LENGTH_SHORT).show()
                }

            }

            override fun onApproveClick(position: Int) {
                lifecycleScope.launch(Dispatchers.IO) {
                    updateFile(filesList[position].file_id.toString())
                }
            }

        })

        lifecycleScope.launch(Dispatchers.IO) {
            getFiles()
        }

    }

    @SuppressLint("SetTextI18n")
    private suspend fun updateFile(fileId: String) {
        val filesReference = dbReference.getReference("sharedfiles")
        withContext(Dispatchers.Main) {
            if (!binding.swipeRefreshLayoutFileTrfList.isRefreshing) {
                binding.textViewStatus.text = "Loading..."
                binding.clLoadingFileTransfer.visibility = View.VISIBLE
            }
        }

        filesReference.child(fileId).child("approved_by").setValue(LoggedInUser.userName)
            .addOnFailureListener {
                binding.clLoadingFileTransfer.visibility = View.GONE
                binding.swipeRefreshLayoutFileTrfList.isRefreshing = false
                Toast.makeText(this, "File Approval Failed!!", Toast.LENGTH_SHORT).show()
                return@addOnFailureListener
            }

        filesReference.child(fileId).child("approval_status").setValue("APPROVED")
            .addOnSuccessListener {
                binding.clLoadingFileTransfer.visibility = View.GONE
                binding.swipeRefreshLayoutFileTrfList.isRefreshing = false
                Toast.makeText(this, "File Approved Successfully.", Toast.LENGTH_SHORT).show()
            }.addOnFailureListener {
                binding.clLoadingFileTransfer.visibility = View.GONE
                binding.swipeRefreshLayoutFileTrfList.isRefreshing = false
                Toast.makeText(this, "File Approval Failed!!", Toast.LENGTH_SHORT).show()
            }
    }

    @SuppressLint("SetTextI18n")
    private suspend fun getFiles() {
        val filesReference = dbReference.getReference("sharedfiles")
            .orderByChild("received_at")
        withContext(Dispatchers.Main) {
            if (!binding.swipeRefreshLayoutFileTrfList.isRefreshing) {
                binding.textViewStatus.text = "Loading..."
                binding.clLoadingFileTransfer.visibility = View.VISIBLE
            }
        }

        filesReference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    filesList.clear()
                    for (file in snapshot.children) {
                        val fileData = file.getValue(ReceivedFileGet::class.java)
                        if (fileData != null) {
                            filesList.add(fileData)
                        }
                    }
                    filesList.reverse()
                    filesAdapter.differ.submitList(filesList.toMutableList())
                    binding.clLoadingFileTransfer.visibility = View.GONE
                    binding.swipeRefreshLayoutFileTrfList.isRefreshing = false
                }
                binding.clLoadingFileTransfer.visibility = View.GONE
                binding.swipeRefreshLayoutFileTrfList.isRefreshing = false
            }

            override fun onCancelled(error: DatabaseError) {
                binding.clLoadingFileTransfer.visibility = View.GONE
                binding.swipeRefreshLayoutFileTrfList.isRefreshing = false
            }

        })
    }

    private suspend fun downloadFile(filePosition: Int, fileOutPut: File) {
        withContext(Dispatchers.Main) {
            binding.textViewStatus.text = "Downloading..."
            binding.clLoadingFileTransfer.visibility = View.VISIBLE
        }
        val fileUrl = filesList[filePosition].file_url.toString()
        val fileUrlReference = storage.getReferenceFromUrl(fileUrl)
        val fileSizeLimit: Long = (1024 * 1024) * 50
        fileUrlReference.getBytes(fileSizeLimit).addOnSuccessListener {
            saveDownloadedFile(it, fileOutPut)
        }.addOnFailureListener {
            Toast.makeText(this@FileTransferListActivity,"File Download Failed!!!.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveDownloadedFile(fileToSave: ByteArray, fileOutPut: File) {
        fileOutPut.writeBytes(fileToSave)
//        withContext(Dispatchers.Main) {
//            binding.clLoadingFileTransfer.visibility = View.GONE
//            binding.textViewStatus.text = "Loading..."
//            Toast.makeText(this@FileTransferListActivity,"File Downloaded Successfully.", Toast.LENGTH_SHORT).show()
//        }
        binding.clLoadingFileTransfer.visibility = View.GONE
        binding.textViewStatus.text = "Loading..."
        Toast.makeText(this@FileTransferListActivity,"File Downloaded Successfully.", Toast.LENGTH_SHORT).show()

    }

    private fun setupRecyclerView() {
        filesAdapter = FileTransferListAdapter()
        binding.rvFileTrfList.apply {
            adapter = filesAdapter
            layoutManager = LinearLayoutManager(this@FileTransferListActivity)

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