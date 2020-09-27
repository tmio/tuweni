/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements. See the NOTICE
 * file distributed with this work for additional information regarding copyright ownership. The ASF licenses this file
 * to You under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.apache.tuweni.bytes

import java.lang.String.format
import java.nio.ByteOrder.BIG_ENDIAN

import java.io.IOException
import java.io.UncheckedIOException
import java.math.BigInteger
import java.nio.BufferOverflowException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.ReadOnlyBufferException
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Arrays
import java.util.Base64
import java.util.Random

import io.netty.buffer.ByteBuf
import io.vertx.core.buffer.Buffer
import kotlin.experimental.and
import kotlin.experimental.inv
import kotlin.experimental.or
import kotlin.experimental.xor

/**
 * A value made of bytes.
 *
 *
 *
 * This interface makes no thread-safety guarantee, and a [Bytes] value is generally not thread safe. However,
 * specific implementations may be thread-safe. For instance, the value returned by [.copy] is guaranteed to be
 * thread-safe as it is immutable.
 */
interface Bytes : Comparable<Bytes> {

  /**
   * Whether this value contains no bytes.
   *
   * @return true if the value contains no bytes
   */
  val isEmpty: Boolean
    get() = size() == 0

  /**
   * Whether this value has only zero bytes.
   *
   * @return `true` if all the bits of this value are zeros.
   */
  val isZero: Boolean
    get() {
      for (i in size() - 1 downTo 0) {
        if (get(i).toInt() != 0)
          return false
      }
      return true
    }

  /** @return The number of bytes this value represents.
   */
  fun size(): Int

  /**
   * Retrieve a byte in this value.
   *
   * @param i The index of the byte to fetch within the value (0-indexed).
   * @return The byte at index `i` in this value.
   * @throws IndexOutOfBoundsException if `i &lt; 0` or {i &gt;= size()}.
   */
  operator fun get(i: Int): Byte

  /**
   * Retrieve the 4 bytes starting at the provided index in this value as an integer.
   *
   * @param i The index from which to get the int, which must less than or equal to `size() - 4`.
   * @return An integer whose value is the 4 bytes from this value starting at index `i`.
   * @throws IndexOutOfBoundsException if `i &lt; 0` or `i &gt; size() - 4`.
   */
  fun getInt(i: Int): Int = getInt(i, BIG_ENDIAN)

  /**
   * Retrieve the 4 bytes starting at the provided index in this value as an integer.
   *
   * @param i The index from which to get the int, which must less than or equal to `size() - 4`.
   * @param order The byte-order for decoding the integer.
   * @return An integer whose value is the 4 bytes from this value starting at index `i`.
   * @throws IndexOutOfBoundsException if `i &lt; 0` or `i &gt; size() - 4`.
   */
  fun getInt(i: Int, order: ByteOrder = BIG_ENDIAN): Int {
    val size = size()
    if (i > size - 4) {
      throw IndexOutOfBoundsException(
        format("Value of size %s has not enough bytes to read a 4 bytes int from index %s", size, i)
      )
    }

    var value = 0
    if (order == BIG_ENDIAN) {
      value = value or (get(i).toInt() and 0xFF shl 24)
      value = value or (get(i + 1).toInt() and 0xFF shl 16)
      value = value or (get(i + 2).toInt() and 0xFF shl 8)
      value = value or (get(i + 3).toInt() and 0xFF)
    } else {
      value = value or (get(i + 3).toInt() and 0xFF shl 24)
      value = value or (get(i + 2).toInt() and 0xFF shl 16)
      value = value or (get(i + 1).toInt() and 0xFF shl 8)
      value = value or (get(i).toInt() and 0xFF)
    }
    return value
  }

  /**
   * The value corresponding to interpreting these bytes as an integer.
   *
   * @return An value corresponding to this value interpreted as an integer.
   * @throws IllegalArgumentException if `size() &gt; 4`.
   */
  fun toInt() = toInt(BIG_ENDIAN)

  /**
   * The value corresponding to interpreting these bytes as an integer.
   *
   * @param order The byte-order for decoding the integer.
   * @return An value corresponding to this value interpreted as an integer.
   * @throws IllegalArgumentException if `size() &gt; 4`.
   */
  fun toInt(order: ByteOrder = BIG_ENDIAN): Int {
    val size = size()
    if (size > 4) {
      throw IllegalArgumentException("Value of size $size has more than 4 bytes")
    }
    if (size == 0) {
      return 0
    }
    if (order == BIG_ENDIAN) {
      var i = size
      var value = get(--i).toInt() and 0xFF
      if (i == 0) {
        return value
      }
      value = value or (get(--i).toInt() and 0xFF shl 8)
      if (i == 0) {
        return value
      }
      value = value or (get(--i).toInt() and 0xFF shl 16)
      return if (i == 0) {
        value
      } else value or (get(--i).toInt() and 0xFF shl 24)
    } else {
      var i = 0
      var value = get(i).toInt() and 0xFF
      if (++i == size) {
        return value
      }
      value = value or (get(i++).toInt() and 0xFF shl 8)
      if (i == size) {
        return value
      }
      value = value or (get(i++).toInt() and 0xFF shl 16)
      return if (i == size) {
        value
      } else value or (get(i).toInt() and 0xFF shl 24)
    }
  }

  /**
   * Retrieves the 8 bytes starting at the provided index in this value as a long.
   *
   * @param i The index from which to get the long, which must less than or equal to `size() - 8`.
   * @return A long whose value is the 8 bytes from this value starting at index `i`.
   * @throws IndexOutOfBoundsException if `i &lt; 0` or `i &gt; size() - 8`.
   */
  fun getLong(i: Int) = getLong(i, BIG_ENDIAN)

