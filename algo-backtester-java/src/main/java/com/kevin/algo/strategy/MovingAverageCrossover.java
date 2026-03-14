package com.kevin.algo.strategy;

import java.time.LocalDate;
import java.util.Optional;

import com.kevin.algo.models.Signal;

/**
 * DESIGN PATTERN: Strategy (Behavioural) — Concrete Strategy
 * -----------------------------------------------------------
 * This is one concrete implementation of the Strategy interface.
 * It encapsulates the MA-crossover signal logic so it can be swapped
 * with any other Strategy without touching the engine.
 *
 * Signal rules:
 *   BUY  → fast MA crosses above slow MA (prevDiff ≤ 0, currDiff > 0)
 *   SELL → fast MA crosses below slow MA (prevDiff ≥ 0, currDiff < 0)
 */
public class MovingAverageCrossover implements Strategy {

    public Optional<Signal> maybeSignal(LocalDate date, double close,
                                        Double fastPrev, Double slowPrev,
                                        Double fastNow, Double slowNow,
                                        boolean inPosition) {
        if (fastPrev == null || slowPrev == null ||
            fastNow == null  || slowNow == null) return Optional.empty();

        double prevDiff = fastPrev - slowPrev;
        double currDiff = fastNow - slowNow;

        if (prevDiff <= 0 && currDiff > 0 && !inPosition)
            return Optional.of(new Signal(date, close, Signal.Type.BUY));
        if (prevDiff >= 0 && currDiff < 0 && inPosition)
            return Optional.of(new Signal(date, close, Signal.Type.SELL));

        return Optional.empty();
    }
}