package org.apache.tuweni.votechain.blockchain;

import org.apache.tuweni.bytes.Bytes32;
import org.apache.tuweni.kv.MapKeyValueStore;
import org.apache.tuweni.units.bigints.UInt256;
import org.apache.tuweni.votechain.blockchain.ops.OperationRegistry;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;

class BlockchainTest {

    @Test
    void testInitBlockchainWithoutAccounts() {
        List<Account> accounts = new ArrayList<>();
        BlockchainConfiguration config = new BlockchainConfiguration(accounts, new OperationRegistry(Collections.emptyList()));
        assertThrows(IllegalArgumentException.class, () -> new Blockchain(config, MapKeyValueStore.open()));
    }

    @Test
    void testInitBlockchainWithoutAmounts() {
        List<Account> accounts = Arrays.asList(new Account(Bytes32.random(), UInt256.ZERO));
        BlockchainConfiguration config = new BlockchainConfiguration(accounts, new OperationRegistry(Collections.emptyList()));
        assertThrows(IllegalArgumentException.class, () -> new Blockchain(config, MapKeyValueStore.open()));
    }

    @Test
    void testInitBlockchain() {
        List<Account> accounts = Arrays.asList(new Account(Bytes32.random(), UInt256.ONE));
        BlockchainConfiguration config = new BlockchainConfiguration(accounts, new OperationRegistry(Collections.emptyList()));
        Blockchain blockchain = new Blockchain(config, MapKeyValueStore.open());
    }

    @Test
    void testInitBlockchainWithMiner() {
        List<Account> accounts = Arrays.asList(new Account(Bytes32.random(), UInt256.ONE));
        BlockchainConfiguration config = new BlockchainConfiguration(accounts, new OperationRegistry(Collections.emptyList()));
        Blockchain blockchain = new Blockchain(config, MapKeyValueStore.open());
    }

    @Test
    void testRunOperation() {
        Account originator = new Account(Bytes32.random(), UInt256.ONE);
        List<Account> accounts = Arrays.asList(originator);
        BlockchainConfiguration config = new BlockchainConfiguration(accounts, new OperationRegistry(Collections.emptyList()));
        Blockchain blockchain = new Blockchain(config, MapKeyValueStore.open());
    }
}