  /**
   * Retrieves the 8 bytes starting at the provided index in this value as a long.
   *
   * @param i The index from which to get the long, which must less than or equal to `size() - 8`.
   * @param order The byte-order for decoding the integer.
   * @return A long whose value is the 8 bytes from this value starting at index `i`.
   * @throws IndexOutOfBoundsException if `i &lt; 0` or `i &gt; size() - 8`.
   */
  fun getLong(i: Int, order: ByteOrder = BIG_ENDIAN): Long {
    val size = size()
    if (i > size - 8) {
      throw IndexOutOfBoundsException(
        format("Value of size %s has not enough bytes to read a 8 bytes long from index %s", size, i)
      )
    }

    var value: Long = 0
    if (order == BIG_ENDIAN) {
      value = value or (get(i).toLong() and 0xFF shl 56)
      value = value or (get(i + 1).toLong() and 0xFF shl 48)
      value = value or (get(i + 2).toLong() and 0xFF shl 40)
      value = value or (get(i + 3).toLong() and 0xFF shl 32)
      value = value or (get(i + 4).toLong() and 0xFF shl 24)
      value = value or (get(i + 5).toLong() and 0xFF shl 16)
      value = value or (get(i + 6).toLong() and 0xFF shl 8)
      value = value or (get(i + 7).toLong() and 0xFF)
    } else {
      value = value or (get(i + 7).toLong() and 0xFF shl 56)
      value = value or (get(i + 6).toLong() and 0xFF shl 48)
      value = value or (get(i + 5).toLong() and 0xFF shl 40)
      value = value or (get(i + 4).toLong() and 0xFF shl 32)
      value = value or (get(i + 3).toLong() and 0xFF shl 24)
      value = value or (get(i + 2).toLong() and 0xFF shl 16)
      value = value or (get(i + 1).toLong() and 0xFF shl 8)
      value = value or (get(i).toLong() and 0xFF)
    }
    return value
  }

  /**
   * The value corresponding to interpreting these bytes as a long.
   *
   * @return An value corresponding to this value interpreted as a long.
   * @throws IllegalArgumentException if `size() &gt; 8`.
   */
  fun toLong() = toLong(BIG_ENDIAN)

  /**
   * The value corresponding to interpreting these bytes as a long.
   *
   * @param order The byte-order for decoding the integer.
   * @return An value corresponding to this value interpreted as a long.
   * @throws IllegalArgumentException if `size() &gt; 8`.
   */
  fun toLong(order: ByteOrder = BIG_ENDIAN): Long {
    val size = size()
    if (size > 8) {
      throw java.lang.IllegalArgumentException("Value of size $size has more than 8 bytes")
    }
    if (size == 0) {
      return 0
    }
    if (order == BIG_ENDIAN) {
      var i = size
      var value = get(--i).toLong() and 0xFF
      if (i == 0) {
        return value
      }
      value = value or (get(--i).toLong() and 0xFF shl 8)
      if (i == 0) {
        return value
      }
      value = value or (get(--i).toLong() and 0xFF shl 16)
      if (i == 0) {
        return value
      }
      value = value or (get(--i).toLong() and 0xFF shl 24)
      if (i == 0) {
        return value
      }
      value = value or (get(--i).toLong() and 0xFF shl 32)
      if (i == 0) {
        return value
      }
      value = value or (get(--i).toLong() and 0xFF shl 40)
      if (i == 0) {
        return value
      }
      value = value or (get(--i).toLong() and 0xFF shl 48)
      return if (i == 0) {
        value
      } else value or (get(--i).toLong() and 0xFF shl 56)
    } else {
      var i = 0
      var value = get(i).toLong() and 0xFF
      if (++i == size) {
        return value
      }
      value = value or (get(i).toLong() and 0xFF shl 8)
      if (++i == size) {
        return value
      }
      value = value or (get(i).toLong() and 0xFF shl 16)
      if (++i == size) {
        return value
      }
      value = value or (get(i).toLong() and 0xFF shl 24)
      if (++i == size) {
        return value
      }
      value = value or (get(i).toLong() and 0xFF shl 32)
      if (++i == size) {
        return value
      }
      value = value or (get(i).toLong() and 0xFF shl 40)
      if (++i == size) {
        return value
      }
      value = value or (get(i).toLong() and 0xFF shl 48)
      return if (++i == size) {
        value
      } else value or (get(i).toLong() and 0xFF shl 56)
    }
  }

  /**
   * The BigInteger corresponding to interpreting these bytes as a two's-complement signed integer.
   *
   * @return A [BigInteger] corresponding to interpreting these bytes as a two's-complement signed integer.
   */
  fun toBigInteger() = toBigInteger(BIG_ENDIAN)

  /**
   * The BigInteger corresponding to interpreting these bytes as a two's-complement signed integer.
   *
   * @param order The byte-order for decoding the integer.
   * @return A [BigInteger] corresponding to interpreting these bytes as a two's-complement signed integer.
   */
  fun toBigInteger(order: ByteOrder = BIG_ENDIAN): BigInteger {
    return if (size() == 0) {
      BigInteger.ZERO
    } else BigInteger(if (order == BIG_ENDIAN) toArrayUnsafe() else reverse().toArrayUnsafe())
  }

  /**
   * The BigInteger corresponding to interpreting these bytes as an unsigned integer.
   *
   * @return A positive (or zero) [BigInteger] corresponding to interpreting these bytes as an unsigned integer.
   */
  fun toUnsignedBigInteger() = toUnsignedBigInteger(BIG_ENDIAN)

  /**
   * The BigInteger corresponding to interpreting these bytes as an unsigned integer.
   *
   * @param order The byte-order for decoding the integer.
   * @return A positive (or zero) [BigInteger] corresponding to interpreting these bytes as an unsigned integer.
   */
  fun toUnsignedBigInteger(order: ByteOrder = BIG_ENDIAN): BigInteger {
    return BigInteger(1, if (order == BIG_ENDIAN) toArrayUnsafe() else reverse().toArrayUnsafe())
  }

  /**
   * Whether the bytes start with a zero bit value.
   *
   * @return true if the first bit equals zero
   */
  fun hasLeadingZero(): Boolean {
    return size() > 0 && (get(0) and 0x80.toByte()).toInt() == 0
  }

  /**
   * @return The number of zero bits preceding the highest-order ("leftmost") one-bit, or `size() * 8` if all bits
   * are zero.
   */
  fun numberOfLeadingZeros(): Int {
    val size = size()
    for (i in 0 until size) {
      val b = get(i)
      if (b.toInt() == 0) {
        continue
      }

      return i * 8 + Integer.numberOfLeadingZeros((b and 0xFF.toByte()).toInt()) - 3 * 8
    }
    return size * 8
  }

