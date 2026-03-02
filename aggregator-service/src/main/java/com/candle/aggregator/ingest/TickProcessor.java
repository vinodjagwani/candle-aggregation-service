package com.candle.aggregator.ingest;

import com.candle.aggregator.config.AppProperties;
import com.candle.aggregator.core.BatchFlusher;
import com.candle.aggregator.core.ShardManager;
import com.candle.aggregator.core.ShardState;
import com.candle.aggregator.domain.BidAskEvent;
import com.candle.aggregator.observation.AggMetrics;
import com.candle.aggregator.sink.KafkaCandlePublisher;
import io.micrometer.core.instrument.Timer;
import java.util.Arrays;
import java.util.concurrent.atomic.LongAdder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public final class TickProcessor {

    private final ShardManager shardManager;
    private final BatchFlusher flusher;
    private final AggMetrics metrics;
    private final KafkaCandlePublisher publisher;


    private final LongAdder ticksSeen = new LongAdder();
    private final LongAdder candlesSeen = new LongAdder();

    private volatile long lastLogMs = System.currentTimeMillis();

    public TickProcessor(
            final AppProperties props,
            final ShardManager shardManager,
            final AggMetrics metrics,
            final KafkaTemplate<String, String> kafka
    ) {
        this.shardManager = shardManager;
        this.metrics = metrics;
        this.flusher = new BatchFlusher(props.flush().rows(), props.flush().every());
        this.publisher = new KafkaCandlePublisher(kafka, props.kafka().candlesTopic());
    }

    public void onTick(final BidAskEvent event) {
        final Timer.Sample sample = metrics.startTickTimer();
        try {
            metrics.tickProcessed();
            ticksSeen.increment();

            final ShardState shard = shardManager.bySymbol(event.symbol());
            final var finalized = shard.aggregator.onTick(event);

            if (!finalized.isEmpty()) {
                candlesSeen.add(finalized.size());
                finalized.forEach(c -> {
                    metrics.candleFinalized(c.interval().code());
                    shard.sink.onCandle(c);
                    shard.pendingRows++;
                    publisher.publish(c);
                });
            }

            flushIfNeeded(shard);
            logRates();
        } finally {
            metrics.stopTickTimer(sample);
        }
    }

    private void flushIfNeeded(final ShardState shard) {
        final long now = System.nanoTime();
        if (shard.pendingRows == 0) {
            return;
        }
        if (!flusher.shouldFlush(shard.pendingRows, shard.lastFlushNanos, now)) {
            return;
        }
        shard.sink.flush();
        shard.pendingRows = 0;
        shard.lastFlushNanos = now;
    }

    private void logRates() {
        final long nowMs = System.currentTimeMillis();
        if (nowMs - lastLogMs < 1000) {
            return;
        }
        lastLogMs = nowMs;
        log.info("ticks/sec={} candles/sec={}", ticksSeen.sumThenReset(), candlesSeen.sumThenReset());
    }

    public void shutdown() {
        Arrays.stream(shardManager.all()).forEach(shard -> {
            try {
                if (shard.pendingRows > 0) {
                    shard.sink.flush();
                }
            } catch (final Exception ex) {
                log.error("An error occurred during shutdown", ex);
            }
            try {
                shard.sink.close();
            } catch (final Exception ex) {
                log.error("An error occurred during sink close", ex);
            }
        });
    }
}