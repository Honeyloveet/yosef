package net.sampro.yosef

import android.annotation.SuppressLint
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import net.sampro.yosef.databinding.ActivityAddUserBinding
import net.sampro.yosef.models.UserAdd
import net.sampro.yosef.utils.NetworkStatus

class AddUserActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddUserBinding

    private lateinit var firebaseAuth: FirebaseAuth

    private lateinit var dbReference: FirebaseDatabase

    private lateinit var appUserName: String
    private lateinit var appUserEmail: String
    private lateinit var appUserPassword: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddUserBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.apply {
            title = "Add User"
        }

        appUserName = intent.getStringExtra("USER_NAME").toString()
        appUserEmail = intent.getStringExtra("USER_EMAIL").toString()
        appUserPassword = intent.getStringExtra("USER_PASSWORD").toString()

        firebaseAuth = FirebaseAuth.getInstance()

        dbReference = FirebaseDatabase.getInstance()

        binding.tvConnectionStatus.text = "Logged in user: ${firebaseAuth.currentUser?.displayName}"

        setUpUserTypeDropDown()

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        binding.btnAddUser.setOnClickListener {

            val isInternetAvailable = NetworkStatus.isNetworkAvailable(this)

            if (isInternetAvailable) {
                val isValidInput = validateInput()

                if (isValidInput) {
                    binding.textInputUserType.editText!!.error = null
                    addUser()
                } else {
                    Toast.makeText(this, "Input Failed", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "There is no internet. Please connect to the internet and try again.", Toast.LENGTH_SHORT).show()
            }

        }

    }

    private fun addUser() {
        binding.clLoadingDataTransfer.visibility = View.VISIBLE

        firebaseAuth.signOut()

        val userName = binding.textInputUserName.editText?.text.toString().replace(" ","").lowercase()
        val userEmail = "$userName@gmail.com"
        val userPassword = binding.textInputPassword.editText?.text.toString()
        val userType = binding.textInputUserType.editText?.text.toString()

        firebaseAuth.createUserWithEmailAndPassword(userEmail, userPassword)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val userInfo = UserAdd().apply {
                        uid = firebaseAuth.currentUser?.uid
                        name = userName
                        email = userEmail
                        password = userPassword
                        type = userType
                    }
                    addUserToDb(userInfo)
                }
            }

    }

    private fun addUserToDb(user: UserAdd) {
        val dbRef = dbReference.getReference("users")

        dbRef.child(user.uid.toString()).setValue(user)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    firebaseAuth.signOut()
                    firebaseAuth.signInWithEmailAndPassword(appUserEmail, appUserPassword)
                    clearInputFields()
                    binding.clLoadingDataTransfer.visibility = View.GONE
                    Log.i("ADD_USER", "User Added Successfully")
                    Toast.makeText(this, "New User Added Successfully", Toast.LENGTH_SHORT).show()
                } else {
                    firebaseAuth.signOut()
                    firebaseAuth.signInWithEmailAndPassword(appUserEmail, appUserPassword)
                    Log.i("ADD_USER", "Failed: ${task.exception}")
                    clearInputFields()
                    binding.clLoadingDataTransfer.visibility = View.GONE
                    Toast.makeText(this, "Adding New User Failed!!!", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener {
                firebaseAuth.signOut()
                firebaseAuth.signInWithEmailAndPassword(appUserEmail, appUserPassword)
                clearInputFields()
                binding.clLoadingDataTransfer.visibility = View.GONE
                Log.i("ADD_USER", "Failed: ${it.stackTrace}")
                Toast.makeText(this, "Adding New User Failed!!!", Toast.LENGTH_SHORT).show()
            }

    }

    private fun setUpUserTypeDropDown() {
        val items = listOf("User", "Admin", "SuperAdmin")
        val arrayAdapter = ArrayAdapter(this, R.layout.drop_down_users, items)
        (binding.textInputUserType.editText as AutoCompleteTextView).setAdapter(arrayAdapter)
    }

    private fun validateInput(): Boolean{

        if (binding.textInputUserName.editText!!.length() == 0) {
            binding.textInputUserName.editText!!.error = "User name is required"
            return false
        }

        if (binding.textInputPassword.editText!!.length() == 0) {
            binding.textInputPassword.editText!!.error = "Password is required"
            return false
        } else if (binding.textInputPassword.editText!!.length() < 7) {
            binding.textInputPassword.editText!!.error = "Password must be minimum 8 characters"
            return false
        }

        if (binding.textInputUserType.editText!!.length() == 0) {
            binding.textInputUserType.editText!!.error = "User Type Must be selected"
            return false
        }

        return true
    }

    @SuppressLint("SetTextI18n")
    private fun clearInputFields() {
        binding.textInputUserName.editText!!.setText("")
        binding.textInputPassword.editText!!.setText("")
        binding.textInputUserType.editText!!.setText("User")
        binding.textInputUserType.editText!!.clearFocus()

        val items = listOf("User", "Admin", "SuperAdmin")
        val arrayAdapter = ArrayAdapter(this, R.layout.drop_down_users, items)
        (binding.textInputUserType.editText as AutoCompleteTextView).setAdapter(arrayAdapter)
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