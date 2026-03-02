package com.candle.aggregator.core;


import com.candle.aggregator.sink.CandleSink;

public final class ShardState {

    public final CandleShard aggregator;
    public final CandleSink sink;

    public long pendingRows;
    public long lastFlushNanos;

    public ShardState(final CandleShard aggregator, final CandleSink sink, final long nowNanos) {
        this.aggregator = aggregator;
        this.sink = sink;
        this.pendingRows = 0;
        this.lastFlushNanos = nowNanos;
    }
}
