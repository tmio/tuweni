package org.apache.tuweni.bytes

internal object BytesValues {

  val MAX_UNSIGNED_SHORT = (1 shl 16) - 1
  val MAX_UNSIGNED_INT = (1L shl 32) - 1
  val MAX_UNSIGNED_LONG = java.lang.Long.MAX_VALUE

  fun fromHexString(str: CharSequence, destSize: Int, lenient: Boolean): Bytes {
    return Bytes.wrap(fromRawHexString(str, destSize, lenient))
  }

  fun fromRawHexString(str: CharSequence, destSize: Int, lenient: Boolean): ByteArray {
    var destSize = destSize
    var len = str.length
    var hex = str
    if (len >= 2 && str[0] == '0' && str[1] == 'x') {
      hex = str.subSequence(2, len)
      len -= 2
    }

    var idxShift = 0
    if (len % 2 != 0) {
      if (!lenient) {
        throw IllegalArgumentException("Invalid odd-length hex binary representation")
      }

      hex = "0$hex"
      len += 1
      idxShift = 1
    }

    val size = len / 2
    if (destSize < 0) {
      destSize = size
    } else {
      if (size > destSize) {
        throw IllegalArgumentException("Hex value is too large: expected at most \$destSize bytes but got \$size")
      }
    }

    val out = ByteArray(destSize)

    val destOffset = destSize - size
    var i = 0
    while (i < len) {
      val h = hexToBin(hex[i])
      val l = hexToBin(hex[i + 1])
      if (h == -1) {
        throw IllegalArgumentException(
          String
            .format(
              "Illegal character '%c' found at index %d in hex binary representation",
              hex[i],
              i - idxShift
            )
        )
      }
      if (l == -1) {
        throw IllegalArgumentException(
          String
            .format(
              "Illegal character '%c' found at index %d in hex binary representation",
              hex[i + 1],
              i + 1 - idxShift
            )
        )
      }

      out[destOffset + i / 2] = (h * 16 + l).toByte()
      i += 2
    }
    return out
  }

  private fun hexToBin(ch: Char): Int {
    return if ('0' <= ch && ch <= '9') {
      ch.toInt() - 48
    } else if ('A' <= ch && ch <= 'F') {
      ch.toInt() - 65 + 10
    } else {
      if ('a' <= ch && ch <= 'f') ch.toInt() - 97 + 10 else -1
    }
  }
}
