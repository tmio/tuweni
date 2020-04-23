package org.apache.tuweni.votechain.miner;

public interface MiningScheduler {

    void register(Runnable callback);

    void start();

    void close();
}
