package org.apache.tuweni.bytes

import org.apache.tuweni.bytes.Bytes.Companion.EMPTY
import java.lang.IndexOutOfBoundsException

internal class ConcatenatedBytes private constructor(private val values: Array<out Bytes>, private val size: Int) :
  AbstractBytes() {

  override fun size(): Int {
    return size
  }

  override fun get(i: Int): Byte {
    var i = i
    if (i > size) {
      throw IndexOutOfBoundsException()
    }
    for (value in values) {
      val vSize = value.size()
      if (i < vSize) {
        return value[i]
      }
      i -= vSize
    }
    throw IllegalStateException("element sizes do not match total size")
  }

  override fun slice(i: Int, length: Int): Bytes {
    var i = i
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
      throw java.lang.IllegalArgumentException("Provided length $length is too big: the value has size $size and has only ${size - i} bytes from $i")
    }

    var j = 0
    var vSize: Int
    while (true) {
      vSize = values[j].size()
      if (i < vSize) {
        break
      }
      i -= vSize
      ++j
    }

    if (i + length < vSize) {
      return values[j].slice(i, length)
    }

    var remaining = length - (vSize - i)
    val firstValue = this.values[j].slice(i)
    val firstOffset = j

    while (remaining > 0) {
      if (++j >= this.values.size) {
        throw IllegalStateException("element sizes do not match total size")
      }
      vSize = this.values[j].size()
      if (length < vSize) {
        break
      }
      remaining -= vSize
    }

    val combined = ArrayList<Bytes>(j - firstOffset + 1)
    combined[0] = firstValue
    if (remaining > 0) {
      if (combined.size > 2) {
        System.arraycopy(this.values, firstOffset + 1, combined, 1, combined.size - 2)
      }
      combined[combined.size - 1] = this.values[j].slice(0, remaining)
    } else if (combined.size > 1) {
      System.arraycopy(this.values, firstOffset + 1, combined, 1, combined.size - 1)
    }
    return ConcatenatedBytes(combined.toTypedArray(), length)
  }

  override fun copy(): Bytes {
    return mutableCopy()
  }

  override fun mutableCopy(): MutableBytes {
    if (size == 0) {
      return MutableBytes.EMPTY
    }
    val result = MutableBytes.create(size)
    copyToUnchecked(result, 0)
    return result
  }

  override fun copyTo(destination: MutableBytes, destinationOffset: Int) {
    if (size == 0) {
      return
    }

    if (destinationOffset > destination.size()) {
      throw IndexOutOfBoundsException()
    }
    if (destination.size() - destinationOffset < size) {
      throw java.lang.IllegalArgumentException("Provided length $size is too big: the value has size $size and has only ${destination.size() - destinationOffset} bytes from $destinationOffset")
    }

    copyToUnchecked(destination, destinationOffset)
  }

  override fun toArray(): ByteArray {
    if (size == 0) {
      return ByteArray(0)
    }

    val result = MutableBytes.create(size)
    copyToUnchecked(result, 0)
    return result.toArrayUnsafe()
  }

  private fun copyToUnchecked(destination: MutableBytes, destinationOffset: Int) {
    var destinationOffset = destinationOffset
    var offset = 0
    for (value in values) {
      val vSize = value.size()
      if (offset + vSize > size) {
        throw IllegalStateException("element sizes do not match total size")
      }
      value.copyTo(destination, destinationOffset)
      offset += vSize
      destinationOffset += vSize
    }
  }

  companion object {

    fun wrap(vararg values: Bytes): Bytes {
      if (values.isEmpty()) {
        return EMPTY
      }
      if (values.size == 1) {
        return values[0]
      }

      var count = 0
      var totalSize = 0

      for (value in values) {
        val size = value.size()
        try {
          totalSize = Math.addExact(totalSize, size)
        } catch (e: ArithmeticException) {
          throw IllegalArgumentException("Combined length of values is too long (> Integer.MAX_VALUE)")
        }

        if (value is ConcatenatedBytes) {
          count += value.values.size
        } else if (size != 0) {
          count += 1
        }
      }

      if (count == 0) {
        return Bytes.EMPTY
      }
      if (count == values.size) {
        return ConcatenatedBytes(values, totalSize)
      }

      val concatenated = mutableListOf<Bytes>()
      var i = 0
      for (value in values) {
        if (value is ConcatenatedBytes) {
          val subvalues = value.values
          System.arraycopy(subvalues, 0, concatenated, i, subvalues.size)
          i += subvalues.size
        } else if (value.size() != 0) {
          concatenated.add(value)
        }
      }
      return ConcatenatedBytes(concatenated.toTypedArray(), totalSize)
    }
  }
}
