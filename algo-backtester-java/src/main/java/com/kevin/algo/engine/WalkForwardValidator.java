package com.kevin.algo.engine;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.kevin.algo.core.Candle;
import com.kevin.algo.core.Result;
import com.kevin.algo.data.DataFeed;
import com.kevin.algo.data.ListDataFeed;
import com.kevin.algo.indicators.Indicator;
import com.kevin.algo.indicators.IndicatorFactory;
import com.kevin.algo.portfolio.Portfolio;
import com.kevin.algo.strategy.MovingAverageCrossover;
import com.kevin.algo.strategy.Strategy;

/**
 * WalkForwardValidator
 * --------------------
 * Prevents overfitting by splitting data into a training window and a
 * test window, then optimising strategy parameters on train only and
 * evaluating on the unseen test window.
 *
 * How it works:
 *   1. Drain the DataFeed into a List<Candle>
 *   2. Split: trainData = [0, splitIdx), testData = [splitIdx, end)
 *   3. Grid-search fast/slow periods on trainData → pick best Sharpe
 *   4. Re-run with best params on testData (never used during step 3)
 *   5. Return WalkForwardResult containing both train and test metrics
 *
 * Grid search covers:
 *   fast: 3, 5, 8, 10, 15, 20
 *   slow: 20, 30, 50, 100, 200
 *   (invalid combos where fast >= slow are skipped)
 *
 * Currently supports macrossover strategy only (parameter search is on
 * fast/slow MA periods). MomentumStrategy and MeanReversionStrategy use
 * fixed indicator periods and are evaluated as-is on the test window.
 */
public class WalkForwardValidator {

    /** Result returned to Main.java and serialised into JSON. */
    public static class WalkForwardResult {
        public double trainSharpe;
        public double testSharpe;
        public double testReturnPct;
        public double testMaxDrawdown;
        public int bestFast;
        public int bestSlow;
        public String trainStart;
        public String trainEnd;
        public String testStart;
        public String testEnd;
    }

    private final DataFeed   sourceFeed;
    private final double     trainRatio;
    private final String     strategyName;
    private final String     indicatorName;
    private final double     startCash;

    public WalkForwardValidator(DataFeed sourceFeed, double trainRatio,
                                String strategyName, String indicatorName,
                                double startCash) {
        this.sourceFeed    = sourceFeed;
        this.trainRatio    = trainRatio;
        this.strategyName  = strategyName;
        this.indicatorName = indicatorName;
        this.startCash     = startCash;
    }

    public WalkForwardResult run() {
        // 1) Drain feed into list
        List<Candle> all = new ArrayList<>();
        while (sourceFeed.hasNext()) all.add(sourceFeed.next());

        if (all.size() < 20)
            throw new IllegalStateException("Not enough data for walk-forward validation (need at least 20 bars)");

        int splitIdx = (int)(all.size() * trainRatio);
        List<Candle> trainData = all.subList(0, splitIdx);
        List<Candle> testData  = all.subList(splitIdx, all.size());

        // 2) Grid search on training data (macrossover only)
        int[]  fastCandidates = {3, 5, 8, 10, 15, 20};
        int[]  slowCandidates = {20, 30, 50, 100, 200};
        double bestSharpe = Double.NEGATIVE_INFINITY;
        int bestFast = fastCandidates[0];
        int bestSlow = slowCandidates[0];

        for (int f : fastCandidates) {
            for (int s : slowCandidates) {
                if (f >= s) continue;
                double sharpe = runSingleBacktest(trainData, f, s).sharpe;
                if (sharpe > bestSharpe) {
                    bestSharpe = sharpe;
                    bestFast   = f;
                    bestSlow   = s;
                }
            }
        }

        // 3) Evaluate best params on unseen test data
        Result.Metrics testMetrics = runSingleBacktest(testData, bestFast, bestSlow);

        // 4) Assemble result
        WalkForwardResult wfr = new WalkForwardResult();
        wfr.trainSharpe    = bestSharpe;
        wfr.testSharpe     = testMetrics.sharpe;
        wfr.testReturnPct  = testMetrics.totalReturnPct;
        wfr.testMaxDrawdown = testMetrics.maxDrawdown;
        wfr.bestFast       = bestFast;
        wfr.bestSlow       = bestSlow;
        wfr.trainStart     = dateStr(trainData.get(0).getDate());
        wfr.trainEnd       = dateStr(trainData.get(trainData.size() - 1).getDate());
        wfr.testStart      = dateStr(testData.get(0).getDate());
        wfr.testEnd        = dateStr(testData.get(testData.size() - 1).getDate());
        return wfr;
    }

    private Result.Metrics runSingleBacktest(List<Candle> data, int fast, int slow) {
        DataFeed feed   = new ListDataFeed(data);
        Strategy strat  = new MovingAverageCrossover();
        Portfolio pf    = new Portfolio(startCash, 0, 0);

        Map<String, Indicator> indicators = new LinkedHashMap<>();
        indicators.put("fast", IndicatorFactory.create(indicatorName, fast));
        indicators.put("slow", IndicatorFactory.create(indicatorName, slow));

        BacktestConfig cfg = new BacktestConfig.Builder()
            .feed(feed)
            .indicators(indicators)
            .strategy(strat)
            .portfolio(pf)
            .build();

        ResultCollector collector = new ResultCollector();
        BacktestEngine engine = new BacktestEngine();
        engine.addListener(collector);
        engine.run(cfg);

        return MetricsCalculator.compute(pf.closedTrades(), collector.equity, startCash);
    }

    private static String dateStr(LocalDate d) { return d != null ? d.toString() : ""; }
}
