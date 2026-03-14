package com.kevin.algo.engine;

import java.util.ArrayList;
import java.util.List;

import com.kevin.algo.models.BarOut;
import com.kevin.algo.models.EquityPoint;
import com.kevin.algo.models.Signal;

/**
 * DESIGN PATTERN: Observer (Behavioural) — Concrete Observer
 * -----------------------------------------------------------
 * Implements BacktestListener to passively collect every event the engine fires.
 * After the run completes, Main.java reads the three public lists to build JSON.
 *
 * Why Observer here?
 *   Without it, BacktestEngine would need to know about JSON output, series
 *   collection, and equity tracking — mixing concerns. With Observer, the engine
 *   stays focused on looping over bars; collection is the listener's job.
 *
 * Three event handlers:
 *   onBar(BarOut)       → appends to series list (OHLC + indicator values)
 *   onSignal(Signal)    → appends to signals list (BUY / SELL events)
 *   onEquity(EquityPoint) → appends to equity list (portfolio value over time)
 */
public class ResultCollector implements BacktestListener {

    public final List<BarOut> series = new ArrayList<>();
    public final List<Signal> signals = new ArrayList<>();
    public final List<EquityPoint> equity = new ArrayList<>();

    @Override
    public void onBar(BarOut bar) {
        series.add(bar);
    }

    @Override
    public void onSignal(Signal signal) {
        signals.add(signal);
    }

    @Override
    public void onEquity(EquityPoint point) {
        equity.add(point);
    }
}
