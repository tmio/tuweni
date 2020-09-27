package org.apache.tuweni.bytes

import io.netty.buffer.ByteBuf
import io.vertx.core.buffer.Buffer
import java.nio.ByteBuffer
import kotlin.experimental.and

/**
 * A mutable [Bytes] value.
 */
interface MutableBytes : Bytes {

  /**
   * Set a byte in this value.
   *
   * @param i The index of the byte to set.
   * @param b The value to set that byte to.
   * @throws IndexOutOfBoundsException if `i < 0` or {i &gt;= size()}.
   */
  operator fun set(i: Int, b: Byte)

  /**
   * Set a byte in this value.
   *
   * @param offset The offset of the bytes to set.
   * @param bytes The value to set bytes to.
   * @throws IndexOutOfBoundsException if `i < 0` or {i &gt;= size()}.
   */
  operator fun set(offset: Int, bytes: Bytes) {
    for (i in 0 until bytes.size()) {
      set(offset + i, bytes[i])
    }
  }

  /**
   * Set the 4 bytes starting at the specified index to the specified integer value.
   *
   * @param i The index, which must less than or equal to `size() - 4`.
   * @param value The integer value.
   * @throws IndexOutOfBoundsException if `i &lt; 0` or `i &gt; size() - 4`.
   */
  open fun setInt(i: Int, value: Int) {
    var i = i
    val size = size()
    if (i > size) {
      throw IndexOutOfBoundsException()
    }
    if (i > size - 4) {
      throw IndexOutOfBoundsException("Value of size $size has not enough bytes to write a 4 bytes int from index $i")
    }

    set(i++, value.ushr(24).toByte())
    set(i++, (value.ushr(16) and 0xFF).toByte())
    set(i++, (value.ushr(8) and 0xFF).toByte())
    set(i, (value and 0xFF).toByte())
  }

  /**
   * Set the 8 bytes starting at the specified index to the specified long value.
   *
   * @param i The index, which must less than or equal to `size() - 8`.
   * @param value The long value.
   * @throws IndexOutOfBoundsException if `i &lt; 0` or `i &gt; size() - 8`.
   */
  open fun setLong(i: Int, value: Long) {
    var i = i
    val size = size()
    if (i > size) {
      throw IndexOutOfBoundsException()
    }
    if (i > size - 8) {
      throw IndexOutOfBoundsException("Value of size $size has not enough bytes to write a 8 bytes long from index $i")
    }

    set(i++, value.ushr(56).toByte())
    set(i++, (value.ushr(48) and 0xFF).toByte())
    set(i++, (value.ushr(40) and 0xFF).toByte())
    set(i++, (value.ushr(32) and 0xFF).toByte())
    set(i++, (value.ushr(24) and 0xFF).toByte())
    set(i++, (value.ushr(16) and 0xFF).toByte())
    set(i++, (value.ushr(8) and 0xFF).toByte())
    set(i, (value and 0xFF).toByte())
  }

  /**
   * Increments the value of the bytes by 1, treating the value as big endian.
   *
   * If incrementing overflows the value then all bits flip, i.e. incrementing 0xFFFF will return 0x0000.
   *
   * @return this value
   */
  open fun increment(): MutableBytes {
    for (i in size() - 1 downTo 0) {
      if (get(i) == 0xFF.toByte()) {
        set(i, 0x00.toByte())
      } else {
        var currentValue = get(i)
        set(i, ++currentValue)
        break
      }
    }
    return this
  }

  /**
   * Decrements the value of the bytes by 1, treating the value as big endian.
   *
   * If decrementing underflows the value then all bits flip, i.e. decrementing 0x0000 will return 0xFFFF.
   *
   * @return this value
   */
  open fun decrement(): MutableBytes {
    for (i in size() - 1 downTo 0) {
      if (get(i) == 0x00.toByte()) {
        set(i, 0xFF.toByte())
      } else {
        var currentValue = get(i)
        set(i, --currentValue)
        break
      }
    }
    return this
  }

