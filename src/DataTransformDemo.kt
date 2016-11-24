import kotlin.serialization.ElementValueTransformer
import kotlin.serialization.KSerialClassDesc
import kotlin.serialization.Serializable

/**
 * @author Roman Elizarov
 */

@Serializable
data class Person(val firstName:String, val lastName:String)

object LowercaseTransformer : ElementValueTransformer() {
    override fun transformStringValue(desc: KSerialClassDesc, index: Int, value: String): String =
            value.toLowerCase()
}

fun main(args: Array<String>) {
    val p = Person("Dart", "Vader")
    println("Original person: $p")
    val q = LowercaseTransformer.transform(p)
    println("Transformed person: $q")
}