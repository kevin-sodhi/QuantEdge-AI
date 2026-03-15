package com.kevin.algo.web;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.kevin.algo.core.Candle;
import com.kevin.algo.core.Result;
import com.kevin.algo.data.DataFeed;
import com.kevin.algo.data.ListDataFeed;
import com.kevin.algo.engine.BacktestConfig;
import com.kevin.algo.engine.BacktestEngine;
import com.kevin.algo.engine.MetricsCalculator;
import com.kevin.algo.engine.ResultCollector;
import com.kevin.algo.engine.WalkForwardValidator;
import com.kevin.algo.indicators.BollingerBandsLower;
import com.kevin.algo.indicators.BollingerBandsMiddle;
import com.kevin.algo.indicators.BollingerBandsUpper;
import com.kevin.algo.indicators.Indicator;
import com.kevin.algo.indicators.IndicatorFactory;
import com.kevin.algo.indicators.RSI;
import com.kevin.algo.indicators.VolumeSMA;
import com.kevin.algo.portfolio.Portfolio;
import com.kevin.algo.strategy.Strategy;
import com.kevin.algo.strategy.StrategyFactory;
import com.kevin.algo.web.dto.BacktestRequest;
import com.kevin.algo.web.dto.WalkForwardRequest;

/**
 * BacktestService
 * ---------------
 * Executes backtests and walk-forward validation using the existing
 * engine layer. All data flows in memory — no CSV files.
 *
 * This is the same logic as cli/Main.java, refactored into a Spring
 * service so the REST controller can call it.
 */
@Service
public class BacktestService {

    /**
     * Run a full backtest.
     * Returns a Map so Jackson serialises it with the same shape
     * the frontend already expects from the old JAR output.
     */
    public Map<String, Object> runBacktest(BacktestRequest req) {
        List<Candle> candles = toCandleList(req.candles);

        DataFeed      feed       = new ListDataFeed(candles);
        Strategy      strategy   = StrategyFactory.create(req.strategy);
        Portfolio     portfolio  = new Portfolio(req.initialCapital, req.fee, req.slip);
        Map<String, Indicator> indicators = buildIndicators(req.strategy, req.indicator, req.fast, req.slow);

        BacktestConfig cfg = new BacktestConfig.Builder()
            .feed(feed)
            .indicators(indicators)
            .strategy(strategy)
            .portfolio(portfolio)
            .build();

        ResultCollector collector = new ResultCollector();
        BacktestEngine  engine    = new BacktestEngine();
        engine.addListener(collector);
        engine.run(cfg);

        Result.Metrics metrics = MetricsCalculator.compute(
            portfolio.closedTrades(), collector.equity, req.initialCapital);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("ok",      true);
        response.put("message", "Backtest complete");
        response.put("params",  paramsMap(req));
        response.put("metrics", metrics);
        response.put("trades",  portfolio.closedTrades());
        response.put("series",  collector.series);
        response.put("signals", collector.signals);
        response.put("equity",  collector.equity);
        return response;
    }

    /**
     * Run walk-forward validation (macrossover only — grid searches fast/slow).
     */
    public Map<String, Object> runWalkForward(WalkForwardRequest req) {
        List<Candle> candles = toCandleList(req.candles);

        DataFeed feed = new ListDataFeed(candles);
        WalkForwardValidator wfv = new WalkForwardValidator(
            feed, req.trainRatio, req.strategy, req.indicator, req.initialCapital);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("ok",          true);
        response.put("ticker",      req.ticker);
        response.put("strategy",    req.strategy);
        response.put("trainRatio",  req.trainRatio);
        response.put("walkForward", wfv.run());
        return response;
    }

    // ---- private helpers ----

    private List<Candle> toCandleList(List<com.kevin.algo.web.dto.CandleData> data) {
        if (data == null || data.isEmpty())
            throw new IllegalArgumentException("candles list is required and must not be empty");
        return data.stream()
                   .map(com.kevin.algo.web.dto.CandleData::toCandle)
                   .collect(Collectors.toList());
    }

    private Map<String, Indicator> buildIndicators(String strategy, String indicator, int fast, int slow) {
        Map<String, Indicator> map = new LinkedHashMap<>();
        switch (strategy.toLowerCase()) {
            case "momentum" -> {
                map.put("ema50",      IndicatorFactory.create("ema", 50));
                map.put("ema200",     IndicatorFactory.create("ema", 200));
                map.put("rsi",        new RSI(14));
                map.put("volume_sma", new VolumeSMA());
            }
            case "meanreversion" -> {
                map.put("bb_upper",  new BollingerBandsUpper());
                map.put("bb_middle", new BollingerBandsMiddle());
                map.put("bb_lower",  new BollingerBandsLower());
                map.put("rsi",       new RSI(14));
            }
            default -> {
                map.put("fast", IndicatorFactory.create(indicator, fast));
                map.put("slow", IndicatorFactory.create(indicator, slow));
            }
        }
        return map;
    }

    private Map<String, Object> paramsMap(BacktestRequest req) {
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("ticker",    req.ticker);
        p.put("period",    req.period);
        p.put("strategy",  req.strategy);
        p.put("indicator", req.indicator);
        p.put("fast",      req.fast);
        p.put("slow",      req.slow);
        p.put("cash",      req.initialCapital);
        p.put("fee",       req.fee);
        p.put("slip",      req.slip);
        return p;
    }
}
