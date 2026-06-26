package com.kevin.algo.indicators;

import com.kevin.algo.core.Candle;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class SMATest {

    private static Candle candle(double close) {
        return new Candle(LocalDate.now(), close, close, close, close, 1000L);
    }

    @Test
    void notReadyUntilWindowFull() {
        SMA sma = new SMA(3);
        sma.accumulate(candle(10));
        sma.accumulate(candle(20));
        assertFalse(sma.isReady());
        assertTrue(Double.isNaN(sma.value()));
    }

    @Test
    void readyAndCorrectAfterPeriodBars() {
        SMA sma = new SMA(3);
        sma.accumulate(candle(10));
        sma.accumulate(candle(20));
        sma.accumulate(candle(30));
        assertTrue(sma.isReady());
        assertEquals(20.0, sma.value(), 1e-9);
    }

    @Test
    void slidingWindowEvictsOldest() {
        SMA sma = new SMA(3);
        sma.accumulate(candle(10));
        sma.accumulate(candle(20));
        sma.accumulate(candle(30));
        sma.accumulate(candle(40)); // evicts 10 → window [20, 30, 40]
        assertEquals(30.0, sma.value(), 1e-9);
    }

    @Test
    void periodOneTriviallyReady() {
        SMA sma = new SMA(1);
        sma.accumulate(candle(42.5));
        assertTrue(sma.isReady());
        assertEquals(42.5, sma.value(), 1e-9);
    }

    @Test
    void invalidPeriodThrows() {
        assertThrows(IllegalArgumentException.class, () -> new SMA(0));
        assertThrows(IllegalArgumentException.class, () -> new SMA(-1));
    }
}
