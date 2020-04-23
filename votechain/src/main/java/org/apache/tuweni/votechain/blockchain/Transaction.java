package org.apache.tuweni.votechain.blockchain;

import org.apache.tuweni.votechain.blockchain.ops.Operation;

import java.util.List;

public class Transaction {
    private final List<Operation> operations;

    public Transaction(List<Operation> operations) {
        this.operations = operations;
    }

    public List<Operation> getOperations() {
        return operations;
    }
}
