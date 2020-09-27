package org.apache.tuweni.bytes

import java.security.SecureRandom
import java.util.*


/**
 * A [Bytes] value that is guaranteed to contain exactly 48 bytes.
 */
interface Bytes48 : Bytes {

  override fun size(): Int {
    return SIZE
  }

  /**
   * Return a bit-wise AND of these bytes and the supplied bytes.
   *
   * @param other The bytes to perform the operation with.
   * @return The result of a bit-wise AND.
   */
  fun and(other: Bytes48): Bytes48 {
    return and(other, MutableBytes48.create())
  }

  /**
   * Return a bit-wise OR of these bytes and the supplied bytes.
   *
   * @param other The bytes to perform the operation with.
   * @return The result of a bit-wise OR.
   */
  fun or(other: Bytes48): Bytes48 {
    return or(other, MutableBytes48.create())
  }

  /**
   * Return a bit-wise XOR of these bytes and the supplied bytes.
   *
   * @param other The bytes to perform the operation with.
   * @return The result of a bit-wise XOR.
   */
  fun xor(other: Bytes48): Bytes48 {
    return xor(other, MutableBytes48.create())
  }

  override fun not(): Bytes48 {
    return not(MutableBytes48.create())
  }

  override fun shiftRight(distance: Int): Bytes48 {
    return shiftRight(distance, MutableBytes48.create())
  }

  override fun shiftLeft(distance: Int): Bytes48 {
    return shiftLeft(distance, MutableBytes48.create())
  }

  override fun copy(): Bytes48

  override fun mutableCopy(): MutableBytes48

