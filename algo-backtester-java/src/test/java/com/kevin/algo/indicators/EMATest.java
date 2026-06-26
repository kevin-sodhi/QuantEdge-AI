package com.kevin.algo.indicators;

import com.kevin.algo.core.Candle;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class EMATest {

    private static Candle candle(double close) {
        return new Candle(LocalDate.now(), close, close, close, close, 1000L);
    }

    @Test
    void notReadyDuringWarmup() {
        EMA ema = new EMA(3);
        ema.accumulate(candle(10));
        ema.accumulate(candle(20));
        assertFalse(ema.isReady());
        assertTrue(Double.isNaN(ema.value()));
    }

    @Test
    void seededWithSMAAfterPeriodBars() {
        // First 3 bars seed with SMA: (10+20+30)/3 = 20
        EMA ema = new EMA(3);
        ema.accumulate(candle(10));
        ema.accumulate(candle(20));
        ema.accumulate(candle(30));
        assertTrue(ema.isReady());
        assertEquals(20.0, ema.value(), 1e-9);
    }

    @Test
    void exponentialSmoothingAppliedAfterSeed() {
        // alpha = 2/(3+1) = 0.5
        // seed = 20, then price 40 → EMA = 0.5*40 + 0.5*20 = 30
        EMA ema = new EMA(3);
        ema.accumulate(candle(10));
        ema.accumulate(candle(20));
        ema.accumulate(candle(30));
        ema.accumulate(candle(40));
        assertEquals(30.0, ema.value(), 1e-9);
    }

    @Test
    void emaCloserThanSmaToRecentPriceSpike() {
        // Feed flat prices to seed both indicators, then spike.
        // EMA puts more weight on recent bars, so it should be closer to the spike than SMA.
        EMA ema = new EMA(5);
        SMA sma = new SMA(5);
        double[] warmup = {10, 10, 10, 10, 10}; // seed both at 10
        for (double p : warmup) {
            Candle c = candle(p);
            ema.accumulate(c);
            sma.accumulate(c);
        }
        // Spike upward
        double spike = 50.0;
        Candle spikeCand = candle(spike);
        ema.accumulate(spikeCand);
        sma.accumulate(spikeCand);

        // EMA should be closer to 50 than SMA is
        double emaDistToSpike = Math.abs(ema.value() - spike);
        double smaDistToSpike = Math.abs(sma.value() - spike);
        assertTrue(emaDistToSpike < smaDistToSpike,
                "EMA (" + ema.value() + ") should be closer to spike than SMA (" + sma.value() + ")");
    }

    @Test
    void invalidPeriodThrows() {
        assertThrows(IllegalArgumentException.class, () -> new EMA(0));
    }
}
