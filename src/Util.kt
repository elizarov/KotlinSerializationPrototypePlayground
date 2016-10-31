/**
 * @author Roman Elizarov
 */

fun Int.toHexDigit() = when(this) {
    in 0..9 -> (this + '0'.toInt()).toChar()
    in 10..15 -> (this - 10 + 'A'.toInt()).toChar()
    else -> throw IllegalArgumentException(this.toString())
}

fun ByteArray.toHexString(): String {
    val sb = StringBuilder()
    for (i in this) {
        sb.append((i.toInt() and 0xf).toHexDigit())
        sb.append((i.toInt() shr 4 and 0xf).toHexDigit())
    }
    return sb.toString()
}
