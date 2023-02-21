package net.sampro.yosef

import android.annotation.SuppressLint
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.sampro.yosef.adapters.UsersAdapter
import net.sampro.yosef.databinding.ActivityUsersBinding
import net.sampro.yosef.models.UserGet
import net.sampro.yosef.utils.LoggedInUser

class UsersActivity : AppCompatActivity() {

    private lateinit var binding: ActivityUsersBinding

    private lateinit var dbReference: FirebaseDatabase

    private lateinit var firebaseAuth: FirebaseAuth

    private lateinit var usersList: MutableList<UserGet>
    private lateinit var userAdapter: UsersAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUsersBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.apply {
            title = "Users"
        }

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        firebaseAuth = FirebaseAuth.getInstance()

        usersList = mutableListOf()
        dbReference = FirebaseDatabase.getInstance()

        setupRecyclerView()

        binding.fabAddUser.setOnClickListener {
            val intent = Intent(this, AddUserActivity::class.java)
            startActivity(intent)
        }

        binding.swipeRefreshLayoutUsers.setOnRefreshListener {
            usersList.clear()
            lifecycleScope.launch(Dispatchers.IO) {
                getUsers()
            }
        }

        userAdapter.setOnItemClickListener(object : UsersAdapter.OnItemClickListener {
            override fun onChangePasswordClick(position: Int) {
                showChangePasswordDialog(position)
            }

            override fun onDeleteUserClick(position: Int) {
                Toast.makeText(this@UsersActivity, "Delete User Clicked.", Toast.LENGTH_SHORT).show()
            }

        })

        lifecycleScope.launch(Dispatchers.IO) {
            getUsers()
        }
    }

    private fun showChangePasswordDialog(position: Int) {

        val userName = usersList[position].name.toString()
        val input = EditText(this)
        input.hint = "Enter New Password"

        val alertDialog = MaterialAlertDialogBuilder(this)
        alertDialog.setTitle("Change $userName's Password")
        //alertDialog.setMessage("Enter New Password")
        alertDialog.setView(input)
        alertDialog.setNeutralButton("Cancel") { dialog, which ->
            dialog.dismiss()
        }
        alertDialog.setPositiveButton("Save") { dialog, which ->
            if (input.text.toString().isNotEmpty()) {
                dialog.dismiss()
                changePassword(position, input.text.toString())
            } else {
                Toast.makeText(this@UsersActivity, "Please Enter New Password.", Toast.LENGTH_SHORT).show()
            }
        }

        alertDialog.show()
    }

    private fun changePassword(position: Int, newPassword: String) {
        binding.clLoadingUsers.visibility = View.VISIBLE

        val mainUserPosition = getMainUserIndex()
        val mainUserEmail = usersList[mainUserPosition].email.toString()
        val mainUserPassword = usersList[mainUserPosition].password.toString()

        val loginEmail = usersList[position].email.toString()
        val loginPassword = usersList[position].password.toString()
        firebaseAuth.signOut()
        firebaseAuth.signInWithEmailAndPassword(loginEmail, loginPassword)
            .addOnCompleteListener {
                if (it.isSuccessful) {
                    val user = firebaseAuth.currentUser
                    user!!.updatePassword(newPassword)
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                val usersReference = dbReference.getReference("users")
                                usersReference.child(usersList[position].uid.toString()).child("password").setValue(newPassword)
                                firebaseAuth.signOut()
                                firebaseAuth.signInWithEmailAndPassword(mainUserEmail, mainUserPassword)
                                    .addOnCompleteListener {
                                        if (it.isSuccessful) {
                                            binding.clLoadingUsers.visibility = View.GONE
                                            Toast.makeText(this@UsersActivity, "Password Changed Successfully!!.", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                            } else {
                                binding.clLoadingUsers.visibility = View.GONE
                                Toast.makeText(this@UsersActivity, "Password Change Failed!!.", Toast.LENGTH_SHORT).show()
                            }
                        }
                } else {
                    binding.clLoadingUsers.visibility = View.GONE
                    Toast.makeText(this@UsersActivity, "Password Change Failed!!.", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun getMainUserIndex(): Int {
        var userPosition = -1

        usersList.forEachIndexed { index, userGet ->
            if (userGet.name == LoggedInUser.userName) {
                userPosition = index
            }
        }

        return userPosition
    }

    @SuppressLint("SetTextI18n")
    private suspend fun getUsers() {
        val usersReference = dbReference.getReference("users")
        withContext(Dispatchers.Main) {
            if (!binding.swipeRefreshLayoutUsers.isRefreshing) {
                binding.textViewStatus.text = "Loading..."
                binding.clLoadingUsers.visibility = View.VISIBLE
            }
        }

        usersReference.addValueEventListener(object : ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    usersList.clear()
                    for (user in snapshot.children) {
                        val userData = user.getValue(UserGet::class.java)
                        if (userData != null) {
                            usersList.add(userData)
                        }
                    }
                    userAdapter.differ.submitList(usersList.toMutableList())
                    binding.clLoadingUsers.visibility = View.GONE
                    binding.swipeRefreshLayoutUsers.isRefreshing = false
                }
            }

            override fun onCancelled(error: DatabaseError) {
                binding.clLoadingUsers.visibility = View.GONE
                binding.swipeRefreshLayoutUsers.isRefreshing = false
            }

        })
    }

    private fun setupRecyclerView() {
        userAdapter = UsersAdapter()
        binding.rvFileTrfUsers.apply {
            adapter = userAdapter
            layoutManager = LinearLayoutManager(this@UsersActivity)

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