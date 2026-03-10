package com.kevin.algo.indicators;

import java.util.LinkedList;
import java.util.Queue;

/**
 * Simple Moving Average (rolling window)
 * FORMULA FOR SMA 
 *      SMA = sum of last N closing prices / N
 *  Where N = the period (e.g. 3 for fast, 5 for slow).
 */
public class SMA {

    private final int period;
    private final Queue<Double> window = new LinkedList<>();
    private double sum = 0.0;

    public SMA(int period) { this.period = period; }

    /** Add a new value and update the moving average 
    Day 1:  close=186.3  →  window: [186.3]               not ready (need 3+ for fast)
    Day 2:  close=188.1  →  window: [186.3, 188.1]         not ready
    Day 3:  close=190.5  →  window: [186.3, 188.1, 190.5]  READY → value = 188.3
    Day 4:  close=192.0  →  window: [188.1, 190.5, 192.0]  oldest dropped, new avg 
    */
    public void add(double price) {
        sum += price; // add new price to total
        window.add(price); // add to back of queue

        if (window.size() > period) { // here the period is the user input of SMA (3 or 4)
            sum -= window.remove(); // // remove oldest price from total and queue
        }
    }
    /**.  Step by step with fast SMA (User Input period=3):
        Day 1: price=186.3 -> sum=186.3,  window=[186.3]               size=1, not ready
        Day 2: price=188.1 -> sum=374.4,  window=[186.3, 188.1]        size=2, not ready
        Day 3: price=190.5 -> sum=564.9,  window=[186.3, 188.1, 190.5] size=3, READY
                                    SMA = 564.9 / 3 = 188.3
        Day 4: price=192.0 -> sum=570.6   window=[186.3, 188.1, 190.5, 192.0] size=4 > 3
                    remove 186.3 →  sum=384.3, window=[188.1, 190.5, 192.0]
                                    SMA = 384.3 / 3 = 128.1
 
     */



    /** Convenience overload if Candle class calls this */
    public void accumulate(com.kevin.algo.core.Candle bar) {
        add(bar.getClose());
    }

    /** Is SMA ready (enough samples)? */
    public boolean isReady() {   // if the size of the linkedlist is > user input of Days/Period
        return window.size() >= period;
    }

    /** Current SMA value; NaN if not ready 
     *   FORMULA FOR SMA 
            SMA = sum of last N closing prices / N
    */
    public double value() {
        return isReady() ? sum / window.size() : Double.NaN;
    }

    /** Optional: last n samples for debug */
    public int size() { return window.size(); }
}