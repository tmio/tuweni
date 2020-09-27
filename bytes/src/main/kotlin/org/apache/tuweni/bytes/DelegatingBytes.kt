package org.apache.tuweni.bytes

import io.vertx.core.buffer.Buffer
import java.lang.IllegalArgumentException
import java.math.BigInteger
import java.nio.ByteBuffer
import java.security.MessageDigest

/**
 * A class that holds and delegates all operations to its inner bytes field.
 *
 *
 *
 * This class may be used to create more types that represent bytes, but need a different name for business logic.
 */
open class DelegatingBytes protected constructor(private val delegate: Bytes) : AbstractBytes(), Bytes {

  override fun size(): Int {
    return delegate.size()
  }

  override fun get(i: Int): Byte {
    return delegate[i]
  }

  override fun slice(index: Int, length: Int): Bytes {
    return delegate.slice(index, length)
  }

  override fun copy(): Bytes {
    return Bytes.wrap(toArray())
  }

  override fun mutableCopy(): MutableBytes {
    return MutableBytes.wrap(toArray())
  }
}

/**
 * A class that holds and delegates all operations to its inner bytes field.
 *
 *
 *
 * This class may be used to create more types that represent 32 bytes, but need a different name for business logic.
 */
open class DelegatingBytes32 protected constructor(private val delegate: Bytes) : AbstractBytes(), Bytes32 {

  override fun size(): Int {
    return Bytes32.SIZE
  }

  override fun get(i: Int): Byte {
    return delegate[i]
  }

  override fun slice(index: Int, length: Int): Bytes {
    return delegate.slice(index, length)
  }

  override fun copy(): Bytes32 {
    return Bytes32.wrap(toArray())
  }

  override fun mutableCopy(): MutableBytes32 {
    return MutableBytes32.wrap(toArray())
  }
}

/**
 * A class that holds and delegates all operations to its inner bytes field.
 *
 *
 *
 * This class may be used to create more types that represent 48 bytes, but need a different name for business logic.
 */
class DelegatingBytes48 protected constructor(private val delegate: Bytes) : AbstractBytes(), Bytes48 {

  override fun size(): Int {
    return Bytes48.SIZE
  }

  override fun get(i: Int): Byte {
    return delegate[i]
  }

  override fun slice(index: Int, length: Int): Bytes {
    return delegate.slice(index, length)
  }

  override fun copy(): Bytes48 {
    return Bytes48.wrap(toArray())
  }

  override fun mutableCopy(): MutableBytes48 {
    return MutableBytes48.wrap(toArray())
  }

}

internal class DelegatingMutableBytes32 private constructor(private val delegate: MutableBytes) : MutableBytes32 {

  override val isZero: Boolean
    get() = delegate.isZero

  override fun set(i: Int, b: Byte) {
    delegate[i] = b
  }

  override fun setInt(i: Int, value: Int) {
    delegate.setInt(i, value)
  }

  override fun setLong(i: Int, value: Long) {
    delegate.setLong(i, value)
  }

  override fun increment(): MutableBytes {
    return delegate.increment()
  }

  override fun decrement(): MutableBytes {
    return delegate.decrement()
  }

  override fun mutableSlice(i: Int, length: Int): MutableBytes {
    return delegate.mutableSlice(i, length)
  }

  override fun fill(b: Byte) {
    delegate.fill(b)
  }

  override fun clear() {
    delegate.clear()
  }

  override fun size(): Int {
    return delegate.size()
  }

  override fun get(i: Int): Byte {
    return delegate[i]
  }

  override fun getInt(i: Int): Int {
    return delegate.getInt(i)
  }

  override fun toInt(): Int {
    return delegate.toInt()
  }

  override fun getLong(i: Int): Long {
    return delegate.getLong(i)
  }

  override fun toLong(): Long {
    return delegate.toLong()
  }

  override fun toBigInteger(): BigInteger {
    return delegate.toBigInteger()
  }

  override fun toUnsignedBigInteger(): BigInteger {
    return delegate.toUnsignedBigInteger()
  }

  override fun numberOfLeadingZeros(): Int {
    return delegate.numberOfLeadingZeros()
  }

