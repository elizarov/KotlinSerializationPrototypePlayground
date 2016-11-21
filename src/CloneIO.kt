import kotlin.serialization.ElementValueTransformer
import kotlin.serialization.KSerializer

/**
 * @author Roman Elizarov
 */

object Clone : ElementValueTransformer() {}

fun testCloneIO(serializer: KSerializer<Any>, obj: Any): Result {
    // clone
    val other = Clone.transform(serializer, obj)
    // result
    return Result(obj, other, "cloned")
}

fun main(args: Array<String>) {
    testMethod(::testCloneIO)
}
