package it.domenicoblanco.shopit


import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.gridlayout.widget.GridLayout
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.firebase.ui.database.FirebaseRecyclerAdapter
import com.firebase.ui.database.FirebaseRecyclerOptions
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage

/**
 * Wishlist adapter
 * Loads data obtained from the realtime database into the recycler view
 */
class WishListAdapter(options: FirebaseRecyclerOptions<Product>):
    FirebaseRecyclerAdapter<Product, WishListAdapter.WishListViewHolder>(options) {
    private val db = Firebase.database("https://shop-it-f599e-default-rtdb.europe-west1.firebasedatabase.app")
    private val fbUId = Firebase.auth.uid
    private var userWishlist: ArrayList<String>? = null
    private val storage = Firebase.storage
    private lateinit var parent: ViewGroup

    /**
     * Inflates a new row into the recycler view
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WishListViewHolder {
        this.parent = parent
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.layout_product_row, parent, false)

        return WishListViewHolder(view)
    }

    /**
     * Binds data from the database into the newly created row
     */
    override fun onBindViewHolder(holder: WishListViewHolder, position: Int, model: Product) {
        val imageReference = storage.getReferenceFromUrl(model.image.toString())

        holder.rowName.setText(model.name)
        holder.getCurrentId()
        holder.rowDesc.setText(model.description)
        holder.rowPrice.setText(model.price.toString())
        holder.rowRate.rating = calculateRating(model.stars)
        holder.rowRate.isEnabled = false
        holder.heartBtn.isChecked = true
        Glide.with(parent.context).load(imageReference).into(holder.rowImage)
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
            starsLength = 1L
        }

        return (starsValue/starsLength).toFloat()
    }

    /**
     * ViewHolder that refers to the new row, it handles some internal operations to preset value
     * that will be used on the binding phase
     */
    inner class WishListViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView), View.OnClickListener{
        val rowName: EditText
        val rowDesc: EditText
        val rowPrice: EditText
        val rowRate: RatingBar
        val rowImage: ImageView
        val heartBtn: ToggleButton

        val cardView: GridLayout
        var currentID: String? = null

        init {
            rowName = itemView.findViewById(R.id.product_title)
            rowDesc = itemView.findViewById(R.id.product_description)
            rowPrice = itemView.findViewById(R.id.product_price)
            rowRate = itemView.findViewById(R.id.product_rating)
            rowImage = itemView.findViewById(R.id.product_image)
            heartBtn = itemView.findViewById(R.id.product_wishlist)

            cardView = itemView.findViewById(R.id.product_grid)
            heartBtn.setOnClickListener(this)
        }

        /**
         * Obtains Product ID from realtime database
         */
        fun getCurrentId() {
            db.getReference("products").orderByChild("name")
                .equalTo(rowName.text.toString()).get()
                .addOnSuccessListener {
                    currentID = it.children.elementAt(0).key.toString()

                    db.getReference("/users/$fbUId/wishlist").get()
                        .addOnSuccessListener {
                            userWishlist = it.value as ArrayList<String>?
                            if (userWishlist?.contains(currentID.toString()) == true) {
                                cardView.visibility = View.VISIBLE
                            } else {
                                cardView.visibility = View.GONE
                            }
                        }
                }
        }

        /**
         * Removes a product from user's wishlist
         */
        private fun removeFromWishList() {
            db.getReference("users").child("${fbUId}/wishlist").get()
                .addOnSuccessListener {
                    val savedData = it.value as ArrayList<String>?
                    savedData?.remove("$currentID")

                    userWishlist = savedData
                    it.ref.setValue(savedData)
                    cardView.visibility = View.GONE
                }
        }

        override fun onClick(p0: View?) {
            if (p0?.id == heartBtn.id && !heartBtn.isChecked) {
                removeFromWishList()
            }
        }

    }
}