package org.apache.tuweni.bytes

internal class ArrayWrappingBytes48(bytes: ByteArray, offset: Int) :
  ArrayWrappingBytes(checkLength(bytes, offset), offset, Bytes48.SIZE), Bytes48 {

  constructor(bytes: ByteArray) : this(checkLength(bytes), 0) {}

  companion object {
    // Ensures a proper error message.
    private fun checkLength(bytes: ByteArray): ByteArray {
      if (bytes.size != Bytes48.SIZE) {
        throw IllegalArgumentException("Expected " + Bytes48.SIZE + " bytes but got " + bytes.size)
      }
      return bytes
    }

    // Ensures a proper error message.
    private fun checkLength(bytes: ByteArray, offset: Int): ByteArray {
      if (bytes.size - offset < Bytes48.SIZE) {
        throw IllegalArgumentException("Expected at least " + Bytes48.SIZE + " bytes from offset " + offset + " but got only " + (bytes.size - offset))
      }
      return bytes
    }
  }

  override fun size(): Int {
    return super<Bytes48>.size()
  }

  override fun copy(): Bytes48 {
    return if (offset == 0 && length == bytes.size) {
      this
    } else ArrayWrappingBytes48(toArray())
  }

  override fun mutableCopy(): MutableBytes48 {
    return MutableArrayWrappingBytes48(toArray())
  }
}
