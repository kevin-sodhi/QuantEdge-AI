package com.kevin.algo.indicators;

import com.kevin.algo.dsa.ArrayQueue;

/**
 * DESIGN PATTERN: Template Method (Behavioural) — Concrete implementation
 * -------------------------------------------------------------------------
 * Extends BaseMovingAverage and provides the concrete update() hook.
 * BaseMovingAverage.accumulate() calls update() — SMA fills in that step.
 *
 * Data structure used: ArrayQueue (circular ring buffer)
 *   - O(1) enqueue and dequeue (vs LinkedList node allocation each bar)
 *   - Fixed-size array, no GC pressure during the main loop
 *
 * FORMULA:  SMA = sum of last N closing prices / N
 *   where N = period (e.g. 3 for fast MA, 5 for slow MA)
 *
 * PATTERN: Circular Buffer via ArrayQueue
 *   When the window is full, the oldest price is evicted before the new
 *   one is added. The running sum is maintained incrementally to avoid
 *   re-summing the window on every bar.
 */
public class SMA extends BaseMovingAverage {

    private final ArrayQueue window;
    private double sum = 0.0;

    public SMA(int period) {
        super(period);
        this.window = new ArrayQueue(period);
    }

    /**
     * Update with a new price.
     * Day 1:  close=186.3  →  window: [186.3]               not ready (need 3)
     * Day 2:  close=188.1  →  window: [186.3, 188.1]         not ready
     * Day 3:  close=190.5  →  window: [186.3, 188.1, 190.5]  READY → value = 188.3
     * Day 4:  close=192.0  →  window: [188.1, 190.5, 192.0]  oldest dropped, new avg
     */
    @Override
    protected void update(double price) {
        if (window.isFull()) {
            sum -= window.poll(); // evict oldest price
        }
        window.offer(price);
        sum += price;
        if (count < period) count++;
    }

    @Override
    public double value() {
        return isReady() ? sum / window.size() : Double.NaN;
    }
}
