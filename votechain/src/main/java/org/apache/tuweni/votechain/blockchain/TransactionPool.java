package org.apache.tuweni.votechain.blockchain;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TransactionPool {
    private final List<Transaction> txs = new ArrayList<>();
    public void addTransaction(Transaction tx) {
        txs.add(tx);
    }

    public List<Transaction> getPendingTransactions() {
        return txs;
    }
}
