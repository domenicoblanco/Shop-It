package it.domenicoblanco.shopit

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * The default activity, it allows to login and register
 */
class MainActivity : AppCompatActivity(), View.OnClickListener {
    private val db = Firebase.database("https://shop-it-f599e-default-rtdb.europe-west1.firebasedatabase.app")
    private val auth = Firebase.auth
    private lateinit var loginBtn: Button
    private lateinit var registerBtn: Button
    private lateinit var loginRegisterIntent: Intent

    init {
        // Enables local cache
        db.setPersistenceEnabled(true)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        loginBtn = findViewById(R.id.login_button)
        registerBtn = findViewById(R.id.register_button)

        loginBtn.setOnClickListener(this)
        registerBtn.setOnClickListener(this)


        if (auth.currentUser?.isAnonymous == false) {
            lifecycleScope.launch { // An asynchronous task to prevent to block the UI thread
                checkIfAdmin() // Preload user, in order to skip the login process
            }
        }

        loginRegisterIntent = Intent(this, LoginActivity::class.java)
    }

    /**
     * Calls Firebase Realtime Database and checks if the current logged user is an administrator
     * In any case the result will lead to the next activity
     */
    private suspend fun checkIfAdmin() {
        db.getReference("users/${auth.uid}/admin").get()
            .addOnSuccessListener {
                val isAdmin = it.value as Boolean?

                if (isAdmin != null) {
                    val goToProducts = Intent(this, ProductsActivity::class.java)
                    goToProducts.putExtra("isAdmin", isAdmin)
                    startActivity(goToProducts)
                }
            }
            .await()
    }

    override fun onClick(p0: View?) {
        when (p0?.id) {
            R.id.login_button -> loginRegisterIntent.putExtra("op", "login")
            R.id.register_button -> loginRegisterIntent.putExtra("op", "register")
        }

        startActivity(loginRegisterIntent)
    }
}