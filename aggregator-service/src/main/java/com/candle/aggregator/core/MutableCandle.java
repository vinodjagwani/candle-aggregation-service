package com.candle.aggregator.core;

import com.candle.aggregator.domain.Candle;

final class MutableCandle {

    long bucketStartSec;
    double open;
    double high;
    double low;
    double close;
    long volume;
    boolean init;

    void reset(final long bucketStartSec, final double firstPrice) {
        this.bucketStartSec = bucketStartSec;
        this.open = firstPrice;
        this.high = firstPrice;
        this.low = firstPrice;
        this.close = firstPrice;
        this.volume = 0;
        this.init = true;
    }

    void update(final double price) {
        if (!init) {
            return;
        }

        if (price > high) {
            high = price;
        }
        if (price < low) {
            low = price;
        }

        close = price;
        volume++;
    }

    Candle freeze() {
        return new Candle(bucketStartSec, open, high, low, close, volume);
    }
}