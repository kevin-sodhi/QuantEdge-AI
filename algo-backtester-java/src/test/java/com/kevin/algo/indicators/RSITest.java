package com.kevin.algo.indicators;

import com.kevin.algo.core.Candle;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class RSITest {

    private static Candle candle(double close) {
        return new Candle(LocalDate.now(), close, close, close, close, 1000L);
    }

    private static void feed(RSI rsi, double... prices) {
        for (double p : prices) rsi.accumulate(candle(p));
    }

    @Test
    void notReadyDuringWarmup() {
        RSI rsi = new RSI(3);
        feed(rsi, 10, 11, 12);
        assertFalse(rsi.isReady());
    }

    @Test
    void readyAfterPeriodPlusOneBar() {
        // RSI(3) needs 3 price *changes*, so 4 bars (first bar sets prevClose only)
        RSI rsi = new RSI(3);
        feed(rsi, 10, 11, 12, 13);
        assertTrue(rsi.isReady());
    }

    @Test
    void allGainsProducesRsi100() {
        RSI rsi = new RSI(3);
        feed(rsi, 10, 11, 12, 13, 14); // all gains, no losses
        assertEquals(100.0, rsi.value(), 1e-9);
    }

    @Test
    void allLossesProducesRsiNearZero() {
        RSI rsi = new RSI(3);
        feed(rsi, 14, 13, 12, 11, 10); // all losses, no gains
        assertEquals(0.0, rsi.value(), 1e-9);
    }

    @Test
    void rsiStaysBetweenZeroAnd100() {
        RSI rsi = new RSI(14);
        double[] prices = {100, 102, 101, 105, 103, 107, 106, 110, 108, 112, 111, 115, 113, 117, 116, 120};
        feed(rsi, prices);
        assertTrue(rsi.isReady());
        double val = rsi.value();
        assertTrue(val >= 0.0 && val <= 100.0,
                "RSI out of range: " + val);
    }

    @Test
    void invalidPeriodThrows() {
        assertThrows(IllegalArgumentException.class, () -> new RSI(0));
        assertThrows(IllegalArgumentException.class, () -> new RSI(-5));
    }
}
