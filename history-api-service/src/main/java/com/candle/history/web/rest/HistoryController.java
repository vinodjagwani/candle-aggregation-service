package com.candle.history.web.rest;

import com.candle.history.service.HistoryService;
import com.candle.history.web.dto.HistoryRequest;
import com.candle.history.web.dto.TvHistoryResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/history")
public class HistoryController {

    private final HistoryService historyService;

    @Operation(
            summary = "Get candle history",
            description = "Returns TradingView-compatible candle arrays for the requested symbol/interval and time range."
    )
    @GetMapping(value = "/candles", produces = MediaType.APPLICATION_JSON_VALUE)
    public TvHistoryResponse getHistory(
            @Valid
            @ParameterObject
            @ModelAttribute
            @Parameter(description = "Candle history query parameters") final HistoryRequest request
    ) {
        log.info("Incoming history request: {}", request);

        return historyService.getHistory(
                request.symbol(),
                request.interval(),
                Instant.ofEpochSecond(request.from()),
                Instant.ofEpochSecond(request.to()),
                request.limit()
        );
    }
}