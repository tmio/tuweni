package org.apache.tuweni.votechain;

import org.apache.tuweni.bytes.Bytes32;
import org.apache.tuweni.crypto.SECP256K1;
import org.apache.tuweni.junit.BouncyCastleExtension;
import org.apache.tuweni.kv.MapKeyValueStore;
import org.apache.tuweni.units.bigints.UInt256;
import org.apache.tuweni.votechain.blockchain.Account;
import org.apache.tuweni.votechain.blockchain.Block;
import org.apache.tuweni.votechain.blockchain.Blockchain;
import org.apache.tuweni.votechain.blockchain.BlockchainConfiguration;
import org.apache.tuweni.votechain.blockchain.ops.Operation;
import org.apache.tuweni.votechain.blockchain.ops.OperationDefinition;
import org.apache.tuweni.votechain.blockchain.ops.OperationRegistry;
import org.apache.tuweni.votechain.miner.OnDemandBlockScheduler;
import org.apache.tuweni.votechain.network.InProcessNetwork;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

@ExtendWith(BouncyCastleExtension.class)
class BlockchainNodeTest {

    @Test
    void testThreeParticipants() {
        Account originator = new Account(Bytes32.random(), UInt256.ONE);

        List<Account> accounts = Arrays.asList(originator);

        BlockchainConfiguration config = new BlockchainConfiguration(accounts, new OperationRegistry(Arrays.asList(new OperationDefinition("send", (blockchain, args) -> {
        }))));
        InProcessNetwork network = new InProcessNetwork();
        BlockchainNode node1;
        BlockchainNode node2;
        BlockchainNode node3;
        OnDemandBlockScheduler scheduler = new OnDemandBlockScheduler();
        {
            Blockchain blockchain = new Blockchain(config, MapKeyValueStore.open());
            node1 = new BlockchainNode(SECP256K1.KeyPair.random(), blockchain, network, new OnDemandBlockScheduler(), (parentBlock, txs) -> null);
        }
        {
            Blockchain blockchain = new Blockchain(config, MapKeyValueStore.open());
            node2 = new BlockchainNode(SECP256K1.KeyPair.random(), blockchain, network, new OnDemandBlockScheduler(), (parentBlock, txs) -> null);
        }
        {
            Blockchain blockchain = new Blockchain(config, MapKeyValueStore.open());
            node3 = new BlockchainNode(SECP256K1.KeyPair.random(), blockchain, network, scheduler, (parentBlock, txs) -> new Block(txs));
        }

        Operation op = new Operation("send");

        node1.executeOperation(op);
        scheduler.trigger();
        Block minedBlock = node3.getBlockchain().getBlock(1L);
        assertNotNull(minedBlock);
        Block receivedBlock = node2.getBlockchain().getBlock(1L);
        assertEquals(minedBlock, receivedBlock);
        Block receivedBlock2 = node1.getBlockchain().getBlock(1L);
        assertEquals(minedBlock, receivedBlock2);
    }

    @Test
    void testThreeParticipantsBalanceChange() {
        final Account originator = new Account(Bytes32.random(), UInt256.ONE);

        List<Account> accounts = Arrays.asList(originator);

        BlockchainConfiguration config = new BlockchainConfiguration(accounts, new OperationRegistry(Arrays.asList(new OperationDefinition("send", (blockchain, args) -> {blockchain.addToAccount(originator.getId(), UInt256.ONE);}))));
        InProcessNetwork network = new InProcessNetwork();
        BlockchainNode node1;
        BlockchainNode node2;
        BlockchainNode node3;
        OnDemandBlockScheduler scheduler = new OnDemandBlockScheduler();
        {
            Blockchain blockchain = new Blockchain(config, MapKeyValueStore.open());
            node1 = new BlockchainNode(SECP256K1.KeyPair.random(), blockchain, network, new OnDemandBlockScheduler(), (parentBlock, txs) -> null);
        }
        {
            Blockchain blockchain = new Blockchain(config, MapKeyValueStore.open());
            node2 = new BlockchainNode(SECP256K1.KeyPair.random(), blockchain, network, new OnDemandBlockScheduler(), (parentBlock, txs) -> null);
        }
        {
            Blockchain blockchain = new Blockchain(config, MapKeyValueStore.open());
            node3 = new BlockchainNode(SECP256K1.KeyPair.random(), blockchain, network, scheduler, (parentBlock, txs) -> new Block(txs));
        }

        Operation op = new Operation("send");

        node3.executeOperation(op);
        scheduler.trigger();
        Block minedBlock = node3.getBlockchain().getBlock(1L);
        assertNotNull(minedBlock);
        Block receivedBlock = node2.getBlockchain().getBlock(1L);
        assertEquals(minedBlock, receivedBlock);
        Block receivedBlock2 = node1.getBlockchain().getBlock(1L);
        assertEquals(minedBlock, receivedBlock2);
        assertEquals(UInt256.valueOf(2L), node1.getBlockchain().getAccountBalance(originator.getId()));
        assertEquals(UInt256.valueOf(2L), node2.getBlockchain().getAccountBalance(originator.getId()));
        assertEquals(UInt256.valueOf(2L), node3.getBlockchain().getAccountBalance(originator.getId()));
    }

