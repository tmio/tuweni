package org.apache.tuweni.votechain;

import org.apache.tuweni.crypto.SECP256K1;
import org.apache.tuweni.votechain.blockchain.Block;
import org.apache.tuweni.votechain.blockchain.Blockchain;
import org.apache.tuweni.votechain.blockchain.Message;
import org.apache.tuweni.votechain.blockchain.Transaction;
import org.apache.tuweni.votechain.blockchain.TransactionPool;
import org.apache.tuweni.votechain.blockchain.ops.Operation;
import org.apache.tuweni.votechain.miner.BlockMinter;
import org.apache.tuweni.votechain.miner.MiningScheduler;
import org.apache.tuweni.votechain.network.InProcessMessage;
import org.apache.tuweni.votechain.network.Network;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A BlockchainNode represents a node in a peer-to-peer network running a blockchain.
 */
public class BlockchainNode {

    private final SECP256K1.KeyPair keyPair;
    private final Blockchain blockchain;
    private final Network network;
    private final MiningScheduler miningScheduler;
    private final BlockMinter minter;
    private final TransactionPool transactionPool;

    public BlockchainNode(SECP256K1.KeyPair keyPair, Blockchain blockchain, Network network, MiningScheduler miningScheduler, BlockMinter minter) {
        this.keyPair = keyPair;
        this.blockchain = blockchain;
        this.network = network;
        this.miningScheduler = miningScheduler;
        miningScheduler.register(this::mine);
        this.minter = minter;
        this.transactionPool = new TransactionPool();
        network.registerListener(this::receiveMessage);
    }

    private void receiveMessage(Message message) {
        if (message instanceof InProcessMessage) {
            blockchain.addBlock(((InProcessMessage) message).getBlock());
        } else {
            throw new IllegalArgumentException();
        }
    }

    private void mine() {
        List<Transaction> txs = new ArrayList<>(transactionPool.getPendingTransactions());
        transactionPool.getPendingTransactions().clear();
        Block newBlock = minter.mine(blockchain.getHeadBlock(), txs);
        blockchain.addBlock(newBlock);
        network.send(new InProcessMessage(newBlock));
    }

    public void executeOperation(Operation op) {
        Transaction tx = new Transaction(Arrays.asList(op));
        executeTransaction(tx);
    }

    public void executeTransaction(Transaction tx) {
        transactionPool.addTransaction(tx);
    }

    public Blockchain getBlockchain() {
        return blockchain;
    }

    public TransactionPool getTransactionPool() {
        return transactionPool;
    }
}
