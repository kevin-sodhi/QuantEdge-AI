package com.kevin.algo.web.dto;

import java.time.LocalDate;

import com.kevin.algo.core.Candle;

/**
 * CandleData — incoming JSON shape for a single OHLCV bar.
 *
 * Matches the record format Python FastAPI returns from /data:
 *   { "date": "2024-01-15", "open": 180.0, "high": 185.0,
 *     "low": 178.0, "close": 182.0, "volume": 1000000 }
 */
public class CandleData {
    public String date;
    public double open;
    public double high;
    public double low;
    public double close;
    public long   volume;

    /** Convert to the engine's immutable Candle value object. */
    public Candle toCandle() {
        return new Candle(LocalDate.parse(date), open, high, low, close, volume);
    }
}
