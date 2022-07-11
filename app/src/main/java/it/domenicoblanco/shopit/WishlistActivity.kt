package it.domenicoblanco.shopit

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.firebase.ui.database.FirebaseRecyclerOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

/**
 * The wishlist activity
 */
class WishlistActivity : AppCompatActivity() {
    private val db = Firebase.database("https://shop-it-f599e-default-rtdb.europe-west1.firebasedatabase.app")
    private val auth = FirebaseAuth.getInstance()
    private val options = FirebaseRecyclerOptions.Builder<Product>()
        .setQuery(db.getReference("products"), Product::class.java)
        .build()

    private val layoutManager = LinearLayoutManager(this)

    private lateinit var previousAct: Intent
    private var isAdmin:Boolean = false
    private lateinit var adapter: WishListAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyWishList: TextView


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_wishlist)

        recyclerView = findViewById(R.id.products_wishlist)
        emptyWishList = findViewById(R.id.empty_wishlist)
        recyclerView.layoutManager = layoutManager

        previousAct = Intent(this, ProductsActivity::class.java)
        isAdmin = intent?.extras?.getBoolean("isAdmin") ?: false

        adapter = WishListAdapter(options)
        recyclerView.adapter = adapter

        checkWishList()
    }

    /**
     * Checks if wishlist contains at least an element
     * In case is empty shows a message
     */
    private fun checkWishList() {
        db.getReference("users").child("${auth.uid}/wishlist").get()
            .addOnSuccessListener {
                val products = it.value as ArrayList<String>?

                if (products.isNullOrEmpty()) {
                    emptyWishList.isVisible = true
                }
            }
    }

    override fun onStart() {
        super.onStart()
        adapter.startListening()
    }

    override fun onStop() {
        super.onStop()
        adapter.stopListening()
    }

    override fun onBackPressed() {
        super.onBackPressed()
        previousAct.putExtra("isAdmin", isAdmin)
        startActivity(previousAct)
    }
}