package com.candle.aggregator.ingest;

import com.candle.aggregator.config.AppProperties;
import com.candle.aggregator.domain.BidAskEvent;
import com.candle.aggregator.observation.AggMetrics;
import java.util.List;
import java.util.concurrent.atomic.LongAdder;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.context.annotation.Primary;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@Primary
@Component
public final class KafkaTickSource implements TickSource {

    private final ObjectMapper mapper;
    private final TickDlqPublisher dlq;
    private final AppProperties props;
    private final AggMetrics metrics;
    private final LongAdder decodeFailed = new LongAdder();
    private TickHandler handler;
    private volatile long lastLogMs = System.currentTimeMillis();

    public KafkaTickSource(
            final ObjectMapper mapper,
            final TickDlqPublisher dlq,
            final AppProperties props,
            final AggMetrics metrics
    ) {
        this.mapper = mapper;
        this.dlq = dlq;
        this.props = props;
        this.metrics = metrics;
    }

    @Override
    public void start(final TickHandler handler) {
        this.handler = handler;
    }

    @KafkaListener(
            topics = "${app.kafka.ticks-topic}",
            groupId = "candle-agg-v1",
            concurrency = "${app.kafka.concurrency}"
    )
    public void onBatch(final List<ConsumerRecord<String, String>> records, final Acknowledgment ack) {
        final TickHandler h = handler;
        if (h == null || records.isEmpty()) {
            ack.acknowledge();
            return;
        }

        records.stream().map(ConsumerRecord::value).forEach(raw -> {
            final BidAskEvent event = decodeOrNull(raw);
            if (event == null) {
                dlq.send(props.kafka().dlqTopic(), raw, "Bad tick json");
                return;
            }
            try {
                h.onTick(event);
            } catch (final Exception ex) {
                metrics.decodeFailed();
                dlq.send(props.kafka().dlqTopic(), raw, "Tick handler error: " + ex.getMessage());
                metrics.dlqPublished();
                log.error("Tick handler failed; sent to DLQ", ex);
            }
        });

        logDecodeRate();
        ack.acknowledge();
    }

    private BidAskEvent decodeOrNull(final String json) {
        try {
            final JsonNode n = mapper.readTree(json);

            final JsonNode sym = n.get("symbol");
            final JsonNode bid = n.get("bid");
            final JsonNode ask = n.get("ask");
            final JsonNode ts = n.get("timestamp");

            if (sym == null || bid == null || ask == null || ts == null) {
                decodeFailed.increment();
                return null;
            }

            return new BidAskEvent(sym.asString(), bid.asDouble(), ask.asDouble(), ts.asLong());
        } catch (final Exception ex) {
            decodeFailed.increment();
            return null;
        }
    }

    private void logDecodeRate() {
        final long nowMs = System.currentTimeMillis();
        if (nowMs - lastLogMs < 1000) {
            return;
        }
        lastLogMs = nowMs;
        final long failed = decodeFailed.sumThenReset();
        if (failed > 0) {
            log.warn("decodeFailed/sec={}", failed);
        }
    }
}
