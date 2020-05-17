package org.apache.tuweni.votechain.blockchain;

import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;
import org.apache.tuweni.crypto.Hash;
import org.apache.tuweni.votechain.blockchain.ops.Operation;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class Transaction {
    private final List<Operation> operations;

    public Transaction(List<Operation> operations) {
        this.operations = operations;
    }

    public List<Operation> getOperations() {
        return operations;
    }

    public Bytes32 getHash() {
        List<String> elements = new ArrayList<>();
        for (Operation op : operations) {
            elements.add(op.getName());
            elements.addAll(op.parameters());
        }
        Bytes toHash = Bytes.concatenate(elements.stream().map(str -> Bytes.wrap(str.getBytes(StandardCharsets.UTF_8))).collect(Collectors.toList()).toArray(new Bytes[] {}));
        return Hash.keccak256(toHash);
    }
}
