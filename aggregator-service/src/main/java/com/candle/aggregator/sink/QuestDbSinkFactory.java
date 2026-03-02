package com.candle.aggregator.sink;

import com.candle.aggregator.config.AppProperties;
import com.candle.aggregator.observation.AggMetrics;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public final class QuestDbSinkFactory implements CandleSinkFactory {

    private final AppProperties props;
    private final AggMetrics metrics;

    @Override
    public CandleSink create() {
        return new QuestDbSink(props.questdb().ilpConfig(), props.questdb().table(), metrics);
    }
}