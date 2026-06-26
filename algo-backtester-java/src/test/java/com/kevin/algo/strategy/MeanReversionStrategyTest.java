package com.kevin.algo.strategy;

import com.kevin.algo.core.Candle;
import com.kevin.algo.models.Signal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class MeanReversionStrategyTest {

    private MeanReversionStrategy strategy;
    private final LocalDate DATE = LocalDate.of(2024, 1, 15);

    @BeforeEach
    void setUp() {
        strategy = new MeanReversionStrategy();
    }

    private Candle candle(double close) {
        return new Candle(DATE, close, close, close, close, 1000L);
    }

    private Map<String, Double> bands(double upper, double middle, double lower, double rsi) {
        return Map.of("bb_upper", upper, "bb_middle", middle, "bb_lower", lower, "rsi", rsi);
    }

    @Test
    void buyWhenPriceBelowLowerBandAndOversold() {
        // close = 90, lower band = 92, RSI = 25 (oversold)
        Map<String, Double> curr = bands(110.0, 100.0, 92.0, 25.0);
        Optional<Signal> signal = strategy.maybeSignal(DATE, candle(90.0), Map.of(), curr, false);
        assertTrue(signal.isPresent());
        assertEquals(Signal.Type.BUY, signal.get().type);
        assertEquals(90.0, signal.get().price, 1e-9);
    }

    @Test
    void buyWhenPriceExactlyAtLowerBand() {
        // close == lower band (boundary condition: <=)
        Map<String, Double> curr = bands(110.0, 100.0, 92.0, 28.0);
        Optional<Signal> signal = strategy.maybeSignal(DATE, candle(92.0), Map.of(), curr, false);
        assertTrue(signal.isPresent());
        assertEquals(Signal.Type.BUY, signal.get().type);
    }

    @Test
    void noBuyWhenRsiAbove30() {
        // Price below lower band but RSI = 35, not oversold enough
        Map<String, Double> curr = bands(110.0, 100.0, 92.0, 35.0);
        Optional<Signal> signal = strategy.maybeSignal(DATE, candle(90.0), Map.of(), curr, false);
        assertFalse(signal.isPresent());
    }

    @Test
    void noBuyWhenPriceAboveLowerBand() {
        // RSI oversold but price not at band
        Map<String, Double> curr = bands(110.0, 100.0, 92.0, 25.0);
        Optional<Signal> signal = strategy.maybeSignal(DATE, candle(95.0), Map.of(), curr, false);
        assertFalse(signal.isPresent());
    }

    @Test
    void sellWhenPriceReturnsToMiddleBand() {
        // close = 100 = middle band → reversion complete
        Map<String, Double> curr = bands(110.0, 100.0, 92.0, 45.0);
        Optional<Signal> signal = strategy.maybeSignal(DATE, candle(100.0), Map.of(), curr, true);
        assertTrue(signal.isPresent());
        assertEquals(Signal.Type.SELL, signal.get().type);
    }

    @Test
    void sellWhenPriceAboveMiddleBand() {
        // Price overshot the mean
        Map<String, Double> curr = bands(110.0, 100.0, 92.0, 55.0);
        Optional<Signal> signal = strategy.maybeSignal(DATE, candle(105.0), Map.of(), curr, true);
        assertTrue(signal.isPresent());
        assertEquals(Signal.Type.SELL, signal.get().type);
    }

    @Test
    void noSellWhenPriceBelowMiddleBand() {
        // Still below mean, hold position
        Map<String, Double> curr = bands(110.0, 100.0, 92.0, 40.0);
        Optional<Signal> signal = strategy.maybeSignal(DATE, candle(97.0), Map.of(), curr, true);
        assertFalse(signal.isPresent());
    }

    @Test
    void noSignalWhenIndicatorsNotReady() {
        Optional<Signal> signal = strategy.maybeSignal(DATE, candle(90.0), Map.of(), Map.of(), false);
        assertFalse(signal.isPresent());
    }

    @Test
    void noBuyWhenAlreadyInPosition() {
        Map<String, Double> curr = bands(110.0, 100.0, 92.0, 25.0);
        Optional<Signal> signal = strategy.maybeSignal(DATE, candle(90.0), Map.of(), curr, true);
        assertFalse(signal.isPresent());
    }
}
