package com.candle.aggregator.domain;


public record Interval(String code, long seconds) {

    public long bucketStartSec(long unixSec) {
        return (unixSec / seconds) * seconds;
    }
}
