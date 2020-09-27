package org.apache.tuweni.bytes

import java.lang.IllegalArgumentException
import java.lang.IndexOutOfBoundsException
import java.nio.ByteBuffer
import java.util.*

internal open class ByteBufferWrappingBytes @JvmOverloads constructor(
  protected val byteBuffer: ByteBuffer,
  protected val offset: Int = 0,
  protected val length: Int = byteBuffer.limit()
) : AbstractBytes() {

  init {
    if (length < 0) {
      throw IllegalArgumentException("Invalid negative length")
    }
    val bufferLength = byteBuffer.capacity()
    if (offset > bufferLength + 1) {
      throw IndexOutOfBoundsException()
    }
    if (offset + length > bufferLength) {
      throw IllegalArgumentException("Provided length $length is too big: the buffer has size $bufferLength and has only ${bufferLength - offset} bytes from $offset")
    }
  }

  override fun size(): Int {
    return length
  }

  override fun getInt(i: Int): Int {
    return byteBuffer.getInt(offset + i)
  }

  override fun getLong(i: Int): Long {
    return byteBuffer.getLong(offset + i)
  }

  override fun get(i: Int): Byte {
    return byteBuffer.get(offset + i)
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
      throw IllegalArgumentException("Provided length $length is too big: the value has size $length and has only ${length - i} bytes from $i")
    }

    return ByteBufferWrappingBytes(byteBuffer, offset + i, length)
  }

  // MUST be overridden by mutable implementations
  override fun copy(): Bytes {
    return if (offset == 0 && length == byteBuffer.limit()) {
      this
    } else ArrayWrappingBytes(toArray())
  }

  override fun mutableCopy(): MutableBytes {
    return MutableArrayWrappingBytes(toArray())
  }

  override fun appendTo(byteBuffer: ByteBuffer) {
    byteBuffer.put(this.byteBuffer)
  }

  override fun toArray(): ByteArray {
    if (!byteBuffer.hasArray()) {
      return super.toArray()
    }
    val arrayOffset = byteBuffer.arrayOffset()
    return Arrays.copyOfRange(byteBuffer.array(), arrayOffset + offset, arrayOffset + offset + length)
  }

  override fun toArrayUnsafe(): ByteArray {
    if (!byteBuffer.hasArray()) {
      return toArray()
    }
    val array = byteBuffer.array()
    return if (array.size != length || byteBuffer.arrayOffset() != 0) {
      toArray()
    } else array
  }
}