  companion object {
    /** The number of bytes in this value - i.e. 48  */
    val SIZE = 48

    /** A `Bytes48` containing all zero bytes  */
    val ZERO = wrap(ByteArray(SIZE))

    /**
     * Wrap the provided byte array, which must be of length 48, as a [Bytes48].
     *
     *
     *
     * Note that value is not copied, only wrapped, and thus any future update to `value` will be reflected in the
     * returned value.
     *
     * @param bytes The bytes to wrap.
     * @return A [Bytes48] wrapping `value`.
     * @throws IllegalArgumentException if `value.length != 48`.
     */
    fun wrap(bytes: ByteArray): Bytes48 {
      if (bytes.size != SIZE) {
        throw IllegalArgumentException("Expected " + SIZE + " bytes but got " + bytes.size)
      }
      return wrap(bytes, 0)
    }

    /**
     * Wrap a slice/sub-part of the provided array as a [Bytes48].
     *
     *
     *
     * Note that value is not copied, only wrapped, and thus any future update to `value` within the wrapped parts
     * will be reflected in the returned value.
     *
     * @param bytes The bytes to wrap.
     * @param offset The index (inclusive) in `value` of the first byte exposed by the returned value. In other
     * words, you will have `wrap(value, i).get(0) == value[i]`.
     * @return A [Bytes48] that exposes the bytes of `value` from `offset` (inclusive) to
     * `offset + 48` (exclusive).
     * @throws IndexOutOfBoundsException if `offset &lt; 0 || (value.length &gt; 0 && offset >=
     * value.length)`.
     * @throws IllegalArgumentException if `length &lt; 0 || offset + 48 &gt; value.length`.
     */
    fun wrap(bytes: ByteArray, offset: Int): Bytes48 {
      return ArrayWrappingBytes48(bytes, offset)
    }

    /**
     * Wrap a the provided value, which must be of size 48, as a [Bytes48].
     *
     *
     *
     * Note that value is not copied, only wrapped, and thus any future update to `value` will be reflected in the
     * returned value.
     *
     * @param value The bytes to wrap.
     * @return A [Bytes48] that exposes the bytes of `value`.
     * @throws IllegalArgumentException if `value.size() != 48`.
     */
    fun wrap(value: Bytes): Bytes48 {
      if (value is Bytes48) {
        return value
      }
      if (value.size() != SIZE) {
        throw IllegalArgumentException("Expected " + SIZE + " bytes but got " + value.size())
      }
      return DelegatingBytes48(value)
    }

    /**
     * Wrap a slice/sub-part of the provided value as a [Bytes48].
     *
     *
     *
     * Note that value is not copied, only wrapped, and thus any future update to `value` within the wrapped parts
     * will be reflected in the returned value.
     *
     * @param value The bytes to wrap.
     * @param offset The index (inclusive) in `value` of the first byte exposed by the returned value. In other
     * words, you will have `wrap(value, i).get(0) == value.get(i)`.
     * @return A [Bytes48] that exposes the bytes of `value` from `offset` (inclusive) to
     * `offset + 48` (exclusive).
     * @throws IndexOutOfBoundsException if `offset &lt; 0 || (value.size() &gt; 0 && offset >=
     * value.size())`.
     * @throws IllegalArgumentException if `length &lt; 0 || offset + 48 &gt; value.size()`.
     */
    fun wrap(value: Bytes, offset: Int): Bytes48 {
      if (value is Bytes48) {
        return value
      }
      val slice = value.slice(offset, Bytes48.SIZE)
      return slice as? Bytes48 ?: DelegatingBytes48(Bytes48.wrap(slice))
    }

    /**
     * Left pad a [Bytes] value with zero bytes to create a [Bytes48].
     *
     * @param value The bytes value pad.
     * @return A [Bytes48] that exposes the left-padded bytes of `value`.
     * @throws IllegalArgumentException if `value.size() &gt; 48`.
     */
    fun leftPad(value: Bytes): Bytes48 {
      if (value is Bytes48) {
        return value
      }

      if (value.size() > SIZE) {
        throw IllegalArgumentException("Expected at most " + SIZE + " bytes but got " + value.size())
      }
      val result = MutableBytes48.create()
      value.copyTo(result, SIZE - value.size())
      return result
    }


    /**
     * Right pad a [Bytes] value with zero bytes to create a [Bytes48].
     *
     * @param value The bytes value pad.
     * @return A [Bytes48] that exposes the rightw-padded bytes of `value`.
     * @throws IllegalArgumentException if `value.size() &gt; 48`.
     */
    fun rightPad(value: Bytes): Bytes48 {
      if (value is Bytes48) {
        return value
      }

      if (value.size() > SIZE) {
        throw IllegalArgumentException("Expected at most " + SIZE + " bytes but got " + value.size())
      }
      val result = MutableBytes48.create()
      value.copyTo(result, 0)
      return result
    }

    /**
     * Parse a hexadecimal string into a [Bytes48].
     *
     *
     *
     * This method is lenient in that `str` may of an odd length, in which case it will behave exactly as if it had
     * an additional 0 in front.
     *
     * @param str The hexadecimal string to parse, which may or may not start with "0x". That representation may contain
     * less than 48 bytes, in which case the result is left padded with zeros (see [.fromHexStringStrict] if
     * this is not what you want).
     * @return The value corresponding to `str`.
     * @throws IllegalArgumentException if `str` does not correspond to a valid hexadecimal representation or
     * contains more than 48 bytes.
     */
    fun fromHexStringLenient(str: CharSequence): Bytes48 {
      return wrap(BytesValues.fromRawHexString(str, SIZE, true))
    }

    /**
     * Parse a hexadecimal string into a [Bytes48].
     *
     *
     *
     * This method is strict in that `str` must of an even length.
     *
     * @param str The hexadecimal string to parse, which may or may not start with "0x". That representation may contain
     * less than 48 bytes, in which case the result is left padded with zeros (see [.fromHexStringStrict] if
     * this is not what you want).
     * @return The value corresponding to `str`.
     * @throws IllegalArgumentException if `str` does not correspond to a valid hexadecimal representation, is of an
     * odd length, or contains more than 48 bytes.
     */
    fun fromHexString(str: CharSequence): Bytes48 {
      return wrap(BytesValues.fromRawHexString(str, SIZE, false))
    }

    /**
     * Generate random bytes.
     *
     * @param generator The generator for random bytes.
     * @return A value containing random bytes.
     */
    @JvmOverloads
    fun random(generator: Random = SecureRandom()): Bytes48 {
      val array = ByteArray(48)
      generator.nextBytes(array)
      return wrap(array)
    }

    /**
     * Parse a hexadecimal string into a [Bytes48].
     *
     *
     *
     * This method is extra strict in that `str` must of an even length and the provided representation must have
     * exactly 48 bytes.
     *
     * @param str The hexadecimal string to parse, which may or may not start with "0x".
     * @return The value corresponding to `str`.
     * @throws IllegalArgumentException if `str` does not correspond to a valid hexadecimal representation, is of an
     * odd length or does not contain exactly 48 bytes.
     */
    fun fromHexStringStrict(str: CharSequence): Bytes48 {
      return wrap(BytesValues.fromRawHexString(str, -1, false))
    }
  }
}
