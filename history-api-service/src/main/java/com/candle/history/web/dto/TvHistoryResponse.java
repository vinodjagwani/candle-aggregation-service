package com.candle.history.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(name = "TvHistoryResponse", description = "TradingView-compatible candle history response")
public record TvHistoryResponse(

        @JsonProperty("s")
        @Schema(
                description = "Response status",
                example = "ok",
                allowableValues = {"ok", "no_data"}
        )
        String s,

        @JsonProperty("t")
        @Schema(description = "Candle timestamps (epoch seconds)", example = "[1710000000,1710000060]")
        List<Long> t,

        @JsonProperty("o")
        @Schema(description = "Open prices", example = "[100.1,100.2]")
        List<Double> o,

        @JsonProperty("h")
        @Schema(description = "High prices", example = "[101.0,101.2]")
        List<Double> h,

        @JsonProperty("l")
        @Schema(description = "Low prices", example = "[99.8,100.0]")
        List<Double> l,

        @JsonProperty("c")
        @Schema(description = "Close prices", example = "[100.5,100.9]")
        List<Double> c,

        @JsonProperty("v")
        @Schema(description = "Volumes", example = "[12345,23456]")
        List<Long> v
) {

    public static TvHistoryResponse ok(
            final List<Long> t,
            final List<Double> o,
            final List<Double> h,
            final List<Double> l,
            final List<Double> c,
            final List<Long> v
    ) {
        return new TvHistoryResponse("ok", t, o, h, l, c, v);
    }

    public static TvHistoryResponse noData() {
        return new TvHistoryResponse(
                "no_data",
                List.of(), List.of(), List.of(), List.of(), List.of(), List.of()
        );
    }
}