  override fun numberOfLeadingZeroBytes(): Int {
    return delegate.numberOfLeadingZeroBytes()
  }

  override fun hasLeadingZeroByte(): Boolean {
    return delegate.hasLeadingZeroByte()
  }

  override fun hasLeadingZero(): Boolean {
    return delegate.hasLeadingZero()
  }

  override fun bitLength(): Int {
    return delegate.bitLength()
  }

  override fun and(other: Bytes): Bytes {
    return delegate.and(other)
  }

  override fun <T : MutableBytes> and(other: Bytes, result: T): T {
    return delegate.and(other, result)
  }

  override fun or(other: Bytes): Bytes {
    return delegate.or(other)
  }

  override fun <T : MutableBytes> or(other: Bytes, result: T): T {
    return delegate.or(other, result)
  }

  override fun xor(other: Bytes): Bytes {
    return delegate.xor(other)
  }

  override fun <T : MutableBytes> xor(other: Bytes, result: T): T {
    return delegate.xor(other, result)
  }

  override fun <T : MutableBytes> not(result: T): T {
    return delegate.not(result)
  }

  override fun <T : MutableBytes> shiftRight(distance: Int, result: T): T {
    return delegate.shiftRight(distance, result)
  }

  override fun <T : MutableBytes> shiftLeft(distance: Int, result: T): T {
    return delegate.shiftLeft(distance, result)
  }

  override fun slice(index: Int): Bytes {
    return delegate.slice(index)
  }

  override fun slice(index: Int, length: Int): Bytes {
    return delegate.slice(index, length)
  }

  override fun copy(): Bytes32 {
    return Bytes32.wrap(delegate.toArray())
  }

  override fun mutableCopy(): MutableBytes32 {
    return MutableBytes32.wrap(delegate.toArray())
  }

  override fun copyTo(destination: MutableBytes) {
    delegate.copyTo(destination)
  }

  override fun copyTo(destination: MutableBytes, destinationOffset: Int) {
    delegate.copyTo(destination, destinationOffset)
  }

  override fun appendTo(byteBuffer: ByteBuffer) {
    delegate.appendTo(byteBuffer)
  }

  override fun appendTo(buffer: Buffer) {
    delegate.appendTo(buffer)
  }

  override fun commonPrefixLength(other: Bytes): Int {
    return delegate.commonPrefixLength(other)
  }

  override fun commonPrefix(other: Bytes): Bytes {
    return delegate.commonPrefix(other)
  }

  override fun update(digest: MessageDigest) {
    delegate.update(digest)
  }

  override fun toArray(): ByteArray {
    return delegate.toArray()
  }

  override fun toArrayUnsafe(): ByteArray {
    return delegate.toArrayUnsafe()
  }

  override fun toString(): String {
    return delegate.toString()
  }

  override fun toHexString(): String {
    return delegate.toHexString()
  }

  override fun toShortHexString(): String {
    return delegate.toShortHexString()
  }

  override fun equals(obj: Any?): Boolean {
    return delegate == obj
  }

  override fun hashCode(): Int {
    return delegate.hashCode()
  }

  companion object {

    fun delegateTo(value: MutableBytes): MutableBytes32 {
      if (value.size() != Bytes32.SIZE) {
        throw IllegalArgumentException("Expected ${Bytes32.SIZE} bytes but got ${value.size()}")
      }
      return DelegatingMutableBytes32(value)
    }
  }
}

internal class DelegatingMutableBytes48 private constructor(private val delegate: MutableBytes) : MutableBytes48 {

  override val isZero: Boolean
    get() = delegate.isZero

  override fun set(i: Int, b: Byte) {
    delegate[i] = b
  }

  override fun setInt(i: Int, value: Int) {
    delegate.setInt(i, value)
  }

  override fun setLong(i: Int, value: Long) {
    delegate.setLong(i, value)
  }

  override fun increment(): MutableBytes {
    return delegate.increment()
  }

  override fun decrement(): MutableBytes {
    return delegate.decrement()
  }

  override fun mutableSlice(i: Int, length: Int): MutableBytes {
    return delegate.mutableSlice(i, length)
  }

  override fun fill(b: Byte) {
    delegate.fill(b)
  }

