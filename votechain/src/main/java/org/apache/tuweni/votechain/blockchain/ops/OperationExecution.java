package org.apache.tuweni.votechain.blockchain.ops;

import org.apache.tuweni.votechain.blockchain.Blockchain;

import java.util.List;

public interface OperationExecution {

    public void execute(Blockchain blockchain, List<String> parameters);
}
