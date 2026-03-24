package com.kevin.algo.web.dto;

import java.util.List;

/**
 * BacktestRequest — body of POST /api/backtest.
 *
 * Node.js sends this after fetching OHLCV from Python FastAPI.
 * All candle data travels in memory — no CSV files on disk.
 */
public class BacktestRequest {
    /** OHLCV bars from Python /data endpoint. */
    public List<CandleData> candles;

    /** Ticker symbol — for audit trail in response (e.g. "AAPL"). */
    public String ticker     = "";

    /** yfinance period — for audit trail (e.g. "1y"). */
    public String period     = "1y";

    /** Strategy name: macrossover | momentum | meanreversion */
    public String strategy   = "macrossover";

    /** Indicator type for MA crossover: sma | ema */
    public String indicator  = "sma";

    /** Fast MA period (macrossover only). */
    public int    fast        = 5;

    /** Slow MA period (macrossover only). */
    public int    slow        = 20;

    /** Starting portfolio cash. */
    public double initialCapital = 10_000.0;

    /** Per-trade commission as fraction (0.001 = 0.1%). */
    public double fee         = 0.0;

    /** Slippage as fraction. */
    public double slip        = 0.0;
}
