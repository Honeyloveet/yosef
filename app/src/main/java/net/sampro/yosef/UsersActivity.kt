package net.sampro.yosef

import android.annotation.SuppressLint
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
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

class UsersActivity : AppCompatActivity() {

    private lateinit var binding: ActivityUsersBinding

    private lateinit var dbReference: FirebaseDatabase

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
                Toast.makeText(this@UsersActivity, "Change Password Clicked.", Toast.LENGTH_SHORT).show()
            }

            override fun onDeleteUserClick(position: Int) {
                Toast.makeText(this@UsersActivity, "Delete User Clicked.", Toast.LENGTH_SHORT).show()
            }

        })

        lifecycleScope.launch(Dispatchers.IO) {
            getUsers()
        }
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