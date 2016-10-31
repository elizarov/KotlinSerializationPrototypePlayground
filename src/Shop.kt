import kotlin.serialization.KSerializable

// Shop from Kotlin Koans

@KSerializable
data class Shop(val name: String, val customers: List<Customer>)

@KSerializable
data class Customer(val name: String, val city: City, val orders: List<Order>) {
    override fun toString() = "$name from ${city.name} with $orders"
}

@KSerializable
data class Order(val products: List<Product>, val isDelivered: Boolean) {
    override fun toString() = "$products${ if (isDelivered) " delivered" else "" }"
}

@KSerializable
data class Product(val name: String, val price: Double) {
    override fun toString() = "'$name' for $price"
}

@KSerializable
data class City(val name: String) {
    override fun toString() = name
}