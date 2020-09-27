package org.apache.tuweni.bytes

internal class ArrayWrappingBytes32(bytes: ByteArray, offset: Int) :
  ArrayWrappingBytes(checkLength(bytes, offset), offset, Bytes32.SIZE), Bytes32 {

  constructor(bytes: ByteArray) : this(checkLength(bytes), 0) {}

  companion object {
    // Ensures a proper error message.
    private fun checkLength(bytes: ByteArray): ByteArray {
      if (bytes.size != Bytes32.SIZE) {
        throw IllegalArgumentException("Expected " + Bytes32.SIZE + " bytes but got " + bytes.size)
      }
      return bytes
    }

    // Ensures a proper error message.
    private fun checkLength(bytes: ByteArray, offset: Int): ByteArray {
      if (bytes.size - offset < Bytes32.SIZE) {
        throw IllegalArgumentException("Expected at least " + Bytes32.SIZE + " bytes from offset " + offset + " but got only " + (bytes.size - offset))
      }
      return bytes
    }
  }

  override fun size(): Int {
    return super<Bytes32>.size()
  }

  override fun copy(): Bytes32 {
    return if (offset == 0 && length == bytes.size) {
      this
    } else ArrayWrappingBytes32(toArray())
  }

  override fun mutableCopy(): MutableBytes32 {
    return MutableArrayWrappingBytes32(toArray())
  }
}
