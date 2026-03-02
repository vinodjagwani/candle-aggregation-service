package com.candle.aggregator.sim;

import com.candle.aggregator.config.AppProperties;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@Component
@ConditionalOnProperty(prefix = "app.simulator", name = "enabled", havingValue = "true")
public final class TickSimulator {

    private final AppProperties props;
    private final KafkaTemplate<String, String> kafka;
    private final ObjectMapper mapper = new ObjectMapper();

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        final var t = new Thread(r, "tick-simulator");
        t.setDaemon(true);
        return t;
    });

    private final LongAdder emitted = new LongAdder();
    private final LongAdder sendErrors = new LongAdder();

    private volatile long lastLogMs = System.currentTimeMillis();
    private double carry = 0.0;

    private PriceEngine engine;

    public TickSimulator(final KafkaTemplate<String, String> kafka, final AppProperties props) {
        this.kafka = kafka;
        this.props = props;
    }

    @PostConstruct
    public void start() {
        final var sim = props.simulator();
        this.engine = new PriceEngine(sim.basePrice(), sim.volatility(), sim.symbols());

        scheduler.scheduleAtFixedRate(this::safeLoop, 0, 1, TimeUnit.MILLISECONDS);
    }

    private void safeLoop() {
        try {
            emitLoop();
        } catch (final Exception t) {
            log.error("TickSimulator crashed but will continue: ", t);
        }
    }

    private void emitLoop() {
        final var sim = props.simulator();
        final int tps = sim.ticksPerSecond();

        carry += (tps / 1000.0);
        final int rounds = (int) carry;

        if (rounds <= 0) {
            logOncePerSec();
            return;
        }

        carry -= rounds;

        final long now = System.currentTimeMillis();
        for (int r = 0; r < rounds; r++) {
            for (final var symbol : sim.symbols()) {
                final double mid = engine.nextMid(symbol);
                final double spread = sim.spread();
                final double bid = mid - spread * 0.5;
                final double ask = mid + spread * 0.5;

                final String json;
                try {
                    json = mapper.writeValueAsString(Map.of(
                            "symbol", symbol,
                            "bid", bid,
                            "ask", ask,
                            "timestamp", now
                    ));
                } catch (final Exception e) {
                    sendErrors.increment();
                    continue;
                }

                kafka.send(props.kafka().ticksTopic(), symbol, json)
                        .whenComplete((ok, ex) -> {
                            if (ex != null) {
                                sendErrors.increment();
                            } else {
                                emitted.increment();
                            }
                        });
            }
        }

        logOncePerSec();
    }

    private void logOncePerSec() {
        final long nowMs = System.currentTimeMillis();
        if (nowMs - lastLogMs < 1000) {
            return;
        }
        lastLogMs = nowMs;
        log.info("sim emitted/sec={} sendErrors/sec={}", emitted.sumThenReset(), sendErrors.sumThenReset());
    }

    @PreDestroy
    public void stop() {
        scheduler.shutdownNow();
    }
}