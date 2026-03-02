package com.candle.aggregator.core;

import com.candle.aggregator.domain.BidAskEvent;
import java.util.List;

public sealed interface CandleAggregator permits CandleShard {

    List<CandleShard.Finalized> onTick(BidAskEvent event);
}