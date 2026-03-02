package com.candle.history.repository;

import com.candle.history.repository.entity.Candle;
import java.time.Instant;
import java.util.List;

public interface CandleRepository {

    List<Candle> findCandles(String symbol, String interval, Instant from, Instant to, int limit);
}