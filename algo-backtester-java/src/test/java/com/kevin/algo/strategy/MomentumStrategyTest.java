package com.kevin.algo.strategy;

import com.kevin.algo.core.Candle;
import com.kevin.algo.models.Signal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class MomentumStrategyTest {

    private MomentumStrategy strategy;
    private final LocalDate DATE = LocalDate.of(2024, 1, 15);

    @BeforeEach
    void setUp() {
        strategy = new MomentumStrategy();
    }

    private Candle candle(double close, long volume) {
        return new Candle(DATE, close, close, close, close, volume);
    }

    private Map<String, Double> goldenCrossPrev() {
        return Map.of("ema50", 195.0, "ema200", 200.0, "rsi", 55.0, "volume_sma", 1_000_000.0);
    }

    private Map<String, Double> goldenCrossCurr(double rsi) {
        return Map.of("ema50", 205.0, "ema200", 200.0, "rsi", rsi, "volume_sma", 1_000_000.0);
    }

    @Test
    void buyOnGoldenCrossWithRsiAndVolumeConfirmed() {
        Optional<Signal> signal = strategy.maybeSignal(
                DATE, candle(200, 2_000_000L),
                goldenCrossPrev(), goldenCrossCurr(60.0), false);
        assertTrue(signal.isPresent());
        assertEquals(Signal.Type.BUY, signal.get().type);
    }

    @Test
    void noBuyWhenRsiOverbought() {
        Optional<Signal> signal = strategy.maybeSignal(
                DATE, candle(200, 2_000_000L),
                goldenCrossPrev(), goldenCrossCurr(75.0), false); // RSI > 70
        assertFalse(signal.isPresent());
    }

    @Test
    void noBuyWhenVolumeBelowAverage() {
        Optional<Signal> signal = strategy.maybeSignal(
                DATE, candle(200, 500_000L), // volume < volume_sma 1M
                goldenCrossPrev(), goldenCrossCurr(60.0), false);
        assertFalse(signal.isPresent());
    }

    @Test
    void noBuyWhenRsiBelowBullishRange() {
        Optional<Signal> signal = strategy.maybeSignal(
                DATE, candle(200, 2_000_000L),
                goldenCrossPrev(), goldenCrossCurr(45.0), false); // RSI < 50
        assertFalse(signal.isPresent());
    }

    @Test
    void sellOnDeathCross() {
        Map<String, Double> prev = Map.of("ema50", 205.0, "ema200", 200.0, "rsi", 55.0, "volume_sma", 1_000_000.0);
        Map<String, Double> curr = Map.of("ema50", 195.0, "ema200", 200.0, "rsi", 55.0, "volume_sma", 1_000_000.0);
        Optional<Signal> signal = strategy.maybeSignal(DATE, candle(190, 1_000_000L), prev, curr, true);
        assertTrue(signal.isPresent());
        assertEquals(Signal.Type.SELL, signal.get().type);
    }

    @Test
    void sellWhenRsiOverboughtInPosition() {
        Map<String, Double> prev = Map.of("ema50", 205.0, "ema200", 200.0, "rsi", 65.0, "volume_sma", 1_000_000.0);
        Map<String, Double> curr = Map.of("ema50", 207.0, "ema200", 200.0, "rsi", 75.0, "volume_sma", 1_000_000.0);
        Optional<Signal> signal = strategy.maybeSignal(DATE, candle(210, 1_000_000L), prev, curr, true);
        assertTrue(signal.isPresent());
        assertEquals(Signal.Type.SELL, signal.get().type);
    }

    @Test
    void noSignalWhenIndicatorsMissing() {
        Optional<Signal> signal = strategy.maybeSignal(DATE, candle(200, 1_000_000L),
                Map.of(), Map.of(), false);
        assertFalse(signal.isPresent());
    }

    @Test
    void noBuyWhenAlreadyInPosition() {
        Optional<Signal> signal = strategy.maybeSignal(
                DATE, candle(200, 2_000_000L),
                goldenCrossPrev(), goldenCrossCurr(60.0), true); // already in position
        assertFalse(signal.isPresent());
    }
}
