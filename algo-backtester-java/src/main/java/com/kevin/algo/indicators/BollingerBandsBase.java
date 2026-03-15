package com.kevin.algo.indicators;

import com.kevin.algo.core.Candle;
import com.kevin.algo.dsa.ArrayQueue;

/**
 * BollingerBandsBase
 * ------------------
 * Abstract base for BollingerBandsUpper, BollingerBandsMiddle, BollingerBandsLower.
 * Maintains a rolling window of `period` closing prices using ArrayQueue and
 * tracks rollingSum + rollingSumSq for O(1) mean and std-dev per bar.
 *
 * Subclasses only override value() to return their specific band:
 *   Upper  = mean + multiplier * stdDev
 *   Middle = mean
 *   Lower  = mean - multiplier * stdDev
 *
 * Multiplier defaults to 2.0 (standard Bollinger Bands).
 * Period defaults to 20.
 */
abstract class BollingerBandsBase implements Indicator {

    private final ArrayQueue window;
    private double rollingSum   = 0.0;
    private double rollingSumSq = 0.0;
    private int count           = 0;
    protected final int period;
    protected final double multiplier;

    BollingerBandsBase(int period, double multiplier) {
        if (period <= 0) throw new IllegalArgumentException("period must be > 0");
        this.period     = period;
        this.multiplier = multiplier;
        this.window     = new ArrayQueue(period);
    }

    BollingerBandsBase() {
        this(20, 2.0);
    }

    @Override
    public void accumulate(Candle bar) {
        double price = bar.getClose();
        if (window.isFull()) {
            double oldest = window.poll();
            rollingSum   -= oldest;
            rollingSumSq -= oldest * oldest;
        }
        window.offer(price);
        rollingSum   += price;
        rollingSumSq += price * price;
        count++;
    }

    @Override
    public boolean isReady() { return count >= period; }

    protected double mean() {
        return rollingSum / window.size();
    }

    protected double stdDev() {
        int n = window.size();
        double m = mean();
        double variance = (rollingSumSq / n) - (m * m);
        return Math.sqrt(Math.max(0.0, variance));
    }
}
