package com.kevin.algo.engine;

import com.kevin.algo.models.BarOut;
import com.kevin.algo.models.EquityPoint;
import com.kevin.algo.models.Signal;

/**
 * DESIGN PATTERN: Observer (Behavioural) — Observer interface
 * ------------------------------------------------------------
 * Defines the callback contract for anything that wants to react to
 * backtest events as they happen bar-by-bar.
 *
 * How it fits here:
 *   Subject (Observable) → BacktestEngine (calls notify methods each bar)
 *   Observer             → this interface
 *   Concrete Observer    → ResultCollector (collects data for JSON output)
 *
 * Multiple listeners can be registered with engine.addListener().
 * The engine doesn't know or care what each listener does — it just fires
 * the three events: onBar, onSignal, onEquity.
 *
 * To add new behaviour (e.g. live logging, risk checks): implement this
 * interface and register with addListener(). Zero changes to the engine.
 */
public interface BacktestListener {
    void onBar(BarOut bar);
    void onSignal(Signal signal);
    void onEquity(EquityPoint point);
}
