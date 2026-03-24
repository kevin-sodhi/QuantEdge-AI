package com.kevin.algo.engine;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.kevin.algo.core.Candle;
import com.kevin.algo.data.DataFeed;
import com.kevin.algo.indicators.Indicator;
import com.kevin.algo.models.BarOut;
import com.kevin.algo.models.EquityPoint;
import com.kevin.algo.portfolio.Portfolio;
import com.kevin.algo.strategy.Strategy;

/**
 * DESIGN PATTERN: Facade (Structural)
 * -------------------------------------
 * Provides one simple entry point — run(BacktestConfig) — that coordinates
 * data feeding, indicator updates, signal generation, portfolio management,
 * and event notification.
 *
 * DESIGN PATTERN: Observer (Behavioural) — Subject / Observable
 * --------------------------------------------------------------
 * Fires onBar, onSignal, onEquity events to registered BacktestListeners.
 *
 * The engine now iterates over a Map<String, Indicator> per bar, building
 * prevVals and currVals maps that are passed to the strategy. This lets any
 * strategy read any named indicator without engine changes.
 */
public class BacktestEngine {

    private final List<BacktestListener> listeners = new ArrayList<>();

    public BacktestEngine addListener(BacktestListener listener) {
        listeners.add(listener);
        return this;
    }

    public void run(BacktestConfig cfg) {
        run(cfg.feed, cfg.indicators, cfg.strategy, cfg.portfolio);
    }

    public void run(DataFeed feed, Map<String, Indicator> indicators, Strategy strat, Portfolio pf) {
        // prevVals starts empty — strategies must null-check before using
        Map<String, Double> prevVals = new HashMap<>();

        while (feed.hasNext()) {
            Candle c = feed.next();

            // 1) Accumulate every indicator for this bar
            for (Indicator ind : indicators.values()) {
                ind.accumulate(c);
            }

            // 2) Build currVals: null for indicators not yet warmed up
            Map<String, Double> currVals = new HashMap<>();
            for (Map.Entry<String, Indicator> entry : indicators.entrySet()) {
                currVals.put(entry.getKey(),
                    entry.getValue().isReady() ? entry.getValue().value() : null);
            }

            // 3) Build BarOut with all current indicator values
            BarOut bar = new BarOut(c.getDate(), c.getOpen(), c.getHigh(), c.getLow(), c.getClose());
            currVals.forEach(bar::addIndicator);
            listeners.forEach(l -> l.onBar(bar));

            // 4) Ask strategy for a signal
            strat.maybeSignal(c.getDate(), c, prevVals, currVals, pf.inPosition())
                .ifPresent(sig -> {
                    switch (sig.type) {
                        case BUY  -> pf.onBuy(sig.date, sig.price);
                        case SELL -> pf.onSell(sig.date, sig.price);
                    }
                    listeners.forEach(l -> l.onSignal(sig));
                });

            // 5) Record equity
            EquityPoint ep = new EquityPoint(c.getDate(), pf.equityAt(c.getClose()));
            listeners.forEach(l -> l.onEquity(ep));

            // 6) Shift: currVals becomes prevVals for next bar
            prevVals = new HashMap<>(currVals);
        }
    }
}