  /**
   * Whether the bytes start with a zero byte value.
   *
   * @return true if the first byte equals zero
   */
  fun hasLeadingZeroByte(): Boolean {
    return size() > 0 && get(0).toInt() == 0
  }

  /**
   * @return The number of leading zero bytes of the value.
   */
  fun numberOfLeadingZeroBytes(): Int {
    val size = size()
    for (i in 0 until size) {
      if (get(i).toInt() != 0) {
        return i
      }
    }
    return size
  }

  /**
   * @return The number of trailing zero bytes of the value.
   */
  fun numberOfTrailingZeroBytes(): Int {
    val size = size()
    for (i in size downTo 1) {
      if (get(i - 1).toInt() != 0) {
        return size - i
      }
    }
    return size
  }

  /**
   * @return The number of bits following and including the highest-order ("leftmost") one-bit, or zero if all bits are
   * zero.
   */
  fun bitLength(): Int {
    val size = size()
    for (i in 0 until size) {
      val b = get(i)
      if (b.toInt() == 0)
        continue

      return size * 8 - i * 8 - (Integer.numberOfLeadingZeros((b and 0xFF.toByte()).toInt()) - 3 * 8)
    }
    return 0
  }

  /**
   * Return a bit-wise AND of these bytes and the supplied bytes.
   *
   * If this value and the supplied value are different lengths, then the shorter will be zero-padded to the left.
   *
   * @param other The bytes to perform the operation with.
   * @return The result of a bit-wise AND.
   */
  fun and(other: Bytes): Bytes {
    return and(other, MutableBytes.create(Math.max(size(), other.size())))
  }

  /**
   * Calculate a bit-wise AND of these bytes and the supplied bytes.
   *
   *
   *
   * If this value or the supplied value are shorter in length than the output vector, then they will be zero-padded to
   * the left. Likewise, if either this value or the supplied valid is longer in length than the output vector, then
   * they will be truncated to the left.
   *
   * @param other The bytes to perform the operation with.
   * @param result The mutable output vector for the result.
   * @param <T> The [MutableBytes] value type.
   * @return The `result` output vector.
  </T> */
  fun <T : MutableBytes> and(other: Bytes, result: T): T {
    val rSize = result.size()
    val offsetSelf = rSize - size()
    val offsetOther = rSize - other.size()
    for (i in 0 until rSize) {
      val b1 = if (i < offsetSelf) 0x00 else get(i - offsetSelf)
      val b2 = if (i < offsetOther) 0x00 else other[i - offsetOther]
      result.set(i, (b1 and b2))
    }
    return result
  }

  /**
   * Return a bit-wise OR of these bytes and the supplied bytes.
   *
   *
   *
   * If this value and the supplied value are different lengths, then the shorter will be zero-padded to the left.
   *
   * @param other The bytes to perform the operation with.
   * @return The result of a bit-wise OR.
   */
  fun or(other: Bytes): Bytes {
    return or(other, MutableBytes.create(Math.max(size(), other.size())))
  }

  /**
   * Calculate a bit-wise OR of these bytes and the supplied bytes.
   *
   *
   *
   * If this value or the supplied value are shorter in length than the output vector, then they will be zero-padded to
   * the left. Likewise, if either this value or the supplied valid is longer in length than the output vector, then
   * they will be truncated to the left.
   *
   * @param other The bytes to perform the operation with.
   * @param result The mutable output vector for the result.
   * @param <T> The [MutableBytes] value type.
   * @return The `result` output vector.
  </T> */
  fun <T : MutableBytes> or(other: Bytes, result: T): T {
    val rSize = result.size()
    val offsetSelf = rSize - size()
    val offsetOther = rSize - other.size()
    for (i in 0 until rSize) {
      val b1 = if (i < offsetSelf) 0x00 else get(i - offsetSelf)
      val b2 = if (i < offsetOther) 0x00 else other[i - offsetOther]
      result.set(i, (b1 or b2))
    }
    return result
  }

  /**
   * Return a bit-wise XOR of these bytes and the supplied bytes.
   *
   *
   *
   * If this value and the supplied value are different lengths, then the shorter will be zero-padded to the left.
   *
   * @param other The bytes to perform the operation with.
   * @return The result of a bit-wise XOR.
   */
  fun xor(other: Bytes): Bytes {
    return xor(other, MutableBytes.create(Math.max(size(), other.size())))
  }

  /**
   * Calculate a bit-wise XOR of these bytes and the supplied bytes.
   *
   *
   *
   * If this value or the supplied value are shorter in length than the output vector, then they will be zero-padded to
   * the left. Likewise, if either this value or the supplied valid is longer in length than the output vector, then
   * they will be truncated to the left.
   *
   * @param other The bytes to perform the operation with.
   * @param result The mutable output vector for the result.
   * @param <T> The [MutableBytes] value type.
   * @return The `result` output vector.
  </T> */
  fun <T : MutableBytes> xor(other: Bytes, result: T): T {
    val rSize = result.size()
    val offsetSelf = rSize - size()
    val offsetOther = rSize - other.size()
    for (i in 0 until rSize) {
      val b1 = if (i < offsetSelf) 0x00 else get(i - offsetSelf)
      val b2 = if (i < offsetOther) 0x00 else other[i - offsetOther]
      result.set(i, (b1 xor b2).toByte())
    }
    return result
  }

  /**
   * Return a bit-wise NOT of these bytes.
   *
   * @return The result of a bit-wise NOT.
   */
  operator fun not(): Bytes {
    return not(MutableBytes.create(size()))
  }

  /**
   * Calculate a bit-wise NOT of these bytes.
   *
   *
   *
   * If this value is shorter in length than the output vector, then it will be zero-padded to the left. Likewise, if
   * this value is longer in length than the output vector, then it will be truncated to the left.
   *
   * @param result The mutable output vector for the result.
   * @param <T> The [MutableBytes] value type.
   * @return The `result` output vector.
  </T> */
  fun <T : MutableBytes> not(result: T): T {
    val rSize = result.size()
    val offsetSelf = rSize - size()
    for (i in 0 until rSize) {
      val b1 = if (i < offsetSelf) 0x00 else get(i - offsetSelf)
      result.set(i, b1.inv())
    }
    return result
  }

