package it.domenicoblanco.shopit

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * Activity to perform login with Firebase
 * Supports a basic authentication
 */
class LoginActivity : AppCompatActivity(), View.OnClickListener {
    // Firebase database reference
    private val db = Firebase.database("https://shop-it-f599e-default-rtdb.europe-west1.firebasedatabase.app")
    private val auth = Firebase.auth // Firebase Session reference
    private lateinit var emailField: EditText
    private lateinit var passField: EditText
    private lateinit var opBtn: Button
    private lateinit var operationToDo: String
    private lateinit var goToProducts: Intent
    private var isAdmin: Boolean? = false


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        emailField = findViewById(R.id.editTextTextEmailAddress)
        passField = findViewById(R.id.editTextTextPassword)
        opBtn = findViewById(R.id.buttonCompleteRegister)
        goToProducts = Intent(this, ProductsActivity::class.java)

        opBtn.setOnClickListener(this)

        // Extracts additional data from the intent
        operationToDo = intent.extras?.get("op").toString()

        if (auth.currentUser?.isAnonymous == false) {
            lifecycleScope.launch { // An asynchronous task to prevent to block the UI thread
                checkIfAdmin() // Preload user, in order to skip the login process
            }
        }

        // Changes the button text (and following operations)
        if (operationToDo == "login") {
            opBtn.text = getString(R.string.login)
        }
    }

    /**
     * Calls Firebase Realtime Database and checks if the current logged user is an administrator
     * In any case the result will lead to the next activity
     */
    private suspend fun checkIfAdmin() {
        db.getReference("users/${auth.uid}/admin").get()
            .addOnSuccessListener {
                isAdmin = it.value as Boolean?

                if (isAdmin != null) {
                    goToProducts.putExtra("isAdmin", isAdmin)
                    startActivity(goToProducts)
                }
            }
            .await()
    }

    /**
     * The login helper, is called if the intent specified to login and when the user
     * will complete to fill the form and press the login button.
     * The user must have already verified its email, in order for the process to proceed
     */
    private fun attempLogin() {
        auth.signInWithEmailAndPassword(emailField.text.toString(), passField.text.toString())
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Sign in success, update UI with the signed-in user's information
                    if (auth.currentUser?.isEmailVerified == true) {
                        lifecycleScope.launch {
                            checkIfAdmin()
                        }
                    } else {
                        Toast.makeText(baseContext, getString(R.string.please_verify_email),
                            Toast.LENGTH_SHORT).show()
                    }
                } else {
                    // If sign in fails, display a message to the user.
                    passField.text.clear()
                    Toast.makeText(baseContext, R.string.auth_failed,
                        Toast.LENGTH_SHORT).show()
                }
            }
    }

    /**
     * The registration helper, is called if the intent specified to register and when the user
     * will complete to fill the form and press the register button
     */
    private fun attempRegistration() {
        auth.createUserWithEmailAndPassword(emailField.text.toString(), passField.text.toString())
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser

                    user?.sendEmailVerification()
                        ?.addOnCompleteListener { _ ->
                            if (task.isSuccessful) {
                                Toast.makeText(this, R.string.check_inbox,
                                    Toast.LENGTH_SHORT).show()

                                setUserData(auth.uid)

                                opBtn.text = getString(R.string.login)
                                operationToDo = "login"
                            }
                        }

                } else {
                    // If sign in fails, display a message to the user.
                    Toast.makeText(baseContext, R.string.registration_failed,
                        Toast.LENGTH_SHORT).show()
                }
            }
    }

    /**
     * Creates a new user object inside the realtime database
     */
    private fun setUserData(uID: String?) {
        val newUser = User()
        db.getReference("/users/${uID}").setValue(newUser)
    }

    override fun onClick(p0: View?) {
        if (p0?.id == R.id.buttonCompleteRegister) {
            when (operationToDo) {
                "login" -> attempLogin()
                "register" -> attempRegistration()
            }
        }
    }
}