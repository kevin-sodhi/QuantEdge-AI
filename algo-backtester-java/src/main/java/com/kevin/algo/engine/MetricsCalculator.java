package com.kevin.algo.engine;

import java.util.List;

import com.kevin.algo.core.Result;
import com.kevin.algo.models.EquityPoint;
import com.kevin.algo.models.Trade;

/**
 * DESIGN PATTERN: Utility / Static Helper (not a GoF pattern, but a common idiom)
 * ---------------------------------------------------------------------------------
 * MetricsCalculator has no state — all methods are static. It is a pure function
 * class: given closed trades + equity curve → produce a Metrics object.
 * This keeps metric computation decoupled from both the engine and the result model.
 *
 * Why separate from BacktestEngine?
 *   Single Responsibility Principle: the engine's job is to iterate bars and fire
 *   events. Computing Sharpe ratio, max drawdown, and win rate is a separate concern.
 *   Keeping them apart makes each class easier to test and change independently.
 *
 * Metrics computed:
 *   netPnl         → sum of all closed-trade P&L
 *   winRatePct     → percentage of trades with positive P&L
 *   totalReturnPct → (finalEquity / startCash - 1) × 100
 *   maxDrawdown    → largest peak-to-trough drop in the equity curve (%)
 *   sharpe         → annualised Sharpe ratio (252 trading days assumed, no risk-free rate)
 */
public class MetricsCalculator {

    public static Result.Metrics compute(List<Trade> trades, List<EquityPoint> equity, double startCash) {
        Result.Metrics m = new Result.Metrics();
        m.trades = trades.size();

        if (!trades.isEmpty()) {
            double netPnl = 0.0;
            int wins = 0;
            for (Trade t : trades) {
                netPnl += t.pnl;
                if (t.pnl > 0) wins++;
            }
            m.netPnl = netPnl;
            m.winRatePct = 100.0 * wins / trades.size();
        }

        if (!equity.isEmpty()) {
            double finalEq = equity.get(equity.size() - 1).equity;
            m.totalReturnPct = (finalEq / startCash - 1.0) * 100.0;
            m.maxDrawdown = computeMaxDrawdown(equity);
            m.sharpe = computeSharpe(equity);
        }

        m.barsRead = equity.size();
        return m;
    }

    private static double computeMaxDrawdown(List<EquityPoint> equity) {
        double peak = Double.NEGATIVE_INFINITY;
        double maxDD = 0.0;
        for (EquityPoint ep : equity) {
            if (ep.equity > peak) peak = ep.equity;
            double dd = (peak - ep.equity) / peak;
            if (dd > maxDD) maxDD = dd;
        }
        return maxDD * 100.0; // as percentage
    }

    private static double computeSharpe(List<EquityPoint> equity) {
        if (equity.size() < 2) return 0.0;
        double[] returns = new double[equity.size() - 1];
        for (int i = 1; i < equity.size(); i++) {
            double prev = equity.get(i - 1).equity;
            double curr = equity.get(i).equity;
            returns[i - 1] = prev == 0 ? 0.0 : (curr - prev) / prev;
        }
        double mean = 0.0;
        for (double r : returns) mean += r;
        mean /= returns.length;

        double variance = 0.0;
        for (double r : returns) variance += (r - mean) * (r - mean);
        double stddev = Math.sqrt(variance / returns.length);

        if (stddev == 0.0) return 0.0;
        // Annualise assuming ~252 trading days
        return (mean / stddev) * Math.sqrt(252);
    }
}
