package org.apache.tuweni.votechain.blockchain;

import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TransactionPoolTest {
     @Test
    void testTransactionPoolAddAndGet() {
         TransactionPool txPool = new TransactionPool();
         txPool.addTransaction(new Transaction(Collections.emptyList()));
         assertEquals(1, txPool.getPendingTransactions().size());
     }
}
