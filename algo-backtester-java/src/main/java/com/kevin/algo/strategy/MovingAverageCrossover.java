package com.kevin.algo.strategy;

import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;

import com.kevin.algo.core.Candle;
import com.kevin.algo.models.Signal;

/**
 * DESIGN PATTERN: Strategy (Behavioural) — Concrete Strategy
 * -----------------------------------------------------------
 * MA-crossover: reads "fast" and "slow" from the named indicator maps.
 *
 * Signal rules:
 *   BUY  → fast MA crosses above slow MA (prevDiff ≤ 0, currDiff > 0)
 *   SELL → fast MA crosses below slow MA (prevDiff ≥ 0, currDiff < 0)
 */
public class MovingAverageCrossover implements Strategy {

    @Override
    public Optional<Signal> maybeSignal(LocalDate date, Candle bar,
                                        Map<String, Double> prevVals,
                                        Map<String, Double> currVals,
                                        boolean inPosition) {
        Double fastPrev = prevVals.get("fast");
        Double slowPrev = prevVals.get("slow");
        Double fastNow  = currVals.get("fast");
        Double slowNow  = currVals.get("slow");

        if (fastPrev == null || slowPrev == null ||
            fastNow == null  || slowNow == null) return Optional.empty();

        double prevDiff = fastPrev - slowPrev;
        double currDiff = fastNow  - slowNow;

        if (prevDiff <= 0 && currDiff > 0 && !inPosition)
            return Optional.of(new Signal(date, bar.getClose(), Signal.Type.BUY));
        if (prevDiff >= 0 && currDiff < 0 && inPosition)
            return Optional.of(new Signal(date, bar.getClose(), Signal.Type.SELL));

        return Optional.empty();
    }
}