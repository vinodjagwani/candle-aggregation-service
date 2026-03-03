package com.candle.aggregator.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.candle.aggregator.domain.BidAskEvent;
import com.candle.aggregator.domain.Interval;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

final class CandleShardTest {

    private static BidAskEvent tick(final String symbol, final double mid, final long timestampMillis) {
        return new BidAskEvent(symbol, mid - 0.1, mid + 0.1, timestampMillis);
    }

    @Test
    @DisplayName("TestFinalizesOneSecondCandleUsingWatermark")
    void testFinalizesOneSecondCandleUsingWatermark() {
        final Interval[] intervals = {new Interval("1s", 1)};
        final long allowedLatenessSec = 1;
        final int bucketCapacity = 16;

        final CandleShard shard = new CandleShard(intervals, allowedLatenessSec, bucketCapacity);

        // sec=1 bucket
        assertTrue(shard.onTick(tick("BTC-USD", 10.0, 1_000)).isEmpty());
        assertTrue(shard.onTick(tick("BTC-USD", 11.0, 1_500)).isEmpty());

        // Advance maxSeenSec to 4 => watermark = 3
        // bucketStart=1 => bucketEnd=2 <= watermark => finalized now
        final List<CandleShard.Finalized> out = shard.onTick(tick("BTC-USD", 20.0, 4_000));
        assertEquals(1, out.size());

        final CandleShard.Finalized f = out.getFirst();
        assertEquals("BTC-USD", f.symbol());
        assertEquals("1s", f.interval().code());

        final var c = f.candle();
        assertEquals(1, c.timeSec());
        assertEquals(10.0, c.open(), 1e-9);
        assertEquals(11.0, c.high(), 1e-9);
        assertEquals(10.0, c.low(), 1e-9);
        assertEquals(11.0, c.close(), 1e-9);
        assertEquals(2L, c.volume());
    }

    @Test
    @DisplayName("TestAcceptsLateTickWithinAllowedLatenessAndFinalizesWhenWatermarkPasses")
    void testAcceptsLateTickWithinAllowedLatenessAndFinalizesWhenWatermarkPasses() {
        final Interval[] intervals = {new Interval("1s", 1)};
        final long allowedLatenessSec = 2;
        final int bucketCapacity = 16;

        final CandleShard shard = new CandleShard(intervals, allowedLatenessSec, bucketCapacity);

        // First tick at sec=10
        assertTrue(shard.onTick(tick("ETH-USD", 100.0, 10_000)).isEmpty());

        // Late tick at sec=9 (still within lateness window)
        assertTrue(shard.onTick(tick("ETH-USD", 90.0, 9_000)).isEmpty());

        // Now jump forward so watermark passes bucket 9 end (10)
        // sec=13 => watermark=11 => bucket 9 end=10 <= 11 => finalized HERE
        final var out = shard.onTick(tick("ETH-USD", 110.0, 13_000));

        final var bucket9 = out.stream()
                .filter(f -> f.candle().timeSec() == 9)
                .findFirst()
                .orElseThrow();

        assertEquals("ETH-USD", bucket9.symbol());
        assertEquals(9, bucket9.candle().timeSec());
        assertEquals(90.0, bucket9.candle().open(), 1e-9);
        assertEquals(90.0, bucket9.candle().high(), 1e-9);
        assertEquals(90.0, bucket9.candle().low(), 1e-9);
        assertEquals(90.0, bucket9.candle().close(), 1e-9);
        assertEquals(1L, bucket9.candle().volume());
    }

    @Test
    @DisplayName("TestFinalizesFiveSecondBucket")
    void testFinalizesFiveSecondBucket() {
        final Interval[] intervals = {
                new Interval("1s", 1),
                new Interval("5s", 5)
        };

        final long allowedLatenessSec = 1;
        final int bucketCapacity = 32;

        final CandleShard shard = new CandleShard(intervals, allowedLatenessSec, bucketCapacity);

        // Create ticks in [0..4] seconds, all within 5s bucket starting at 0
        shard.onTick(tick("SOL-USD", 10.0, 0_000));
        shard.onTick(tick("SOL-USD", 11.0, 1_000));
        shard.onTick(tick("SOL-USD", 12.0, 2_000));
        shard.onTick(tick("SOL-USD", 13.0, 3_000));
        shard.onTick(tick("SOL-USD", 14.0, 4_000));

        // sec=7 => watermark=6
        // 5s bucket [0..5) ends at 5 <= 6 => finalized now
        final var out = shard.onTick(tick("SOL-USD", 99.0, 7_000));

        final var fiveSec = out.stream()
                .filter(f -> f.interval().code().equals("5s"))
                .findFirst()
                .orElseThrow();

        assertEquals("SOL-USD", fiveSec.symbol());
        assertEquals(0, fiveSec.candle().timeSec());
        assertEquals(10.0, fiveSec.candle().open(), 1e-9);
        assertEquals(14.0, fiveSec.candle().high(), 1e-9);
        assertEquals(10.0, fiveSec.candle().low(), 1e-9);
        assertEquals(14.0, fiveSec.candle().close(), 1e-9);
        assertEquals(5L, fiveSec.candle().volume());
    }
}