  /**
   * Shift all bits in this value to the right.
   *
   * @param distance The number of bits to shift by.
   * @return A value containing the shifted bits.
   */
  fun shiftRight(distance: Int): Bytes {
    return shiftRight(distance, MutableBytes.create(size()))
  }

  /**
   * Shift all bits in this value to the right.
   *
   *
   *
   * If this value is shorter in length than the output vector, then it will be zero-padded to the left. Likewise, if
   * this value is longer in length than the output vector, then it will be truncated to the left (after shifting).
   *
   * @param distance The number of bits to shift by.
   * @param result The mutable output vector for the result.
   * @param <T> The [MutableBytes] value type.
   * @return The `result` output vector.
  </T> */
  fun <T : MutableBytes> shiftRight(distance: Int, result: T): T {
    val rSize = result.size()
    val offsetSelf = rSize - size()

    val d = distance / 8
    val s = distance % 8
    var resIdx = rSize - 1
    for (i in rSize - 1 - d downTo 0) {
      val res: Byte
      if (i < offsetSelf) {
        res = 0
      } else {
        val selfIdx = i - offsetSelf
        val leftSide = (get(selfIdx) and 0xFF.toByte()).toInt().ushr(s)
        val rightSide = if (selfIdx == 0) 0 else get(selfIdx - 1).toInt() shl 8 - s
        res = (leftSide or rightSide).toByte()
      }
      result.set(resIdx--, res)
    }
    while (resIdx >= 0) {
      result.set(resIdx, 0.toByte())
      resIdx--
    }
    return result
  }

  /**
   * Shift all bits in this value to the left.
   *
   * @param distance The number of bits to shift by.
   * @return A value containing the shifted bits.
   */
  fun shiftLeft(distance: Int): Bytes {
    return shiftLeft(distance, MutableBytes.create(size()))
  }

  /**
   * Shift all bits in this value to the left.
   *
   *
   *
   * If this value is shorter in length than the output vector, then it will be zero-padded to the left. Likewise, if
   * this value is longer in length than the output vector, then it will be truncated to the left.
   *
   * @param distance The number of bits to shift by.
   * @param result The mutable output vector for the result.
   * @param <T> The [MutableBytes] value type.
   * @return The `result` output vector.
  </T> */
  fun <T : MutableBytes> shiftLeft(distance: Int, result: T): T {
    val size = size()
    val rSize = result.size()
    val offsetSelf = rSize - size

    val d = distance / 8
    val s = distance % 8
    var resIdx = 0
    for (i in d until rSize) {
      val res: Byte
      if (i < offsetSelf) {
        res = 0
      } else {
        val selfIdx = i - offsetSelf
        val leftSide = get(selfIdx).toInt() shl s
        val rightSide = if (selfIdx == size - 1) 0 else (get(selfIdx + 1) and 0xFF.toByte()).toInt().ushr(8 - s)
        res = (leftSide or rightSide).toByte()
      }
      result.set(resIdx++, res)
    }
    while (resIdx < rSize) {
      result.set(resIdx, 0.toByte())
      resIdx++
    }
    return result
  }

  /**
   * Create a new value representing (a view of) a slice of the bytes of this value.
   *
   *
   *
   * Please note that the resulting slice is only a view and as such maintains a link to the underlying full value. So
   * holding a reference to the returned slice may hold more memory than the slide represents. Use [.copy] on the
   * returned slice if that is not what you want.
   *
   * @param i The start index for the slice.
   * @return A new value providing a view over the bytes from index `i` (included) to the end.
   * @throws IndexOutOfBoundsException if `i &lt; 0`.
   */
  fun slice(i: Int): Bytes {
    if (i == 0) {
      return this
    }
    val size = size()
    return if (i >= size) {
      EMPTY
    } else slice(i, size - i)
  }

  /**
   * Create a new value representing (a view of) a slice of the bytes of this value.
   *
   *
   *
   * Please note that the resulting slice is only a view and as such maintains a link to the underlying full value. So
   * holding a reference to the returned slice may hold more memory than the slide represents. Use [.copy] on the
   * returned slice if that is not what you want.
   *
   * @param i The start index for the slice.
   * @param length The length of the resulting value.
   * @return A new value providing a view over the bytes from index `i` (included) to `i + length`
   * (excluded).
   * @throws IllegalArgumentException if `length &lt; 0`.
   * @throws IndexOutOfBoundsException if `i &lt; 0` or {i &gt;= size()} or {i + length &gt; size()} .
   */
  fun slice(i: Int, length: Int): Bytes

  /**
   * Return a value equivalent to this one but guaranteed to 1) be deeply immutable (i.e. the underlying value will be
   * immutable) and 2) to not retain more bytes than exposed by the value.
   *
   * @return A value, equals to this one, but deeply immutable and that doesn't retain any "unreachable" bytes. For
   * performance reasons, this is allowed to return this value however if it already fit those constraints.
   */
  fun copy(): Bytes

  /**
   * Return a new mutable value initialized with the content of this value.
   *
   * @return A mutable copy of this value. This will copy bytes, modifying the returned value will **not** modify
   * this value.
   */
  fun mutableCopy(): MutableBytes

  /**
   * Copy the bytes of this value to the provided mutable one, which must have the same size.
   *
   * @param destination The mutable value to which to copy the bytes to, which must have the same size as this value. If
   * you want to copy value where size differs, you should use [.slice] and/or
   * [MutableBytes.mutableSlice] and apply the copy to the result.
   * @throws IllegalArgumentException if `this.size() != destination.size()`.
   */
  fun copyTo(destination: MutableBytes) {
    if (destination.size() != size()) {
      throw java.lang.IllegalArgumentException("Cannot copy ${size()} bytes to destination of non-equal size ${destination.size()}")
    }
    copyTo(destination, 0)
  }

