package com.candle.aggregator.domain;

public record BidAskEvent(String symbol, double bid, double ask, long timestampMillis) {

    public double mid() {
        return (bid + ask) * 0.5;
    }
}