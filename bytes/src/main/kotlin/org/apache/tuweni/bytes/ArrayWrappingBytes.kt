package org.apache.tuweni.bytes

import io.vertx.core.buffer.Buffer
import java.nio.ByteBuffer
import java.security.MessageDigest
import java.util.*

internal open class ArrayWrappingBytes @JvmOverloads constructor(
  protected val bytes: ByteArray,
  protected val offset: Int = 0,
  protected val length: Int = bytes.size
) : AbstractBytes() {

  init {
    if (length < 0) {
      throw IllegalArgumentException("Invalid negative length")
    }
    if (bytes.size > 0) {
      if (offset > bytes.size) {
        throw IndexOutOfBoundsException()
      }
    }
    if (offset + length > bytes.size) {
      throw IllegalArgumentException("Provided length " + length + " is too big: the value has only " + (bytes.size - offset) + " bytes from offset " + offset)
    }
  }

  override fun size(): Int {
    return length
  }

  override fun get(i: Int): Byte {
    // Check bounds because while the array access would throw, the error message would be confusing
    // for the caller.
    if (i > size()) {
      throw IndexOutOfBoundsException()
    }
    return bytes[offset + i]
  }

  override fun slice(i: Int, length: Int): Bytes {
    if (i == 0 && length == this.length) {
      return this
    }
    if (length == 0) {
      return Bytes.EMPTY
    }

    if (i > this.length) {
      throw IndexOutOfBoundsException()
    }
    if (i + length > this.length) {
      throw IllegalArgumentException("Provided length " + length + " is too big: the value has size " + this.length + " and has only " + (this.length - i) + " bytes from " + i)
    }

    return if (length == Bytes32.SIZE)
      ArrayWrappingBytes32(bytes, offset + i)
    else
      ArrayWrappingBytes(bytes, offset + i, length)
  }

  // MUST be overridden by mutable implementations
  override fun copy(): Bytes {
    return if (offset == 0 && length == bytes.size) {
      this
    } else ArrayWrappingBytes(toArray())
  }

  override fun mutableCopy(): MutableBytes {
    return MutableArrayWrappingBytes(toArray())
  }

  override fun commonPrefixLength(other: Bytes): Int {
    if (other !is ArrayWrappingBytes) {
      return super.commonPrefixLength(other)
    }
    var i = 0
    while (i < length && i < other.length && bytes[offset + i] == other.bytes[other.offset + i]) {
      i++
    }
    return i
  }

  override fun update(digest: MessageDigest) {
    digest.update(bytes, offset, length)
  }

  override fun copyTo(destination: MutableBytes, destinationOffset: Int) {
    if (destination !is MutableArrayWrappingBytes) {
      super.copyTo(destination, destinationOffset)
      return
    }

    val size = size()
    if (size == 0) {
      return
    }

    if (destinationOffset > destination.size()) {
      throw IndexOutOfBoundsException()
    }
    if (destination.size() - destinationOffset < size) {
      throw IllegalArgumentException("Cannot copy " + size + " bytes, destination has only " + (destination.size() - destinationOffset) + " bytes from index " + destinationOffset)
    }

    System.arraycopy(bytes, offset, destination.bytes, destination.offset + destinationOffset, size)
  }

  override fun appendTo(byteBuffer: ByteBuffer) {
    byteBuffer.put(bytes, offset, length)
  }

  override fun appendTo(buffer: Buffer) {
    buffer.appendBytes(bytes, offset, length)
  }

  override fun equals(obj: Any?): Boolean {
    if (obj === this) {
      return true
    }
    if (obj !is ArrayWrappingBytes) {
      return super.equals(obj)
    }
    val other = obj as ArrayWrappingBytes?
    if (length != other!!.length) {
      return false
    }
    for (i in 0 until length) {
      if (bytes[offset + i] != other.bytes[other.offset + i]) {
        return false
      }
    }
    return true
  }

  override fun hashCode(): Int {
    var result = 1
    val size = size()
    for (i in 0 until size) {
      result = 31 * result + bytes[offset + i]
    }
    return result
  }

  override fun toArray(): ByteArray {
    return Arrays.copyOfRange(bytes, offset, offset + length)
  }

  override fun toArrayUnsafe(): ByteArray {
    return if (offset == 0 && length == bytes.size) {
      bytes
    } else toArray()
  }
}