    @Test
    void testThreeParticipantsInvalidOperation() {
        final Account originator = new Account(Bytes32.random(), UInt256.ONE);

        List<Account> accounts = Arrays.asList(originator);

        OperationRegistry registry = new OperationRegistry(Arrays.asList(new OperationDefinition("send", (blockchain, args) -> blockchain.addToAccount(originator.getId(), UInt256.ONE))));
        BlockchainConfiguration config = new BlockchainConfiguration(accounts, registry);

        OperationRegistry registry2 = new OperationRegistry(Arrays.asList(new OperationDefinition("send", (blockchain, args) -> {throw new UnsupportedOperationException();})));
        BlockchainConfiguration config2 = new BlockchainConfiguration(accounts, registry2);

        InProcessNetwork network = new InProcessNetwork();
        BlockchainNode node1;
        BlockchainNode node2;
        BlockchainNode node3;
        OnDemandBlockScheduler scheduler = new OnDemandBlockScheduler();
        {
            Blockchain blockchain = new Blockchain(config2, MapKeyValueStore.open());
            node1 = new BlockchainNode(SECP256K1.KeyPair.random(), blockchain, network, new OnDemandBlockScheduler(), (parentBlock, txs) -> null);
        }
        {
            Blockchain blockchain = new Blockchain(config2, MapKeyValueStore.open());
            node2 = new BlockchainNode(SECP256K1.KeyPair.random(), blockchain, network, new OnDemandBlockScheduler(), (parentBlock, txs) -> null);
        }
        {
            Blockchain blockchain = new Blockchain(config, MapKeyValueStore.open());
            node3 = new BlockchainNode(SECP256K1.KeyPair.random(), blockchain, network, scheduler, (parentBlock, txs) -> new Block(txs));
        }

        Operation op = new Operation("send");

        node3.executeOperation(op);
        scheduler.trigger();
        Block minedBlock = node3.getBlockchain().getBlock(1L);
        assertNotNull(minedBlock);
        Block receivedBlock = node2.getBlockchain().getBlock(1L);
        assertNull(receivedBlock);
        Block receivedBlock2 = node1.getBlockchain().getBlock(1L);
        assertNull(receivedBlock2);
    }

    @Test
    void testTenBlocks() {
        final Account originator = new Account(Bytes32.random(), UInt256.ONE);

        List<Account> accounts = Arrays.asList(originator);

        OperationRegistry registry = new OperationRegistry(Arrays.asList(new OperationDefinition("send", (blockchain, args) -> blockchain.addToAccount(originator.getId(), UInt256.ONE))));
        BlockchainConfiguration config = new BlockchainConfiguration(accounts, registry);

        InProcessNetwork network = new InProcessNetwork();
        BlockchainNode node1;
        BlockchainNode node2;
        BlockchainNode node3;
        OnDemandBlockScheduler scheduler = new OnDemandBlockScheduler();
        {
            Blockchain blockchain = new Blockchain(config, MapKeyValueStore.open());
            node1 = new BlockchainNode(SECP256K1.KeyPair.random(), blockchain, network, new OnDemandBlockScheduler(), (parentBlock, txs) -> null);
        }
        {
            Blockchain blockchain = new Blockchain(config, MapKeyValueStore.open());
            node2 = new BlockchainNode(SECP256K1.KeyPair.random(), blockchain, network, new OnDemandBlockScheduler(), (parentBlock, txs) -> null);
        }
        {
            Blockchain blockchain = new Blockchain(config, MapKeyValueStore.open());
            node3 = new BlockchainNode(SECP256K1.KeyPair.random(), blockchain, network, scheduler, (parentBlock, txs) -> new Block(txs));
        }

        for (int i = 0 ; i < 10; i++) {
            Operation op = new Operation("send");
            node3.executeOperation(op);
            scheduler.trigger();
        }
        Block minedBlock = node3.getBlockchain().getBlock(10L);
        assertNotNull(minedBlock);
        Block receivedBlock = node2.getBlockchain().getBlock(10L);
        assertNull(receivedBlock);
        Block receivedBlock2 = node1.getBlockchain().getBlock(10L);
        assertNull(receivedBlock2);
    }
}
