package com.candle.history.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Schema(name = "HistoryRequest", description = "Query parameters for TradingView candle history")
public record HistoryRequest(

        @JsonProperty("symbol")
        @Schema(description = "Trading symbol", example = "BTCUSDT", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "Symbol must not be blank")
        String symbol,

        @JsonProperty("interval")
        @Schema(description = "Candle interval", example = "1m", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "Interval must not be blank")
        @Pattern(
                regexp = "^(1m|5m|15m|1h|4h|1d)$",
                message = "Interval must be one of: 1m, 5m, 15m, 1h, 4h, 1d"
        )
        String interval,

        @JsonProperty("from")
        @Schema(description = "From timestamp (epoch seconds)", example = "1710000000", requiredMode = Schema.RequiredMode.REQUIRED)
        @Min(value = 0, message = "From timestamp must be >= 0")
        long from,

        @JsonProperty("to")
        @Schema(description = "To timestamp (epoch seconds)", example = "1710003600", requiredMode = Schema.RequiredMode.REQUIRED)
        @Min(value = 0, message = "To timestamp must be >= 0")
        long to,

        @JsonProperty("limit")
        @Schema(description = "Max candles to return", example = "1000", defaultValue = "1000")
        @Min(value = 1, message = "Limit must be at least 1")
        @Max(value = 10_000, message = "Limit too large")
        int limit
) {


}