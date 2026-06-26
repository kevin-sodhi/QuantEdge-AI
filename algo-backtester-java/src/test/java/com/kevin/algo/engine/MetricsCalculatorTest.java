package com.kevin.algo.engine;

import com.kevin.algo.core.Result;
import com.kevin.algo.models.EquityPoint;
import com.kevin.algo.models.Trade;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

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
}