  override fun clear() {
    delegate.clear()
  }

  override fun size(): Int {
    return delegate.size()
  }

  override fun get(i: Int): Byte {
    return delegate[i]
  }

  override fun getInt(i: Int): Int {
    return delegate.getInt(i)
  }

  override fun toInt(): Int {
    return delegate.toInt()
  }

  override fun getLong(i: Int): Long {
    return delegate.getLong(i)
  }

  override fun toLong(): Long {
    return delegate.toLong()
  }

  override fun toBigInteger(): BigInteger {
    return delegate.toBigInteger()
  }

  override fun toUnsignedBigInteger(): BigInteger {
    return delegate.toUnsignedBigInteger()
  }

  override fun numberOfLeadingZeros(): Int {
    return delegate.numberOfLeadingZeros()
  }

  override fun numberOfLeadingZeroBytes(): Int {
    return delegate.numberOfLeadingZeroBytes()
  }

  override fun hasLeadingZeroByte(): Boolean {
    return delegate.hasLeadingZeroByte()
  }

  override fun hasLeadingZero(): Boolean {
    return delegate.hasLeadingZero()
  }

  override fun bitLength(): Int {
    return delegate.bitLength()
  }

  override fun and(other: Bytes): Bytes {
    return delegate.and(other)
  }

  override fun <T : MutableBytes> and(other: Bytes, result: T): T {
    return delegate.and(other, result)
  }

  override fun or(other: Bytes): Bytes {
    return delegate.or(other)
  }

  override fun <T : MutableBytes> or(other: Bytes, result: T): T {
    return delegate.or(other, result)
  }

  override fun xor(other: Bytes): Bytes {
    return delegate.xor(other)
  }

  override fun <T : MutableBytes> xor(other: Bytes, result: T): T {
    return delegate.xor(other, result)
  }

  override fun <T : MutableBytes> not(result: T): T {
    return delegate.not(result)
  }

  override fun <T : MutableBytes> shiftRight(distance: Int, result: T): T {
    return delegate.shiftRight(distance, result)
  }

  override fun <T : MutableBytes> shiftLeft(distance: Int, result: T): T {
    return delegate.shiftLeft(distance, result)
  }

  override fun slice(index: Int): Bytes {
    return delegate.slice(index)
  }

  override fun slice(index: Int, length: Int): Bytes {
    return delegate.slice(index, length)
  }

  override fun copy(): Bytes48 {
    return Bytes48.wrap(delegate.toArray())
  }

  override fun mutableCopy(): MutableBytes48 {
    return MutableBytes48.wrap(delegate.toArray())
  }

  override fun copyTo(destination: MutableBytes) {
    delegate.copyTo(destination)
  }

  override fun copyTo(destination: MutableBytes, destinationOffset: Int) {
    delegate.copyTo(destination, destinationOffset)
  }

  override fun appendTo(byteBuffer: ByteBuffer) {
    delegate.appendTo(byteBuffer)
  }

  override fun appendTo(buffer: Buffer) {
    delegate.appendTo(buffer)
  }

  override fun commonPrefixLength(other: Bytes): Int {
    return delegate.commonPrefixLength(other)
  }

  override fun commonPrefix(other: Bytes): Bytes {
    return delegate.commonPrefix(other)
  }

  override fun update(digest: MessageDigest) {
    delegate.update(digest)
  }

  override fun toArray(): ByteArray {
    return delegate.toArray()
  }

  override fun toArrayUnsafe(): ByteArray {
    return delegate.toArrayUnsafe()
  }

  override fun toString(): String {
    return delegate.toString()
  }

  override fun toHexString(): String {
    return delegate.toHexString()
  }

  override fun toShortHexString(): String {
    return delegate.toShortHexString()
  }

  override fun equals(obj: Any?): Boolean {
    return delegate == obj
  }

  override fun hashCode(): Int {
    return delegate.hashCode()
  }

  companion object {

    fun delegateTo(value: MutableBytes): MutableBytes48 {
      if (value.size() != Bytes48.SIZE) {
        throw IllegalArgumentException("Expected ${Bytes48.SIZE} bytes but got ${value.size()}")
      }
      return DelegatingMutableBytes48(value)
    }
  }
}
