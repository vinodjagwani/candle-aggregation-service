package com.candle.history.repository.impl;

import com.candle.history.config.AppProperties;
import com.candle.history.exception.BusinessServiceException;
import com.candle.history.exception.dto.ErrorCodeEnum;
import com.candle.history.repository.CandleRepository;
import com.candle.history.repository.entity.Candle;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Slf4j
@Repository
@RequiredArgsConstructor
public class QuestDbCandleRepository implements CandleRepository {

    private static final String FIND_CANDLES_QUERY_TEMPLATE = """
            SELECT symbol, interval, open, high, low, close, volume, timestamp
            FROM %s
            WHERE symbol = :symbol
              AND interval = :interval
              AND timestamp >= :from
              AND timestamp <= :to
            ORDER BY timestamp ASC
            LIMIT :limit
            """;

    private final JdbcClient jdbcClient;
    private final AppProperties applicationProperties;

    @Override
    public List<Candle> findCandles(
            final String symbol,
            final String interval,
            final Instant from,
            final Instant to,
            final int limit
    ) {

        final var tableName = applicationProperties.candles().table();
        final var formattedQuery = FIND_CANDLES_QUERY_TEMPLATE.formatted(tableName);

        log.debug(
                "Executing QuestDB candle query | tableName={} | symbol={} | interval={} | from={} | to={} | limit={}",
                tableName, symbol, interval, from, to, limit
        );

        try {
            final var candles = jdbcClient.sql(formattedQuery)
                    .param("symbol", symbol)
                    .param("interval", interval)
                    .param("from", Timestamp.from(from))
                    .param("to", Timestamp.from(to))
                    .param("limit", limit)
                    .query((resultSet, _) -> new Candle(
                            resultSet.getString("symbol"),
                            resultSet.getString("interval"),
                            resultSet.getDouble("open"),
                            resultSet.getDouble("high"),
                            resultSet.getDouble("low"),
                            resultSet.getDouble("close"),
                            resultSet.getLong("volume"),
                            resultSet.getTimestamp("timestamp").toInstant()
                    ))
                    .list();

            log.debug(
                    "QuestDB candle query completed | rowsFetched={}", candles.size());

            return candles;

        } catch (final Exception ex) {

            log.error(
                    "QuestDB candle query failed | tableName={} | symbol={} | interval={} | from={} | to={} | limit={}",
                    tableName, symbol, interval, from, to, limit, ex
            );

            throw new BusinessServiceException(ErrorCodeEnum.DB_QUERY, ex.getMessage());
        }
    }
}