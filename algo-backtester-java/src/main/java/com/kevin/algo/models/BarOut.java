package com.kevin.algo.models;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * DESIGN PATTERN: Fluent Interface / Method Chaining (Creational idiom)
 * -----------------------------------------------------------------------
 * BarOut uses addIndicator() returning `this` so the engine can build a
 * fully populated bar in a readable one-liner:
 *
 *   new BarOut(date, o, h, l, c)
 *       .addIndicator("fast", fNow)
 *       .addIndicator("slow", sNow);
 *
 * Without method chaining, this would need 3 separate statements per bar.
 *
 * DESIGN PATTERN: Generic Map for open/closed extensibility
 * ----------------------------------------------------------
 * Indicators are stored in a Map<String, Double> rather than named fields
 * (fastSma, slowSma). This means BarOut works for any number of indicators
 * with any names — the class never needs to change when new indicators are added.
 *
 * Note: LinkedHashMap preserves insertion order so JSON output stays consistent.
 */
public class BarOut {
    public LocalDate date;
    public double open, high, low, close;
    public Map<String, Double> indicators = new LinkedHashMap<>();

    public BarOut(LocalDate date, double open, double high, double low, double close) {
        this.date = date;
        this.open = open;
        this.high = high;
        this.low = low;
        this.close = close;
    }

    public BarOut addIndicator(String name, Double value) {
        indicators.put(name, value);
        return this;
    }
}
