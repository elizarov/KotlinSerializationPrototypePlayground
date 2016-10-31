import java.io.PrintWriter
import java.io.StringReader
import java.io.StringWriter
import kotlin.reflect.KClass
import kotlin.serialization.ElementValueInput
import kotlin.serialization.ElementValueOutput
import kotlin.serialization.KSerialClassDesc
import kotlin.serialization.KSerializer

class KeyValueOutput(val out: PrintWriter) : ElementValueOutput() {
    override fun writeBegin(desc: KSerialClassDesc) = out.print('{')
    override fun writeEnd(desc: KSerialClassDesc) = out.print('}')

    override fun writeElement(desc: KSerialClassDesc, index: Int) {
        if (index > 0) out.print(", ")
        out.print(desc.getElementName(index));
        out.print(':')
    }

    override fun writeNullValue() = out.print("null")
    override fun writeValue(value: Any) = out.print(value)

    override fun writeStringValue(value: String) {
        out.print('"')
        out.print(value)
        out.print('"')
    }

    override fun writeCharValue(value: Char) = writeStringValue(value.toString())
}

class KeyValueInput(val inp: Parser) : ElementValueInput() {
    override fun readBegin(desc: KSerialClassDesc) = inp.expectAfterWhiteSpace('{')
    override fun readEnd(desc: KSerialClassDesc) = inp.expectAfterWhiteSpace('}')

    override fun readElement(desc: KSerialClassDesc): Int {
        inp.skipWhitespace(',')
        val name = inp.nextUntil(':', '}')
        if (name.isEmpty())
            return READ_DONE
        val index = desc.getElementIndex(name)
        inp.expect(':')
        return index
    }

    private fun readToken(): String {
        inp.skipWhitespace()
        return inp.nextUntil(' ', ',', '}')
    }

    override fun readNotNullMark(): Boolean {
        inp.skipWhitespace()
        if (inp.cur != 'n'.toInt()) return true
        return false
    }

    override fun readNullValue(): Nothing? {
        check(readToken() == "null") { "'null' expected" }
        return null
    }

    override fun readBooleanValue(): Boolean = readToken().toBoolean()
    override fun readByteValue(): Byte = readToken().toByte()
    override fun readShortValue(): Short = readToken().toShort()
    override fun readIntValue(): Int = readToken().toInt()
    override fun readLongValue(): Long = readToken().toLong()
    override fun readFloatValue(): Float = readToken().toFloat()
    override fun readDoubleValue(): Double = readToken().toDouble()

    override fun <T : Enum<T>> readEnumValue(enumClass: KClass<T>): T {
        return java.lang.Enum.valueOf(enumClass.java, readToken())
    }

    override fun readStringValue(): String {
        inp.expectAfterWhiteSpace('"')
        val value = inp.nextUntil('"')
        inp.expect('"')
        return value
    }

    override fun readCharValue(): Char = readStringValue().single()
}

fun testKeyValueIO(serializer: KSerializer<Any>, obj: Any): Result {
    // save
    val sw = StringWriter()
    val out = KeyValueOutput(PrintWriter(sw))
    out.write(serializer, obj)
    // load
    val str = sw.toString()
    val inp = KeyValueInput(Parser(StringReader(str)))
    val other = inp.read(serializer)
    // result
    return Result(obj, other, "${str.length} chars $str")
}

fun main(args: Array<String>) {
//    testCase(CountyData, CountyData("US", listOf(CityData(1, "New York"), CityData(2, "Chicago"))), ::testKeyValueIO)
//    testCase(Zoo, zoo, ::testKeyValueIO)
    testMethod(::testKeyValueIO)
}
