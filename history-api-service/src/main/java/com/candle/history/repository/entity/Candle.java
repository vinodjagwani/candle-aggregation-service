package com.candle.history.repository.entity;

import java.time.Instant;

public record Candle(
        String symbol,
        String interval,
        double open,
        double high,
        double low,
        double close,
        long volume,
        Instant timestamp
) {

}