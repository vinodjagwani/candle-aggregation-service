package com.candle.aggregator.core;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.function.Consumer;

final class RecentBuckets {

    private final int capacity;
    private final Deque<MutableCandle> candles;

    RecentBuckets(final int capacity) {
        this.capacity = Math.max(3, capacity);
        this.candles = new ArrayDeque<>(this.capacity);
    }

    MutableCandle findOrCreate(final long bucketStartSec, final double firstPrice) {
        for (final var c : candles) {
            if (c.init && c.bucketStartSec == bucketStartSec) {
                return c;
            }
        }

        ensureCapacity();

        final var c = new MutableCandle();
        c.reset(bucketStartSec, firstPrice);
        candles.addLast(c);
        return c;
    }

    void forEach(final Consumer<MutableCandle> action) {
        candles.forEach(action);
    }

    private void ensureCapacity() {
        while (candles.size() >= capacity) {
            final var first = candles.peekFirst();
            if (first == null) {
                return;
            }

            if (!first.init) {
                candles.removeFirst();
                continue;
            }

            candles.removeFirst();
            return;
        }
    }
}