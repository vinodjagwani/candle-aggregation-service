package com.candle.aggregator.core;

import com.candle.aggregator.domain.BidAskEvent;
import com.candle.aggregator.domain.Candle;
import com.candle.aggregator.domain.Interval;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.IntStream;

public final class CandleShard implements CandleAggregator {

    private final Map<String, RecentBuckets[]> bucketsBySymbol = new ConcurrentHashMap<>();
    private final Interval[] intervals;
    private final long allowedLatenessSec;
    private final int bucketCapacity;

    private long maxSeenSec = Long.MIN_VALUE;

    public CandleShard(
            final Interval[] intervals,
            final long allowedLatenessSec,
            final int bucketCapacity
    ) {
        this.intervals = intervals;
        this.allowedLatenessSec = allowedLatenessSec;
        this.bucketCapacity = bucketCapacity;
    }

    private static void tryFinalize(
            final String symbol,
            final Interval interval,
            final long intervalSec,
            final long watermarkSec,
            final MutableCandle candle,
            final List<Finalized> out
    ) {
        if (!candle.init) {
            return;
        }

        final long bucketEnd = candle.bucketStartSec + intervalSec;
        if (bucketEnd > watermarkSec) {
            return;
        }

        out.add(new Finalized(symbol, interval, candle.freeze()));
        candle.init = false;
    }

    @Override
    public List<Finalized> onTick(final BidAskEvent event) {
        final long eventSec = event.timestampMillis() / 1000L;
        maxSeenSec = Math.max(maxSeenSec, eventSec);

        final double price = event.mid();
        final String symbol = event.symbol();

        final RecentBuckets[] perInterval = bucketsBySymbol.computeIfAbsent(symbol, _ -> newBucketArray());
        updateAllIntervals(perInterval, eventSec, price);

        final long watermarkSec = maxSeenSec - allowedLatenessSec;
        return finalizeByWatermark(symbol, perInterval, watermarkSec);
    }

    private RecentBuckets[] newBucketArray() {
        return IntStream.range(0, intervals.length).mapToObj(_ -> new RecentBuckets(bucketCapacity))
                .toArray(RecentBuckets[]::new);
    }

    private void updateAllIntervals(final RecentBuckets[] perInterval, final long eventSec, final double price) {
        IntStream.range(0, intervals.length).forEach(i -> {
            final var interval = intervals[i];
            final long bucketStart = interval.bucketStartSec(eventSec);
            final var candle = perInterval[i].findOrCreate(bucketStart, price);
            candle.update(price);
        });
    }

    private List<Finalized> finalizeByWatermark(
            final String symbol,
            final RecentBuckets[] perInterval,
            final long watermarkSec
    ) {
        final var out = new ArrayList<Finalized>(8);

        IntStream.range(0, intervals.length).forEach(i -> {
            final var interval = intervals[i];
            final long intervalSec = interval.seconds();
            perInterval[i].forEach(c -> tryFinalize(symbol, interval, intervalSec, watermarkSec, c, out));
        });

        return out;
    }

    public record Finalized(String symbol, Interval interval, Candle candle) {

    }
}