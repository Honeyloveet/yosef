package net.sampro.yosef

import android.annotation.SuppressLint
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.sampro.yosef.adapters.FileTransferListAdapter
import net.sampro.yosef.databinding.ActivityFileTransferListBinding
import net.sampro.yosef.models.ReceivedFileGet
import net.sampro.yosef.utils.LoggedInUser

class FileTransferListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFileTransferListBinding

    private lateinit var dbReference: FirebaseDatabase

    private lateinit var filesList: MutableList<ReceivedFileGet>
    private lateinit var filesAdapter: FileTransferListAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFileTransferListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.apply {
            title = "File Transfers"
        }

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
                Toast.makeText(this@FileTransferListActivity, "Download Clicked.", Toast.LENGTH_SHORT).show()
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

    private fun setupRecyclerView() {
        filesAdapter = FileTransferListAdapter()
        binding.rvFileTrfList.apply {
            adapter = filesAdapter
            layoutManager = LinearLayoutManager(this@FileTransferListActivity)

        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menuItemSend -> {
                val intent = Intent(this, FileTransferActivity::class.java)
                startActivity(intent)
            }
            R.id.menuItemUsers -> {
                val intent = Intent(this, UsersActivity::class.java)
                startActivity(intent)
            }
            R.id.menuItemLogOut -> {
                FirebaseAuth.getInstance().signOut()
                val intent = Intent(this, LoginActivity::class.java)
                startActivity(intent)
                this.finish()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }
}