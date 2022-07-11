package it.domenicoblanco.shopit

/**
 * The product as it is memorized in Firebase realtime database, needs also an empty constructor
 */
data class Product(
    val name: String?,
    val description: String?,
    val image: String?,
    val price: Double?,
    val stars: ArrayList<Long>?
) {
    constructor(): this(null, null, null, null, null)
}