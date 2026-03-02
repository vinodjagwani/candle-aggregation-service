package com.candle.aggregator.observation;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.stereotype.Component;

@Component
public final class AggMetrics {

    private final Counter ticksProcessed;
    private final Counter decodeFailed;
    private final Counter dlqPublished;

    private final Timer tickProcessTime;
    private final Timer questWriteTime;
    private final Timer questFlushTime;

    private final ConcurrentMap<String, Counter> candlesFinalizedByInterval = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Counter> questWritesByInterval = new ConcurrentHashMap<>();

    private final MeterRegistry registry;

    public AggMetrics(final MeterRegistry registry) {
        this.registry = registry;

        this.ticksProcessed = Counter.builder("candle_ticks_processed_total")
                .description("Total ticks processed")
                .register(registry);

        this.decodeFailed = Counter.builder("candle_ticks_decode_failed_total")
                .description("Total ticks that failed decoding")
                .register(registry);

        this.dlqPublished = Counter.builder("candle_ticks_dlq_published_total")
                .description("Total ticks published to DLQ")
                .register(registry);

        this.tickProcessTime = Timer.builder("candle_tick_process_seconds")
                .description("Time spent processing a single tick end-to-end")
                .publishPercentileHistogram()
                .register(registry);

        this.questWriteTime = Timer.builder("candle_questdb_write_seconds")
                .description("Time spent writing one candle to QuestDB")
                .publishPercentileHistogram()
                .register(registry);

        this.questFlushTime = Timer.builder("candle_questdb_flush_seconds")
                .description("Time spent flushing QuestDB sender")
                .publishPercentileHistogram()
                .register(registry);
    }

    public void tickProcessed() {
        ticksProcessed.increment();
    }

    public void decodeFailed() {
        decodeFailed.increment();
    }

    public void dlqPublished() {
        dlqPublished.increment();
    }

    public void candleFinalized(final String intervalCode) {
        counterByInterval(candlesFinalizedByInterval, "candles_finalized", intervalCode,
                "Finalized candles by interval").increment();
    }

    public void questWrite(final String intervalCode) {
        counterByInterval(questWritesByInterval, "questdb_writes", intervalCode,
                "QuestDB writes by interval").increment();
    }

    public Timer.Sample startTickTimer() {
        return Timer.start(registry);
    }

    public void stopTickTimer(final Timer.Sample sample) {
        sample.stop(tickProcessTime);
    }

    public Timer.Sample startQuestWriteTimer() {
        return Timer.start(registry);
    }

    public void stopQuestWriteTimer(final Timer.Sample sample) {
        sample.stop(questWriteTime);
    }

    public Timer.Sample startQuestFlushTimer() {
        return Timer.start(registry);
    }

    public void stopQuestFlushTimer(final Timer.Sample sample) {
        sample.stop(questFlushTime);
    }

    private Counter counterByInterval(
            final ConcurrentMap<String, Counter> map,
            final String baseName,
            final String intervalCode,
            final String description
    ) {
        return map.computeIfAbsent(intervalCode, code ->
                Counter.builder("candle_" + baseName + "_total")
                        .description(description)
                        .tag("interval", code)
                        .register(registry)
        );
    }
}
