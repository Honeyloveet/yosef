package net.sampro.yosef

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import net.sampro.yosef.databinding.ActivityLoginBinding
import net.sampro.yosef.utils.LoggedInUser

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var firebaseAuth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseAuth = FirebaseAuth.getInstance()

        binding.btnLogin.setOnClickListener {
            val userName = binding.editTextUserName.text.toString().replace(" ","").lowercase()
            val password = binding.editTextUserPassword.text.toString()

            if (userName.isNotEmpty() && password.isNotEmpty()) {
                val email = "$userName@gmail.com"
                firebaseAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener {
                    if (it.isSuccessful) {
                        LoggedInUser.uid = firebaseAuth.currentUser?.uid ?: "No UID"
                        LoggedInUser.userName = firebaseAuth.currentUser?.displayName ?: "No Display Name"
                        LoggedInUser.email = firebaseAuth.currentUser?.email ?: "No Email"
                        LoggedInUser.type = "To be implemented"

                        val intent = Intent(this, FileTransferActivity::class.java)
                        startActivity(intent)

                        this.finish()
                    } else {
                        Toast.makeText(this, it.exception.toString(), Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                Toast.makeText(this, "Please enter user name and password.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onStart() {
        super.onStart()

        if (firebaseAuth.currentUser != null) {
            LoggedInUser.uid = firebaseAuth.currentUser?.uid ?: "No UID"
            LoggedInUser.userName = firebaseAuth.currentUser?.displayName ?: "No Display Name"
            LoggedInUser.email = firebaseAuth.currentUser?.email ?: "No Email"
            LoggedInUser.type = "To be implemented"

            val intent = Intent(this, FileTransferActivity::class.java)
            startActivity(intent)
            this.finish()
        }
    }
}