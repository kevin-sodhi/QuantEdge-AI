package com.kevin.algo.data;

import java.util.List;

import com.kevin.algo.core.Candle;

/**
 * ListDataFeed
 * ------------
 * DataFeed implementation that wraps an in-memory List<Candle>.
 * Used by WalkForwardValidator to run backtests on train/test sublists
 * without touching the file system again.
 */
public class ListDataFeed implements DataFeed {

    private final List<Candle> candles;
    private int cursor = 0;

    public ListDataFeed(List<Candle> candles) {
        this.candles = candles;
    }

    @Override
    public boolean hasNext() { return cursor < candles.size(); }

    @Override
    public Candle next() { return candles.get(cursor++); }
}