  /**
   * Create a mutable slice of the bytes of this value.
   *
   *
   *
   * Note: the resulting slice is only a view over the original value. Holding a reference to the returned slice may
   * hold more memory than the slide represents. Use [.copy] on the returned slice to avoid this.
   *
   * @param i The start index for the slice.
   * @param length The length of the resulting value.
   * @return A new mutable view over the bytes of this value from index `i` (included) to index `i + length`
   * (excluded).
   * @throws IllegalArgumentException if `length &lt; 0`.
   * @throws IndexOutOfBoundsException if `i &lt; 0` or {i &gt;= size()} or {i + length &gt; size()} .
   */
  fun mutableSlice(i: Int, length: Int): MutableBytes

  /**
   * Fill all the bytes of this value with the specified byte.
   *
   * @param b The byte to use to fill the value.
   */
  open fun fill(b: Byte) {
    val size = size()
    for (i in 0 until size) {
      set(i, b)
    }
  }

  /**
   * Set all bytes in this value to 0.
   */
  open fun clear() {
    fill(0.toByte())
  }

  companion object {

    /**
     * The empty value (with 0 bytes).
     */
    val EMPTY = wrap(ByteArray(0))

    /**
     * Create a new mutable byte value.
     *
     * @param size The size of the returned value.
     * @return A [MutableBytes] value.
     */
    fun create(size: Int): MutableBytes {
      return if (size == 32) {
        MutableBytes32.create()
      } else MutableByteBufferWrappingBytes(ByteBuffer.allocate(size))
    }

    /**
     * Wrap a byte array in a [MutableBytes] value.
     *
     * @param value The value to wrap.
     * @return A [MutableBytes] value wrapping `value`.
     */
    fun wrap(value: ByteArray): MutableBytes {
      return MutableArrayWrappingBytes(value)
    }

    /**
     * Wrap a slice of a byte array as a [MutableBytes] value.
     *
     *
     *
     * Note that value is not copied and thus any future update to `value` within the slice will be reflected in the
     * returned value.
     *
     * @param value The value to wrap.
     * @param offset The index (inclusive) in `value` of the first byte exposed by the returned value. In other
     * words, you will have `wrap(value, o, l).get(0) == value[o]`.
     * @param length The length of the resulting value.
     * @return A [Bytes] value that expose the bytes of `value` from `offset` (inclusive) to
     * `offset + length` (exclusive).
     * @throws IndexOutOfBoundsException if `offset &lt; 0 || (value.length > 0 && offset >=
     * value.length)`.
     * @throws IllegalArgumentException if `length &lt; 0 || offset + length > value.length`.
     */
    fun wrap(value: ByteArray, offset: Int, length: Int): MutableBytes {
      return if (length == 32) {
        MutableArrayWrappingBytes32(value, offset)
      } else MutableArrayWrappingBytes(value, offset, length)
    }

    /**
     * Wrap a full Vert.x [Buffer] as a [MutableBytes] value.
     *
     *
     *
     * Note that any change to the content of the buffer may be reflected in the returned value.
     *
     * @param buffer The buffer to wrap.
     * @return A [MutableBytes] value.
     */
    fun wrapBuffer(buffer: Buffer): MutableBytes {
      return if (buffer.length() == 0) {
        EMPTY
      } else MutableBufferWrappingBytes(buffer)
    }

    /**
     * Wrap a slice of a Vert.x [Buffer] as a [MutableBytes] value.
     *
     *
     *
     * Note that any change to the content of the buffer may be reflected in the returned value, and any change to the
     * returned value will be reflected in the buffer.
     *
     * @param buffer The buffer to wrap.
     * @param offset The offset in `buffer` from which to expose the bytes in the returned value. That is,
     * `wrapBuffer(buffer, i, 1).get(0) == buffer.getByte(i)`.
     * @param size The size of the returned value.
     * @return A [MutableBytes] value.
     * @throws IndexOutOfBoundsException if `offset &lt; 0 || (buffer.length() > 0 && offset >=
     * buffer.length())`.
     * @throws IllegalArgumentException if `length &lt; 0 || offset + length > buffer.length()`.
     */
    fun wrapBuffer(buffer: Buffer, offset: Int, size: Int): MutableBytes {
      return if (size == 0) {
        EMPTY
      } else MutableBufferWrappingBytes(buffer, offset, size)
    }

    /**
     * Wrap a full Netty [ByteBuf] as a [MutableBytes] value.
     *
     *
     *
     * Note that any change to the content of the buffer may be reflected in the returned value.
     *
     * @param byteBuf The [ByteBuf] to wrap.
     * @return A [MutableBytes] value.
     */
    fun wrapByteBuf(byteBuf: ByteBuf): MutableBytes {
      return if (byteBuf.capacity() == 0) {
        EMPTY
      } else MutableByteBufWrappingBytes(byteBuf)
    }

    /**
     * Wrap a slice of a Netty [ByteBuf] as a [MutableBytes] value.
     *
     *
     *
     * Note that any change to the content of the buffer may be reflected in the returned value, and any change to the
     * returned value will be reflected in the buffer.
     *
     * @param byteBuf The [ByteBuf] to wrap.
     * @param offset The offset in `byteBuf` from which to expose the bytes in the returned value. That is,
     * `wrapByteBuf(byteBuf, i, 1).get(0) == byteBuf.getByte(i)`.
     * @param size The size of the returned value.
     * @return A [MutableBytes] value.
     * @throws IndexOutOfBoundsException if `offset &lt; 0 || (byteBuf.capacity() > 0 && offset >=
     * byteBuf.capacity())`.
     * @throws IllegalArgumentException if `length &lt; 0 || offset + length > byteBuf.capacity()`.
     */
    fun wrapByteBuf(byteBuf: ByteBuf, offset: Int, size: Int): MutableBytes {
      return if (size == 0) {
        EMPTY
      } else MutableByteBufWrappingBytes(byteBuf, offset, size)
    }

    /**
     * Wrap a full Java NIO [ByteBuffer] as a [MutableBytes] value.
     *
     *
     *
     * Note that any change to the content of the buffer may be reflected in the returned value.
     *
     * @param byteBuffer The [ByteBuffer] to wrap.
     * @return A [MutableBytes] value.
     */
    fun wrapByteBuffer(byteBuffer: ByteBuffer): MutableBytes {
      return if (byteBuffer.limit() == 0) {
        EMPTY
      } else MutableByteBufferWrappingBytes(byteBuffer)
    }

    /**
     * Wrap a slice of a Java NIO [ByteBuffer] as a [MutableBytes] value.
     *
     *
     *
     * Note that any change to the content of the buffer may be reflected in the returned value, and any change to the
     * returned value will be reflected in the buffer.
     *
     * @param byteBuffer The [ByteBuffer] to wrap.
     * @param offset The offset in `byteBuffer` from which to expose the bytes in the returned value. That is,
     * `wrapByteBuffer(byteBuffer, i, 1).get(0) == byteBuffer.getByte(i)`.
     * @param size The size of the returned value.
     * @return A [MutableBytes] value.
     * @throws IndexOutOfBoundsException if `offset &lt; 0 || (byteBuffer.limit() > 0 && offset >=
     * byteBuffer.limit())`.
     * @throws IllegalArgumentException if `length &lt; 0 || offset + length > byteBuffer.limit()`.
     */
    fun wrapByteBuffer(byteBuffer: ByteBuffer, offset: Int, size: Int): MutableBytes {
      return if (size == 0) {
        EMPTY
      } else MutableByteBufferWrappingBytes(byteBuffer, offset, size)
    }

    /**
     * Create a value that contains the specified bytes in their specified order.
     *
     * @param bytes The bytes that must compose the returned value.
     * @return A value containing the specified bytes.
     */
    fun of(vararg bytes: Byte): MutableBytes {
      return wrap(bytes)
    }

    /**
     * Create a value that contains the specified bytes in their specified order.
     *
     * @param bytes The bytes.
     * @return A value containing bytes are the one from `bytes`.
     * @throws IllegalArgumentException if any of the specified would be truncated when storing as a byte.
     */
    fun of(vararg bytes: Int): MutableBytes {
      val result = ByteArray(bytes.size)
      for (i in bytes.indices) {
        val b = bytes[i]
        if (b != (b.toByte() and 0xff.toByte()).toInt()) {
          throw IllegalArgumentException((i + 1).toString() + "th value " + b + " does not fit a byte")
        }
        result[i] = b.toByte()
      }
      return wrap(result)
    }
  }
}
