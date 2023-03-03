package net.sampro.yosef

import android.annotation.SuppressLint
import android.app.Dialog
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.Toast
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import net.sampro.yosef.databinding.ActivityUserDetailsBinding

class UserDetailsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityUserDetailsBinding

    private lateinit var dbReference: FirebaseDatabase

    private lateinit var firebaseAuth: FirebaseAuth

    private lateinit var userId: String
    private lateinit var userName: String
    private lateinit var userEmail: String
    private lateinit var userPassword: String
    private lateinit var userType: String

    private lateinit var mainUserName: String
    private lateinit var mainUserEmail: String
    private lateinit var mainUserPassword: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUserDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.apply {
            title = "User Detail"
        }

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        firebaseAuth = FirebaseAuth.getInstance()

        dbReference = FirebaseDatabase.getInstance()

        mainUserName = intent.getStringExtra("MAIN_USER_NAME").toString()
        mainUserEmail = intent.getStringExtra("MAIN_USER_EMAIL").toString()
        mainUserPassword = intent.getStringExtra("MAIN_USER_PASSWORD").toString()

        userId = intent.getStringExtra("USER_ID").toString()
        userName = intent.getStringExtra("USER_NAME").toString()
        userEmail = intent.getStringExtra("USER_EMAIL").toString()
        userPassword = intent.getStringExtra("USER_PASSWORD").toString()
        userType = intent.getStringExtra("USER_TYPE").toString()

        binding.textViewUserName.text = userName
        binding.textViewPassword.text = userPassword
        binding.textViewUserType.text = userType

        binding.imageViewEditPassword.setOnClickListener {
            showChangePasswordDialog()
        }

        binding.imageViewEditUserType.setOnClickListener {
            showEditUserTypeDialog()
        }

        binding.buttonDeleteUser.setOnClickListener {
            showDeleteUserDialog()
        }

    }

    private fun showChangePasswordDialog() {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.custom_change_user_password_dialog)

        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT)

        val btnChange = dialog.findViewById<Button>(R.id.btnChange)
        val btnCancel = dialog.findViewById<Button>(R.id.btnCancel)
        val textInputPassword = dialog.findViewById<TextInputLayout>(R.id.textInputPassword)

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        btnChange.setOnClickListener {
            val isValidInput = validateInput(textInputPassword)
            if (isValidInput) {
                dialog.dismiss()
                val newPassword = textInputPassword.editText?.text.toString().trim()
                changeUserPassword(newPassword)
            }

            //Toast.makeText(this, "Button Change Clicked!!!!", Toast.LENGTH_SHORT).show()
        }

        dialog.show()
    }

    private fun validateInput(textInputPassword: TextInputLayout): Boolean {

        if (textInputPassword.editText!!.length() == 0) {
            textInputPassword.editText!!.error = "Password is required"
            return false
        } else if (textInputPassword.editText!!.length() < 7) {
            textInputPassword.editText!!.error = "Password must be minimum 8 characters"
            return false
        }

        return true
    }

    @SuppressLint("SetTextI18n")
    private fun changeUserPassword(newPassword: String) {
        binding.textViewStatus.text = "Saving new password..."
        binding.clLoadingDataTransfer.visibility = View.VISIBLE

        firebaseAuth.signOut()
        firebaseAuth.signInWithEmailAndPassword(userEmail, userPassword)
            .addOnSuccessListener {
                val user = firebaseAuth.currentUser
                user!!.updatePassword(newPassword)
                    .addOnSuccessListener {
                        val usersReference = dbReference.getReference("users")
                        usersReference.child(userId).child("password").setValue(newPassword)
                            .addOnSuccessListener {
                                firebaseAuth.signOut()
                                firebaseAuth.signInWithEmailAndPassword(mainUserEmail, mainUserPassword)
                                    .addOnSuccessListener {
                                        binding.clLoadingDataTransfer.visibility = View.GONE
                                        Toast.makeText(this, "Password Changed Successfully!!.", Toast.LENGTH_SHORT).show()
                                    }
                                    .addOnFailureListener {
                                        binding.clLoadingDataTransfer.visibility = View.GONE
                                        Toast.makeText(this, "Changing Password Failed!!.", Toast.LENGTH_SHORT).show()
                                    }
                            }
                            .addOnFailureListener {
                                binding.clLoadingDataTransfer.visibility = View.GONE
                                Toast.makeText(this, "Changing Password Failed!!.", Toast.LENGTH_SHORT).show()
                            }
                    }
                    .addOnFailureListener {
                        binding.clLoadingDataTransfer.visibility = View.GONE
                        Toast.makeText(this, "Changing Password Failed!!.", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener {
                binding.clLoadingDataTransfer.visibility = View.GONE
                Toast.makeText(this, "Changing Password Failed!!.", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showEditUserTypeDialog() {

        val items = listOf("User", "Admin", "SuperAdmin")
        val arrayAdapter = ArrayAdapter(this, R.layout.drop_down_users, items)

        val dialog = Dialog(this)
        dialog.setContentView(R.layout.custom_edit_user_type_dialog)

        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT)

        val btnChange = dialog.findViewById<Button>(R.id.btnChange)
        val btnCancel = dialog.findViewById<Button>(R.id.btnCancel)
        val textInputUserType = dialog.findViewById<TextInputLayout>(R.id.textInputUserType)

        textInputUserType.editText?.setText(userType)
        textInputUserType.editText?.clearFocus()
        (textInputUserType.editText as AutoCompleteTextView).setAdapter(arrayAdapter)

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        btnChange.setOnClickListener {
            dialog.dismiss()
            val newUserType = textInputUserType.editText?.text.toString()
            changeUserType(newUserType)
        }

        dialog.show()

    }

    @SuppressLint("SetTextI18n")
    private fun changeUserType(usertype: String) {
        binding.textViewStatus.text = "Saving User Type..."
        binding.clLoadingDataTransfer.visibility = View.VISIBLE

        val usersReference = dbReference.getReference("users")

        usersReference.child(userId).child("type").setValue(usertype)
            .addOnSuccessListener {
                binding.clLoadingDataTransfer.visibility = View.GONE
                binding.textViewUserType.text = userType
                Toast.makeText(this, "UserType Changed Successfully!!", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                binding.clLoadingDataTransfer.visibility = View.GONE
                Toast.makeText(this, "Failed Changing User Type!!", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showDeleteUserDialog() {

        val alertDialog = MaterialAlertDialogBuilder(this)

        alertDialog.setTitle("Delete $userName")
        alertDialog.setMessage("Are you sure you want to delete this user?")
        alertDialog.setCancelable(false)
        alertDialog.setNeutralButton("No") { dialog, _ ->
            dialog.dismiss()
        }
        alertDialog.setPositiveButton("Yes") { dialog, _ ->
            dialog.dismiss()
            deleteUser()
        }

        alertDialog.show()
    }

    @SuppressLint("SetTextI18n")
    private fun deleteUser() {
        binding.textViewStatus.text = "Deleting User..."
        binding.clLoadingDataTransfer.visibility = View.VISIBLE

        firebaseAuth.signOut()
        firebaseAuth.signInWithEmailAndPassword(userEmail, userPassword)
            .addOnSuccessListener {
                val user = firebaseAuth.currentUser
                user!!.delete()
                    .addOnSuccessListener {
                        firebaseAuth.signOut()
                        firebaseAuth.signInWithEmailAndPassword(mainUserEmail, mainUserPassword)
                            .addOnSuccessListener {
                                val usersReference = dbReference.getReference("users")
                                usersReference.child(userId).setValue(null)
                                    .addOnSuccessListener {
                                        binding.clLoadingDataTransfer.visibility = View.GONE
                                        binding.buttonDeleteUser.isEnabled = false
                                        Toast.makeText(this, "User Deleted Successfully!!.", Toast.LENGTH_SHORT).show()
                                    }
                                    .addOnFailureListener {
                                        binding.clLoadingDataTransfer.visibility = View.GONE
                                        Toast.makeText(this, "Deleting User Failed!!.", Toast.LENGTH_SHORT).show()
                                    }
                            }
                            .addOnFailureListener {
                                binding.clLoadingDataTransfer.visibility = View.GONE
                                Toast.makeText(this, "Deleting User Failed!!.", Toast.LENGTH_SHORT).show()
                            }
                    }
                    .addOnFailureListener {
                        binding.clLoadingDataTransfer.visibility = View.GONE
                        Toast.makeText(this, "Deleting User Failed!!.", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener {
                binding.clLoadingDataTransfer.visibility = View.GONE
                Toast.makeText(this, "Deleting User Failed!!.", Toast.LENGTH_SHORT).show()
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