package org.apache.tuweni.bytes

import io.vertx.core.buffer.Buffer
import java.lang.IllegalArgumentException
import java.lang.IndexOutOfBoundsException

internal open class BufferWrappingBytes : AbstractBytes {

  protected val buffer: Buffer

  constructor(buffer: Buffer) {
    this.buffer = buffer
  }

  constructor(buffer: Buffer, offset: Int, length: Int) {
    if (length < 0) {
      throw IllegalArgumentException("Invalid negative length")
    }
    val bufferLength = buffer.length()
    if (offset > buffer.length() + 1) {
      throw IndexOutOfBoundsException()
    }
    if (offset + length > bufferLength) {
      throw IllegalArgumentException("Provided length $length is too big: the buffer has size $bufferLength and has only ${bufferLength - offset} bytes from $offset")
    }

    if (offset == 0 && length == bufferLength) {
      this.buffer = buffer
    } else {
      this.buffer = buffer.slice(offset, offset + length)
    }
  }

  override fun size(): Int {
    return buffer.length()
  }

  override fun get(i: Int): Byte {
    return buffer.getByte(i)
  }

  override fun getInt(i: Int): Int {
    return buffer.getInt(i)
  }

  override fun getLong(i: Int): Long {
    return buffer.getLong(i)
  }

  override fun slice(i: Int, length: Int): Bytes {
    val size = buffer.length()
    if (i == 0 && length == size) {
      return this
    }
    if (length == 0) {
      return Bytes.EMPTY
    }

    if (i > size) {
      throw IndexOutOfBoundsException()
    }
    if (i + length > size) {
      throw IllegalArgumentException("Provided length $length is too big: the value has size $size and has only ${size - i} bytes from $i")
    }

    return BufferWrappingBytes(buffer.slice(i, i + length))
  }

  // MUST be overridden by mutable implementations
  override fun copy(): Bytes {
    return Bytes.wrap(toArray())
  }

  override fun mutableCopy(): MutableBytes {
    return MutableBytes.wrap(toArray())
  }

  override fun appendTo(buffer: Buffer) {
    buffer.appendBuffer(this.buffer)
  }

  override fun toArray(): ByteArray {
    return buffer.bytes
  }
}
