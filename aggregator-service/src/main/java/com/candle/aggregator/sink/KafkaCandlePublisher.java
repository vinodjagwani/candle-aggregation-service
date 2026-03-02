package com.candle.aggregator.sink;

import com.candle.aggregator.core.CandleShard;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import tools.jackson.databind.ObjectMapper;

@Slf4j
public final class KafkaCandlePublisher {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final String topic;
    private final KafkaTemplate<String, String> kafka;

    public KafkaCandlePublisher(final KafkaTemplate<String, String> kafka, final String topic) {
        this.kafka = kafka;
        this.topic = topic;
    }

    private static String toJson(final CandleShard.Finalized finalized) {
        try {
            final var candle = finalized.candle();
            return objectMapper.writeValueAsString(Map.of(
                    "symbol", finalized.symbol(),
                    "interval", finalized.interval().code(),
                    "time", candle.timeSec(),
                    "open", candle.open(),
                    "high", candle.high(),
                    "low", candle.low(),
                    "close", candle.close(),
                    "volume", candle.volume()
            ));
        } catch (final Exception ex) {
            log.error("An error occurred during serialize", ex);
            throw new IllegalStateException("Failed to serialize candle", ex);
        }
    }

    public void publish(final CandleShard.Finalized finalized) {
        kafka.send(topic, finalized.symbol(), toJson(finalized));
    }
}
