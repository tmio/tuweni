package org.apache.tuweni.votechain.blockchain;

import org.apache.tuweni.bytes.Bytes32;
import org.apache.tuweni.kv.KeyValueStore;
import org.apache.tuweni.units.bigints.UInt256;
import org.apache.tuweni.votechain.blockchain.ops.Operation;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Client API to interface with the voter chain.
 *
 * The API allows to load up the chain configuration, submit transactions and retrieve data.
 */
public final class Blockchain {
    private final Block genesisBlock;
    private final BlockchainConfiguration config;
    private final KeyValueStore<Bytes32, Block> blockStore;
    private final Map<Long, Block> blockByNumber = new HashMap<>();
    private final Map<Bytes32, Account> accounts = new HashMap<>();

    public Blockchain(BlockchainConfiguration config, KeyValueStore<Bytes32, Block> blockStore) {
        this.config = config;
        this.blockStore = blockStore;
        this.genesisBlock = createGenesisBlock(config);
        for (Account initialAccount: config.getAccounts()) {
            accounts.put(initialAccount.getId(), new Account(initialAccount.getId(), initialAccount.getBalance()));
        }
    }

    private Block createGenesisBlock(BlockchainConfiguration config) {
        return new Block(Collections.emptyList());
    }

    public UInt256 getAccountBalance(Bytes32 accountId) {
        return accounts.get(accountId).getBalance();
    }

    public Block getBlock(long l) {
        return blockByNumber.get(l);
    }

    public Block getHeadBlock() {
        return genesisBlock;
    }

    public void addBlock(Block newBlock) {
        if (blockByNumber.get(1L) != null) {
            return;
        }
        try {
            for (Transaction tx : newBlock.getTransactions()) {
                for (Operation op : tx.getOperations()) {
                    config.getOperationRegistry().get(op.getName()).getExecution().execute(this, op.parameters());
                }
            }
        } catch (Throwable t) {
            return;
        }


        blockByNumber.put(1L, newBlock);
    }

    public void addToAccount(Bytes32 id, UInt256 value) {
accounts.get(id).add(value);
    }
}
