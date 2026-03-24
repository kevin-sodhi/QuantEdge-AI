"""
QuantEdge Python Service — Phase 2
===================================
FastAPI service on port 8000. Provides live market data and technical
indicators to the Node.js webserver.

Endpoints:
  GET /health                         — service health check
  GET /data?ticker=AAPL&period=1y     — OHLCV data as JSON
  GET /data/csv?ticker=AAPL&period=1y — OHLCV as plain CSV (for Java engine)
  GET /indicators?ticker=AAPL&period=1y — technical indicators (latest bar)

Start:
  cd quantedge-python
  pip install -r requirements.txt
  uvicorn main:app --reload --port 8000
"""

import os

from fastapi import FastAPI, HTTPException, Query
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import PlainTextResponse
from pydantic import BaseModel
import httpx

from services.data_service import fetch_ohlcv, fetch_ohlcv_csv, fetch_indicators, generate_signal

JAVA_SERVICE_URL = os.environ.get("JAVA_SERVICE_URL", "http://localhost:8080")


class BacktestRequest(BaseModel):
    ticker: str
    period: str = "1y"
    strategy: str = "macrossover"
    indicator: str = "sma"
    fast: int = 5
    slow: int = 20
    initialCapital: float = 10000

app = FastAPI(
    title="QuantEdge Python Service",
    description="Real-time market data and indicators for the QuantEdge backtesting platform.",
    version="0.1.0",
)

# Allow cross-origin requests from React frontend (dev + Railway prod)
_cors_origins = os.environ.get("CORS_ORIGIN", "*").split(",")
app.add_middleware(
    CORSMiddleware,
    allow_origins=_cors_origins,
    allow_methods=["*"],
    allow_headers=["*"],
)


@app.get("/health")
def health():
    return {"status": "ok", "service": "quantedge-python", "version": "0.1.0"}


@app.get("/data")
def get_data(
    ticker: str = Query(..., description="Ticker symbol, e.g. AAPL"),
    period: str = Query("1y", description="yfinance period: 1d, 5d, 1mo, 3mo, 6mo, 1y, 2y, 5y, 10y, ytd, max"),
):
    """Return OHLCV data as JSON records."""
    try:
        df = fetch_ohlcv(ticker, period)
    except ValueError as e:
        raise HTTPException(status_code=400, detail=str(e))
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Data fetch failed: {e}")

    records = df.reset_index().to_dict(orient="records")
    return {
        "ticker": ticker.upper(),
        "period": period,
        "bars": len(records),
        "data": records,
    }


@app.get("/data/csv", response_class=PlainTextResponse)
def get_data_csv(
    ticker: str = Query(..., description="Ticker symbol, e.g. AAPL"),
    period: str = Query("1y", description="yfinance period"),
):
    """Return OHLCV as plain CSV — consumed by the Java CsvDataFeed."""
    try:
        csv_text = fetch_ohlcv_csv(ticker, period)
    except ValueError as e:
        raise HTTPException(status_code=400, detail=str(e))
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Data fetch failed: {e}")

    return PlainTextResponse(content=csv_text, media_type="text/plain")


@app.get("/indicators")
def get_indicators(
    ticker: str = Query(..., description="Ticker symbol, e.g. AAPL"),
    period: str = Query("1y", description="yfinance period"),
):
    """Return technical indicators for the most recent bar."""
    try:
        result = fetch_indicators(ticker, period)
    except ValueError as e:
        raise HTTPException(status_code=400, detail=str(e))
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Indicator computation failed: {e}")

    return result


@app.get("/price-signal")
def get_price_signal(
    ticker:   str = Query(...,      description="Ticker symbol, e.g. AAPL"),
    strategy: str = Query("momentum", description="Strategy: momentum, macrossover, meanreversion"),
    period:   str = Query("1y",     description="yfinance period: 1y, 2y, 5y, etc."),
):
    """
    Evaluate a strategy on live market data and return a BUY / SELL / HOLD signal.
    This is the bridge between backtesting and live trading — called by the execution
    agent in Phase 8 to decide whether to place a paper order on Alpaca.
    """
    try:
        result = generate_signal(ticker, strategy, period)
    except ValueError as e:
        raise HTTPException(status_code=400, detail=str(e))
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Signal generation failed: {e}")

    return result


@app.post("/api/backtest")
async def run_backtest(req: BacktestRequest):
    """Orchestrate backtest: fetch OHLCV from Python, run strategy on Java engine."""
    if not req.ticker:
        raise HTTPException(status_code=400, detail="ticker is required")

    if req.strategy == "macrossover":
        if req.fast <= 0 or req.slow <= 0:
            raise HTTPException(status_code=400, detail="fast and slow must be positive integers")
        if req.fast >= req.slow:
            raise HTTPException(status_code=400, detail="fast must be less than slow")

    try:
        df = fetch_ohlcv(req.ticker.upper(), req.period)
    except ValueError as e:
        raise HTTPException(status_code=400, detail=str(e))
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Data fetch failed: {e}")

    candles = [
        {
            "date":   str(row.get("Date", row.get("date", ""))),
            "open":   row.get("Open",   row.get("open")),
            "high":   row.get("High",   row.get("high")),
            "low":    row.get("Low",    row.get("low")),
            "close":  row.get("Close",  row.get("close")),
            "volume": row.get("Volume", row.get("volume")),
        }
        for row in df.reset_index().to_dict(orient="records")
    ]

    payload = {
        "candles":        candles,
        "ticker":         req.ticker.upper(),
        "period":         req.period,
        "strategy":       req.strategy,
        "indicator":      req.indicator,
        "fast":           req.fast,
        "slow":           req.slow,
        "initialCapital": req.initialCapital,
    }

    try:
        async with httpx.AsyncClient(timeout=30.0) as client:
            java_res = await client.post(f"{JAVA_SERVICE_URL}/api/backtest", json=payload)
    except httpx.ConnectError:
        raise HTTPException(status_code=502, detail="Cannot reach Java service at port 8080")
    except Exception as e:
        raise HTTPException(status_code=502, detail=f"Java service error: {e}")

    if java_res.status_code != 200:
        raise HTTPException(status_code=java_res.status_code, detail=java_res.text)

    return java_res.json()
