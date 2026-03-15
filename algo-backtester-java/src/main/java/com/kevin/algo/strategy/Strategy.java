package com.kevin.algo.strategy;

import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;

import com.kevin.algo.core.Candle;
import com.kevin.algo.models.Signal;

/**
 * DESIGN PATTERN: Strategy (Behavioural)
 * ---------------------------------------
 * Defines the algorithm interface for generating trade signals.
 * Any concrete class that implements Strategy can be swapped in at runtime
 * without the engine knowing which one it is — this is the core of the
 * Strategy pattern.
 *
 * How it fits here:
 *   Context       → BacktestEngine (calls maybeSignal each bar)
 *   Strategy      → this interface
 *   ConcreteStrat → MovingAverageCrossover, MomentumStrategy, MeanReversionStrategy
 *
 * The signature uses Map<String, Double> so any number of named indicators can
 * be passed without changing this interface. Each strategy reads only the keys
 * it cares about (e.g. "fast", "slow", "rsi", "bb_lower").
 *
 * Adding a new strategy = write a new class that implements this interface,
 * then register it in StrategyFactory. Zero changes to the engine.
 */
public interface Strategy {
    Optional<Signal> maybeSignal(
        LocalDate date,
        Candle bar,
        Map<String, Double> prevVals,
        Map<String, Double> currVals,
        boolean inPosition
    );
}
