package com.kevin.algo.indicators;

import com.kevin.algo.core.Candle;

/**
 * Indicator
 * ---------
 * Common interface so strategies/engine can treat all indicators uniformly.
 *
 * accumulate(bar): ingest the next Candle and update internal state
 * isReady()      : returns true when indicator has enough data to be meaningful
 * value()        : returns the current indicator numeric value
 */
public interface Indicator {
    void accumulate(Candle bar);
    boolean isReady();
    double value();
}

