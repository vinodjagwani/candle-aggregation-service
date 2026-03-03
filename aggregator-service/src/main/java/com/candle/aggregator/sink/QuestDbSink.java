package com.candle.aggregator.sink;

import com.candle.aggregator.core.CandleShard;
import com.candle.aggregator.observation.AggMetrics;
import io.micrometer.core.instrument.Timer;
import io.questdb.client.Sender;
import java.time.temporal.ChronoUnit;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class QuestDbSink implements CandleSink {

    private final String ilpConfig;
    private final String tableName;
    private final AggMetrics metrics;
    private Sender sender;


    public QuestDbSink(final String ilpConfig, final String tableName, final AggMetrics metrics) {
        this.sender = Sender.fromConfig(ilpConfig);
        this.ilpConfig = ilpConfig;
        this.tableName = tableName;
        this.metrics = metrics;
    }

    private static long epochSecToNanos(final long epochSec) {
        return Math.multiplyExact(epochSec, 1_000_000_000L);
    }

    private static boolean isReasonableEpochSeconds(final long epochSec) {
        return epochSec >= 946684800L && epochSec <= 4102444800L;
    }

    @Override
    public void onCandle(final CandleShard.Finalized finalized) {
        final Timer.Sample sample = metrics.startQuestWriteTimer();
        try {
            final var candle = finalized.candle();
            final long epochSec = candle.timeSec();

            if (!isReasonableEpochSeconds(epochSec)) {
                log.warn("Skipping candle with suspicious timeSec={} symbol={} interval={}",
                        epochSec, finalized.symbol(), finalized.interval().code());
                return;
            }

            metrics.questWrite(finalized.interval().code());

            final long tsNanos = epochSecToNanos(epochSec);
            try {
                writeOnce(finalized, tsNanos);
            } catch (final Exception first) {
                log.warn("QuestDB write failed (reset sender and retry once): {}", first.toString());
                resetSender();
                writeOnce(finalized, tsNanos);
            }
        } finally {
            metrics.stopQuestWriteTimer(sample);
        }
    }

    private void writeOnce(final CandleShard.Finalized finalized, final long tsNanos) {
        final var candle = finalized.candle();
        sender.table(tableName)
                .symbol("symbol", finalized.symbol())
                .symbol("interval", finalized.interval().code())
                .doubleColumn("open", candle.open())
                .doubleColumn("high", candle.high())
                .doubleColumn("low", candle.low())
                .doubleColumn("close", candle.close())
                .longColumn("volume", candle.volume())
                .at(tsNanos, ChronoUnit.NANOS);
    }

    @Override
    public void flush() {
        final Timer.Sample sample = metrics.startQuestFlushTimer();
        try {
            sender.flush();
        } catch (final Exception first) {
            log.warn("QuestDB flush failed (reset sender and retry once): {}", first.toString());
            resetSender();
            sender.flush();
        } finally {
            metrics.stopQuestFlushTimer(sample);
        }
    }

    private void resetSender() {
        try {
            sender.close();
        } catch (final Exception ex) {
            log.warn("An error occurred while db sink closing", ex);
        }
        sender = Sender.fromConfig(ilpConfig);
    }

    @Override
    public void close() {
        try {
            flush();
        } catch (final Exception ex) {
            log.warn("An error occurred while db sink flush", ex);
        }
        try {
            sender.close();
        } catch (final Exception ex) {
            log.warn("An error occurred while db sink closing", ex);
        }
    }
}
