import java.io.PrintWriter
import java.io.Reader
import java.io.StringReader
import java.io.StringWriter
import kotlin.reflect.KClass
import kotlin.reflect.isSubclassOf
import kotlin.serialization.*

/**
 * @author Roman Elizarov
 */

object JSON {
    // todo: add 'replacer' param
    fun <T> stringify(saver: KSerialSaver<T>, obj: T): String {
        val sw = StringWriter()
        val output = JsonOutput(Mode.OBJ, PrintWriter(sw))
        output.write(saver, obj)
        return sw.toString()
    }

    inline fun <reified T : Any> stringify(obj: T): String = stringify(T::class.serializer(), obj)

    // todo: add 'reviver' param
    fun <T> parse(str: String, loader: KSerialLoader<T>): T {
        val parser = Parser(StringReader(str))
        val input = JsonInput(Mode.OBJ, parser)
        val result = input.read(loader)
        check(parser.curToken is Token.EOF)
        return result
    }

    inline fun <reified T : Any> parse(str: String): T = parse(str, T::class.serializer())

    //================================= implementation =================================

    private const val NULL = "null"

    private const val COMMA = ','
    private const val COLON = ':'
    private const val BEGIN_OBJ = '{'
    private const val END_OBJ = '}'
    private const val BEGIN_LIST = '['
    private const val END_LIST = ']'
    private const val STRING_DELIM = '"'
    private const val STRING_ESC = '\\'

    private const val NONE = 0.toChar()

    private enum class Mode(val begin: Char, val end: Char) {
        OBJ(BEGIN_OBJ, END_OBJ),
        LIST(BEGIN_LIST, END_LIST),
        MAP(BEGIN_OBJ, END_OBJ),
        ENTRY(NONE, NONE)
    }

    private val PRIMITIVE_CLASSES = setOf(
            Boolean::class, Byte::class, Short::class, Int::class, Long::class,
            Float::class, Double::class, Char::class, String::class
    )

    private fun switchMode(mode: Mode, desc: KSerialClassDesc, typeParams: Array<out KSerializer<*>>): Mode =
            when (desc.kind) {
                KSerialClassKind.LIST, KSerialClassKind.SET -> Mode.LIST
                KSerialClassKind.MAP -> {
                    val keyClass = typeParams[0].serializableClass
                    if (PRIMITIVE_CLASSES.contains(keyClass) || keyClass.isSubclassOf(Enum::class))
                        Mode.MAP else Mode.LIST
                }
                KSerialClassKind.ENTRY -> if (mode == Mode.MAP) Mode.ENTRY else Mode.OBJ
                else -> Mode.OBJ
            }

    private class JsonOutput(val mode: Mode, val w: PrintWriter) : ElementValueOutput() {
        private var forceStr: Boolean = false

        override fun writeBegin(desc: KSerialClassDesc, vararg typeParams: KSerializer<*>): KOutput {
            val newMode = switchMode(mode, desc, typeParams)
            if (newMode.begin != NONE) w.print(newMode.begin)
            return if (mode == newMode) this else JsonOutput(newMode, w) // todo: reuse instance per mode
        }

        override fun writeEnd(desc: KSerialClassDesc) {
            if (mode.end != NONE) w.print(mode.end)
        }

        override fun writeElement(desc: KSerialClassDesc, index: Int): Boolean {
            when (mode) {
                Mode.LIST, Mode.MAP -> {
                    if (index == 0) return false
                    if (index > 1)
                        w.print(COMMA)
                }
                Mode.ENTRY -> {
                    if (index == 0)
                        forceStr = true
                    if (index == 1) {
                        w.print(COLON)
                        forceStr = false
                    }
                }
                else -> {
                    if (index > 0)
                        w.print(COMMA)
                    writeStringValue(desc.getElementName(index))
                    w.print(COLON)
                }
            }
            return true
        }

        override fun writeNullValue() {
            w.print(NULL)
        }

        override fun writeBooleanValue(value: Boolean) { if (forceStr) writeStringValue(value.toString()) else w.print(value) }
        override fun writeByteValue(value: Byte) { if (forceStr) writeStringValue(value.toString()) else w.print(value) }
        override fun writeShortValue(value: Short) { if (forceStr) writeStringValue(value.toString()) else w.print(value) }
        override fun writeIntValue(value: Int) { if (forceStr) writeStringValue(value.toString()) else w.print(value) }
        override fun writeLongValue(value: Long) { if (forceStr) writeStringValue(value.toString()) else w.print(value) }

        override fun writeFloatValue(value: Float) {
            if (forceStr || !value.isFinite()) writeStringValue(value.toString()) else
                w.print(value)
        }

        override fun writeDoubleValue(value: Double) {
            if (forceStr || !value.isFinite()) writeStringValue(value.toString()) else
                w.print(value)
        }

        override fun writeCharValue(value: Char) {
            writeStringValue(value.toString())
        }

        override fun writeStringValue(value: String) {
            w.print(STRING_DELIM)
            for (c in value)
                when (c) {
                    '\\' -> w.print("\\\\")
                    '"' -> w.print("\\\"")
                    else -> w.print(c)
                }
            w.print(STRING_DELIM)
        }

        override fun writeValue(value: Any) {
            writeStringValue(value.toString())
        }
    }

    private class JsonInput(val mode: Mode, val p: Parser) : ElementValueInput() {
        val curToken: Token get() = p.curToken
        fun next(): Token = p.next()

        var curIndex = 0
        var entryIndex = 0

        override fun readBegin(desc: KSerialClassDesc, vararg typeParams: KSerializer<*>): KInput {
            val newMode = switchMode(mode, desc, typeParams)
            if (newMode.begin != NONE) {
                val token = next()
                check(token is Token.Ch && token.c == newMode.begin)
            }
            return when (newMode) {
                Mode.LIST, Mode.MAP -> JsonInput(newMode, p) // need fresh cur index
                else -> if (mode == newMode) this else
                            JsonInput(newMode, p) // todo: reuse instance per mode
            }
        }

