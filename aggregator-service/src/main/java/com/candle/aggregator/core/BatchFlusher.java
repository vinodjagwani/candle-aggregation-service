package com.candle.aggregator.core;

import java.time.Duration;

public final class BatchFlusher {

    private final long flushRows;
    private final long flushEveryNanos;

    public BatchFlusher(final long flushRows, final Duration flushEvery) {
        this.flushRows = flushRows;
        this.flushEveryNanos = flushEvery.toNanos();
    }

    public boolean shouldFlush(final long pendingRows, final long lastFlushNanos, final long nowNanos) {
        return pendingRows >= flushRows || (nowNanos - lastFlushNanos) >= flushEveryNanos;
    }
}