package org.apache.tuweni.votechain.blockchain;

import org.apache.tuweni.votechain.blockchain.ops.OperationRegistry;

import java.util.List;

public final class BlockchainConfiguration {

    private final List<Account> accounts;
    private final OperationRegistry operationRegistry;

    public BlockchainConfiguration(List<Account> initialAccounts, OperationRegistry registry) {
        this.accounts = initialAccounts;
        this.operationRegistry = registry;
    }

    public List<Account> getAccounts() {
        return accounts;
    }

    public OperationRegistry getOperationRegistry() {
        return operationRegistry;
    }
}
