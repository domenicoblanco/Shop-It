package it.domenicoblanco.shopit

/**
 * The user as it is memorized in Firebase realtime database
 */
data class User (
    val isAdmin: Boolean = false,
    val stars: Map<String, List<String>?> = hashMapOf(
        "0" to null,
        "1" to null,
        "2" to null,
        "3" to null,
        "4" to null
    ),
    val wishlist: ArrayList<String>? = null
)