  /**
   * Copy the bytes of this value to the provided mutable one from a particular offset.
   *
   *
   *
   * This is a (potentially slightly more efficient) shortcut for `copyTo(destination.mutableSlice(destinationOffset, this.size()))`.
   *
   * @param destination The mutable value to which to copy the bytes to, which must have enough bytes from
   * `destinationOffset` for the copied value.
   * @param destinationOffset The offset in `destination` at which the copy starts.
   * @throws IllegalArgumentException if the destination doesn't have enough room, that is if `this.size() &gt; (destination.size() - destinationOffset)`.
   */
  fun copyTo(destination: MutableBytes, destinationOffset: Int) {

    // Special casing an empty source or the following checks might throw (even though we have
    // nothing to copy anyway) and this gets inconvenient for generic methods using copyTo() as
    // they may have to special case empty values because of this. As an example,
    // concatenate(EMPTY, EMPTY) would need to be special cased without this.
    val size = size()
    if (size == 0) {
      return
    }

    if (
      destination.size() - destinationOffset < size) {
      throw java.lang.IllegalArgumentException("Cannot copy $size bytes, destination has only ${destination.size() - destinationOffset} bytes from index $destinationOffset")
    }

    for (i in 0 until size) {
      destination.set(destinationOffset + i, get(i))
    }
  }

  /**
   * Append the bytes of this value to the [ByteBuffer].
   *
   * @param byteBuffer The [ByteBuffer] to which to append this value.
   * @throws BufferOverflowException If the writer attempts to write more than the provided buffer can hold.
   * @throws ReadOnlyBufferException If the provided buffer is read-only.
   */
  fun appendTo(byteBuffer: ByteBuffer) {
    for (i in 0 until size()) {
      byteBuffer.put(get(i))
    }
  }

  /**
   * Append the bytes of this value to the provided Vert.x [Buffer].
   *
   *
   *
   * Note that since a Vert.x [Buffer] will grow as necessary, this method never fails.
   *
   * @param buffer The [Buffer] to which to append this value.
   */
  fun appendTo(buffer: Buffer) {
    for (i in 0 until size()) {
      buffer.appendByte(get(i))
    }
  }

  /**
   * Append this value as a sequence of hexadecimal characters.
   *
   * @param appendable The appendable
   * @param <T> The appendable type.
   * @return The appendable.
  </T> */
  fun <T : Appendable> appendHexTo(appendable: T): T {
    try {
      val size = size()
      for (i in 0 until size) {
        val b = get(i)
        appendable.append(AbstractBytes.HEX_CODE[b.toInt() shr 4 and 15])
        appendable.append(AbstractBytes.HEX_CODE[(b and 15).toInt()])
      }
      return appendable
    } catch (e: IOException) {
      throw UncheckedIOException(e)
    }

  }

  /**
   * Return the number of bytes in common between this set of bytes and another.
   *
   * @param other The bytes to compare to.
   * @return The number of common bytes.
   */
  fun commonPrefixLength(other: Bytes): Int {
    val ourSize = size()
    val otherSize = other.size()
    var i = 0
    while (i < ourSize && i < otherSize && get(i) == other[i]) {
      i++
    }
    return i
  }

  /**
   * Return a slice over the common prefix between this set of bytes and another.
   *
   * @param other The bytes to compare to.
   * @return A slice covering the common prefix.
   */
  fun commonPrefix(other: Bytes): Bytes {
    return slice(0, commonPrefixLength(other))
  }

  /**
   * Return a slice of representing the same value but without any leading zero bytes.
   *
   * @return `value` if its left-most byte is non zero, or a slice that exclude any leading zero bytes.
   */
  fun trimLeadingZeros(): Bytes {
    val size = size()
    for (i in 0 until size) {
      if (get(i).toInt() != 0) {
        return slice(i)
      }
    }
    return Bytes.EMPTY
  }

  /**
   * Update the provided message digest with the bytes of this value.
   *
   * @param digest The digest to update.
   */
  fun update(digest: MessageDigest) {
    digest.update(toArrayUnsafe())
  }

  /**
   * Computes the reverse array of bytes of the current bytes.
   *
   * @return a new Bytes value, containing the bytes in reverse order
   */
  fun reverse(): Bytes {
    val reverse = ByteArray(size())
    for (i in 0 until size()) {
      reverse[size() - i - 1] = get(i)
    }
    return Bytes.wrap(reverse)
  }

  /**
   * Extract the bytes of this value into a byte array.
   *
   * @return A byte array with the same content than this value.
   */
  fun toArray() = toArray(BIG_ENDIAN)

  /**
   * Extract the bytes of this value into a byte array.
   *
   * @param byteOrder the byte order to apply : big endian or little endian
   * @return A byte array with the same content than this value.
   */
  fun toArray(byteOrder: ByteOrder = BIG_ENDIAN): ByteArray {
    val size = size()
    val array = ByteArray(size)
    if (byteOrder == BIG_ENDIAN) {
      for (i in 0 until size) {
        array[i] = get(i)
      }
    } else {
      for (i in 0 until size()) {
        array[size() - i - 1] = get(i)
      }
    }
    return array
  }

  /**
   * Get the bytes represented by this value as byte array.
   *
   *
   *
   * Contrarily to [.toArray], this may avoid allocating a new array and directly return the backing array of
   * this value if said value is array backed and doing so is possible. As such, modifications to the returned array may
   * or may not impact this value. As such, this method should be used with care and hence the "unsafe" moniker.
   *
   * @return A byte array with the same content than this value, which may or may not be the direct backing of this
   * value.
   */
  fun toArrayUnsafe(): ByteArray {
    return toArray()
  }

  /**
   * Return the hexadecimal string representation of this value.
   *
   * @return The hexadecimal representation of this value, starting with "0x".
   */
  override fun toString(): String

  /**
   * @return This value represented as hexadecimal, starting with "0x".
   */
  fun toHexString(): String {
    return appendHexTo(StringBuilder("0x")).toString()
  }

  /**
   * @return This value represented as hexadecimal, with no prefix.
   */
  fun toUnprefixedHexString(): String {
    return appendHexTo(StringBuilder()).toString()
  }

  fun toEllipsisHexString(): String {
    val size = size()
    if (size < 6) {
      return toHexString()
    }
    val appendable = StringBuilder("0x")
    for (i in 0..1) {
      val b = get(i)
      appendable.append(AbstractBytes.HEX_CODE[b.toInt() shr 4 and 15])
      appendable.append(AbstractBytes.HEX_CODE[(b and 15).toInt()])
    }
    appendable.append("..")
    for (i in 0..1) {
      val b = get(i + size - 2)
      appendable.append(AbstractBytes.HEX_CODE[b.toInt() shr 4 and 15])
      appendable.append(AbstractBytes.HEX_CODE[(b and 15).toInt()])
    }
    return appendable.toString()
  }

