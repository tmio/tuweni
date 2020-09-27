package org.apache.tuweni.bytes

/**
 * A mutable [Bytes48], that is a mutable [Bytes] value of exactly 48 bytes.
 */
interface MutableBytes48 : MutableBytes, Bytes48 {
  companion object {

    /**
     * Create a new mutable 48 bytes value.
     *
     * @return A newly allocated [MutableBytes] value.
     */
    fun create(): MutableBytes48 {
      return MutableArrayWrappingBytes48(ByteArray(Bytes48.SIZE))
    }

    /**
     * Wrap a 48 bytes array as a mutable 48 bytes value.
     *
     * @param value The value to wrap.
     * @return A [MutableBytes48] wrapping `value`.
     * @throws IllegalArgumentException if `value.length != 48`.
     */
    fun wrap(value: ByteArray): MutableBytes48 {
      return MutableArrayWrappingBytes48(value)
    }

    /**
     * Wrap a the provided array as a [MutableBytes48].
     *
     *
     *
     * Note that value is not copied, only wrapped, and thus any future update to `value` within the wrapped parts
     * will be reflected in the returned value.
     *
     * @param value The bytes to wrap.
     * @param offset The index (inclusive) in `value` of the first byte exposed by the returned value. In other
     * words, you will have `wrap(value, i).get(0) == value[i]`.
     * @return A [MutableBytes48] that exposes the bytes of `value` from `offset` (inclusive) to
     * `offset + 48` (exclusive).
     * @throws IndexOutOfBoundsException if `offset &lt; 0 || (value.length &gt; 0 && offset >=
     * value.length)`.
     * @throws IllegalArgumentException if `length &lt; 0 || offset + 48 &gt; value.length`.
     */
    fun wrap(value: ByteArray, offset: Int): MutableBytes48 {
      return MutableArrayWrappingBytes48(value, offset)
    }

    /**
     * Wrap a the provided value, which must be of size 48, as a [MutableBytes48].
     *
     *
     *
     * Note that value is not copied, only wrapped, and thus any future update to `value` will be reflected in the
     * returned value.
     *
     * @param value The bytes to wrap.
     * @return A [MutableBytes48] that exposes the bytes of `value`.
     * @throws IllegalArgumentException if `value.size() != 48`.
     */
    fun wrap(value: MutableBytes): MutableBytes48 {
      return value as? MutableBytes48 ?: DelegatingMutableBytes48.delegateTo(value)
    }

    /**
     * Wrap a slice/sub-part of the provided value as a [MutableBytes48].
     *
     *
     *
     * Note that the value is not copied, and thus any future update to `value` within the wrapped parts will be
     * reflected in the returned value.
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
    fun wrap(value: MutableBytes, offset: Int): MutableBytes48 {
      if (value is MutableBytes48) {
        return value
      }
      val slice = value.mutableSlice(offset, Bytes48.SIZE)
      return slice as? MutableBytes48 ?: DelegatingMutableBytes48.delegateTo(slice)
    }
  }
}
