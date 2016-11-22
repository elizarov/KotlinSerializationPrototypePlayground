import kotlin.serialization.KSerializer

/**
 * @author Roman Elizarov
 */

fun testJsonIO(serializer: KSerializer<Any>, obj: Any): Result {
    // save
    val str = JSON.stringify(serializer, obj)
    // load
    val other = JSON.parse(str, serializer)
    // result
    return Result(obj, other, "${str.length} chars $str")
}

fun main(args: Array<String>) {
    testMethod(::testJsonIO)
}
