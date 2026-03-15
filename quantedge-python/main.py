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

from fastapi import FastAPI, HTTPException, Query
from fastapi.responses import PlainTextResponse

from services.data_service import fetch_ohlcv, fetch_ohlcv_csv, fetch_indicators

app = FastAPI(
    title="QuantEdge Python Service",
    description="Real-time market data and indicators for the QuantEdge backtesting platform.",
    version="0.1.0",
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
