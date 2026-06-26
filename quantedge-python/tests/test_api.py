"""
API integration tests for the QuantEdge Python service.

All external I/O is mocked — no Twelve Data key or running Java service needed.
Run with: pytest tests/ -v
"""

import json
from unittest.mock import AsyncMock, MagicMock, patch

import pandas as pd
import pytest
from httpx import AsyncClient, ASGITransport

from main import app

# ---------------------------------------------------------------------------
# Fixtures
# ---------------------------------------------------------------------------

@pytest.fixture
def sample_df():
    """Minimal 5-bar OHLCV DataFrame matching the shape fetch_ohlcv() returns."""
    dates = ["2024-01-01", "2024-01-02", "2024-01-03", "2024-01-04", "2024-01-05"]
    data = {
        "Open":   [100.0, 101.0, 102.0, 103.0, 104.0],
        "High":   [105.0, 106.0, 107.0, 108.0, 109.0],
        "Low":    [ 98.0,  99.0, 100.0, 101.0, 102.0],
        "Close":  [101.0, 102.0, 103.0, 104.0, 105.0],
        "Volume": [1_000_000] * 5,
    }
    df = pd.DataFrame(data, index=pd.Index(dates, name="Date"))
    return df


@pytest.fixture
def java_backtest_response():
    return {
        "ok": True,
        "metrics": {
            "barsRead": 5, "trades": 1, "netPnl": 250.0,
            "totalReturnPct": 2.5, "winRatePct": 100.0,
            "maxDrawdown": 0.0, "sharpe": 1.8,
        },
        "signals": [{"date": "2024-01-03", "price": 103.0, "type": "BUY"}],
        "series": [],
        "equity": [{"date": "2024-01-01", "equity": 10000.0}],
    }


# ---------------------------------------------------------------------------
# Health
# ---------------------------------------------------------------------------

@pytest.mark.anyio
async def test_health_returns_ok():
    async with AsyncClient(transport=ASGITransport(app=app), base_url="http://test") as client:
        resp = await client.get("/health")
    assert resp.status_code == 200
    body = resp.json()
    assert body["status"] == "ok"
    assert body["service"] == "quantedge-python"


# ---------------------------------------------------------------------------
# /api/backtest — input validation
# ---------------------------------------------------------------------------

@pytest.mark.anyio
async def test_backtest_rejects_empty_ticker():
    async with AsyncClient(transport=ASGITransport(app=app), base_url="http://test") as client:
        resp = await client.post("/api/backtest", json={
            "ticker": "", "strategy": "macrossover", "fast": 5, "slow": 20,
        })
    assert resp.status_code == 400
    assert "ticker" in resp.json()["detail"].lower()


@pytest.mark.anyio
async def test_backtest_rejects_fast_equal_to_slow():
    async with AsyncClient(transport=ASGITransport(app=app), base_url="http://test") as client:
        resp = await client.post("/api/backtest", json={
            "ticker": "AAPL", "strategy": "macrossover",
            "fast": 10, "slow": 10,
        })
    assert resp.status_code == 400
    assert "fast" in resp.json()["detail"].lower()


@pytest.mark.anyio
async def test_backtest_rejects_fast_greater_than_slow():
    async with AsyncClient(transport=ASGITransport(app=app), base_url="http://test") as client:
        resp = await client.post("/api/backtest", json={
            "ticker": "AAPL", "strategy": "macrossover",
            "fast": 20, "slow": 5,
        })
    assert resp.status_code == 400


@pytest.mark.anyio
async def test_backtest_rejects_zero_fast():
    async with AsyncClient(transport=ASGITransport(app=app), base_url="http://test") as client:
        resp = await client.post("/api/backtest", json={
            "ticker": "AAPL", "strategy": "macrossover",
            "fast": 0, "slow": 20,
        })
    assert resp.status_code == 400


# ---------------------------------------------------------------------------
# /api/backtest — happy path with mocked external services
# ---------------------------------------------------------------------------

