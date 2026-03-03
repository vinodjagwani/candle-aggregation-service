package com.candle.aggregator.core;

import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

final class RecentBucketsTest {

    @Test
    @DisplayName("TestEvictsFinalizedBucketsBeforeUnfinished")
    void testEvictsFinalizedBucketsBeforeUnfinished() {
        final RecentBuckets buckets = new RecentBuckets(3);

        final MutableCandle c0 = buckets.findOrCreate(0, 10.0);
        final MutableCandle c1 = buckets.findOrCreate(1, 11.0);

        c0.init = false;

        buckets.findOrCreate(3, 13.0);

        final MutableCandle stillThere = buckets.findOrCreate(1, 999.0);
        assertSame(c1, stillThere);
    }

    @Test
    @DisplayName("TestEvictsOldestUnfinishedWhenAllAreUnfinished")
    void testEvictsOldestUnfinishedWhenAllAreUnfinished() {
        final RecentBuckets buckets = new RecentBuckets(3);

        final MutableCandle c0 = buckets.findOrCreate(0, 10.0);
        buckets.findOrCreate(1, 11.0);
        buckets.findOrCreate(2, 12.0);

        buckets.findOrCreate(3, 13.0);

        final MutableCandle recreated0 = buckets.findOrCreate(0, 10.0);
        assertNotSame(c0, recreated0);
    }
}