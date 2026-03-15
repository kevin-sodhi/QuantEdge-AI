package com.kevin.algo.indicators;

import com.kevin.algo.core.Candle;

/**
 * RSI (Relative Strength Index) — Wilder's Smoothing Method
 * ----------------------------------------------------------
 * Period: 14 (configurable).
 *
 * Algorithm:
 *   Warm-up phase (first `period` price changes):
 *     Accumulate raw gains and losses, seed avgGain/avgLoss as SMA.
 *   Live phase (after warm-up):
 *     avgGain = (avgGain * (period-1) + gain) / period  — Wilder smoothing
 *     avgLoss = (avgLoss * (period-1) + loss) / period
 *
 *   RSI = 100 - (100 / (1 + RS))   where RS = avgGain / avgLoss
 *
 * No lookahead: each call to accumulate() uses only past and current close.
 */
public class RSI implements Indicator {

    private final int period;
    private double prevClose = 0;
    private double sumGain   = 0;
    private double sumLoss   = 0;
    private double avgGain   = 0;
    private double avgLoss   = 0;
    private int count        = 0;
    private boolean ready    = false;

    public RSI(int period) {
        if (period <= 0) throw new IllegalArgumentException("RSI period must be > 0");
        this.period = period;
    }

    @Override
    public void accumulate(Candle bar) {
        double close = bar.getClose();

        // First bar: store prevClose, nothing to compute yet
        if (count == 0) {
            prevClose = close;
            count++;
            return;
        }

        double change = close - prevClose;
        double gain   = change > 0 ?  change : 0.0;
        double loss   = change < 0 ? -change : 0.0;

        if (!ready) {
            // Accumulate raw sums for warm-up
            sumGain += gain;
            sumLoss += loss;
            count++;
            // Once we have `period` changes, seed Wilder's averages
            if (count > period) {
                avgGain = sumGain / period;
                avgLoss = sumLoss / period;
                ready = true;
            }
        } else {
            // Wilder's smoothing
            avgGain = (avgGain * (period - 1) + gain) / period;
            avgLoss = (avgLoss * (period - 1) + loss) / period;
        }

        prevClose = close;
    }

    @Override
    public boolean isReady() { return ready; }

    @Override
    public double value() {
        if (avgLoss == 0.0) return 100.0;
        double rs = avgGain / avgLoss;
        return 100.0 - (100.0 / (1.0 + rs));
    }
}