  /** @return This value represented as a minimal hexadecimal string (without any leading zero).
   */
  fun toShortHexString(): String {
    val hex = appendHexTo(StringBuilder())

    var i = 0
    while (i < hex.length && hex[i] == '0') {
      i++
    }
    return "0x" + hex.substring(i)
  }

  /**
   * @return This value represented as a minimal hexadecimal string (without any leading zero, except if it's valued
   * zero or empty, in which case it returns 0x0).
   */
  fun toQuantityHexString(): String {
    if (Bytes.EMPTY == this) {
      return "0x0"
    }
    val hex = appendHexTo(StringBuilder())

    var i = 0
    while (i < hex.length - 1 && hex[i] == '0') {
      i++
    }
    return "0x" + hex.substring(if (hex[hex.length - 1] == '0') i else ++i)
  }

  /**
   * @return This value represented as base 64.
   */
  fun toBase64String(): String {
    return Base64.getEncoder().encodeToString(toArrayUnsafe())
  }

  override fun compareTo(other: Bytes): Int {
    val sizeCmp = bitLength().compareTo(other.bitLength())
    if (sizeCmp != 0) {
      return sizeCmp
    }

    for (i in 0 until size()) {
      val cmp = (get(i) and 0xff.toByte()).toInt().compareTo((other[i] and 0xff.toByte()).toInt())
      if (cmp != 0) {
        return cmp
      }
    }
    return 0
  }

