package com.candle.aggregator.ingest;

import com.candle.aggregator.domain.BidAskEvent;

@FunctionalInterface
public interface TickSource {

    void start(TickHandler handler);

    @FunctionalInterface
    interface TickHandler {

        void onTick(BidAskEvent event);
    }
}
