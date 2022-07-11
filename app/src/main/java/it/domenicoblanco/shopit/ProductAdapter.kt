package it.domenicoblanco.shopit

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.firebase.ui.database.FirebaseRecyclerAdapter
import com.firebase.ui.database.FirebaseRecyclerOptions
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage

/**
 * Product adapter
 * Loads data obtained from the realtime database into the recycler view
 */
class ProductAdapter(options: FirebaseRecyclerOptions<Product>, private val isAdmin: Boolean):
    FirebaseRecyclerAdapter<Product, ProductAdapter.ProductsViewHolder>(options) {
    private val db =
        Firebase.database("https://shop-it-f599e-default-rtdb.europe-west1.firebasedatabase.app")
    private val fbUId = Firebase.auth.uid
    private var userWishlist: ArrayList<String>? = null
    private val storage = Firebase.storage

    private lateinit var context: Context

    /**
     * Inflates a new row into the recycler view
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductsViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.layout_product_row, parent, false)
        context = parent.context

        return ProductsViewHolder(view)
    }

    /**
     * Binds data from the database into the newly created row
     */
    override fun onBindViewHolder(holder: ProductsViewHolder, position: Int, model: Product) {
        val imageReference = storage.getReferenceFromUrl(model.image.toString())

        if (isAdmin) {
            holder.adminButtons.visibility = View.VISIBLE
        }

        holder.rowName.setText(model.name)
        holder.getCurrentId()
        holder.rowDesc.setText(model.description)
        holder.rowPrice.setText(model.price.toString())
        holder.rowRate.rating = calculateRating(model.stars)
        // Obtains the image from the db, if necessary
        Glide.with(context).load(imageReference).into(holder.rowImage)
    }

    /**
     * Calculate the rate from the data in the db and renders it properly
     *
     * @return Rate value
     */
    private fun calculateRating(starsArray: ArrayList<Long>?): Float {
        if (starsArray == null || starsArray.isEmpty()) {
            return 0f
        }

        var starsLength = 0L
        var starsValue = 0L

        starsArray.forEachIndexed { index, value ->
            starsLength += value
            starsValue += (index + 1) * value
        }

        if (starsLength == 0L) {
            return 0f
        }

        return (starsValue / starsLength).toFloat()
    }

    /**
     * ViewHolder that refers to the new row, it handles some internal operations to preset value
     * that will be used on the binding phase
     */
    inner class ProductsViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView),
        View.OnClickListener,
        RatingBar.OnRatingBarChangeListener {
        private val storageRef = storage.reference

        val rowName: EditText
        val rowDesc: EditText
        val rowPrice: EditText
        val rowRate: RatingBar
        val rowImage: ImageView

        /* Admin buttons */
        val adminButtons: LinearLayout
        private val editBtn: ImageButton
        private val saveBtn: ImageButton
        private val deleteBtn: ImageButton

        private val heartBtn: ToggleButton

        // Tracks the previous name to search in the db
        private lateinit var previousName: CharSequence
        // Product ID calculated once to prevent multiple calls to the db
        private var currentID: String? = null

        init {
            rowName = itemView.findViewById(R.id.product_title)
            rowDesc = itemView.findViewById(R.id.product_description)
            rowPrice = itemView.findViewById(R.id.product_price)
            rowRate = itemView.findViewById(R.id.product_rating)
            rowImage = itemView.findViewById(R.id.product_image)

            adminButtons = itemView.findViewById(R.id.product_admin_buttons)
            editBtn = itemView.findViewById(R.id.product_edit)
            saveBtn = itemView.findViewById(R.id.product_save)
            deleteBtn = itemView.findViewById(R.id.product_delete)
            heartBtn = itemView.findViewById(R.id.product_wishlist)

            editBtn.setOnClickListener(this)
            saveBtn.setOnClickListener(this)
            deleteBtn.setOnClickListener(this)
            heartBtn.setOnClickListener(this)
            rowRate.onRatingBarChangeListener = this
        }

        /**
         * Obtains Product ID from realtime database
         */
        fun getCurrentId() {
            db.getReference("products").orderByChild("name")
                .equalTo(rowName.text.toString()).get()
                .addOnSuccessListener {
                    currentID = it.children.elementAt(0).key.toString()

                    db.getReference("users/${fbUId}/wishlist").get()
                        .addOnSuccessListener {
                            userWishlist = it.value as ArrayList<String>?
                            heartBtn.isChecked = userWishlist?.contains("$currentID") == true
                        }
                }
        }

        /**
         * Deletes a product by its ID
         */
        private fun deleteProduct() {
            db.getReference("products").child("$currentID").get()
                .addOnSuccessListener {
                    val imageToDelete = storageRef.child("${currentID}.jpg")
                    imageToDelete.delete()
                        .addOnSuccessListener {
                            Log.d("Image delete", "Success")
                        }
                        .addOnFailureListener {
                            Log.w("Image delete", it)
                        }

                    it.ref.removeValue()
                }
        }

        /**
         * Enable edit mode inside the row
         */
        private fun editProduct() {
            previousName = rowName.text
            editBtn.visibility = View.GONE
            saveBtn.visibility = View.VISIBLE
            rowName.isFocusableInTouchMode = true
            rowDesc.isFocusableInTouchMode = true
            rowPrice.isFocusableInTouchMode = true
        }

        /**
         * Update data modified into the db
         */
        private fun saveProduct() {
            saveBtn.visibility = View.GONE
            editBtn.visibility = View.VISIBLE
            rowName.isFocusableInTouchMode = false
            rowName.clearFocus()
            rowDesc.isFocusableInTouchMode = false
            rowDesc.clearFocus()
            rowPrice.isFocusableInTouchMode = false
            rowPrice.clearFocus()


            db.getReference("products").child("$currentID").get()
                .addOnSuccessListener {
                    it.ref.updateChildren(
                        mapOf(
                            "name" to rowName.text.toString(),
                            "description" to rowDesc.text.toString(),
                            "price" to rowPrice.text.toString().toDouble()
                        )
                    )
                }
        }

        /**
         * Add a rate into the db and updates the view
         */
        private fun updateCurrentRating(rate: Float) {
            db.getReference("products").child("$currentID/stars").get()
                .addOnSuccessListener {
                    val stars = it.value as ArrayList<Long>
                    stars[rate.toInt() - 1] += 1L

                    it.ref.setValue(stars)
                        .addOnSuccessListener {
                            rowRate.rating = calculateRating(stars)
                        }
                }
        }

        /**
         * Adds a product into the user's wishlist
         */
        private fun addToWishList() {
            db.getReference("users").child("${fbUId}/wishlist").get()
                .addOnSuccessListener {
                    var savedData = it.value as ArrayList<String>?

                    if (savedData.isNullOrEmpty()) {
                        savedData = arrayListOf("$currentID")
                    } else {
                        savedData.add("$currentID")
                    }

                    it.ref.setValue(savedData)
                }
        }

        /**
         * Removes a product from user's wishlist
         */
        private fun removeFromWishList() {
            db.getReference("users").child("${fbUId}/wishlist").get()
                .addOnSuccessListener {
                    val savedData = it.value as ArrayList<*>?
                    savedData?.remove("$currentID")

                    it.ref.setValue(savedData)
                }
        }

        override fun onClick(p0: View?) {
            when (p0?.id) {
                editBtn.id -> editProduct()
                saveBtn.id -> saveProduct()
                deleteBtn.id -> deleteProduct()
                heartBtn.id -> {
                    if (heartBtn.isChecked) {
                        addToWishList()
                    } else {
                        removeFromWishList()
                    }
                }
            }
        }

        /**
         * An event listener for whenever an user adds a new rate
         */
        override fun onRatingChanged(p0: RatingBar?, p1: Float, p2: Boolean) {
            if (p0?.id == rowRate.id && p2) {
                updateCurrentRating(p1)
            }
        }
    }
}