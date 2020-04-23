package org.apache.tuweni.votechain.network;

import org.apache.tuweni.votechain.blockchain.Message;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

public class InProcessNetwork implements Network {

    private Set<Consumer<Message>> listeners = Collections.synchronizedSet(new HashSet<>());
    @Override
    public void send(Message message) {
        for (Consumer<Message> listener : listeners) {
            listener.accept(message);
        }

    }

    @Override
    public void registerListener(Consumer<Message> listener) {
        listeners.add(listener);
    }


}
