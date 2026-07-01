package com.kevin.algo.engine;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

import com.kevin.algo.core.Result;
import com.kevin.algo.models.EquityPoint;
import com.kevin.algo.models.Trade;

/**
 * MetricsCalculator — pure static utility, no state.
 *
 * Given closed trades + equity curve → produces a fully populated Metrics object.
 *
 * Metrics:
 *   sharpe        — annualised Sharpe ratio (252 trading days, no risk-free rate)
 *   sortino       — annualised Sortino ratio (penalises only downside volatility)
 *   calmar        — annualised return / max drawdown (drawdown-adjusted performance)
 *   sharpeCI95Low / High — 95% bootstrap confidence interval on Sharpe (1000 resamples)
 *   netPnl        — sum of all closed-trade P&L
 *   winRatePct    — % of trades with positive P&L
 *   totalReturnPct — (finalEquity / startCash - 1) × 100
 *   maxDrawdown   — largest peak-to-trough drop in equity curve (%)
 */
public class MetricsCalculator {

    private static final int    BOOTSTRAP_SAMPLES = 1000;
    private static final double TRADING_DAYS      = 252.0;

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
            m.netPnl      = netPnl;
            m.winRatePct  = 100.0 * wins / trades.size();
        }

        if (!equity.isEmpty()) {
            double finalEq = equity.get(equity.size() - 1).equity;
            m.totalReturnPct = (finalEq / startCash - 1.0) * 100.0;
            m.maxDrawdown    = computeMaxDrawdown(equity);

            double[] returns = dailyReturns(equity);
            m.sharpe         = sharpeFromReturns(returns);
            m.sortino        = computeSortino(returns);
            m.calmar         = computeCalmar(m.totalReturnPct, m.maxDrawdown, equity.size());

            double[] ci      = bootstrapSharpeCI(returns, BOOTSTRAP_SAMPLES, new Random(42));
            m.sharpeCI95Low  = ci[0];
            m.sharpeCI95High = ci[1];
        }

        m.barsRead = equity.size();
        return m;
    }

    // ---- private helpers ------------------------------------------------

    static double[] dailyReturns(List<EquityPoint> equity) {
        double[] r = new double[equity.size() - 1];
        for (int i = 1; i < equity.size(); i++) {
            double prev = equity.get(i - 1).equity;
            double curr = equity.get(i).equity;
            r[i - 1] = prev == 0 ? 0.0 : (curr - prev) / prev;
        }
        return r;
    }

    static double sharpeFromReturns(double[] returns) {
        if (returns.length < 2) return 0.0;
        double mean = mean(returns);
        double std  = stddev(returns, mean);
        return std == 0.0 ? 0.0 : (mean / std) * Math.sqrt(TRADING_DAYS);
    }

    /**
     * Sortino ratio — same as Sharpe but downside deviation replaces std dev.
     * Downside deviation = sqrt(sum of (min(r, 0))^2 / N) across ALL N bars
     * (not just negative bars). This matches the standard Sortino definition.
     */
    private static double computeSortino(double[] returns) {
        if (returns.length < 2) return 0.0;
        double mean = mean(returns);

        double downsideVariance = 0.0;
        for (double r : returns) {
            double neg = Math.min(r, 0.0);
            downsideVariance += neg * neg;
        }
        double downsideStd = Math.sqrt(downsideVariance / returns.length);

        // No downside returns at all — strategy never lost money.
        // Fall back to Sharpe so the metric is still meaningful.
        if (downsideStd == 0.0) return sharpeFromReturns(returns);
        return (mean / downsideStd) * Math.sqrt(TRADING_DAYS);
    }

    /**
     * Calmar ratio — annualised return divided by max drawdown.
     * Annualised return = totalReturnPct / (bars / 252).
     * Returns 0 if max drawdown is zero (no losing period).
     */
    private static double computeCalmar(double totalReturnPct, double maxDrawdown, int bars) {
        if (maxDrawdown == 0.0 || bars == 0) return 0.0;
        double years            = bars / TRADING_DAYS;
        double annualisedReturn = totalReturnPct / years;
        return annualisedReturn / maxDrawdown;
    }

    /**
     * Bootstrap 95% confidence interval on Sharpe.
     * Resamples the daily returns array with replacement 1000 times,
     * computes Sharpe on each resample, then returns the 2.5th and 97.5th percentiles.
     * Seeded Random ensures reproducible results across runs.
     */
    static double[] bootstrapSharpeCI(double[] returns, int nSamples, Random rng) {
        if (returns.length < 2) return new double[]{0.0, 0.0};

        double[] sharpes = new double[nSamples];
        int n = returns.length;
        double[] sample = new double[n];

        for (int i = 0; i < nSamples; i++) {
            for (int j = 0; j < n; j++) {
                sample[j] = returns[rng.nextInt(n)];
            }
            sharpes[i] = sharpeFromReturns(sample);
        }

        Arrays.sort(sharpes);
        int lo = (int)(0.025 * nSamples);
        int hi = (int)(0.975 * nSamples) - 1;
        return new double[]{sharpes[lo], sharpes[hi]};
    }

    private static double computeMaxDrawdown(List<EquityPoint> equity) {
        double peak  = Double.NEGATIVE_INFINITY;
        double maxDD = 0.0;
        for (EquityPoint ep : equity) {
            if (ep.equity > peak) peak = ep.equity;
            double dd = (peak - ep.equity) / peak;
            if (dd > maxDD) maxDD = dd;
        }
        return maxDD * 100.0;
    }

    private static double mean(double[] arr) {
        double sum = 0.0;
        for (double v : arr) sum += v;
        return sum / arr.length;
    }

    private static double stddev(double[] arr, double mean) {
        double variance = 0.0;
        for (double v : arr) variance += (v - mean) * (v - mean);
        return Math.sqrt(variance / arr.length);
    }
}
