// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.bytes;

import static org.apache.tuweni.bytes.Checks.checkArgument;
import static org.apache.tuweni.bytes.Checks.checkElementIndex;

import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.util.concurrent.atomic.AtomicReference;

import io.vertx.core.buffer.Buffer;
import org.identityconnectors.common.security.GuardedByteArray;

class GuardedByteArrayBytes extends AbstractBytes {

  protected final GuardedByteArray bytes;
  protected final int offset;
  protected final int length;

  GuardedByteArrayBytes(byte[] bytes) {
    this(bytes, 0, bytes.length);
  }

  GuardedByteArrayBytes(byte[] bytes, int offset, int length) {
    checkArgument(length >= 0, "Invalid negative length");
    if (bytes.length > 0) {
      checkElementIndex(offset, bytes.length);
    }
    checkArgument(
        offset + length <= bytes.length,
        "Provided length %s is too big: the value has only %s bytes from offset %s",
        length,
        bytes.length - offset,
        offset);

    this.bytes = new GuardedByteArray(bytes);
    this.bytes.makeReadOnly();
    this.offset = offset;
    this.length = length;
  }

  @Override
  public int size() {
    return length;
  }

  @Override
  public byte get(int i) {
    // Check bounds because while the array access would throw, the error message would be confusing
    // for the caller.
    checkElementIndex(i, size());
    AtomicReference<Byte> b = new AtomicReference<>();
    bytes.access(bytes -> b.set(bytes[offset + i]));
    return b.get();
  }

  @Override
  public Bytes slice(int i, int length) {
    if (i == 0 && length == this.length) {
      return this;
    }
    if (length == 0) {
      return Bytes.EMPTY;
    }

    checkElementIndex(i, this.length);
    checkArgument(
        i + length <= this.length,
        "Provided length %s is too big: the value has size %s and has only %s bytes from %s",
        length,
        this.length,
        this.length - i,
        i);
    AtomicReference<byte[]> clearBytes = new AtomicReference<>();
    bytes.access(
        data -> {
          byte[] result = new byte[length];
          System.arraycopy(data, offset + i, result, 0, length);
          clearBytes.set(result);
        });

    return length == Bytes32.SIZE
        ? new ArrayWrappingBytes32(clearBytes.get())
        : new ArrayWrappingBytes(clearBytes.get(), 0, length);
  }

  // MUST be overridden by mutable implementations
  @Override
  public Bytes copy() {
    return new ArrayWrappingBytes(toArray());
  }

  @Override
  public MutableBytes mutableCopy() {
    return new MutableArrayWrappingBytes(toArray());
  }

  @Override
  public void update(MessageDigest digest) {
    digest.update(toArray(), offset, length);
  }

  @Override
  public void copyTo(MutableBytes destination, int destinationOffset) {
    if (!(destination instanceof MutableArrayWrappingBytes)) {
      super.copyTo(destination, destinationOffset);
      return;
    }

    int size = size();
    if (size == 0) {
      return;
    }

    checkElementIndex(destinationOffset, destination.size());
    checkArgument(
        destination.size() - destinationOffset >= size,
        "Cannot copy %s bytes, destination has only %s bytes from index %s",
        size,
        destination.size() - destinationOffset,
        destinationOffset);

    MutableArrayWrappingBytes d = (MutableArrayWrappingBytes) destination;
    System.arraycopy(toArray(), offset, d.bytes, d.offset + destinationOffset, size);
  }

  @Override
  public void appendTo(ByteBuffer byteBuffer) {
    byteBuffer.put(toArray(), offset, length);
  }

  @Override
  public void appendTo(Buffer buffer) {
    buffer.appendBytes(toArray(), offset, length);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (!(obj instanceof GuardedByteArrayBytes)) {
      return super.equals(obj);
    }
    GuardedByteArrayBytes other = (GuardedByteArrayBytes) obj;
    if (length != other.length) {
      return false;
    }
    for (int i = 0; i < length; ++i) {
      if (get(offset + i) != other.get(other.offset + i)) {
        return false;
      }
    }
    return true;
  }

  @Override
  public int hashCode() {
    int result = 1;
    int size = size();
    for (int i = 0; i < size; i++) {
      result = 31 * result + get(offset + i);
    }
    return result;
  }

  @Override
  public byte[] toArray() {
    AtomicReference<byte[]> clearBytes = new AtomicReference<>();
    bytes.access(
        data -> {
          byte[] result = new byte[length];
          System.arraycopy(data, offset, result, 0, length);
          clearBytes.set(result);
        });
    return clearBytes.get();
  }

  @Override
  public byte[] toArrayUnsafe() {
    return toArray();
  }
}
