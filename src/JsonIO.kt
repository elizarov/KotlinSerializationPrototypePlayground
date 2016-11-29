import kotlin.serialization.KSerializer

/**
 * @author Roman Elizarov
 */

fun testJsonIO(serializer: KSerializer<Any>, obj: Any): Result {
    val str = JSON.stringify(serializer, obj)
    val other = JSON.parse(str, serializer)
    return Result(obj, other, "${str.length} chars $str")
}

fun testJsonUnquotedIO(serializer: KSerializer<Any>, obj: Any): Result {
    val str = JSON.unquoted.stringify(serializer, obj)
    val other = JSON.parse(str, serializer)
    return Result(obj, other, "${str.length} chars $str")
}

fun testJsonIndentedIO(serializer: KSerializer<Any>, obj: Any): Result {
    val str = JSON.indented.stringify(serializer, obj)
    val other = JSON.parse(str, serializer)
    return Result(obj, other, "${str.length} chars $str")
}

fun main(args: Array<String>) {
    testMethod(::testJsonIO)
//    testMethod(::testJsonUnquotedIO)
//    testMethod(::testJsonIndentedIO)
}
