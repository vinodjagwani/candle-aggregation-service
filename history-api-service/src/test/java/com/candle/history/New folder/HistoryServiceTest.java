package com.candle.history.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.candle.history.config.AppProperties;
import com.candle.history.repository.CandleRepository;
import com.candle.history.repository.entity.Candle;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
final class HistoryServiceTest {

    @Mock
    private CandleRepository candleRepository;

    @Mock
    private AppProperties applicationProperties;

    @Mock
    private AppProperties.Candles candlesProperties;

    @InjectMocks
    private HistoryService historyService;

    @Test
    @DisplayName("TestGetHistoryNoDataReturnsNoData")
    void testGetHistoryNoDataReturnsNoData() {
        when(applicationProperties.candles()).thenReturn(candlesProperties);
        when(candlesProperties.maxPoints()).thenReturn(1000);

        when(candleRepository.findCandles(
                eq("BTCUSDT"),
                eq("1m"),
                eq(Instant.ofEpochSecond(100)),
                eq(Instant.ofEpochSecond(200)),
                eq(10)
        )).thenReturn(new ArrayList<>());

        final var response = historyService.getHistory(
                "BTCUSDT",
                "1m",
                Instant.ofEpochSecond(100),
                Instant.ofEpochSecond(200),
                10
        );

        assertThat(response.s()).isEqualTo("no_data");
        assertThat(response.t()).isEmpty();
        assertThat(response.o()).isEmpty();
        assertThat(response.h()).isEmpty();
        assertThat(response.l()).isEmpty();
        assertThat(response.c()).isEmpty();
        assertThat(response.v()).isEmpty();
    }

    @Test
    @DisplayName("TestGetHistorySwapsFromToWhenFromAfterTo")
    void testGetHistorySwapsFromToWhenFromAfterTo() {
        when(applicationProperties.candles()).thenReturn(candlesProperties);
        when(candlesProperties.maxPoints()).thenReturn(1000);

        final var requestedFrom = Instant.ofEpochSecond(200);
        final var requestedTo = Instant.ofEpochSecond(100);

        final var returnedCandles = new ArrayList<>(List.of(
                new Candle(
                        "BTCUSDT",
                        "1m",
                        1.0, 2.0, 0.5, 1.5,
                        100L,
                        Instant.ofEpochSecond(150)
                )
        ));

        when(candleRepository.findCandles(
                eq("BTCUSDT"),
                eq("1m"),
                eq(requestedTo),
                eq(requestedFrom),
                eq(10)
        )).thenReturn(returnedCandles);

        historyService.getHistory("BTCUSDT", "1m", requestedFrom, requestedTo, 10);

        verify(candleRepository).findCandles(
                eq("BTCUSDT"),
                eq("1m"),
                eq(requestedTo),
                eq(requestedFrom),
                eq(10)
        );
    }

    @Test
    @DisplayName("TestGetHistoryClampsLimitToMaxPoints")
    void testGetHistoryClampsLimitToMaxPoints() {
        when(applicationProperties.candles()).thenReturn(candlesProperties);
        when(candlesProperties.maxPoints()).thenReturn(100);

        final var returnedCandles = new ArrayList<>(List.of(
                new Candle(
                        "BTCUSDT",
                        "1m",
                        1.0, 2.0, 0.5, 1.5,
                        100L,
                        Instant.ofEpochSecond(150)
                )
        ));

        when(candleRepository.findCandles(
                eq("BTCUSDT"),
                eq("1m"),
                eq(Instant.ofEpochSecond(100)),
                eq(Instant.ofEpochSecond(200)),
                eq(100)
        )).thenReturn(returnedCandles);

        historyService.getHistory(
                "BTCUSDT",
                "1m",
                Instant.ofEpochSecond(100),
                Instant.ofEpochSecond(200),
                10_000
        );

        verify(candleRepository).findCandles(
                eq("BTCUSDT"),
                eq("1m"),
                eq(Instant.ofEpochSecond(100)),
                eq(Instant.ofEpochSecond(200)),
                eq(100)
        );
    }

    @Test
    @DisplayName("TestGetHistorySortsByTimestampAndMapsArrays")
    void testGetHistorySortsByTimestampAndMapsArrays() {
        when(applicationProperties.candles()).thenReturn(candlesProperties);
        when(candlesProperties.maxPoints()).thenReturn(1000);

        final var candleLater = new Candle(
                "BTCUSDT",
                "1m",
                2.0, 3.0, 1.0, 2.5,
                200L,
                Instant.ofEpochSecond(200)
        );

        final var candleEarlier = new Candle(
                "BTCUSDT",
                "1m",
                1.0, 2.0, 0.5, 1.5,
                100L,
                Instant.ofEpochSecond(100)
        );

        final var returnedCandles = new ArrayList<>(List.of(candleLater, candleEarlier));

        when(candleRepository.findCandles(
                eq("BTCUSDT"),
                eq("1m"),
                eq(Instant.ofEpochSecond(0)),
                eq(Instant.ofEpochSecond(300)),
                eq(2)
        )).thenReturn(returnedCandles);

        final var response = historyService.getHistory(
                "BTCUSDT",
                "1m",
                Instant.ofEpochSecond(0),
                Instant.ofEpochSecond(300),
                2
        );

        assertThat(response.s()).isEqualTo("ok");

        assertThat(response.t()).containsExactly(100L, 200L);

        assertThat(response.o()).containsExactly(1.0, 2.0);
        assertThat(response.h()).containsExactly(2.0, 3.0);
        assertThat(response.l()).containsExactly(0.5, 1.0);
        assertThat(response.c()).containsExactly(1.5, 2.5);
        assertThat(response.v()).containsExactly(100L, 200L);
    }
}