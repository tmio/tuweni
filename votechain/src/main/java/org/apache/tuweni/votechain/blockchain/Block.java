package org.apache.tuweni.votechain.blockchain;

import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;
import org.apache.tuweni.crypto.Hash;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class Block {

    private final Bytes32 parentHash;
    private final List<Transaction> transactions;

    public Block(List<Transaction> tx, Bytes32 parentHash) {
        this.transactions = tx;
        this.parentHash = parentHash;
    }

    public List<Transaction> getTransactions() {
        return transactions;
    }

    public Bytes32 getHash() {
        List<Bytes> hashes = new ArrayList<>();
        if (parentHash != null) {
            hashes.add(parentHash);
        } else {
            hashes.add(Bytes.wrap("TODO".getBytes(StandardCharsets.UTF_8)));
        }
        for (Transaction tx : transactions) {
            hashes.add(tx.getHash());
        }

        return Hash.keccak256(Bytes.concatenate(hashes.toArray(new Bytes[] {})));
    }

    public Bytes32 getParentHash() {
        return parentHash;
    }
}
