package org.apache.tuweni.votechain.blockchain;

import java.util.List;

public class Block {

    private List<Transaction> transactions;

    public Block(List<Transaction> tx) {
        this.transactions = tx;
    }

    public List<Transaction> getTransactions() {
        return transactions;
    }
}
