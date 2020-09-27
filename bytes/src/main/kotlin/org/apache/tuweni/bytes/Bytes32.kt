package org.apache.tuweni.bytes

import java.security.SecureRandom
import java.util.*


/**
 * A [Bytes] value that is guaranteed to contain exactly 32 bytes.
 */
interface Bytes32 : Bytes {

  override fun size(): Int {
    return SIZE
  }

  /**
   * Return a bit-wise AND of these bytes and the supplied bytes.
   *
   * @param other The bytes to perform the operation with.
   * @return The result of a bit-wise AND.
   */
  fun and(other: Bytes32): Bytes32 {
    return and(other, MutableBytes32.create())
  }

  /**
   * Return a bit-wise OR of these bytes and the supplied bytes.
   *
   * @param other The bytes to perform the operation with.
   * @return The result of a bit-wise OR.
   */
  fun or(other: Bytes32): Bytes32 {
    return or(other, MutableBytes32.create())
  }

  /**
   * Return a bit-wise XOR of these bytes and the supplied bytes.
   *
   * @param other The bytes to perform the operation with.
   * @return The result of a bit-wise XOR.
   */
  fun xor(other: Bytes32): Bytes32 {
    return xor(other, MutableBytes32.create())
  }

  override fun not(): Bytes32 {
    return not(MutableBytes32.create())
  }

  override fun shiftRight(distance: Int): Bytes32 {
    return shiftRight(distance, MutableBytes32.create())
  }

  override fun shiftLeft(distance: Int): Bytes32 {
    return shiftLeft(distance, MutableBytes32.create())
  }

  override fun copy(): Bytes32

  override fun mutableCopy(): MutableBytes32

