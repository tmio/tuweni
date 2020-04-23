package org.apache.tuweni.votechain.network;

import org.apache.tuweni.votechain.blockchain.Block;
import org.apache.tuweni.votechain.blockchain.Message;

public class InProcessMessage implements Message {

    private final Block block;

    public InProcessMessage(Block block) {
        this.block = block;
    }

    public Block getBlock() {
        return block;
    }
}
