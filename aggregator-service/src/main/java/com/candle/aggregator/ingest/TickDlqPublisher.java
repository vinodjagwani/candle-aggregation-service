package com.candle.aggregator.ingest;

import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@Component
@RequiredArgsConstructor
public final class TickDlqPublisher {

    private final ObjectMapper mapper;
    private final KafkaTemplate<String, String> kafka;

    public void send(final String topic, final String rawValue, final String error) {
        try {
            final String json = mapper.writeValueAsString(Map.of(
                    "error", error,
                    "raw", rawValue
            ));
            kafka.send(topic, null, json);
        } catch (final Exception ex) {
            log.error("DLQ send failed", ex);
        }
    }
}