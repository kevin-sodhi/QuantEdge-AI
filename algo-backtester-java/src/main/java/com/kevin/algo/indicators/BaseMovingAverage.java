package com.kevin.algo.indicators;

import com.kevin.algo.core.Candle;

/**
 * DESIGN PATTERN: Template Method (Behavioural)
 * ----------------------------------------------
 * Defines the skeleton of the indicator algorithm in this abstract class,
 * deferring one step — update(double price) — to subclasses.
 *
 * Template method = accumulate(Candle bar):
 *   1. Extract close price from the Candle            ← fixed here
 *   2. Call update(price) — the hook                  ← overridden by SMA/EMA
 *   3. isReady() checks count vs period               ← fixed here
 *
 * Concrete subclasses (SMA, EMA) only implement update() and value();
 * they never need to re-implement the Candle unpacking or readiness logic.
 *
 * DESIGN PATTERN: also implements Indicator (Strategy / Interface Segregation)
 * The Indicator interface decouples the engine from concrete types.
 */
public abstract class BaseMovingAverage implements Indicator {
    protected final int period;
    protected int count = 0;

    public BaseMovingAverage(int period) {
        if (period <= 0) throw new IllegalArgumentException("period must be > 0");
        this.period = period;
    }

    @Override
    public void accumulate(Candle bar) {
        update(bar.getClose());
    }

    protected abstract void update(double price);

    @Override
    public boolean isReady() {
        return count >= period;
    }
}