        override fun readEnd(desc: KSerialClassDesc) {
            if (mode.end != NONE) {
                val token = next()
                check(token is Token.Ch && token.c == mode.end)
            }
        }

        override fun readNotNullMark(): Boolean {
            val token = curToken
            if (token is Token.Lit && token.str == NULL) return false
            return true
        }

        override fun readNullValue(): Nothing? {
            val token = next()
            check(token is Token.Lit && token.str == NULL) { "expected 'null'" }
            return null
        }

        override fun readElement(desc: KSerialClassDesc): Int {
            val commaToken = curToken
            if (commaToken is Token.Ch && commaToken.c == COMMA) next()
            when (mode) {
                Mode.LIST, Mode.MAP -> {
                    if (!curToken.canBeginValue)
                        return READ_DONE
                    return ++curIndex
                }
                Mode.ENTRY -> {
                    when (entryIndex++) {
                        0 -> return 0
                        1 -> {
                            val token = next()
                            check(token is Token.Ch && token.c == COLON)
                            return 1
                        }
                        else -> {
                            entryIndex = 0
                            return READ_DONE
                        }
                    }
                }
                else -> {
                    if (!curToken.canBeginValue)
                        return READ_DONE
                    val keyToken = next()
                    check(keyToken.hasStr)
                    val colonToken = next()
                    check(colonToken is Token.Ch && colonToken.c == COLON)
                    return desc.getElementIndex(keyToken.str)
                }
            }
        }

        override fun readBooleanValue(): Boolean = next().let { check(it.hasStr); it.str.toBoolean() }
        override fun readByteValue(): Byte = next().let { check(it.hasStr); it.str.toByte() }
        override fun readShortValue(): Short = next().let { check(it.hasStr); it.str.toShort() }
        override fun readIntValue(): Int = next().let { check(it.hasStr); it.str.toInt() }
        override fun readLongValue(): Long = next().let { check(it.hasStr); it.str.toLong() }
        override fun readFloatValue(): Float = next().let { check(it.hasStr); it.str.toFloat() }
        override fun readDoubleValue(): Double = next().let { check(it.hasStr); it.str.toDouble() }
        override fun readCharValue(): Char = next().let { check(it.hasStr); it.str.single() }
        override fun readStringValue(): String = next().let { check(it.hasStr); it.str }

        override fun <T : Enum<T>> readEnumValue(enumClass: KClass<T>): T = next().let {
            check(it.hasStr)
            java.lang.Enum.valueOf(enumClass.java, it.str)
        }
    }

    private class Parser(val r: Reader) {
        var nextChar: Int = -1
        var curToken: Token = Token.EOF

        init {
            nextChar = r.read()
            next()
        }

        fun nextLiteral(): String {
            val sb = StringBuilder()
            parse@ while(true) {
                sb.append(nextChar.toChar())
                nextChar = r.read()
                if (nextChar < 0) break@parse
                when (nextChar.toChar()) {
                    in 'a'..'z', in 'A'..'Z', in '0'..'9', '+', '-', '.' -> continue@parse
                    else -> break@parse
                }
            }
            return sb.toString()
        }

        fun nextString(): String {
            val sb = StringBuilder()
            parse@ while(true) {
                nextChar = r.read()
                if (nextChar < 0) break@parse
                when (nextChar.toChar()) {
                    STRING_DELIM -> {
                        nextChar = r.read()
                        break@parse
                    }
                    STRING_ESC -> {
                        nextChar = r.read()
                        if (nextChar < 0) break@parse
                        when (nextChar.toChar()) {
                            'b' -> sb.append(0x08.toChar())
                            'f' -> sb.append(0x0c.toChar())
                            'n' -> sb.append(0x0a.toChar())
                            't' -> sb.append(0x09.toChar())
                        // todo: support 'u'
                            else -> sb.append(nextChar.toChar())
                        }
                        continue@parse
                    }
                    else -> {
                        sb.append(nextChar.toChar())
                        continue@parse
                    }
                }
            }
            return sb.toString()
        }

        fun next(): Token {
            val prevToken = curToken
            parse@ while(true) {
                when (nextChar) {
                    -1 -> {
                        curToken = Token.EOF
                        return prevToken
                    }
                    0x20, 0x09, 0x0a, 0x0d -> {
                        nextChar = r.read()
                        continue@parse
                    } // skip space
                    else -> when (nextChar.toChar()) {
                        COMMA, COLON, BEGIN_OBJ, END_OBJ, BEGIN_LIST, END_LIST -> {
                            curToken = Token.Ch(nextChar.toChar())
                            nextChar = r.read()
                            return prevToken
                        }
                        in 'a'..'z', in 'A'..'Z', in '0'..'9', '+', '-', '.' -> {
                            curToken = Token.Lit(nextLiteral())
                            return prevToken
                        }
                        '"' -> {
                            curToken = Token.Str(nextString())
                            return prevToken
                        }
                    }
                }
            }
        }
    }

    private sealed class Token(val hasStr: Boolean, val str: String) {
        object EOF : Token(false, "")
        class Ch(val c: Char) : Token(false, "") // character
        class Lit(str: String) : Token(true, str) // literal
        class Str(str: String) : Token(true, str) // string
    }

    private val Token.canBeginValue: Boolean get() = when (this) {
        is Token.EOF -> false
        is Token.Ch -> when (c) {
            BEGIN_LIST, BEGIN_OBJ -> true
            else -> false
        }
        is Token.Lit -> true
        is Token.Str -> true
    }
}