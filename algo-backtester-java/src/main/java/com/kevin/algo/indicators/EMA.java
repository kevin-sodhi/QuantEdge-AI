package com.kevin.algo.indicators;

/**
 * DESIGN PATTERN: Template Method (Behavioural) — Concrete implementation
 * -------------------------------------------------------------------------
 * Extends BaseMovingAverage and provides the concrete update() hook for EMA.
 * The warm-up and exponential-smoothing phases both live in update() only;
 * all other lifecycle steps (accumulate, isReady) are inherited unchanged.
 *
 * FORMULA: EMA_t = alpha * price_t + (1 - alpha) * EMA_{t-1}
 *   alpha = 2 / (period + 1)
 *
 * Two-phase algorithm inside update():
 *   Phase 1 (warm-up): accumulate first `period` prices and compute SMA seed.
 *   Phase 2 (live):    apply exponential smoothing on every subsequent price.
 *
 * This is an O(1) per-bar algorithm — no window array needed.
 */
public class EMA extends BaseMovingAverage {

    private final double alpha;
    private double ema = 0.0;
    private double seedSum = 0.0; // accumulates prices during warm-up

    public EMA(int period) {
        super(period);
        this.alpha = 2.0 / (period + 1);
    }

    @Override
    protected void update(double price) {
        if (count < period) {
            // Warm-up: accumulate prices for seed SMA
            seedSum += price;
            count++;
            if (count == period) {
                ema = seedSum / period; // seed with SMA
            }
        } else {
            ema = alpha * price + (1 - alpha) * ema;
        }
    }

    @Override
    public double value() {
        return isReady() ? ema : Double.NaN;
    }
}
