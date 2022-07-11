package it.domenicoblanco.shopit

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import java.io.InputStream

/**
 * Activity to create a new product and add it to the database
 */
class NewProductActivity : AppCompatActivity(), View.OnClickListener, TextWatcher {
    /* Database related constants */
    private val db = Firebase.database("https://shop-it-f599e-default-rtdb.europe-west1.firebasedatabase.app")
    private val storageRef = Firebase.storage.reference
    private val defaultStarsMap = arrayListOf(0L, 0L, 0L, 0L, 0L)

    /* Image chooser */
    private var imageStream: InputStream? = null
    private val imgPicker = Intent(Intent.ACTION_GET_CONTENT) // Get files from device
    // Callback whenever the result is obtained
    private val intentLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        when(it.resultCode) {
            RESULT_OK -> getImageFromDevice(it.data)
        }
    }

    private lateinit var returnToProduct: Intent // Returns to ProductActivity

    /* UI Elements */
    private lateinit var prodName: EditText
    private lateinit var prodDesc: EditText
    private lateinit var prodPrice: EditText
    private lateinit var prodImgButton: ImageButton
    private lateinit var prodImg: ImageView
    private lateinit var addProdButton: Button

    private var isAdmin: Boolean? = false


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_new_product)

        prodName = findViewById(R.id.product_name)
        prodDesc = findViewById(R.id.product_description)
        prodPrice = findViewById(R.id.product_price)
        prodImgButton = findViewById(R.id.product_image_button)
        prodImg = findViewById(R.id.product_image)
        addProdButton = findViewById(R.id.add_product)

        isAdmin = intent?.extras?.getBoolean("isAdmin")

        returnToProduct = Intent(this, ProductsActivity::class.java)
        imgPicker.type = "image/*" // Filtering intent to show only images

        prodImgButton.setOnClickListener(this)
        addProdButton.setOnClickListener(this)
    }

    /**
     * Called from a successful intent response, it obtains a new InputStream that will be uploaded
     * to the cloud storage and sets the image into the current view
     */
    private fun getImageFromDevice(data: Intent?) {
        if (data?.data != null) {
            imageStream = contentResolver.openInputStream(data.data!!)
            prodImg.setImageURI(data.data)

            checkIfAllFieldsAreValid()
        }
    }

    /**
     * Uploads the image to Firebase Cloud Storage
     *
     * @return URL to uploaded image
     */
    private suspend fun uploadImage(fileName: String?): String {
        val uploadingImage = storageRef.child("${fileName}.jpg")
        uploadingImage.putStream(imageStream!!).await()

        return uploadingImage.downloadUrl.await().toString()
    }

    /**
     * Uploads the newly created product to Firebase realtime database
     */
    private fun addProdToFirestore() {
        val products = db.getReference("products")
        val newEntryOnDB = products.push()
        var imageUrl = ""

        runBlocking { // A blocking coroutine to prevent a change of activity while updating the db
            launch {
                imageUrl = uploadImage(newEntryOnDB.key)
            }
        }

        val newProduct = Product(
            prodName.text.toString(),
            prodDesc.text.toString(),
            imageUrl,
            prodPrice.text.toString().toDouble(),
            defaultStarsMap
        )

        newEntryOnDB.setValue(newProduct)
            .addOnSuccessListener {
                returnToProduct.putExtra("isAdmin", isAdmin)
                startActivity(returnToProduct)
            }
            .addOnFailureListener {
                Toast.makeText(this, R.string.error_during_add_new_product, Toast.LENGTH_SHORT)
                    .show()
            }
    }

    override fun onClick(p0: View?) {
        when(p0?.id) {
            prodImgButton.id -> intentLauncher.launch(imgPicker)
            addProdButton.id -> addProdToFirestore()
        }
    }

    /**
     * Enables or disables the add product button
     */
    private fun checkIfAllFieldsAreValid() {
        addProdButton.isEnabled = (prodName.text.isNotEmpty() && prodDesc.text.isNotEmpty()
                && prodPrice.text.isNotEmpty() && imageStream != null)
    }

    override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
        // Nothing to watch here
    }

    override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
        checkIfAllFieldsAreValid()
    }

    override fun afterTextChanged(p0: Editable?) {
        // Nothing to watch here
    }

    /**
     * Override to save the isAdmin value, in order to prevent unnecessary calls to the db
     */
    override fun onBackPressed() {
        super.onBackPressed()
        returnToProduct.putExtra("isAdmin", isAdmin)
        startActivity(returnToProduct)
    }
}