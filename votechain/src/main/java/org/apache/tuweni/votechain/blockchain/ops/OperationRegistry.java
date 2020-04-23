package org.apache.tuweni.votechain.blockchain.ops;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class OperationRegistry {

    private Map<String, OperationDefinition> definitions;

    public OperationRegistry(List<OperationDefinition> operationDefinitions) {
this.definitions = new HashMap<>();
for (OperationDefinition def : operationDefinitions) {
    definitions.put(def.getName(), def);
}
    }

    public OperationDefinition get(String name) {
        return definitions.get(name);
    }
}
