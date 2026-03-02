package com.candle.aggregator.sink;

import com.candle.aggregator.core.CandleShard;

public interface CandleSink extends AutoCloseable {

    void onCandle(CandleShard.Finalized candle);

    void flush();

    @Override
    void close();
}
