package com.candle.aggregator.sim;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

public final class PriceEngine {

    private final Map<String, Double> midBySymbol = new ConcurrentHashMap<>();
    private final Random random = new Random();

    private final double basePrice;
    private final double volatility;

    public PriceEngine(final double basePrice, final double volatility, final List<String> symbols) {
        this.basePrice = basePrice;
        this.volatility = volatility;
        symbols.forEach(symbol -> midBySymbol.put(symbol, basePrice * (0.9 + random.nextDouble() * 0.2)));
    }

    public double nextMid(final String symbol) {
        final double current = midBySymbol.getOrDefault(symbol, basePrice);
        final double delta = random.nextGaussian() * volatility;
        final double next = Math.max(0.01, current + delta);
        midBySymbol.put(symbol, next);
        return next;
    }
}