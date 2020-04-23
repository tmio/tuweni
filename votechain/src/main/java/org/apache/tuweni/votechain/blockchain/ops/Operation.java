package org.apache.tuweni.votechain.blockchain.ops;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class Operation {

    private final String name;
    private final List<String> parameters;

    public Operation(String name, String... parameters) {
        this(name, Arrays.asList(parameters));
    }

    public Operation(String name, List<String> parameters) {
        this.name = name;
        this.parameters = parameters;
    }

   public String getName() {
        return name;
   }

    public List<String> parameters() {
        return parameters;
    }
}
