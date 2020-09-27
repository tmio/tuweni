package org.apache.tuweni.bytes

import org.apache.tuweni.bytes.Bytes32.Companion.SIZE
import org.apache.tuweni.bytes.Bytes48.Companion.SIZE
import java.lang.IllegalArgumentException
import java.lang.IndexOutOfBoundsException
import java.util.*

internal open class MutableArrayWrappingBytes : ArrayWrappingBytes, MutableBytes {

  constructor(bytes: ByteArray) : super(bytes) {}

  constructor(bytes: ByteArray, offset: Int, length: Int) : super(bytes, offset, length) {}

  override fun set(i: Int, b: Byte) {
    // Check bounds because while the array access would throw, the error message would be confusing
    // for the caller.
    if (i > length) {
      throw IndexOutOfBoundsException()
    }
    bytes[offset + i] = b
  }

  override fun increment(): MutableBytes {
    for (i in length - 1 downTo offset) {
      if (bytes[i] == 0xFF.toByte()) {
        bytes[i] = 0x00.toByte()
      } else {
        ++bytes[i]
        break
      }
    }
    return this
  }

  override fun decrement(): MutableBytes {
    for (i in length - 1 downTo offset) {
      if (bytes[i] == 0x00.toByte()) {
        bytes[i] = 0xFF.toByte()
      } else {
        --bytes[i]
        break
      }
    }
    return this
  }

  override fun mutableSlice(i: Int, length: Int): MutableBytes {
    if (i == 0 && length == this.length)
      return this
    if (length == 0)
      return MutableBytes.EMPTY

    if (i > this.length) {
      throw IndexOutOfBoundsException()
    }
    if (i + length > this.length) {
      throw IllegalArgumentException("Specified length $length is too large: the value has size ${this.length} and has only ${this.length - i} bytes from $i")
    }
    return if (length == Bytes32.SIZE)
      MutableArrayWrappingBytes32(bytes, offset + i)
    else
      MutableArrayWrappingBytes(bytes, offset + i, length)
  }

  override fun fill(b: Byte) {
    Arrays.fill(bytes, offset, offset + length, b)
  }

  override fun copy(): Bytes {
    return ArrayWrappingBytes(toArray())
  }
}

internal class MutableArrayWrappingBytes32 @JvmOverloads constructor(bytes: ByteArray, offset: Int = 0) :
  MutableArrayWrappingBytes(bytes, offset, Bytes32.Companion.SIZE), MutableBytes32 {

  override fun size(): Int {
    return super<MutableBytes32>.size()
  }

  override fun copy(): Bytes32 {
    return ArrayWrappingBytes32(toArray())
  }

  override fun mutableCopy(): MutableBytes32 {
    return MutableArrayWrappingBytes32(toArray())
  }
}

internal class MutableArrayWrappingBytes48 @JvmOverloads constructor(bytes: ByteArray, offset: Int = 0) :
  MutableArrayWrappingBytes(bytes, offset, Bytes48.Companion.SIZE), MutableBytes48 {

  override fun size(): Int {
    return super<MutableBytes48>.size()
  }

  override fun copy(): Bytes48 {
    return ArrayWrappingBytes48(toArray())
  }

  override fun mutableCopy(): MutableBytes48 {
    return MutableArrayWrappingBytes48(toArray())
  }
}

