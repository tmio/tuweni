package org.apache.tuweni.votechain.network;

import org.apache.tuweni.votechain.blockchain.Message;

import java.util.function.Consumer;

public interface Network {

    void send(Message message);

    void registerListener(Consumer<Message> listener);
}
