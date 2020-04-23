package org.apache.tuweni.votechain.blockchain;

import org.apache.tuweni.bytes.Bytes32;
import org.apache.tuweni.units.bigints.UInt256;

public final class Account {

    private final Bytes32 id;
    private UInt256 balance;

    public Account(Bytes32 id, UInt256 amount) {
        this.id = id;
        this.balance = amount;
    }

    public Bytes32 getId() {
        return id;
    }

    public void add(UInt256 value) {
        this.balance = balance.add(value);
    }

    public UInt256 getBalance() {
        return balance;
    }
}
