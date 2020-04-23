package org.apache.tuweni.votechain.miner;

import org.apache.tuweni.votechain.blockchain.Block;
import org.apache.tuweni.votechain.blockchain.Transaction;

import java.util.Collection;
import java.util.List;

public interface BlockMinter {

    Block mine(Block parentBlock, List<Transaction> transactions);
}
