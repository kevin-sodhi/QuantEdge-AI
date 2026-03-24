"""
data_service.py
---------------
Fetches OHLCV data from Yahoo Finance via yfinance and computes
technical indicators using the `ta` library.

fetch_ohlcv(ticker, period) → pandas DataFrame with columns:
    Date, Open, High, Low, Close, Volume

fetch_ohlcv_csv(ticker, period) → plain-text CSV string compatible
    with Java CsvDataFeed (expects Date,Open,High,Low,Close,Volume header)

fetch_indicators(ticker, period) → dict of indicator values for the
    most recent bar (RSI, MACD, EMA50, EMA200, BB upper/middle/lower, ATR, Vol ratio)
"""

import io
import yfinance as yf
import pandas as pd
import ta


VALID_PERIODS = {"1d", "5d", "1mo", "3mo", "6mo", "1y", "2y", "5y", "10y", "ytd", "max"}


def fetch_ohlcv(ticker: str, period: str = "1y") -> pd.DataFrame:
    """Return OHLCV DataFrame indexed by date string."""
    if period not in VALID_PERIODS:
        raise ValueError(f"Invalid period '{period}'. Must be one of: {sorted(VALID_PERIODS)}")

    ticker = ticker.upper().strip()

    # Use Ticker.history() — more reliable on cloud servers than yf.download()
    t = yf.Ticker(ticker)
    df = t.history(period=period, auto_adjust=True)

    if df.empty:
        # Fallback to yf.download() in case Ticker.history() fails
        df = yf.download(ticker, period=period, auto_adjust=True, progress=False)

    if df.empty:
        raise ValueError(f"No data returned for ticker '{ticker}'. Check the symbol.")

    # Flatten MultiIndex columns that yfinance sometimes returns
    if isinstance(df.columns, pd.MultiIndex):
        df.columns = df.columns.get_level_values(0)

    df = df[["Open", "High", "Low", "Close", "Volume"]].copy()
    df.index = pd.to_datetime(df.index).strftime("%Y-%m-%d")
    df.index.name = "Date"
    df = df.dropna()
    return df


def fetch_ohlcv_csv(ticker: str, period: str = "1y") -> str:
    """Return CSV string ready for Java CsvDataFeed."""
    df = fetch_ohlcv(ticker, period)
    buf = io.StringIO()
    df.to_csv(buf)
    return buf.getvalue()


