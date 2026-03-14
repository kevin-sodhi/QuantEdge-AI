package com.kevin.algo.engine;

import java.util.ArrayList;
import java.util.List;

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
 * Provides one simple entry point — run(BacktestConfig) — that internally
 * coordinates data feeding, indicator updates, signal generation, portfolio
 * management, and event notification. Callers don't need to know the order
 * or details of these steps; they just call run().
 *
 * DESIGN PATTERN: Observer (Behavioural) — Subject / Observable
 * --------------------------------------------------------------
 * The engine maintains a list of BacktestListeners and fires three events
 * on every bar: onBar, onSignal (when a signal fires), onEquity.
 * Listeners are registered via addListener() — the engine doesn't know
 * or care what they do with the events.
 *
 * DESIGN PATTERN: Strategy (usage)
 * ----------------------------------
 * The engine accepts a Strategy interface, not a concrete class.
 * Swapping SMA → EMA or MACrossover → RSI strategy requires no engine changes.
 *
 * DESIGN PATTERN: Builder (usage)
 * ---------------------------------
 * The convenience overload run(BacktestConfig) unpacks the builder-constructed
 * config so that Main.java never has to call the 5-argument overload directly.
 */
public class BacktestEngine {

    private final List<BacktestListener> listeners = new ArrayList<>();

    public BacktestEngine addListener(BacktestListener listener) {
        listeners.add(listener);
        return this;
    }

    /** Convenience overload that accepts a BacktestConfig (Builder pattern). */
    public void run(BacktestConfig cfg) {
        run(cfg.feed, cfg.fast, cfg.slow, cfg.strategy, cfg.portfolio);
    }

    public void run(DataFeed feed, Indicator fast, Indicator slow, Strategy strat, Portfolio pf) {
        Double prevFast = null, prevSlow = null;

        while (feed.hasNext()) {
            // One Candle = one row = one day of OHLCV data
            Candle c = feed.next();
            fast.accumulate(c);
            slow.accumulate(c);

            Double fNow = fast.isReady() ? fast.value() : null;
            Double sNow = slow.isReady() ? slow.value() : null;

            // Build bar output with generic indicator map
            BarOut bar = new BarOut(c.getDate(), c.getOpen(), c.getHigh(), c.getLow(), c.getClose())
                    .addIndicator("fast", fNow)
                    .addIndicator("slow", sNow);
            listeners.forEach(l -> l.onBar(bar));

            // Generate signal when indicators are ready
            strat.maybeSignal(c.getDate(), c.getClose(), prevFast, prevSlow, fNow, sNow, pf.inPosition())
                .ifPresent(sig -> {
                    switch (sig.type) {
                        case BUY  -> pf.onBuy(sig.date, sig.price);
                        case SELL -> pf.onSell(sig.date, sig.price);
                    }
                    listeners.forEach(l -> l.onSignal(sig));
                });

            EquityPoint ep = new EquityPoint(c.getDate(), pf.equityAt(c.getClose()));
            listeners.forEach(l -> l.onEquity(ep));

            prevFast = fNow;
            prevSlow = sNow;
        }
    }
}
