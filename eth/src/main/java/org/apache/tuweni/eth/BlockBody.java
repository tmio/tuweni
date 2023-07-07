// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.eth;

import static java.util.Objects.requireNonNull;

import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.rlp.RLP;
import org.apache.tuweni.rlp.RLPException;
import org.apache.tuweni.rlp.RLPReader;
import org.apache.tuweni.rlp.RLPWriter;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** An Ethereum block body. */
public final class BlockBody {

  /**
   * Deserialize a block body from RLP encoded bytes.
   *
   * @param encoded The RLP encoded block.
   * @return The deserialized block body.
   * @throws RLPException If there is an error decoding the block body.
   */
  public static BlockBody fromBytes(Bytes encoded) {
    requireNonNull(encoded);
    return RLP.decodeList(encoded, BlockBody::readFrom);
  }

  public static BlockBody readFrom(RLPReader reader) {
    List<Transaction> txs = new ArrayList<>();
    reader.readList(
        (listReader, l) -> {
          while (!listReader.isComplete()) {
            txs.add(listReader.readList(Transaction::readFrom));
          }
        });
    List<BlockHeader> ommers = new ArrayList<>();
    reader.readList(
        (listReader, l) -> {
          while (!listReader.isComplete()) {
            ommers.add(listReader.readList(BlockHeader::readFrom));
          }
        });

    return new BlockBody(txs, ommers);
  }

  private final List<Transaction> transactions;
  private final List<BlockHeader> ommers;
  private final List<Hash> ommerHashes;

  /**
   * Creates a new block body.
   *
   * @param transactions the list of transactions in this block.
   * @param ommers the list of ommers for this block.
   */
  public BlockBody(List<Transaction> transactions, List<BlockHeader> ommers) {
    this(transactions, null, ommers);
  }

  /**
   * Creates a new incomplete block body where ommer block headers are missing, only their hashes
   * are provided.
   *
   * @param transactions the list of transactions in this block.
   * @param ommerHashes the list of ommer hashes for this block.
   * @param ommers the list of ommers for this block.
   */
  BlockBody(List<Transaction> transactions, List<Hash> ommerHashes, List<BlockHeader> ommers) {
    requireNonNull(transactions);
    this.transactions = transactions;
    this.ommers = ommers;
    this.ommerHashes = ommerHashes;
  }

  /**
   * Provides the block transactions
   *
   * @return the transactions of the block.
   */
  public List<Transaction> getTransactions() {
    return transactions;
  }

  /**
   * Provides the block ommers
   *
   * @return the list of ommers for this block.
   */
  public List<BlockHeader> getOmmers() {
    return ommers;
  }

  /**
   * Provides the block ommer hashes
   *
   * @return the list of ommer hashes for this block.
   */
  public List<Hash> getOmmerHashes() {
    return ommerHashes;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    BlockBody blockBody = (BlockBody) o;
    return Objects.equals(transactions, blockBody.transactions)
        && Objects.equals(ommers, blockBody.ommers)
        && Objects.equals(ommerHashes, blockBody.ommerHashes);
  }

  @Override
  public int hashCode() {
    return Objects.hash(transactions, ommers, ommerHashes);
  }

  /**
   * Provides the block body bytes
   *
   * @return The RLP serialized form of this block body.
   */
  public Bytes toBytes() {
    return RLP.encodeList(this::writeTo);
  }

  @Override
  public String toString() {
    return "BlockBody{" + "transactions=" + transactions + ", ommers=" + ommers + '}';
  }

  public void writeTo(RLPWriter writer) {
    if (ommers == null) {
      throw new IllegalArgumentException("Cannot write incomplete block body");
    }
    writer.writeList(
        listWriter -> {
          for (Transaction tx : transactions) {
            listWriter.writeList(tx::writeTo);
          }
        });
    writer.writeList(
        listWriter -> {
          for (BlockHeader ommer : ommers) {
            listWriter.writeList(ommer::writeTo);
          }
        });
  }
}
