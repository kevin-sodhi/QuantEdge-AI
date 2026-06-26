package com.kevin.algo.strategy;

import com.kevin.algo.core.Candle;
import com.kevin.algo.models.Signal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class MovingAverageCrossoverTest {

    private MovingAverageCrossover strategy;
    private final LocalDate DATE = LocalDate.of(2024, 1, 15);

    @BeforeEach
    void setUp() {
        strategy = new MovingAverageCrossover();
    }

    private Candle candle(double close) {
        return new Candle(DATE, close, close, close, close, 1000L);
    }

    @Test
    void buySignalOnGoldenCross() {
        // fast was below slow, now above → BUY
        Map<String, Double> prev = Map.of("fast", 9.0, "slow", 10.0);
        Map<String, Double> curr = Map.of("fast", 11.0, "slow", 10.0);
        Optional<Signal> signal = strategy.maybeSignal(DATE, candle(100), prev, curr, false);
        assertTrue(signal.isPresent());
        assertEquals(Signal.Type.BUY, signal.get().type);
        assertEquals(100.0, signal.get().price, 1e-9);
    }

    @Test
    void sellSignalOnDeathCross() {
        // fast was above slow, now below → SELL
        Map<String, Double> prev = Map.of("fast", 11.0, "slow", 10.0);
        Map<String, Double> curr = Map.of("fast", 9.0, "slow", 10.0);
        Optional<Signal> signal = strategy.maybeSignal(DATE, candle(95), prev, curr, true);
        assertTrue(signal.isPresent());
        assertEquals(Signal.Type.SELL, signal.get().type);
    }

    @Test
    void noBuyWhenAlreadyInPosition() {
        Map<String, Double> prev = Map.of("fast", 9.0, "slow", 10.0);
        Map<String, Double> curr = Map.of("fast", 11.0, "slow", 10.0);
        Optional<Signal> signal = strategy.maybeSignal(DATE, candle(100), prev, curr, true);
        assertFalse(signal.isPresent());
    }

    @Test
    void noSellWhenNotInPosition() {
        Map<String, Double> prev = Map.of("fast", 11.0, "slow", 10.0);
        Map<String, Double> curr = Map.of("fast", 9.0, "slow", 10.0);
        Optional<Signal> signal = strategy.maybeSignal(DATE, candle(95), prev, curr, false);
        assertFalse(signal.isPresent());
    }

    @Test
    void noSignalWhenIndicatorsNotReady() {
        Map<String, Double> prev = Map.of();
        Map<String, Double> curr = Map.of();
        Optional<Signal> signal = strategy.maybeSignal(DATE, candle(100), prev, curr, false);
        assertFalse(signal.isPresent());
    }

    @Test
    void noSignalWhenFastEqualsSlowAndUnchanged() {
        // No crossover — fast == slow both bars
        Map<String, Double> prev = Map.of("fast", 10.0, "slow", 10.0);
        Map<String, Double> curr = Map.of("fast", 10.0, "slow", 10.0);
        Optional<Signal> signal = strategy.maybeSignal(DATE, candle(100), prev, curr, false);
        assertFalse(signal.isPresent());
    }

    @Test
    void signalDateMatchesInputDate() {
        Map<String, Double> prev = Map.of("fast", 9.0, "slow", 10.0);
        Map<String, Double> curr = Map.of("fast", 11.0, "slow", 10.0);
        Optional<Signal> signal = strategy.maybeSignal(DATE, candle(100), prev, curr, false);
        assertTrue(signal.isPresent());
        assertEquals(DATE, signal.get().date);
    }
}
