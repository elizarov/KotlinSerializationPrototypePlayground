import java.io.*
import kotlin.serialization.ElementValueInput
import kotlin.serialization.ElementValueOutput
import kotlin.serialization.KSerializer

class DataBinaryOutput(val out: DataOutput) : ElementValueOutput() {
    override fun writeBooleanValue(value: Boolean) = out.writeByte(if (value) 1 else 0)
    override fun writeByteValue(value: Byte) = out.writeByte(value.toInt())
    override fun writeShortValue(value: Short) = out.writeShort(value.toInt())
    override fun writeIntValue(value: Int) = out.writeInt(value)
    override fun writeLongValue(value: Long) = out.writeLong(value)
    override fun writeFloatValue(value: Float) = out.writeFloat(value)
    override fun writeDoubleValue(value: Double) = out.writeDouble(value)
    override fun writeCharValue(value: Char) = out.writeChar(value.toInt())
    override fun writeStringValue(value: String) = out.writeUTF(value)
}

class DataBinaryInput(val inp: DataInput) : ElementValueInput() {
    override fun readBooleanValue(): Boolean = inp.readByte().toInt() != 0
    override fun readByteValue(): Byte = inp.readByte()
    override fun readShortValue(): Short = inp.readShort()
    override fun readIntValue(): Int = inp.readInt()
    override fun readLongValue(): Long = inp.readLong()
    override fun readFloatValue(): Float = inp.readFloat()
    override fun readDoubleValue(): Double = inp.readDouble()
    override fun readCharValue(): Char = inp.readChar()
    override fun readStringValue(): String = inp.readUTF()
}

fun testDataBinaryIO(serializer: KSerializer<Any>, obj: Any): Result {
    // save
    val baos = ByteArrayOutputStream()
    val out = DataBinaryOutput(DataOutputStream(baos))
    out.write(serializer, obj)
    // load
    val bytes = baos.toByteArray()
    val inp = DataBinaryInput(DataInputStream(ByteArrayInputStream(bytes)))
    val other = inp.read(serializer)
    // result
    return Result(obj, other, "${bytes.size} bytes ${bytes.toHexString()}")
}

fun main(args: Array<String>) {
//    testCase(Shop, shop, ::testDataBinaryIO)
    testMethod(::testDataBinaryIO)
}
