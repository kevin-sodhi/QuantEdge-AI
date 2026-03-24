package com.kevin.algo.engine;

import java.util.LinkedHashMap;
import java.util.Map;

import com.kevin.algo.data.DataFeed;
import com.kevin.algo.indicators.Indicator;
import com.kevin.algo.portfolio.Portfolio;
import com.kevin.algo.strategy.Strategy;

/**
 * DESIGN PATTERN: Builder (Creational)
 * --------------------------------------
 * Constructs an immutable BacktestConfig step-by-step. The indicators field
 * is now a Map<String, Indicator> so any number of named indicators can be
 * passed — enabling multi-indicator strategies (Momentum, Mean Reversion).
 *
 * The Builder provides both a bulk indicators(Map) setter and an addIndicator
 * convenience method for fluent one-by-one registration.
 */
public class BacktestConfig {

    public final DataFeed feed;
    public final Map<String, Indicator> indicators;
    public final Strategy strategy;
    public final Portfolio portfolio;

    private BacktestConfig(Builder b) {
        this.feed       = b.feed;
        this.indicators = b.indicators;
        this.strategy   = b.strategy;
        this.portfolio  = b.portfolio;
    }

    public static class Builder {
        private DataFeed feed;
        private Map<String, Indicator> indicators = new LinkedHashMap<>();
        private Strategy strategy;
        private Portfolio portfolio;

        public Builder feed(DataFeed f)       { this.feed = f;               return this; }
        public Builder strategy(Strategy s)   { this.strategy = s;           return this; }
        public Builder portfolio(Portfolio p) { this.portfolio = p;          return this; }

        public Builder indicators(Map<String, Indicator> m) {
            this.indicators = new LinkedHashMap<>(m);
            return this;
        }

        public Builder addIndicator(String name, Indicator i) {
            this.indicators.put(name, i);
            return this;
        }

        public BacktestConfig build() {
            if (feed == null || indicators.isEmpty() || strategy == null || portfolio == null)
                throw new IllegalStateException("feed, indicators, strategy, and portfolio must all be set");
            return new BacktestConfig(this);
        }
    }
}
