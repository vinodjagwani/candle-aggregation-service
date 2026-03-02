package com.candle.aggregator.domain;

public record Candle(
        long timeSec,
        double open,
        double high,
        double low,
        double close,
        long volume
) {

}