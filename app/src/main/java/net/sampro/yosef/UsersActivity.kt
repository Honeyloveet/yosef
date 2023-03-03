package net.sampro.yosef

import android.annotation.SuppressLint
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.provider.FirebaseInitProvider
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

    private lateinit var newUserList: List<UserGet>
    private lateinit var usersList: MutableList<UserGet>
    private lateinit var userAdapter: UsersAdapter

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUsersBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.apply {
            title = "Users"
        }

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        firebaseAuth = FirebaseAuth.getInstance()

        newUserList = listOf()
        usersList = mutableListOf()
        dbReference = FirebaseDatabase.getInstance()

        binding.tvConnectionStatus.text = "Logged in user: ${firebaseAuth.currentUser?.displayName}"

        setupRecyclerView()

        binding.fabAddUser.setOnClickListener {

            val currentUserPosition = getLoggedInAppUserIndex()

            val currentUserName = usersList[currentUserPosition].name
            val currentUserEmail = usersList[currentUserPosition].email
            val currentUserPassword = usersList[currentUserPosition].password

            val intent = Intent(this, AddUserActivity::class.java)

            intent.putExtra("USER_NAME", currentUserName)
            intent.putExtra("USER_EMAIL", currentUserEmail)
            intent.putExtra("USER_PASSWORD", currentUserPassword)

            startActivity(intent)
        }

        binding.swipeRefreshLayoutUsers.setOnRefreshListener {
            usersList.clear()
            lifecycleScope.launch(Dispatchers.IO) {
                getUsers()
            }
        }

        userAdapter.setOnItemClickListener(object : UsersAdapter.OnItemClickListener {

            override fun onViewUserClick(position: Int) {
                val currentUserPosition = getLoggedInAppUserIndex()

                val currentUserName = usersList[currentUserPosition].name
                val currentUserEmail = usersList[currentUserPosition].email
                val currentUserPassword = usersList[currentUserPosition].password

                val userId = usersList[position].uid
                val userName = usersList[position].name
                val userEmail = usersList[position].email
                val userPassword = usersList[position].password
                val userType = usersList[position].type

                val intent = Intent(this@UsersActivity, UserDetailsActivity::class.java)

                intent.putExtra("USER_ID", userId)
                intent.putExtra("USER_NAME", userName)
                intent.putExtra("USER_EMAIL", userEmail)
                intent.putExtra("USER_PASSWORD", userPassword)
                intent.putExtra("USER_TYPE", userType)
                intent.putExtra("MAIN_USER_NAME", currentUserName)
                intent.putExtra("MAIN_USER_EMAIL", currentUserEmail)
                intent.putExtra("MAIN_USER_PASSWORD", currentUserPassword)

                startActivity(intent)
                //showChangePasswordDialog(position)
            }

            override fun onDeleteUserClick(position: Int) {

//                val intent = Intent(this@UsersActivity, UserDetailsActivity::class.java)
//                startActivity(intent)
                showDeleteUserDialog(position)
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
        alertDialog.setNeutralButton("Cancel") { dialog, _ ->
            dialog.dismiss()
        }
        alertDialog.setPositiveButton("Save") { dialog, _ ->
            if (input.text.toString().isNotEmpty() && input.text.toString().length > 6) {
                dialog.dismiss()
                changePassword(position, input.text.toString())
            } else {
                Toast.makeText(this@UsersActivity, "Password cannot be less than 7 characters", Toast.LENGTH_SHORT).show()
            }
        }

        alertDialog.show()
    }

    private fun showDeleteUserDialog(position: Int) {
        val userName = usersList[position].name.toString()

        val alertDialog = MaterialAlertDialogBuilder(this)
        alertDialog.setTitle("Delete $userName")
        alertDialog.setMessage("Are you sure you want to delete this user?")
        alertDialog.setNeutralButton("No") { dialog, _ ->
            dialog.dismiss()
        }
        alertDialog.setPositiveButton("Yes") { dialog, _ ->
            deleteUser(position)
        }

        alertDialog.show()
    }

    private fun deleteUser(position: Int) {
        binding.clLoadingUsers.visibility = View.VISIBLE

        val mainUserPosition = getLoggedInAppUserIndex()
        val mainUserEmail = usersList[mainUserPosition].email.toString()
        val mainUserPassword = usersList[mainUserPosition].password.toString()

        val loginEmail = usersList[position].email.toString()
        val loginPassword = usersList[position].password.toString()

        firebaseAuth.signOut()
        firebaseAuth.signInWithEmailAndPassword(loginEmail, loginPassword)
            .addOnCompleteListener { deleteUserSignInTask ->
                if (deleteUserSignInTask.isSuccessful) {
                    val user = firebaseAuth.currentUser
                    user!!.delete()
                        .addOnCompleteListener { deleteUserTask ->
                            if (deleteUserTask.isSuccessful) {
                                firebaseAuth.signOut()
                                firebaseAuth.signInWithEmailAndPassword(mainUserEmail, mainUserPassword)
                                    .addOnCompleteListener { mainUserSignInTask ->
                                        if (mainUserSignInTask.isSuccessful) {
                                            val usersReference = dbReference.getReference("users")
                                            usersReference.child(usersList[position].uid.toString()).setValue(null)
                                            binding.clLoadingUsers.visibility = View.GONE
                                            Toast.makeText(this@UsersActivity, "User Deleted Successfully!!.", Toast.LENGTH_SHORT).show()
                                        } else {
                                            binding.clLoadingUsers.visibility = View.GONE
                                            Toast.makeText(this@UsersActivity, "Deleting User Failed!!.", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                            } else {
                                binding.clLoadingUsers.visibility = View.GONE
                                Toast.makeText(this@UsersActivity, "Deleting User Failed!!.", Toast.LENGTH_SHORT).show()
                            }
                        }
                } else {
                    binding.clLoadingUsers.visibility = View.GONE
                    Toast.makeText(this@UsersActivity, "Deleting User Failed!!.", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener {
                Log.i("UsersActivity", "Delete User Failed: ${it.stackTrace}")
                binding.clLoadingUsers.visibility = View.GONE
                Toast.makeText(this@UsersActivity, "Delete User Failed!!.", Toast.LENGTH_SHORT).show()
            }

    }

    private fun changePassword(position: Int, newPassword: String) {
        binding.clLoadingUsers.visibility = View.VISIBLE

        val mainUserPosition = getLoggedInAppUserIndex()
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
            .addOnFailureListener {
                Log.i("UsersActivity", "Password Change Failed: ${it.stackTrace}")
                binding.clLoadingUsers.visibility = View.GONE
                Toast.makeText(this@UsersActivity, "Password Change Failed!!.", Toast.LENGTH_SHORT).show()
            }
    }

    private fun getLoggedInAppUserIndex(): Int {
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
                    newUserList = listOf()
                    usersList.clear()
                    for (user in snapshot.children) {
                        val userData = user.getValue(UserGet::class.java)
                        if (userData != null) {
                            usersList.add(userData)
                        }
                    }
                    newUserList = usersList.toList()
                    userAdapter.differ.submitList(newUserList)
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