import kotlin.serialization.Serializable

// Shop from Kotlin Koans

@Serializable
data class Shop(val name: String, val customers: List<Customer>)

@Serializable
data class Customer(val name: String, val city: City, val orders: List<Order>) {
    override fun toString() = "$name from ${city.name} with $orders"
}

@Serializable
data class Order(val products: List<Product>, val isDelivered: Boolean) {
    override fun toString() = "$products${ if (isDelivered) " delivered" else "" }"
}

@Serializable
data class Product(val name: String, val price: Double) {
    override fun toString() = "'$name' for $price"
}

@Serializable
data class City(val name: String) {
    override fun toString() = name
}