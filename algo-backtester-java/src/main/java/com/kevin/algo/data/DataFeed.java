package com.kevin.algo.data;

import com.kevin.algo.core.Candle;

/**
 * DataFeed
 * --------
 * A very small interface that lets the engine/strategies read
 * price bars without caring *where* they come from (CSV, DB, API).
 *
 * hasNext(): are there more bars?
 * next()   : return the next Candle (and advance the cursor)
 */
public interface DataFeed {
    boolean hasNext();
    Candle next();
}