@pytest.mark.anyio
async def test_backtest_returns_java_response(sample_df, java_backtest_response):
    mock_response = MagicMock()
    mock_response.status_code = 200
    mock_response.json.return_value = java_backtest_response

    mock_client = AsyncMock()
    mock_client.post = AsyncMock(return_value=mock_response)
    mock_client.__aenter__ = AsyncMock(return_value=mock_client)
    mock_client.__aexit__ = AsyncMock(return_value=False)

    with patch("main.fetch_ohlcv", return_value=sample_df), \
         patch("httpx.AsyncClient", return_value=mock_client):
        async with AsyncClient(transport=ASGITransport(app=app), base_url="http://test") as client:
            resp = await client.post("/api/backtest", json={
                "ticker": "AAPL", "period": "1y",
                "strategy": "macrossover", "fast": 5, "slow": 20,
                "initialCapital": 10000,
            })

    assert resp.status_code == 200
    body = resp.json()
    assert body["ok"] is True
    assert body["metrics"]["trades"] == 1
    assert body["metrics"]["netPnl"] == 250.0


@pytest.mark.anyio
async def test_backtest_passes_correct_candle_count_to_java(sample_df, java_backtest_response):
    captured_payload = {}

    mock_response = MagicMock()
    mock_response.status_code = 200
    mock_response.json.return_value = java_backtest_response

    async def capture_post(url, json=None, **kwargs):
        captured_payload.update(json or {})
        return mock_response

    mock_client = AsyncMock()
    mock_client.post = capture_post
    mock_client.__aenter__ = AsyncMock(return_value=mock_client)
    mock_client.__aexit__ = AsyncMock(return_value=False)

    with patch("main.fetch_ohlcv", return_value=sample_df), \
         patch("httpx.AsyncClient", return_value=mock_client):
        async with AsyncClient(transport=ASGITransport(app=app), base_url="http://test") as client:
            await client.post("/api/backtest", json={
                "ticker": "AAPL", "strategy": "macrossover", "fast": 5, "slow": 20,
            })

    assert len(captured_payload["candles"]) == len(sample_df)
    assert captured_payload["ticker"] == "AAPL"


@pytest.mark.anyio
async def test_backtest_returns_502_when_java_unreachable(sample_df):
    import httpx as _httpx

    mock_client = AsyncMock()
    mock_client.post = AsyncMock(side_effect=_httpx.ConnectError("refused"))
    mock_client.__aenter__ = AsyncMock(return_value=mock_client)
    mock_client.__aexit__ = AsyncMock(return_value=False)

    with patch("main.fetch_ohlcv", return_value=sample_df), \
         patch("httpx.AsyncClient", return_value=mock_client):
        async with AsyncClient(transport=ASGITransport(app=app), base_url="http://test") as client:
            resp = await client.post("/api/backtest", json={
                "ticker": "AAPL", "strategy": "macrossover", "fast": 5, "slow": 20,
            })

    assert resp.status_code == 502


# ---------------------------------------------------------------------------
# /data endpoint
# ---------------------------------------------------------------------------

@pytest.mark.anyio
async def test_data_endpoint_returns_correct_shape(sample_df):
    with patch("main.fetch_ohlcv", return_value=sample_df):
        async with AsyncClient(transport=ASGITransport(app=app), base_url="http://test") as client:
            resp = await client.get("/data", params={"ticker": "AAPL", "period": "1y"})

    assert resp.status_code == 200
    body = resp.json()
    assert body["ticker"] == "AAPL"
    assert body["bars"] == 5
    assert len(body["data"]) == 5
    first = body["data"][0]
    assert "Close" in first
    assert "Volume" in first


@pytest.mark.anyio
async def test_data_endpoint_propagates_value_error():
    with patch("main.fetch_ohlcv", side_effect=ValueError("Invalid ticker")):
        async with AsyncClient(transport=ASGITransport(app=app), base_url="http://test") as client:
            resp = await client.get("/data", params={"ticker": "INVALID!!!", "period": "1y"})

    assert resp.status_code == 400
