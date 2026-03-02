package com.candle.history.config;


import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app")
public record AppProperties(
        @Valid QuestDb questdb,
        @Valid Candles candles
) {

    public record QuestDb(@NotBlank String httpUrl) {

    }

    public record Candles(
            @NotBlank String table,
            @Min(1) int maxPoints,
            @NotEmpty @Valid List<IntervalDefinition> intervals
    ) {

    }

    public record IntervalDefinition(
            @NotBlank String code,
            @Min(1) long seconds
    ) {

    }
}