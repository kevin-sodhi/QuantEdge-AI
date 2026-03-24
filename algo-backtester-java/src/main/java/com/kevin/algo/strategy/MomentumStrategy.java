package com.kevin.algo.strategy;

import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;

import com.kevin.algo.core.Candle;
import com.kevin.algo.models.Signal;

/**
 * MomentumStrategy — Golden Cross + RSI filter + Volume confirmation
 * ------------------------------------------------------------------
 * Indicator keys expected from the engine:
 *   "ema50"      → 50-day EMA
 *   "ema200"     → 200-day EMA
 *   "rsi"        → RSI(14)
 *   "volume_sma" → 20-day SMA of volume
 *
 * BUY signal — ALL three conditions must be true, and not already in position:
 *   1. Golden cross: EMA50 crossed above EMA200
 *      (prevVals["ema50"] <= prevVals["ema200"]) AND (currVals["ema50"] > currVals["ema200"])
 *   2. RSI in bullish range, not overbought: 50 <= RSI <= 70
 *   3. Volume above 20-day average (confirms institutional participation)
 *
 * SELL signal — either exit condition, when in position:
 *   1. Death cross: EMA50 crossed below EMA200
 *      (prevVals["ema50"] >= prevVals["ema200"]) AND (currVals["ema50"] < currVals["ema200"])
 *   2. RSI overbought: RSI > 70
 *
 * Returns Optional.empty() if any required indicator is null (still warming up).
 */
public class MomentumStrategy implements Strategy {

    @Override
    public Optional<Signal> maybeSignal(LocalDate date, Candle bar,
                                        Map<String, Double> prevVals,
                                        Map<String, Double> currVals,
                                        boolean inPosition) {
        Double prevEma50  = prevVals.get("ema50");
        Double prevEma200 = prevVals.get("ema200");
        Double currEma50  = currVals.get("ema50");
        Double currEma200 = currVals.get("ema200");
        Double rsi        = currVals.get("rsi");
        Double volSma     = currVals.get("volume_sma");

        // Require all indicators ready — EMA200 is the last to warm up
        if (prevEma50 == null || prevEma200 == null ||
            currEma50 == null || currEma200 == null ||
            rsi == null || volSma == null) {
            return Optional.empty();
        }

        boolean goldenCross = prevEma50 <= prevEma200 && currEma50 > currEma200;
        boolean deathCross  = prevEma50 >= prevEma200 && currEma50 < currEma200;
        boolean rsiBullish  = rsi >= 50 && rsi <= 70;
        boolean rsiOverbought = rsi > 70;
        boolean volumeConfirmed = bar.getVolume() > volSma;

        if (!inPosition && goldenCross && rsiBullish && volumeConfirmed)
            return Optional.of(new Signal(date, bar.getClose(), Signal.Type.BUY));

        if (inPosition && (deathCross || rsiOverbought))
            return Optional.of(new Signal(date, bar.getClose(), Signal.Type.SELL));

        return Optional.empty();
    }
}
