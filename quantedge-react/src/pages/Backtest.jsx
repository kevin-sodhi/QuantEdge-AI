import { useState } from 'react';
import Navbar           from '../components/Navbar.jsx';
import CandlestickChart from '../components/CandlestickChart.jsx';
import BacktestForm     from '../components/BacktestForm.jsx';
import MetricsTable     from '../components/MetricsTable.jsx';

// In dev: env vars are empty → Vite proxy handles routing via vite.config.js
// In prod (Railway): set VITE_PYTHON_URL and VITE_NODE_URL to Railway service URLs
const PYTHON_URL = import.meta.env.VITE_PYTHON_URL || '';
const NODE_URL   = import.meta.env.VITE_NODE_URL   || '';

/** Normalise Python /data response → lightweight-charts candle format */
function normaliseCandles(rawData) {
  return rawData
    .map(r => ({
      time:  r.Date   ?? r.date,
      open:  r.Open   ?? r.open,
      high:  r.High   ?? r.high,
      low:   r.Low    ?? r.low,
      close: r.Close  ?? r.close,
    }))
    .filter(c => c.time)
    .sort((a, b) => (a.time > b.time ? 1 : -1));
}

export default function Backtest() {
  const [candles,  setCandles]  = useState([]);
  const [signals,  setSignals]  = useState([]);
  const [metrics,  setMetrics]  = useState(null);
  const [loading,  setLoading]  = useState(null);  // 'chart' | 'backtest' | null
  const [error,    setError]    = useState('');
  const [ticker,   setTicker]   = useState('');

  async function handleLoadChart({ ticker, period }) {
    setError('');
    setLoading('chart');
    setSignals([]);
    setMetrics(null);
    setTicker(ticker);
    try {
      const dataPath = PYTHON_URL ? `${PYTHON_URL}/data` : '/python/data';
      const res  = await fetch(`${dataPath}?ticker=${encodeURIComponent(ticker)}&period=${encodeURIComponent(period)}`);
      if (!res.ok) throw new Error(`Data fetch failed: ${res.status}`);
      const json = await res.json();
      setCandles(normaliseCandles(json.data || []));
    } catch (e) {
      setError(e.message);
    } finally {
      setLoading(null);
    }
  }

  async function handleRunBacktest(params) {
    setError('');
    setLoading('backtest');
    setSignals([]);
    setMetrics(null);

    // Load chart first if not loaded
    if (!candles.length) {
      await handleLoadChart({ ticker: params.ticker, period: params.period });
    }

    try {
      const res = await fetch(`${NODE_URL}/api/backtest`, {
        method:  'POST',
        headers: { 'Content-Type': 'application/json' },
        body:    JSON.stringify(params),
      });
      if (!res.ok) {
        const err = await res.json().catch(() => ({}));
        throw new Error(err.error || `Backtest failed: ${res.status}`);
      }
      const result = await res.json();
      setSignals(result.signals || []);
      setMetrics(result.metrics || null);
    } catch (e) {
      setError(e.message);
    } finally {
      setLoading(null);
    }
  }

  return (
    <div style={{ minHeight: '100vh', background: 'var(--black)' }}>
      <Navbar />

      <main style={{ paddingTop: '120px', paddingBottom: '4rem', maxWidth: '1300px', margin: '0 auto', padding: '120px 2rem 4rem' }}>
        {/* Header */}
        <div style={{ marginBottom: '2rem' }}>
          <h1 style={{ fontFamily: 'var(--font-head)', fontSize: '2.5rem', letterSpacing: '0.06em', color: 'var(--white)' }}>
            BACKTEST <span style={{ color: 'var(--green)' }}>{ticker || 'DASHBOARD'}</span>
          </h1>
          <p style={{ color: 'var(--muted)', fontSize: '0.875rem', marginTop: '0.25rem' }}>
            Live OHLCV data via yfinance · Java backtesting engine · Signal overlay
          </p>
        </div>

        {error && (
          <div style={{
            background: 'rgba(255,61,90,0.1)', border: '1px solid var(--red)',
            color: 'var(--red)', borderRadius: '8px', padding: '0.75rem 1rem',
            fontSize: '0.875rem', marginBottom: '1.5rem', fontFamily: 'var(--font-mono)',
          }}>
            {error}
          </div>
        )}

        <div style={{ display: 'grid', gridTemplateColumns: '1fr 320px', gap: '1.5rem', alignItems: 'start' }}>
          {/* Left: Chart */}
          <div style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
            <CandlestickChart candles={candles} signals={signals} height={460} />

            {/* Signal legend */}
            {signals.length > 0 && (
              <div style={{ display: 'flex', gap: '1.5rem' }}>
                <span style={{ fontFamily: 'var(--font-mono)', fontSize: '0.8rem', color: 'var(--green)' }}>
                  ▲ BUY ({signals.filter(s => s.type === 'BUY').length})
                </span>
                <span style={{ fontFamily: 'var(--font-mono)', fontSize: '0.8rem', color: 'var(--red)' }}>
                  ▼ SELL ({signals.filter(s => s.type === 'SELL').length})
                </span>
              </div>
            )}
          </div>

          {/* Right: Form + Metrics */}
          <div style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
            <BacktestForm
              onLoadChart={handleLoadChart}
              onRunBacktest={handleRunBacktest}
              loading={loading}
            />
            {metrics && <MetricsTable metrics={metrics} />}
          </div>
        </div>
      </main>
    </div>
  );
}
