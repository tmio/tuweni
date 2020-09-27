package org.apache.tuweni.bytes

/**
 * An abstract [Bytes] value that provides implementations of [.equals], [.hashCode] and
 * [.toString].
 */
abstract class AbstractBytes : Bytes {

  /**
   * Compare this value and the provided one for equality.
   *
   *
   *
   * Two [Bytes] values are equal is they have contain the exact same bytes.
   *
   * @param obj The object to test for equality with.
   * @return `true` if this value and `obj` are equal.
   */
  override fun equals(obj: Any?): Boolean {
    if (obj === this) {
      return true
    }
    if (obj !is Bytes) {
      return false
    }

    val other = obj as Bytes?
    if (this.size() != other!!.size()) {
      return false
    }

    for (i in 0 until size()) {
      if (this[i] != other[i]) {
        return false
      }
    }
    return true
  }

  override fun hashCode(): Int {
    var result = 1
    for (i in 0 until size()) {
      result = 31 * result + get(i)
    }
    return result
  }

  override fun toString(): String {
    return toHexString()
  }

  companion object {

    internal val HEX_CODE = "0123456789abcdef".toCharArray()
  }
}
