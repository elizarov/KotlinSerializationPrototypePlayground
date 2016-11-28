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

class JSON {
    companion object {
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
            check(parser.curCl == CL_EOF) { "Shall parse complete string"}
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
        private const val STRING = '"'
        private const val STRING_ESC = '\\'

        private const val INVALID = 0.toChar()

        private const val CL_OTHER: Byte = 0
        private const val CL_EOF: Byte = 1
        private const val CL_INVALID: Byte = 2
        private const val CL_WS: Byte = 3
        private const val CL_COMMA: Byte = 4
        private const val CL_COLON: Byte = 5
        private const val CL_BEGIN_OBJ: Byte = 6
        private const val CL_END_OBJ: Byte = 7
        private const val CL_BEGIN_LIST: Byte = 8
        private const val CL_END_LIST: Byte = 9
        private const val CL_STRING: Byte = 10
        private const val CL_STRING_ESC: Byte = 11

        private const val CCL_MAX = 128
        private const val CCL_OFS = 1

        private val CCL = ByteArray(CCL_MAX + CCL_OFS)

        fun initCCL(c : Int, cl: Byte) { CCL[c + CCL_OFS] = cl }
        fun initCCL(c: Char, cl: Byte) { initCCL(c.toInt(), cl) }

        fun ccl(c: Int): Byte = if (c < CCL_MAX + CCL_OFS) CCL[c + CCL_OFS] else CL_OTHER

        init {
            initCCL(-1, CL_EOF)
            for (i in 0..0x20)
                initCCL(i, CL_INVALID)
            initCCL(0x09, CL_WS)
            initCCL(0x0a, CL_WS)
            initCCL(0x0d, CL_WS)
            initCCL(0x20, CL_WS)
            initCCL(COMMA, CL_COMMA)
            initCCL(COLON, CL_COLON)
            initCCL(BEGIN_OBJ, CL_BEGIN_OBJ)
            initCCL(END_OBJ, CL_END_OBJ)
            initCCL(BEGIN_LIST, CL_BEGIN_LIST)
            initCCL(END_LIST, CL_END_LIST)
            initCCL(STRING, CL_STRING)
            initCCL(STRING_ESC, CL_STRING_ESC)
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
    }

    private enum class Mode(val begin: Char, val end: Char) {
        OBJ(BEGIN_OBJ, END_OBJ),
        LIST(BEGIN_LIST, END_LIST),
        MAP(BEGIN_OBJ, END_OBJ),
        ENTRY(INVALID, INVALID);

        val beginCl: Byte = ccl(begin.toInt())
        val endCl: Byte = ccl(end.toInt())
    }

    private class JsonOutput(val mode: Mode, val w: PrintWriter) : ElementValueOutput() {
        private var forceStr: Boolean = false

        override fun writeBegin(desc: KSerialClassDesc, vararg typeParams: KSerializer<*>): KOutput {
            val newMode = switchMode(mode, desc, typeParams)
            if (newMode.begin != INVALID) w.print(newMode.begin)
            return if (mode == newMode) this else JsonOutput(newMode, w) // todo: reuse instance per mode
        }

        override fun writeEnd(desc: KSerialClassDesc) {
            if (mode.end != INVALID) w.print(mode.end)
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
            w.print(STRING)
            for (c in value)
                when (c) {
                    '\\' -> w.print("\\\\")
                    '"' -> w.print("\\\"")
                    else -> w.print(c)
                }
            w.print(STRING)
        }

        override fun writeValue(value: Any) {
            writeStringValue(value.toString())
        }

    }

    private class JsonInput(val mode: Mode, val p: Parser) : ElementValueInput() {
        var curIndex = 0
        var entryIndex = 0

        override fun readBegin(desc: KSerialClassDesc, vararg typeParams: KSerializer<*>): KInput {
            val newMode = switchMode(mode, desc, typeParams)
            if (newMode.begin != INVALID) {
                check(p.curCl == newMode.beginCl)
                p.nextToken()
            }
            return when (newMode) {
                Mode.LIST, Mode.MAP -> JsonInput(newMode, p) // need fresh cur index
                else -> if (mode == newMode) this else
                            JsonInput(newMode, p) // todo: reuse instance per mode
            }
        }

        override fun readEnd(desc: KSerialClassDesc) {
            if (mode.end != INVALID) {
                check(p.curCl == mode.endCl)
                p.nextToken()
            }
        }

        override fun readNotNullMark(): Boolean {
            if (p.curCl == CL_OTHER && p.curStr == NULL) return false
            return true
        }

        override fun readNullValue(): Nothing? {
            check(p.nextToken() == NULL) { "expected 'null'" }
            return null
        }

        override fun readElement(desc: KSerialClassDesc): Int {
            if (p.curCl == CL_COMMA) p.nextToken()
            when (mode) {
                Mode.LIST, Mode.MAP -> {
                    if (!p.canBeginValue)
                        return READ_DONE
                    return ++curIndex
                }
                Mode.ENTRY -> {
                    when (entryIndex++) {
                        0 -> return 0
                        1 -> {
                            check(p.curCl == CL_COLON)
                            p.nextToken()
                            return 1
                        }
                        else -> {
                            entryIndex = 0
                            return READ_DONE
                        }
                    }
                }
                else -> {
                    if (!p.canBeginValue)
                        return READ_DONE
                    val key = p.nextToken()!!
                    check(p.curCl == CL_COLON)
                    p.nextToken()
                    return desc.getElementIndex(key)
                }
            }
        }

        override fun readBooleanValue(): Boolean = p.nextToken()!!.toBoolean()
        override fun readByteValue(): Byte = p.nextToken()!!.toByte()
        override fun readShortValue(): Short = p.nextToken()!!.toShort()
        override fun readIntValue(): Int = p.nextToken()!!.toInt()
        override fun readLongValue(): Long = p.nextToken()!!.toLong()
        override fun readFloatValue(): Float = p.nextToken()!!.toFloat()
        override fun readDoubleValue(): Double = p.nextToken()!!.toDouble()
        override fun readCharValue(): Char = p.nextToken()!!.single()
        override fun readStringValue(): String = p.nextToken()!!

        override fun <T : Enum<T>> readEnumValue(enumClass: KClass<T>): T =
            java.lang.Enum.valueOf(enumClass.java, p.nextToken()!!)
    }

    private class Parser(val r: Reader) {
        // updated by nextChar
        var curChar: Int = -1
        // updated by nextToken
        var curCl: Byte = CL_EOF
        var curStr: String? = null
        var sb = StringBuilder()

        init {
            nextChar()
            nextToken()
        }

        val canBeginValue: Boolean get() = when (curCl) {
            CL_BEGIN_LIST, CL_BEGIN_OBJ, CL_OTHER, CL_STRING -> true
            else -> false
        }

        fun nextToken(): String? {
            val prevStr = curStr
            while(true) {
                curCl = ccl(curChar)
                when (curCl) {
                    CL_WS -> nextChar() // skip whitespace
                    CL_OTHER -> {
                        nextLiteral()
                        curStr = sb.toString()
                        curCl = CL_OTHER
                        return prevStr
                    }
                    CL_STRING -> {
                        nextString()
                        curStr = sb.toString()
                        curCl = CL_STRING
                        return prevStr
                    }
                    else -> {
                        nextChar()
                        curStr = null
                        return prevStr
                    }
                }
            }
        }

        private fun nextChar() {
            curChar = r.read()
        }

        private fun nextLiteral() {
            sb.setLength(0)
            while(true) {
                sb.append(curChar.toChar())
                nextChar()
                if (ccl(curChar) != CL_OTHER) return
            }
        }

        private fun nextString() {
            sb.setLength(0)
            while(true) {
                nextChar()
                when (ccl(curChar)) {
                    CL_EOF -> return
                    CL_STRING -> {
                        nextChar()
                        return
                    }
                    CL_STRING_ESC -> {
                        nextChar()
                        if (curChar < 0) return
                        when (curChar.toChar()) {
                            'b' -> sb.append(0x08.toChar())
                            'f' -> sb.append(0x0c.toChar())
                            'n' -> sb.append(0x0a.toChar())
                            't' -> sb.append(0x09.toChar())
                            'u' -> sb.append(((hex() shl 12) + (hex() shl 8) + (hex() shl 4) + hex()).toChar())
                            else -> sb.append(curChar.toChar())
                        }
                    }
                    else -> sb.append(curChar.toChar())
                }
            }
        }

        private fun hex(): Int {
            if (curChar < 0) return 0
            nextChar()
            if (curChar < 0) return 0
            when (curChar.toChar()) {
                in '0'..'9' -> return curChar - '0'.toInt()
                in 'a'..'f' -> return curChar - 'a'.toInt() + 10
                in 'A'..'F' -> return curChar - 'A'.toInt() + 10
                else -> return 0
            }
        }
    }
}