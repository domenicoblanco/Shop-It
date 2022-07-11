package it.domenicoblanco.shopit

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.*
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.firebase.ui.database.FirebaseRecyclerOptions
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

/**
 * The product's landing page
 */
class ProductsActivity : AppCompatActivity(), View.OnClickListener {
    private val db = Firebase.database("https://shop-it-f599e-default-rtdb.europe-west1.firebasedatabase.app")
    private val auth = FirebaseAuth.getInstance()
    private val options = FirebaseRecyclerOptions.Builder<Product>()
        .setQuery(db.getReference("products"), Product::class.java)
        .build()

    private val layoutManager = LinearLayoutManager(this)

    private lateinit var adapter: ProductAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var nextView: Intent
    private lateinit var fabButton: FloatingActionButton

    private var isAdmin: Boolean = false


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_products)

        isAdmin = intent.extras?.get("isAdmin").toString().toBoolean()

        recyclerView = findViewById(R.id.products_listview)
        recyclerView.layoutManager = layoutManager

        adapter = ProductAdapter(options, isAdmin)
        recyclerView.adapter = adapter

        handleAdminView()
    }

    /**
     * A simple options menu in the topbar to show wishlist and the sign out buttons
     */
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.topmenu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    /**
     * Click event handler for the top menu
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId) {
            R.id.wishlist_button -> {
                nextView = Intent(this, WishlistActivity::class.java)
                nextView.putExtra("isAdmin", isAdmin)
                startActivity(nextView)
            }

            R.id.logout_button -> {
                auth.signOut()
                nextView = Intent(this, MainActivity::class.java)
                startActivity(nextView)
            }
        }
        return super.onOptionsItemSelected(item)
    }

    /**
     * Enables the FAB if the user is an administrator
     */
    private fun handleAdminView() {
        if (isAdmin) {
            fabButton = findViewById(R.id.products_fab)
            fabButton.visibility = FloatingActionButton.VISIBLE
            fabButton.setOnClickListener(this)
        }
    }

    /**
     * Stops firebase data change listener
     */
    override fun onStop() {
        super.onStop()
        adapter.stopListening()
    }

    /**
     * Starts firebase data change listener
     */
    override fun onStart() {
        super.onStart()
        adapter.startListening()
    }

    override fun onClick(p0: View?) {
        if (p0?.id == fabButton.id) { // Moves to New Product
            nextView = Intent(this, NewProductActivity::class.java)
            nextView.putExtra("isAdmin", isAdmin)
            startActivity(nextView)
        }
    }

    /**
     * Returns to main activity
     */
    override fun onBackPressed() {
        super.onBackPressed()
        finish()
    }
}