  companion object {

    /**
     * The empty value (with 0 bytes).
     */
    @JvmStatic
    val EMPTY = wrap(ByteArray(0))

    /**
     * Wrap a slice of a byte array as a [Bytes] value.
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
    @JvmOverloads
    @JvmStatic
    fun wrap(value: ByteArray, offset: Int = 0, length: Int = value.size): Bytes {
      return if (length == 32) {
        ArrayWrappingBytes32(value, offset)
      } else ArrayWrappingBytes(value, offset, length)
    }

    /**
     * Wrap a list of other values into a concatenated view.
     *
     *
     *
     * Note that the values are not copied and thus any future update to the values will be reflected in the returned
     * value. If copying the inputs is desired, use [.concatenate].
     *
     * @param values The values to wrap.
     * @return A value representing a view over the concatenation of all `values`.
     * @throws IllegalArgumentException if the result overflows an int.
     */
    @JvmStatic
    fun wrap(vararg values: Bytes): Bytes {
      return ConcatenatedBytes.wrap(*values)
    }

    /**
     * Create a value containing the concatenation of the values provided.
     *
     * @param values The values to copy and concatenate.
     * @return A value containing the result of concatenating the value from `values` in their provided order.
     * @throws IllegalArgumentException if the result overflows an int.
     */
    @JvmStatic
    fun concatenate(values: List<Bytes>): Bytes {
      if (values.isEmpty()) {
        return EMPTY
      }

      val size: Int
      try {
        size = values.stream().mapToInt { it.size() }
          .reduce(0) { x, y -> Math.addExact(x, y) }
      } catch (e: ArithmeticException) {
        throw IllegalArgumentException("Combined length of values is too long (> Integer.MAX_VALUE)")
      }

      val result = MutableBytes.create(size)
      var offset = 0
      for (value in values) {
        value.copyTo(result, offset)
        offset += value.size()
      }
      return result
    }

    /**
     * Create a value containing the concatenation of the values provided.
     *
     * @param values The values to copy and concatenate.
     * @return A value containing the result of concatenating the value from `values` in their provided order.
     * @throws IllegalArgumentException if the result overflows an int.
     */
    @JvmStatic
    fun concatenate(vararg values: Bytes): Bytes {
      if (values.isEmpty()) {
        return EMPTY
      }

      val size: Int
      try {
        size = Arrays.stream(values).mapToInt { it.size() }
          .reduce(0) { x, y -> Math.addExact(x, y) }
      } catch (e: ArithmeticException) {
        throw IllegalArgumentException("Combined length of values is too long (> Integer.MAX_VALUE)")
      }

      val result = MutableBytes.create(size)
      var offset = 0
      for (value in values) {
        value.copyTo(result, offset)
        offset += value.size()
      }
      return result
    }

    /**
     * Wrap a full Vert.x [Buffer] as a [Bytes] value.
     *
     *
     *
     * Note that any change to the content of the buffer may be reflected in the returned value.
     *
     * @param buffer The buffer to wrap.
     * @return A [Bytes] value.
     */
    @JvmStatic
    fun wrapBuffer(buffer: Buffer): Bytes {
      return if (buffer.length() == 0) {
        EMPTY
      } else BufferWrappingBytes(buffer)
    }

    /**
     * Wrap a slice of a Vert.x [Buffer] as a [Bytes] value.
     *
     *
     *
     * Note that any change to the content of the buffer may be reflected in the returned value.
     *
     * @param buffer The buffer to wrap.
     * @param offset The offset in `buffer` from which to expose the bytes in the returned value. That is,
     * `wrapBuffer(buffer, i, 1).get(0) == buffer.getByte(i)`.
     * @param size The size of the returned value.
     * @return A [Bytes] value.
     * @throws IndexOutOfBoundsException if `offset &lt; 0 || (buffer.length() > 0 && offset >=
     * buffer.length())`.
     * @throws IllegalArgumentException if `length &lt; 0 || offset + length > buffer.length()`.
     */
    @JvmStatic
    fun wrapBuffer(buffer: Buffer, offset: Int, size: Int): Bytes {
      return if (size == 0) {
        EMPTY
      } else BufferWrappingBytes(buffer, offset, size)
    }

    /**
     * Wrap a full Netty [ByteBuf] as a [Bytes] value.
     *
     *
     *
     * Note that any change to the content of the byteBuf may be reflected in the returned value.
     *
     * @param byteBuf The [ByteBuf] to wrap.
     * @return A [Bytes] value.
     */
    @JvmStatic
    fun wrapByteBuf(byteBuf: ByteBuf): Bytes {
      return if (byteBuf.capacity() == 0) {
        EMPTY
      } else ByteBufWrappingBytes(byteBuf)
    }

    /**
     * Wrap a slice of a Netty [ByteBuf] as a [Bytes] value.
     *
     *
     *
     * Note that any change to the content of the buffer may be reflected in the returned value.
     *
     * @param byteBuf The [ByteBuf] to wrap.
     * @param offset The offset in `byteBuf` from which to expose the bytes in the returned value. That is,
     * `wrapByteBuf(byteBuf, i, 1).get(0) == byteBuf.getByte(i)`.
     * @param size The size of the returned value.
     * @return A [Bytes] value.
     * @throws IndexOutOfBoundsException if `offset &lt; 0 || (byteBuf.capacity() > 0 && offset >=
     * byteBuf.capacity())`.
     * @throws IllegalArgumentException if `length &lt; 0 || offset + length > byteBuf.capacity()`.
     */
    @JvmStatic
    fun wrapByteBuf(byteBuf: ByteBuf, offset: Int, size: Int): Bytes {
      return if (size == 0) {
        EMPTY
      } else ByteBufWrappingBytes(byteBuf, offset, size)
    }

    /**
     * Wrap a full Java NIO [ByteBuffer] as a [Bytes] value.
     *
     *
     *
     * Note that any change to the content of the byteBuf may be reflected in the returned value.
     *
     * @param byteBuffer The [ByteBuffer] to wrap.
     * @return A [Bytes] value.
     */
    @JvmStatic
    fun wrapByteBuffer(byteBuffer: ByteBuffer): Bytes {
      return if (byteBuffer.limit() == 0) {
        EMPTY
      } else ByteBufferWrappingBytes(byteBuffer)
    }

    /**
     * Wrap a slice of a Java NIO [ByteBuf] as a [Bytes] value.
     *
     *
     *
     * Note that any change to the content of the buffer may be reflected in the returned value.
     *
     * @param byteBuffer The [ByteBuffer] to wrap.
     * @param offset The offset in `byteBuffer` from which to expose the bytes in the returned value. That is,
     * `wrapByteBuffer(byteBuffer, i, 1).get(0) == byteBuffer.getByte(i)`.
     * @param size The size of the returned value.
     * @return A [Bytes] value.
     * @throws IndexOutOfBoundsException if `offset &lt; 0 || (byteBuffer.limit() > 0 && offset >=
     * byteBuf.limit())`.
     * @throws IllegalArgumentException if `length &lt; 0 || offset + length > byteBuffer.limit()`.
     */
    @JvmStatic
    fun wrapByteBuffer(byteBuffer: ByteBuffer, offset: Int, size: Int): Bytes {
      return if (size == 0) {
        EMPTY
      } else ByteBufferWrappingBytes(byteBuffer, offset, size)
    }

    /**
     * Create a value that contains the specified bytes in their specified order.
     *
     * @param bytes The bytes that must compose the returned value.
     * @return A value containing the specified bytes.
     */
    @JvmStatic
    fun of(vararg bytes: Byte): Bytes {
      return wrap(bytes)
    }

    /**
     * Create a value that contains the specified bytes in their specified order.
     *
     * @param bytes The bytes.
     * @return A value containing bytes are the one from `bytes`.
     * @throws IllegalArgumentException if any of the specified would be truncated when storing as a byte.
     */
    @JvmStatic
    fun of(vararg bytes: Int): Bytes {
      val result = ByteArray(bytes.size)
      for (i in bytes.indices) {
        val b = bytes[i]
        if (b != (b.toByte() and 0xff.toByte()).toInt()) {
          throw java.lang.IllegalArgumentException("${i + 1}th value $b does not fit a byte")
        }
        result[i] = b.toByte()
      }
      return Bytes.wrap(result)
    }

    /**
     * Return a 2-byte value corresponding to the provided value interpreted as an unsigned short.
     *
     * @param value The value, which must be no larger than an unsigned short.
     * @param order The byte-order for the integer encoding.
     * @return A 2 bytes value corresponding to `value`.
     * @throws IllegalArgumentException if `value < 0` or `value` is too big to fit an unsigned 2-byte short
     * (that is, if `value >= (1 << 16)`).
     */
    @JvmStatic
    @JvmOverloads
    fun ofUnsignedShort(value: Int, order: ByteOrder = BIG_ENDIAN): Bytes {
      if (value < 0 || value > BytesValues.MAX_UNSIGNED_SHORT) {
        throw java.lang.IllegalArgumentException("Value $value cannot be represented as an unsigned short (it is negative or too big)")
      }
      val res = ByteArray(2)
      if (order == BIG_ENDIAN) {
        res[0] = (value shr 8 and 0xFF).toByte()
        res[1] = (value and 0xFF).toByte()
      } else {
        res[0] = (value and 0xFF).toByte()
        res[1] = (value shr 8 and 0xFF).toByte()
      }
      return Bytes.wrap(res)
    }

    /**
     * Return a 4-byte value corresponding to the provided value interpreted as an unsigned int.
     *
     * @param value The value, which must be no larger than an unsigned int.
     * @param order The byte-order for the integer encoding.
     * @return A 4 bytes value corresponding to the encoded `value`.
     * @throws IllegalArgumentException if `value < 0` or `value` is too big to fit an unsigned 4-byte int
     * (that is, if `value >= (1L << 32)`).
     */
    @JvmStatic
    @JvmOverloads
    fun ofUnsignedInt(value: Long, order: ByteOrder = BIG_ENDIAN): Bytes {
      if (value < 0 || value > BytesValues.MAX_UNSIGNED_INT) {
        throw IllegalArgumentException("Value $value cannot be represented as an unsigned int (it is negative or too big)")
      }
      val res = ByteArray(4)
      if (order == BIG_ENDIAN) {
        res[0] = (value shr 24 and 0xFF).toByte()
        res[1] = (value shr 16 and 0xFF).toByte()
        res[2] = (value shr 8 and 0xFF).toByte()
        res[3] = (value and 0xFF).toByte()
      } else {
        res[0] = (value and 0xFF).toByte()
        res[1] = (value shr 8 and 0xFF).toByte()
        res[2] = (value shr 16 and 0xFF).toByte()
        res[3] = (value shr 24 and 0xFF).toByte()
      }
      return Bytes.wrap(res)
    }

    /**
     * Return an 8-byte value corresponding to the provided value interpreted as an unsigned long.
     *
     * @param value The value, which will be interpreted as an unsigned long.
     * @param order The byte-order for the integer encoding.
     * @return A 8 bytes value corresponding to `value`.
     * @throws IllegalArgumentException if `value < 0` or `value` is too big to fit an unsigned 8-byte int
     * (that is, if `value >= (1L << 64)`).
     */
    @JvmStatic
    @JvmOverloads
    fun ofUnsignedLong(value: Long, order: ByteOrder = BIG_ENDIAN): Bytes {
      val res = ByteArray(8)
      if (order == BIG_ENDIAN) {
        res[0] = (value shr 56 and 0xFF).toByte()
        res[1] = (value shr 48 and 0xFF).toByte()
        res[2] = (value shr 40 and 0xFF).toByte()
        res[3] = (value shr 32 and 0xFF).toByte()
        res[4] = (value shr 24 and 0xFF).toByte()
        res[5] = (value shr 16 and 0xFF).toByte()
        res[6] = (value shr 8 and 0xFF).toByte()
        res[7] = (value and 0xFF).toByte()
      } else {
        res[0] = (value and 0xFF).toByte()
        res[1] = (value shr 8 and 0xFF).toByte()
        res[2] = (value shr 16 and 0xFF).toByte()
        res[3] = (value shr 24 and 0xFF).toByte()
        res[4] = (value shr 32 and 0xFF).toByte()
        res[5] = (value shr 40 and 0xFF).toByte()
        res[6] = (value shr 48 and 0xFF).toByte()
        res[7] = (value shr 56 and 0xFF).toByte()
      }
      return Bytes.wrap(res)
    }

    /**
     * Return the smallest bytes value whose bytes correspond to the provided long. That is, the returned value may be of
     * size less than 8 if the provided long has leading zero bytes.
     *
     * @param value The long from which to create the bytes value.
     * @return The minimal bytes representation corresponding to `l`.
     */
    @JvmStatic
    fun minimalBytes(value: Long): Bytes {
      if (value == 0L) {
        return Bytes.EMPTY
      }

      val zeros = java.lang.Long.numberOfLeadingZeros(value)
      val resultBytes = 8 - zeros / 8

      val result = ByteArray(resultBytes)
      var shift = 0
      for (i in 0 until resultBytes) {
        result[resultBytes - i - 1] = (value shr shift and 0xFF).toByte()
        shift += 8
      }
      return Bytes.wrap(result)
    }

    /**
     * Parse a hexadecimal string into a [Bytes] value.
     *
     *
     *
     * This method is lenient in that `str` may of an odd length, in which case it will behave exactly as if it had
     * an additional 0 in front.
     *
     * @param str The hexadecimal string to parse, which may or may not start with "0x".
     * @return The value corresponding to `str`.
     * @throws IllegalArgumentException if `str` does not correspond to a valid hexadecimal representation.
     */
    @JvmStatic
    fun fromHexStringLenient(str: CharSequence): Bytes {
      return BytesValues.fromHexString(str, -1, true)
    }

    /**
     * Parse a hexadecimal string into a [Bytes] value of the provided size.
     *
     *
     *
     * This method allows for `str` to have an odd length, in which case it will behave exactly as if it had an
     * additional 0 in front.
     *
     * @param str The hexadecimal string to parse, which may or may not start with "0x".
     * @param destinationSize The size of the returned value, which must be big enough to hold the bytes represented by
     * `str`. If it is strictly bigger those bytes from `str`, the returned value will be left padded
     * with zeros.
     * @return A value of size `destinationSize` corresponding to `str` potentially left-padded.
     * @throws IllegalArgumentException if `str` does not correspond to a valid hexadecimal representation,
     * represents more bytes than `destinationSize` or `destinationSize &lt; 0`.
     */
    @JvmStatic
    fun fromHexStringLenient(str: CharSequence, destinationSize: Int): Bytes {
      if (destinationSize < 0) {
        throw java.lang.IllegalArgumentException("Invalid negative destination size $destinationSize")
      }
      return BytesValues.fromHexString(str, destinationSize, true)
    }

    /**
     * Parse a hexadecimal string into a [Bytes] value.
     *
     *
     *
     * This method requires that `str` have an even length.
     *
     * @param str The hexadecimal string to parse, which may or may not start with "0x".
     * @return The value corresponding to `str`.
     * @throws IllegalArgumentException if `str` does not correspond to a valid hexadecimal representation, or is of
     * an odd length.
     */
    @JvmStatic
    fun fromHexString(str: CharSequence): Bytes {
      return BytesValues.fromHexString(str, -1, false)
    }

    /**
     * Parse a hexadecimal string into a [Bytes] value.
     *
     *
     *
     * This method requires that `str` have an even length.
     *
     * @param str The hexadecimal string to parse, which may or may not start with "0x".
     * @param destinationSize The size of the returned value, which must be big enough to hold the bytes represented by
     * `str`. If it is strictly bigger those bytes from `str`, the returned value will be left padded
     * with zeros.
     * @return A value of size `destinationSize` corresponding to `str` potentially left-padded.
     * @throws IllegalArgumentException if `str` does correspond to a valid hexadecimal representation, or is of an
     * odd length.
     * @throws IllegalArgumentException if `str` does not correspond to a valid hexadecimal representation, or is of
     * an odd length, or represents more bytes than `destinationSize` or `destinationSize &lt; 0`.
     */
    @JvmStatic
    fun fromHexString(str: CharSequence, destinationSize: Int): Bytes {
      if (destinationSize < 0) {
        throw java.lang.IllegalArgumentException("Invalid negative destination size $destinationSize")
      }
      return BytesValues.fromHexString(str, destinationSize, false)
    }

    /**
     * Parse a base 64 string into a [Bytes] value.
     *
     * @param str The base 64 string to parse.
     * @return The value corresponding to `str`.
     */
    @JvmStatic
    fun fromBase64String(str: CharSequence): Bytes {
      return Bytes.wrap(Base64.getDecoder().decode(str.toString()))
    }

    /**
     * Generate random bytes.
     *
     * @param size The number of bytes to generate.
     * @param generator The generator for random bytes.
     * @return A value containing the desired number of random bytes.
     */
    @JvmStatic
    @JvmOverloads
    fun random(size: Int, generator: Random = SecureRandom()): Bytes {
      val array = ByteArray(size)
      generator.nextBytes(array)
      return Bytes.wrap(array)
    }
  }
}
