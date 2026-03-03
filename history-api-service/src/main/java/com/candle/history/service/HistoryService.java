package com.candle.history.service;

import com.candle.history.config.AppProperties;
import com.candle.history.repository.CandleRepository;
import com.candle.history.repository.entity.Candle;
import com.candle.history.web.dto.TvHistoryResponse;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class HistoryService {

    private final CandleRepository candleRepository;
    private final AppProperties applicationProperties;

    public TvHistoryResponse getHistory(
            final String symbol,
            final String interval,
            final Instant requestedFrom,
            final Instant requestedTo,
            final int requestedLimit
    ) {
        final var normalizedFrom = requestedFrom.isAfter(requestedTo) ? requestedTo : requestedFrom;
        final var normalizedTo = requestedFrom.isAfter(requestedTo) ? requestedFrom : requestedTo;

        final var effectiveLimit =
                Math.clamp(requestedLimit, 1, applicationProperties.candles().maxPoints());

        final var candles =
                candleRepository.findCandles(symbol, interval, normalizedFrom, normalizedTo, effectiveLimit);

        if (candles.isEmpty()) {
            return TvHistoryResponse.noData();
        }

        final var sortedCandles = new ArrayList<>(candles);
        sortedCandles.sort(Comparator.comparing(Candle::timestamp));

        final var candleCount = sortedCandles.size();

        final var timestamps = new ArrayList<Long>(candleCount);
        final var opens = new ArrayList<Double>(candleCount);
        final var highs = new ArrayList<Double>(candleCount);
        final var lows = new ArrayList<Double>(candleCount);
        final var closes = new ArrayList<Double>(candleCount);
        final var volumes = new ArrayList<Long>(candleCount);

        sortedCandles.forEach(candle -> {
            timestamps.add(candle.timestamp().getEpochSecond());
            opens.add(candle.open());
            highs.add(candle.high());
            lows.add(candle.low());
            closes.add(candle.close());
            volumes.add(candle.volume());
        });

        return TvHistoryResponse.ok(timestamps, opens, highs, lows, closes, volumes);
    }
}