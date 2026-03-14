package com.kevin.algo.indicators;

/**
 * DESIGN PATTERN: Factory Method (Creational)
 * --------------------------------------------
 * Centralises object creation for Indicator implementations.
 * Callers receive an Indicator interface — they never depend on SMA/EMA directly.
 *
 * How it fits here:
 *   Factory   → IndicatorFactory.create(name, period)
 *   Product   → Indicator interface
 *   Concrete  → SMA, EMA (and future indicators)
 *
 * To add a new indicator: add a new case here and write its class.
 * The engine, strategy, and Main don't need to change.
 */
public class IndicatorFactory {

    public static Indicator create(String name, int period) {
        return switch (name.toLowerCase()) {
            case "sma" -> new SMA(period);
            case "ema" -> new EMA(period);
            default -> throw new IllegalArgumentException("Unknown indicator: " + name);
        };
    }
}