  companion object {
    /** The number of bytes in this value - i.e. 32  */
    val SIZE = 32

    /** A `Bytes32` containing all zero bytes  */
    val ZERO = wrap(ByteArray(32))

    /**
     * Wrap the provided byte array, which must be of length 32, as a [Bytes32].
     *
     *
     *
     * Note that value is not copied, only wrapped, and thus any future update to `value` will be reflected in the
     * returned value.
     *
     * @param bytes The bytes to wrap.
     * @return A [Bytes32] wrapping `value`.
     * @throws IllegalArgumentException if `value.length != 32`.
     */
    fun wrap(bytes: ByteArray): Bytes32 {
      if (bytes.size != SIZE) {
        throw IllegalArgumentException("Expected " + SIZE + " bytes but got " + bytes.size)
      }
      return wrap(bytes, 0)
    }

    /**
     * Wrap a slice/sub-part of the provided array as a [Bytes32].
     *
     *
     *
     * Note that value is not copied, only wrapped, and thus any future update to `value` within the wrapped parts
     * will be reflected in the returned value.
     *
     * @param bytes The bytes to wrap.
     * @param offset The index (inclusive) in `value` of the first byte exposed by the returned value. In other
     * words, you will have `wrap(value, i).get(0) == value[i]`.
     * @return A [Bytes32] that exposes the bytes of `value` from `offset` (inclusive) to
     * `offset + 32` (exclusive).
     * @throws IndexOutOfBoundsException if `offset &lt; 0 || (value.length &gt; 0 && offset >=
     * value.length)`.
     * @throws IllegalArgumentException if `length &lt; 0 || offset + 32 &gt; value.length`.
     */
    fun wrap(bytes: ByteArray, offset: Int): Bytes32 {
      return ArrayWrappingBytes32(bytes, offset)
    }

    /**
     * Wrap a the provided value, which must be of size 32, as a [Bytes32].
     *
     *
     *
     * Note that value is not copied, only wrapped, and thus any future update to `value` will be reflected in the
     * returned value.
     *
     * @param value The bytes to wrap.
     * @return A [Bytes32] that exposes the bytes of `value`.
     * @throws IllegalArgumentException if `value.size() != 32`.
     */
    fun wrap(value: Bytes): Bytes32 {
      if (value is Bytes32) {
        return value
      }
      if (value.size() != SIZE) {
        throw IllegalArgumentException("Expected " + SIZE + " bytes but got " + value.size())
      }
      return DelegatingBytes32(value)
    }

    /**
     * Wrap a slice/sub-part of the provided value as a [Bytes32].
     *
     *
     *
     * Note that value is not copied, only wrapped, and thus any future update to `value` within the wrapped parts
     * will be reflected in the returned value.
     *
     * @param value The bytes to wrap.
     * @param offset The index (inclusive) in `value` of the first byte exposed by the returned value. In other
     * words, you will have `wrap(value, i).get(0) == value.get(i)`.
     * @return A [Bytes32] that exposes the bytes of `value` from `offset` (inclusive) to
     * `offset + 32` (exclusive).
     * @throws IndexOutOfBoundsException if `offset &lt; 0 || (value.size() &gt; 0 && offset >=
     * value.size())`.
     * @throws IllegalArgumentException if `length &lt; 0 || offset + 32 &gt; value.size()`.
     */
    fun wrap(value: Bytes, offset: Int): Bytes32 {
      val slice = value.slice(offset, Bytes32.SIZE)
      return slice as? Bytes32 ?: DelegatingBytes32(value)
    }

    /**
     * Left pad a [Bytes] value with zero bytes to create a [Bytes32].
     *
     * @param value The bytes value pad.
     * @return A [Bytes32] that exposes the left-padded bytes of `value`.
     * @throws IllegalArgumentException if `value.size() &gt; 32`.
     */
    fun leftPad(value: Bytes): Bytes32 {
      if (value is Bytes32) {
        return value
      }
      if (value.size() > SIZE) {
        throw IllegalArgumentException("Expected at most " + SIZE + " bytes but got " + value.size())
      }
      val result = MutableBytes32.create()
      value.copyTo(result, SIZE - value.size())
      return result
    }

    /**
     * Right pad a [Bytes] value with zero bytes to create a [Bytes32].
     *
     * @param value The bytes value pad.
     * @return A [Bytes32] that exposes the rightw-padded bytes of `value`.
     * @throws IllegalArgumentException if `value.size() &gt; 32`.
     */
    fun rightPad(value: Bytes): Bytes32 {
      if (value is Bytes32) {
        return value
      }
      if (value.size() > SIZE) {
        throw IllegalArgumentException("Expected at most " + SIZE + " bytes but got " + value.size())
      }
      val result = MutableBytes32.create()
      value.copyTo(result, 0)
      return result
    }

    /**
     * Parse a hexadecimal string into a [Bytes32].
     *
     *
     *
     * This method is lenient in that `str` may of an odd length, in which case it will behave exactly as if it had
     * an additional 0 in front.
     *
     * @param str The hexadecimal string to parse, which may or may not start with "0x". That representation may contain
     * less than 32 bytes, in which case the result is left padded with zeros (see [.fromHexStringStrict] if
     * this is not what you want).
     * @return The value corresponding to `str`.
     * @throws IllegalArgumentException if `str` does not correspond to a valid hexadecimal representation or
     * contains more than 32 bytes.
     */
    fun fromHexStringLenient(str: CharSequence): Bytes32 {
      return wrap(BytesValues.fromRawHexString(str, SIZE, true))
    }

    /**
     * Parse a hexadecimal string into a [Bytes32].
     *
     *
     *
     * This method is strict in that `str` must of an even length.
     *
     * @param str The hexadecimal string to parse, which may or may not start with "0x". That representation may contain
     * less than 32 bytes, in which case the result is left padded with zeros (see [.fromHexStringStrict] if
     * this is not what you want).
     * @return The value corresponding to `str`.
     * @throws IllegalArgumentException if `str` does not correspond to a valid hexadecimal representation, is of an
     * odd length, or contains more than 32 bytes.
     */
    fun fromHexString(str: CharSequence): Bytes32 {
      return wrap(BytesValues.fromRawHexString(str, SIZE, false))
    }

    /**
     * Generate random bytes.
     *
     * @param generator The generator for random bytes.
     * @return A value containing random bytes.
     */
    @JvmOverloads
    fun random(generator: Random = SecureRandom()): Bytes32 {
      val array = ByteArray(32)
      generator.nextBytes(array)
      return wrap(array)
    }

    /**
     * Parse a hexadecimal string into a [Bytes32].
     *
     *
     *
     * This method is extra strict in that `str` must of an even length and the provided representation must have
     * exactly 32 bytes.
     *
     * @param str The hexadecimal string to parse, which may or may not start with "0x".
     * @return The value corresponding to `str`.
     * @throws IllegalArgumentException if `str` does not correspond to a valid hexadecimal representation, is of an
     * odd length or does not contain exactly 32 bytes.
     */
    fun fromHexStringStrict(str: CharSequence): Bytes32 {
      return wrap(BytesValues.fromRawHexString(str, -1, false))
    }
  }
}
