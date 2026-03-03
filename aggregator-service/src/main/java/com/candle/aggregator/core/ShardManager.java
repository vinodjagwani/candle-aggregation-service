package com.candle.aggregator.core;

import com.candle.aggregator.config.AppProperties;
import com.candle.aggregator.sink.CandleSinkFactory;
import java.util.Arrays;
import java.util.stream.IntStream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public final class ShardManager implements AutoCloseable {

    private final ShardState[] shards;

    public ShardManager(
            final AppProperties props,
            final IntervalRegistry intervals,
            final CandleSinkFactory sinkFactory
    ) {
        final int shardCount = props.kafka().concurrency();
        final long now = System.nanoTime();
        final int bucketCapacity = (int) Math.max(10, props.candle().allowedLatenessSec() + 5);

        this.shards = new ShardState[shardCount];

        IntStream.range(0, shardCount).forEach(i -> {
            final var agg = new CandleShard(intervals.all(), props.candle().allowedLatenessSec(), bucketCapacity);
            final var sink = sinkFactory.create();
            shards[i] = new ShardState(agg, sink, now);
        });
    }

    public ShardState byPartition(final int partition) {
        return shards[Math.floorMod(partition, shards.length)];
    }

    public ShardState[] all() {
        return shards;
    }

    @Override
    public void close() {
        Arrays.stream(shards).forEach(s -> {
            try {
                s.sink.close();
            } catch (final Exception ex) {
                log.error("An error occurred while closing sink", ex);
            }
        });
    }
}