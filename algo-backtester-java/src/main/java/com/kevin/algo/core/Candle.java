package com.kevin.algo.core;
import java.time.LocalDate;
/**
 * Candle
 * ------
 * Immutable value object representing one OHLCV bar (daily for now).
 * We keep it tiny and immutable to avoid accidental state bugs in strategies.
 *
 * Fields:
 *   - date   : LocalDate (YYYY-MM-DD)
 *   - open   : double
 *   - high   : double
 *   - low    : double
 *   - close  : double
 *   - volume : long
 *
 * Later:
 *   - we can add timeframe, symbol, or an index if needed.
 */
public final class Candle {
    private final LocalDate date;
    private final double open;
    private final double high;
    private final double low;
    private final double close;
    private final long volume;


    public Candle(LocalDate date, double open, double high, double low, double close, long volume) {
        if (date == null) throw new IllegalArgumentException("date cannot be null");
        this.date = date;
        this.open = open;
        this.high = high;
        this.low = low;
        this.close = close;
        this.volume = volume;
    }

    public LocalDate getDate()  { return date;  }
    public double getOpen()     { return open;  }
    public double getHigh()     { return high;  }
    public double getLow()      { return low;   }
    public double getClose()    { return close; }
    public long getVolume()     { return volume;}

    public String toString() {
        return "Candle{" +
            "date=" + date +
            ", opn=" + open +
            ", hgh=" + high +
            ", low=" + low +
            ", close=" + close +
            ", volume=" + volume +
            '}';
    }



}
