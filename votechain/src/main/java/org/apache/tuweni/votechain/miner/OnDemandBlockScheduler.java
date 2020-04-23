package org.apache.tuweni.votechain.miner;

public class OnDemandBlockScheduler implements MiningScheduler {

    private Runnable callback;

    @Override
    public void register(Runnable callback) {
        this.callback = callback;
    }

    @Override
    public void start() {
    }

    @Override
    public void close() {
    }

    public void trigger() {
        callback.run();
    }
}
