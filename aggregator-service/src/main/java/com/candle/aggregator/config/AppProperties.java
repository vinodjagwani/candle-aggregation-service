package com.candle.aggregator.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.time.Duration;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app")
public record AppProperties(
        @Valid Kafka kafka,
        @Valid Candle candle,
        @Valid Flush flush,
        @Valid QuestDb questdb,
        @Valid Simulator simulator
) {

    public record Kafka(
            @NotBlank String ticksTopic,
            @NotBlank String candlesTopic,
            @NotBlank String dlqTopic,
            @Min(1) int concurrency
    ) {

    }

    public record Candle(
            @Min(0) long allowedLatenessSec,
            @NotEmpty @Valid List<IntervalConfig> intervals
    ) {

        public record IntervalConfig(@NotBlank String code, @Min(1) long seconds) {

        }
    }

    public record Flush(@Min(1) long rows, @Valid Duration every) {

    }

    public record QuestDb(@NotBlank String ilpConfig, @NotBlank String table) {

    }

    public record Simulator(
            boolean enabled,
            @NotEmpty List<String> symbols,
            @Min(1) int ticksPerSecond,
            double basePrice,
            double volatility,
            double spread
    ) {

    }
}