def generate_signal(ticker: str, strategy: str = "momentum", period: str = "1y") -> dict:
    """
    Evaluate a trading strategy on the latest live data and return a signal.

    Strategies:
      momentum      — EMA50 > EMA200 + RSI 50-70 + volume above average → BUY
                      RSI > 70 → SELL, otherwise HOLD
      macrossover   — EMA50 vs EMA200 trend direction (no prev bar needed)
                      EMA50 > EMA200 → BUY trend, EMA50 < EMA200 → SELL trend
      meanreversion — price below BB lower + RSI < 35 → BUY
                      price above BB upper + RSI > 65 → SELL

    Returns signal (BUY / SELL / HOLD), price, date, indicators used, and reasons.
    """
    ind = fetch_indicators(ticker, period)

    price    = ind["last_close"]
    rsi      = ind["rsi"]
    ema50    = ind["ema50"]
    ema200   = ind["ema200"]
    bb_upper = ind["bb_upper"]
    bb_lower = ind["bb_lower"]
    vol_ratio = ind["volume_ratio"]

    signal  = "HOLD"
    reasons = []
    used    = {}

    strat = strategy.lower().replace(" ", "").replace("_", "")

    if strat == "momentum":
        used = {"rsi": rsi, "ema50": ema50, "ema200": ema200, "volume_ratio": vol_ratio}

        if None in used.values():
            signal = "HOLD"
            reasons = ["Indicators not ready — insufficient data"]
        elif ema50 > ema200 and 50 <= rsi <= 70 and vol_ratio > 1.0:
            signal = "BUY"
            reasons = ["EMA50 above EMA200 (uptrend)", "RSI in bullish range (50-70)", "Volume above average"]
        elif rsi > 70:
            signal = "SELL"
            reasons = ["RSI overbought (> 70)"]
        elif ema50 < ema200:
            signal = "SELL"
            reasons = ["EMA50 below EMA200 (downtrend)"]
        else:
            reasons = ["No confirmed signal — conditions not met"]

    elif strat in ("macrossover", "macross", "movingaveragecrossover"):
        used = {"ema50": ema50, "ema200": ema200}

        if None in used.values():
            signal = "HOLD"
            reasons = ["Indicators not ready — insufficient data"]
        elif ema50 > ema200:
            signal = "BUY"
            reasons = ["EMA50 above EMA200 — uptrend in effect"]
        elif ema50 < ema200:
            signal = "SELL"
            reasons = ["EMA50 below EMA200 — downtrend in effect"]
        else:
            reasons = ["EMA50 equals EMA200 — no clear trend"]

    elif strat in ("meanreversion", "meanrev"):
        used = {"rsi": rsi, "bb_upper": bb_upper, "bb_lower": bb_lower}

        if None in used.values():
            signal = "HOLD"
            reasons = ["Indicators not ready — insufficient data"]
        elif price < bb_lower and rsi is not None and rsi < 35:
            signal = "BUY"
            reasons = ["Price below lower Bollinger Band", "RSI oversold (< 35)"]
        elif price > bb_upper and rsi is not None and rsi > 65:
            signal = "SELL"
            reasons = ["Price above upper Bollinger Band", "RSI overbought (> 65)"]
        else:
            reasons = ["Price within Bollinger Bands — no mean reversion signal"]

    else:
        raise ValueError(f"Unknown strategy '{strategy}'. Use: momentum, macrossover, meanreversion")

    return {
        "ticker":     ticker.upper(),
        "strategy":   strategy,
        "signal":     signal,
        "price":      price,
        "date":       ind["last_date"],
        "indicators": used,
        "reasons":    reasons,
    }


def fetch_indicators(ticker: str, period: str = "1y") -> dict:
    """Compute technical indicators and return latest values."""
    df = fetch_ohlcv(ticker, period)

    close = df["Close"]
    high = df["High"]
    low = df["Low"]
    volume = df["Volume"]

    # RSI
    rsi_series = ta.momentum.RSIIndicator(close=close, window=14).rsi()

    # MACD
    macd_obj = ta.trend.MACD(close=close)
    macd_line = macd_obj.macd()
    macd_signal = macd_obj.macd_signal()
    macd_hist = macd_obj.macd_diff()

    # EMA
    ema50 = ta.trend.EMAIndicator(close=close, window=50).ema_indicator()
    ema200 = ta.trend.EMAIndicator(close=close, window=200).ema_indicator()

    # Bollinger Bands
    bb = ta.volatility.BollingerBands(close=close, window=20, window_dev=2)
    bb_upper = bb.bollinger_hband()
    bb_middle = bb.bollinger_mavg()
    bb_lower = bb.bollinger_lband()

    # ATR
    atr = ta.volatility.AverageTrueRange(high=high, low=low, close=close, window=14).average_true_range()

    # Volume ratio (current vs 20-day average)
    vol_sma20 = volume.rolling(20).mean()
    vol_ratio = volume / vol_sma20

    def _last(series):
        val = series.dropna().iloc[-1] if not series.dropna().empty else None
        return round(float(val), 4) if val is not None else None

    return {
        "ticker": ticker.upper(),
        "period": period,
        "last_date": str(df.index[-1]),
        "last_close": round(float(close.iloc[-1]), 4),
        "rsi": _last(rsi_series),
        "macd_line": _last(macd_line),
        "macd_signal": _last(macd_signal),
        "macd_hist": _last(macd_hist),
        "ema50": _last(ema50),
        "ema200": _last(ema200),
        "bb_upper": _last(bb_upper),
        "bb_middle": _last(bb_middle),
        "bb_lower": _last(bb_lower),
        "atr": _last(atr),
        "volume": int(volume.iloc[-1]),
        "volume_sma20": round(float(vol_sma20.dropna().iloc[-1]), 2) if not vol_sma20.dropna().empty else None,
        "volume_ratio": _last(vol_ratio),
    }
