package com.kevin.algo.engine;

import com.kevin.algo.core.Result;
import com.kevin.algo.models.EquityPoint;
import com.kevin.algo.models.Trade;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class MetricsCalculatorTest {

    private static final LocalDate D = LocalDate.of(2024, 1, 1);

    private static Trade trade(double pnl) {
        Trade t = new Trade(D, 100.0);
        t.exitDate = D.plusDays(5);
        t.exitPrice = 100.0 + pnl;
        t.pnl = pnl;
        return t;
    }

    private static EquityPoint eq(int dayOffset, double equity) {
        return new EquityPoint(D.plusDays(dayOffset), equity);
    }

    @Test
    void zeroTradesAndEmptyEquity() {
        Result.Metrics m = MetricsCalculator.compute(List.of(), List.of(), 10_000.0);
        assertEquals(0, m.trades);
        assertEquals(0.0, m.netPnl, 1e-9);
        assertEquals(0.0, m.winRatePct, 1e-9);
    }

    @Test
    void netPnlSumsAllTrades() {
        List<Trade> trades = List.of(trade(200), trade(-50), trade(100));
        List<EquityPoint> equity = List.of(eq(0, 10_000), eq(1, 10_250));
        Result.Metrics m = MetricsCalculator.compute(trades, equity, 10_000.0);
        assertEquals(250.0, m.netPnl, 1e-9);
    }

    @Test
    void winRateAllWins() {
        List<Trade> trades = List.of(trade(100), trade(200), trade(50));
        List<EquityPoint> equity = List.of(eq(0, 10_000), eq(1, 10_350));
        Result.Metrics m = MetricsCalculator.compute(trades, equity, 10_000.0);
        assertEquals(100.0, m.winRatePct, 1e-9);
    }

    @Test
    void winRateAllLosses() {
        List<Trade> trades = List.of(trade(-100), trade(-200));
        List<EquityPoint> equity = List.of(eq(0, 10_000), eq(1, 9_700));
        Result.Metrics m = MetricsCalculator.compute(trades, equity, 10_000.0);
        assertEquals(0.0, m.winRatePct, 1e-9);
    }

    @Test
    void winRateMixed() {
        List<Trade> trades = List.of(trade(100), trade(-50), trade(200), trade(-30));
        List<EquityPoint> equity = List.of(eq(0, 10_000), eq(1, 10_220));
        Result.Metrics m = MetricsCalculator.compute(trades, equity, 10_000.0);
        assertEquals(50.0, m.winRatePct, 1e-9);
    }

    @Test
    void totalReturnPercent() {
        List<EquityPoint> equity = List.of(eq(0, 10_000), eq(1, 11_000));
        Result.Metrics m = MetricsCalculator.compute(List.of(trade(1000)), equity, 10_000.0);
        assertEquals(10.0, m.totalReturnPct, 1e-9);
    }

    @Test
    void maxDrawdownFlatEquityCurveIsZero() {
        List<EquityPoint> equity = List.of(eq(0, 10_000), eq(1, 10_000), eq(2, 10_000));
        Result.Metrics m = MetricsCalculator.compute(List.of(), equity, 10_000.0);
        assertEquals(0.0, m.maxDrawdown, 1e-9);
    }

    @Test
    void maxDrawdownCalculatedCorrectly() {
        // Peak at 12000, trough at 9000 → DD = (12000-9000)/12000 = 25%
        List<EquityPoint> equity = List.of(
                eq(0, 10_000),
                eq(1, 12_000),  // peak
                eq(2, 10_500),
                eq(3, 9_000)    // trough
        );
        Result.Metrics m = MetricsCalculator.compute(List.of(), equity, 10_000.0);
        assertEquals(25.0, m.maxDrawdown, 1e-6);
    }

    @Test
    void sharpeZeroWhenFlatReturns() {
        List<EquityPoint> equity = List.of(eq(0, 10_000), eq(1, 10_000), eq(2, 10_000));
        Result.Metrics m = MetricsCalculator.compute(List.of(), equity, 10_000.0);
        assertEquals(0.0, m.sharpe, 1e-9);
    }

    @Test
    void sharpePositiveForConsistentlyRisingEquity() {
        List<EquityPoint> equity = new ArrayList<>();
        double e = 10_000;
        for (int i = 0; i < 30; i++) {
            equity.add(new EquityPoint(D.plusDays(i), e));
            e *= 1.001; // consistent 0.1% daily gain
        }
        Result.Metrics m = MetricsCalculator.compute(List.of(), equity, 10_000.0);
        assertTrue(m.sharpe > 0, "Sharpe should be positive for rising equity");
    }

    @Test
    void barsReadMatchesEquitySize() {
        List<EquityPoint> equity = List.of(eq(0, 10_000), eq(1, 10_100), eq(2, 10_200));
        Result.Metrics m = MetricsCalculator.compute(List.of(), equity, 10_000.0);
        assertEquals(3, m.barsRead);
    }

    // ---- Sortino --------------------------------------------------------

    @Test
    void sortinoZeroWhenFlatReturns() {
        List<EquityPoint> equity = List.of(eq(0, 10_000), eq(1, 10_000), eq(2, 10_000));
        Result.Metrics m = MetricsCalculator.compute(List.of(), equity, 10_000.0);
        assertEquals(0.0, m.sortino, 1e-9);
    }

    @Test
    void sortinoPositiveForProfitableStrategy() {
        // Mix of gains and small losses — net positive, so Sortino should be positive
        List<EquityPoint> equity = new ArrayList<>();
        double e = 10_000;
        double[] pattern = {0.01, 0.008, -0.002, 0.012, 0.005, -0.001, 0.009};
        for (int i = 0; i < 40; i++) {
            equity.add(new EquityPoint(D.plusDays(i), e));
            e *= (1 + pattern[i % pattern.length]);
        }
        Result.Metrics m = MetricsCalculator.compute(List.of(), equity, 10_000.0);
        assertTrue(m.sortino > 0, "Sortino should be positive for a net-profitable strategy");
    }

    @Test
    void sortinoHigherThanSharpeWhenLossesSmallRelativeToGains() {
        // Large upside swings, tiny downside → Sortino > Sharpe
        // Sharpe penalises all volatility equally; Sortino only penalises losses
        List<EquityPoint> equity = new ArrayList<>();
        double e = 10_000;
        double[] pattern = {0.02, 0.02, -0.001, 0.02, 0.02, -0.001};
        for (int i = 0; i < 60; i++) {
            equity.add(new EquityPoint(D.plusDays(i), e));
            e *= (1 + pattern[i % pattern.length]);
        }
        Result.Metrics m = MetricsCalculator.compute(List.of(), equity, 10_000.0);
        assertTrue(m.sortino > m.sharpe,
                "Sortino (" + m.sortino + ") should exceed Sharpe (" + m.sharpe + ") when losses are tiny relative to gains");
    }

    // ---- Calmar ---------------------------------------------------------

    @Test
    void calmarZeroWhenNoDrawdown() {
        List<EquityPoint> equity = List.of(eq(0, 10_000), eq(1, 10_100), eq(2, 10_200));
        Result.Metrics m = MetricsCalculator.compute(List.of(), equity, 10_000.0);
        assertEquals(0.0, m.calmar, 1e-9);
    }

    @Test
    void calmarPositiveWhenReturnExceedsDrawdown() {
        // Rising equity with a small dip — positive total return, small drawdown → positive Calmar
        List<EquityPoint> equity = List.of(
                eq(0, 10_000),
                eq(1, 11_000),
                eq(2, 10_500), // small dip
                eq(3, 12_000)
        );
        Result.Metrics m = MetricsCalculator.compute(List.of(), equity, 10_000.0);
        assertTrue(m.calmar > 0, "Calmar should be positive when strategy is profitable");
    }

    @Test
    void calmarNegativeWhenNetLoss() {
        List<EquityPoint> equity = List.of(
                eq(0, 10_000),
                eq(1, 9_500),
                eq(2, 9_000)
        );
        Result.Metrics m = MetricsCalculator.compute(List.of(), equity, 10_000.0);
        assertTrue(m.calmar < 0, "Calmar should be negative when strategy loses money");
    }

    // ---- Bootstrap Sharpe CI --------------------------------------------

    @Test
    void ciLowAlwaysLessThanOrEqualHigh() {
        List<EquityPoint> equity = new ArrayList<>();
        double e = 10_000;
        for (int i = 0; i < 100; i++) {
            equity.add(new EquityPoint(D.plusDays(i), e));
            e *= (1 + (Math.sin(i) * 0.01)); // oscillating returns
        }
        Result.Metrics m = MetricsCalculator.compute(List.of(), equity, 10_000.0);
        assertTrue(m.sharpeCI95Low <= m.sharpeCI95High,
                "CI low must be <= CI high");
    }

    @Test
    void ciContainsPointEstimate() {
        List<EquityPoint> equity = new ArrayList<>();
        double e = 10_000;
        for (int i = 0; i < 200; i++) {
            equity.add(new EquityPoint(D.plusDays(i), e));
            e *= 1.0008;
        }
        Result.Metrics m = MetricsCalculator.compute(List.of(), equity, 10_000.0);
        assertTrue(m.sharpeCI95Low <= m.sharpe && m.sharpe <= m.sharpeCI95High,
                "Point Sharpe (" + m.sharpe + ") should fall within CI [" + m.sharpeCI95Low + ", " + m.sharpeCI95High + "]");
    }

    @Test
    void bootstrapCIReproducibleWithSameSeed() {
        double[] returns = {0.01, -0.005, 0.008, 0.003, -0.002, 0.006, -0.001, 0.004};
        double[] ci1 = MetricsCalculator.bootstrapSharpeCI(returns, 100, new Random(42));
        double[] ci2 = MetricsCalculator.bootstrapSharpeCI(returns, 100, new Random(42));
        assertEquals(ci1[0], ci2[0], 1e-9);
        assertEquals(ci1[1], ci2[1], 1e-9);
    }
}
