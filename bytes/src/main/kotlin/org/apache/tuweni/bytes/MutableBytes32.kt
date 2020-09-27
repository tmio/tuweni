package org.apache.tuweni.bytes

import org.apache.tuweni.bytes.Bytes32.Companion.SIZE

/**
 * A mutable [Bytes32], that is a mutable [Bytes] value of exactly 32 bytes.
 */
interface MutableBytes32 : MutableBytes, Bytes32 {
  companion object {

    /**
     * Create a new mutable 32 bytes value.
     *
     * @return A newly allocated [MutableBytes] value.
     */
    fun create(): MutableBytes32 {
      return MutableArrayWrappingBytes32(ByteArray(SIZE))
    }

    /**
     * Wrap a 32 bytes array as a mutable 32 bytes value.
     *
     * @param value The value to wrap.
     * @return A [MutableBytes32] wrapping `value`.
     * @throws IllegalArgumentException if `value.length != 32`.
     */
    fun wrap(value: ByteArray): MutableBytes32 {
      return MutableArrayWrappingBytes32(value)
    }

    /**
     * Wrap a the provided array as a [MutableBytes32].
     *
     *
     *
     * Note that value is not copied, only wrapped, and thus any future update to `value` within the wrapped parts
     * will be reflected in the returned value.
     *
     * @param value The bytes to wrap.
     * @param offset The index (inclusive) in `value` of the first byte exposed by the returned value. In other
     * words, you will have `wrap(value, i).get(0) == value[i]`.
     * @return A [MutableBytes32] that exposes the bytes of `value` from `offset` (inclusive) to
     * `offset + 32` (exclusive).
     * @throws IndexOutOfBoundsException if `offset &lt; 0 || (value.length &gt; 0 && offset >=
     * value.length)`.
     * @throws IllegalArgumentException if `length &lt; 0 || offset + 32 &gt; value.length`.
     */
    fun wrap(value: ByteArray, offset: Int): MutableBytes32 {
      return MutableArrayWrappingBytes32(value, offset)
    }

    /**
     * Wrap a the provided value, which must be of size 32, as a [MutableBytes32].
     *
     *
     *
     * Note that value is not copied, only wrapped, and thus any future update to `value` will be reflected in the
     * returned value.
     *
     * @param value The bytes to wrap.
     * @return A [MutableBytes32] that exposes the bytes of `value`.
     * @throws IllegalArgumentException if `value.size() != 32`.
     */
    fun wrap(value: MutableBytes): MutableBytes32 {
      return value as? MutableBytes32 ?: DelegatingMutableBytes32.delegateTo(value)
    }

    /**
     * Wrap a slice/sub-part of the provided value as a [MutableBytes32].
     *
     *
     *
     * Note that the value is not copied, and thus any future update to `value` within the wrapped parts will be
     * reflected in the returned value.
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
    fun wrap(value: MutableBytes, offset: Int): MutableBytes32 {
      if (value is MutableBytes32) {
        return value
      }
      val slice = value.mutableSlice(offset, Bytes32.SIZE)
      return slice as? MutableBytes32 ?: DelegatingMutableBytes32.delegateTo(slice)
    }
  }
}
