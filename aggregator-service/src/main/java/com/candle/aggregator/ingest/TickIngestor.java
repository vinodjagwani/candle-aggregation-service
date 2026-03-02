package com.candle.aggregator.ingest;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public final class TickIngestor {

    private final TickSource source;
    private final TickProcessor processor;

    public TickIngestor(final TickSource source, final TickProcessor processor) {
        this.source = source;
        this.processor = processor;
    }

    @PostConstruct
    public void start() {
        source.start(processor::onTick);
        log.info("TickIngestor started using {}", source.getClass().getSimpleName());
    }

    @PreDestroy
    public void stop() {
        processor.shutdown();
        log.info("TickIngestor stopped");
    }
}