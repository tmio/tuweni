package org.apache.tuweni.votechain.blockchain.ops;

public class OperationDefinition {

    private final String name;
    private final OperationExecution execution;

    public OperationDefinition(String name, OperationExecution execution) {
        this.name = name;
        this.execution = execution;
    }

    public String getName() {
        return name;
    }

    public OperationExecution getExecution() {
        return execution;
    }
}
