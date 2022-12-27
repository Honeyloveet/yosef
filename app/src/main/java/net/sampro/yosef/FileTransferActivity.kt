package net.sampro.yosef

import android.annotation.SuppressLint
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.OpenableColumns
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.lifecycle.lifecycleScope
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.sampro.yosef.databinding.ActivityFileTransferBinding
import net.sampro.yosef.models.ReceivedFileAdd
import net.sampro.yosef.models.UserGet
import net.sampro.yosef.utils.LoggedInUser
import net.sampro.yosef.utils.RequestPermissions
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class FileTransferActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFileTransferBinding

    private lateinit var dbReference: FirebaseDatabase

    private lateinit var requestPermissions: RequestPermissions

    private lateinit var storage: StorageReference

    private lateinit var usersList: MutableList<UserGet>
    private val usersArray = arrayListOf<String>()

    private var fileName: String? = null

    private var fileExtension: String? = null

    private var fileNameWithExtension: String? = null

    private var fileNameToUpload: String? = null

    private var fileUri: Uri? = null

    private val selectFileLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {

            fileUri = uri
//            fileNameWithExtension = File(uri.path.toString()).name
//            fileNameWithExtension = filePath?.substringAfterLast("/")
            fileNameWithExtension = getFileNameWithExtension(uri)
//            var os = ByteArrayOutputStream()
//            fileName = File(uri.path.toString()).nameWithoutExtension.removePrefix("primary:")
            fileName = fileNameWithExtension?.substringBeforeLast(".")
//            fileName = File(uri.path.toString()).nameWithoutExtension
            fileExtension = fileNameWithExtension?.substringAfterLast(".")
//            fileExtension = File(uri.path.toString()).extension
//            Log.i(TAG,"Data: ${byteArray} and ${x}")
            binding.autoCompleteSelectedFile.setText(fileNameWithExtension)

//            Log.i("UPLOAD", "File Name With Extension: $fileNameWithExtension File Name: $fileName File Extension: $fileExtension")
        }
    }

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFileTransferBinding.inflate(layoutInflater)
        setContentView(binding.root)

        requestPermissions = RequestPermissions(this, this)

        storage = FirebaseStorage.getInstance().reference

        supportActionBar?.apply {
            title = "Transfer File"
        }

        supportActionBar?.setDisplayHomeAsUpEnabled(true)



        usersList = mutableListOf()

        binding.txtInputLayoutFrom.visibility = View.GONE
        //binding.txtViewHello.text = "${LoggedInUser.uid}\n${LoggedInUser.userName}\n${LoggedInUser.email}\n${LoggedInUser.type}"

        checkPermissions()

        dbReference = FirebaseDatabase.getInstance()

        binding.btnTransferFile.setOnClickListener {

            if (binding.autoCompleteTvTo.text.toString().isNotBlank() && binding.autoCompleteSelectedFile.text.toString().isNotBlank()) {
                if (fileUri != null) {
                    lifecycleScope.launch(Dispatchers.IO) {
                        uploadFile()
                    }
                    //addFileToDb()
                } else {
                    Toast.makeText(this,"fileUri is empty", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this,"Please Select All Fields To Upload.",Toast.LENGTH_SHORT).show()
            }

        }

        binding.btnSelectFile.setOnClickListener {
            val isPermissionsGranted = requestPermissions.checkPermission()
            if (isPermissionsGranted) {
                selectFileLauncher.launch("*/*")
            } else {
                Toast.makeText(this,"Please Allow Storage Read Write Permission!", Toast.LENGTH_SHORT).show()
            }
        }

        getUsers()
    }

    private fun getFileNameWithExtension(uri: Uri): String? {
        var fileNameResult: String? = null

        val scheme = uri.scheme

        if (scheme == "content") {
            val cursor = contentResolver.query(uri, null, null, null)

            try {
                if (cursor != null && cursor.moveToFirst()) {
                    val columnIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    fileNameResult = cursor.getString(columnIndex)
                }
            } finally {
                cursor?.close()
            }
        } else {
//            val filePath = uri.path?.substringAfter(":")
            val filePath = uri.path
            fileNameResult = filePath?.substringAfterLast("/")
        }

        return fileNameResult
    }

    @SuppressLint("SetTextI18n")
    private suspend fun uploadFile() {
        withContext(Dispatchers.Main) {
            binding.textViewStatus.text = "Uploading File..."
            //binding.clLoadingDataTransfer.visibility = View.VISIBLE
        }
        val formatter = SimpleDateFormat("yyyy_MM_dd_HH_mm_ss", Locale.getDefault())
        val dateNow = Date()

        fileNameToUpload = if (fileName == fileExtension) {
            "$fileName-${formatter.format(dateNow)}"
        } else {
            "$fileName-${formatter.format(dateNow)}.$fileExtension"
        }

        Log.i("UPLOAD", "File Upload Name: $fileNameToUpload File Name: $fileName File Extension: $fileExtension")

        val file = storage.child("sharedfiles/$fileNameToUpload")

        file.putFile(fileUri!!).addOnSuccessListener {
            file.downloadUrl.addOnSuccessListener {
                addFileToDb(it.toString())
//                Log.i("URL", "file download url = $it")
                //Toast.makeText(this, "File Upload Success!!", Toast.LENGTH_SHORT).show()
            }.addOnFailureListener {
                binding.clLoadingDataTransfer.visibility = View.GONE
                Toast.makeText(this, "File Upload Failed", Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener {
            binding.clLoadingDataTransfer.visibility = View.GONE
        }
    }

    private fun addFileToDb(fileUrl: String) {
        val dbRef = dbReference.getReference("sharedfiles")
        val fileId = dbRef.push().key!!

        val fileToSave = ReceivedFileAdd().apply {
            file_id = fileId
            from = LoggedInUser.userName
            to = binding.autoCompleteTvTo.text.toString()
            file_name = fileNameToUpload
            file_url = fileUrl
            approval_status = "PENDING"
            approved_by = ""
            received_at = ServerValue.TIMESTAMP
        }

        dbRef.child(fileId).setValue(fileToSave)
            .addOnCompleteListener {
                resetFields()
                binding.clLoadingDataTransfer.visibility = View.GONE
                Toast.makeText(this, "File upload completed successfully!!", Toast.LENGTH_SHORT).show()
            }.addOnFailureListener { err ->
                resetFields()
                binding.clLoadingDataTransfer.visibility = View.GONE
                Toast.makeText(this, "Error ${err.message}", Toast.LENGTH_SHORT).show()
            }

        //Log.i("FILE_EXT", "file name = $fileName extension = $fileExtension")
    }

    private fun resetFields() {
        binding.autoCompleteSelectedFile.setText("")
        binding.autoCompleteTvTo.setText("")
        fileName = null
        fileExtension = null
        fileNameWithExtension = null
        fileNameToUpload = null
        fileUri = null
    }

    @SuppressLint("SetTextI18n")
    private fun getUsers() {
        val usersReference = dbReference.getReference("users")
        binding.textViewStatus.text = "Loading..."
        binding.clLoadingDataTransfer.visibility = View.VISIBLE

        usersReference.addValueEventListener(object : ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                usersList.clear()
                usersArray.clear()
                if (snapshot.exists()) {
                    for (user in snapshot.children) {
                        val userData = user.getValue(UserGet::class.java)
                        if (userData != null) {
                            usersList.add(userData)
                            usersArray.add(userData.name.toString())
                        }
                    }
                    setUpUsersDropDownList()
                    binding.clLoadingDataTransfer.visibility = View.GONE
                }
            }

            override fun onCancelled(error: DatabaseError) {
                binding.clLoadingDataTransfer.visibility = View.GONE
            }

        })
    }

    private fun setUpUsersDropDownList() {
        val arrayAdapterAutoCompleteTvTo = ArrayAdapter(this, R.layout.drop_down_users, usersArray)
        binding.autoCompleteTvTo.setAdapter(arrayAdapterAutoCompleteTvTo)
        if (usersList.isNotEmpty()) {
            Log.i("USERINFO", "Users List is $usersList")
            for (user in usersList) {
                Log.i("USERINFOFULL", "User = ${user.name}, ${user.email}, ${user.type}, ${user.Timestamp}")
            }
        }
    }

    private fun checkPermissions() {

        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            if (!requestPermissions.hasWriteExternalStoragePermission()) {
                requestPermissions.requestReadWritePermissions()
            }
        } else {
            if (!requestPermissions.hasReadExternalStoragePermission()) {
                requestPermissions.requestReadWritePermissions()
            }
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