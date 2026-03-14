package com.kevin.algo.engine;

import com.kevin.algo.data.DataFeed;
import com.kevin.algo.indicators.Indicator;
import com.kevin.algo.portfolio.Portfolio;
import com.kevin.algo.strategy.Strategy;

/**
 * DESIGN PATTERN: Builder (Creational)
 * --------------------------------------
 * Constructs an immutable BacktestConfig object step-by-step instead of
 * passing 5+ parameters in one go to the engine's run() method.
 *
 * Why Builder here?
 *   BacktestEngine.run() used to take (DataFeed, Indicator, Indicator, Strategy, Portfolio).
 *   With 5 parameters all of similar types, callers could silently pass them in the
 *   wrong order. Builder gives each field a named setter, making the call-site readable
 *   and enforcing that all fields are set before build() returns.
 *
 * Structure:
 *   BacktestConfig        → immutable product (all fields are final)
 *   BacktestConfig.Builder → inner builder with fluent setters + build()
 *
 * build() validates that no field is null, throwing early rather than
 * letting the engine fail mid-run with a NullPointerException.
 */
public class BacktestConfig {

    public final DataFeed feed;
    public final Indicator fast;
    public final Indicator slow;
    public final Strategy strategy;
    public final Portfolio portfolio;

    private BacktestConfig(Builder b) {
        this.feed = b.feed;
        this.fast = b.fast;
        this.slow = b.slow;
        this.strategy = b.strategy;
        this.portfolio = b.portfolio;
    }

    public static class Builder {
        private DataFeed feed;
        private Indicator fast;
        private Indicator slow;
        private Strategy strategy;
        private Portfolio portfolio;

        public Builder feed(DataFeed f)       { this.feed = f;       return this; }
        public Builder fast(Indicator i)      { this.fast = i;       return this; }
        public Builder slow(Indicator i)      { this.slow = i;       return this; }
        public Builder strategy(Strategy s)   { this.strategy = s;   return this; }
        public Builder portfolio(Portfolio p) { this.portfolio = p;  return this; }

        public BacktestConfig build() {
            if (feed == null || fast == null || slow == null || strategy == null || portfolio == null)
                throw new IllegalStateException("All BacktestConfig fields must be set");
            return new BacktestConfig(this);
        }
    }
}
