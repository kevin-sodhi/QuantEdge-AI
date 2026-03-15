package com.kevin.algo.strategy;

import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;

import com.kevin.algo.core.Candle;
import com.kevin.algo.models.Signal;

/**
 * MeanReversionStrategy — Lower Bollinger Band bounce + RSI oversold
 * ------------------------------------------------------------------
 * Indicator keys expected from the engine:
 *   "bb_upper"  → 20-day upper Bollinger Band
 *   "bb_middle" → 20-day middle Bollinger Band (SMA)
 *   "bb_lower"  → 20-day lower Bollinger Band
 *   "rsi"       → RSI(14)
 *
 * BUY signal — both conditions must be true, not already in position:
 *   1. Close price touches or breaks below lower Bollinger Band
 *      bar.getClose() <= currVals["bb_lower"]
 *   2. RSI is below 30 (oversold territory)
 *      RSI < 30
 *
 *   Rationale: price has stretched far below its mean AND momentum is
 *   exhausted. High probability of mean reversion.
 *
 * SELL signal — when in position:
 *   1. Close price returns to or above middle Bollinger Band (the mean)
 *      bar.getClose() >= currVals["bb_middle"]
 *
 *   Rationale: reversion to mean is complete. Exit rather than chasing
 *   further upside — the edge has been captured.
 *
 * Returns Optional.empty() if any required indicator is null (warming up).
 */
public class MeanReversionStrategy implements Strategy {

    @Override
    public Optional<Signal> maybeSignal(LocalDate date, Candle bar,
                                        Map<String, Double> prevVals,
                                        Map<String, Double> currVals,
                                        boolean inPosition) {
        Double bbUpper  = currVals.get("bb_upper");
        Double bbMiddle = currVals.get("bb_middle");
        Double bbLower  = currVals.get("bb_lower");
        Double rsi      = currVals.get("rsi");

        if (bbUpper == null || bbMiddle == null || bbLower == null || rsi == null)
            return Optional.empty();

        double close = bar.getClose();
        boolean touchedLowerBand = close <= bbLower;
        boolean oversold         = rsi < 30;
        boolean returnedToMean   = close >= bbMiddle;

        if (!inPosition && touchedLowerBand && oversold)
            return Optional.of(new Signal(date, close, Signal.Type.BUY));

        if (inPosition && returnedToMean)
            return Optional.of(new Signal(date, close, Signal.Type.SELL));

        return Optional.empty();
    }
}
