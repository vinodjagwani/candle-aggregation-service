package com.candle.aggregator.core;

import com.candle.aggregator.config.AppProperties;
import com.candle.aggregator.domain.Interval;
import java.util.HashSet;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public final class IntervalRegistry {

    private final Interval[] intervals;

    public IntervalRegistry(final AppProperties props) {
        final List<Interval> list = props.candle().intervals().stream()
                .map(i -> new Interval(i.code(), i.seconds()))
                .toList();

        validate(list);
        this.intervals = list.toArray(Interval[]::new);
    }

    private static void validate(final List<Interval> list) {
        if (list.isEmpty()) {
            throw new IllegalStateException("No intervals configured");
        }

        final var codes = new HashSet<String>();
        final var seconds = new HashSet<Long>();

        list.forEach(i -> {
            if (!codes.add(i.code())) {
                throw new IllegalStateException("Duplicate interval code: " + i.code());
            }
            if (!seconds.add(i.seconds())) {
                throw new IllegalStateException("Duplicate interval seconds: " + i.seconds());
            }
        });
    }

    public Interval[] all() {
        return intervals;
    }
}