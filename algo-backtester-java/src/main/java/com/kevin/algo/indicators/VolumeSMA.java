package com.kevin.algo.indicators;

import com.kevin.algo.core.Candle;
import com.kevin.algo.dsa.ArrayQueue;

/**
 * VolumeSMA — N-day simple moving average of volume (not price).
 * Does NOT extend BaseMovingAverage because that extracts bar.getClose().
 * This class extracts bar.getVolume() instead.
 *
 * Used by MomentumStrategy to confirm that current volume exceeds its
 * 20-day average (institutional participation, not a thin-market signal).
 */
public class VolumeSMA implements Indicator {

    private final int period;
    private final ArrayQueue window;
    private double sum  = 0.0;
    private int count   = 0;

    public VolumeSMA() { this(20); }

    public VolumeSMA(int period) {
        if (period <= 0) throw new IllegalArgumentException("period must be > 0");
        this.period = period;
        this.window = new ArrayQueue(period);
    }

    @Override
    public void accumulate(Candle bar) {
        double vol = (double) bar.getVolume();
        if (window.isFull()) {
            sum -= window.poll();
        }
        window.offer(vol);
        sum += vol;
        count++;
    }

    @Override
    public boolean isReady() { return count >= period; }

    @Override
    public double value() { return sum / window.size(); }
}
