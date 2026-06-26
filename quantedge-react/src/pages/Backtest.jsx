import { useState } from 'react';
import Navbar              from '../components/Navbar.jsx';
import CandlestickChart    from '../components/CandlestickChart.jsx';
import BacktestForm        from '../components/BacktestForm.jsx';
import MetricsTable        from '../components/MetricsTable.jsx';
import WalkForwardForm     from '../components/WalkForwardForm.jsx';
import WalkForwardResults  from '../components/WalkForwardResults.jsx';

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
  const [mode,     setMode]     = useState('backtest'); // 'backtest' | 'walkforward'

  // Backtest state
  const [candles,  setCandles]  = useState([]);
  const [signals,  setSignals]  = useState([]);
  const [metrics,  setMetrics]  = useState(null);
  const [loading,  setLoading]  = useState(null);  // 'chart' | 'backtest' | 'walkforward' | null
  const [error,    setError]    = useState('');
  const [ticker,   setTicker]   = useState('');

  // Walk-forward state
  const [wfResult, setWfResult] = useState(null);

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
      const res = await fetch(`${PYTHON_URL}/api/backtest`, {
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

  async function handleRunWalkForward(params) {
    setError('');
    setLoading('walkforward');
    setWfResult(null);
    try {
      const res = await fetch(`${PYTHON_URL}/api/walk-forward`, {
        method:  'POST',
        headers: { 'Content-Type': 'application/json' },
        body:    JSON.stringify(params),
      });
      if (!res.ok) {
        const err = await res.json().catch(() => ({}));
        throw new Error(err.detail || err.error || `Walk-forward failed: ${res.status}`);
      }
      setWfResult(await res.json());
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
        <div style={{ marginBottom: '1.5rem' }}>
          <h1 style={{ fontFamily: 'var(--font-head)', fontSize: 'clamp(1.6rem, 5vw, 2.5rem)', letterSpacing: '0.06em', color: 'var(--white)', wordBreak: 'break-word' }}>
            BACKTEST <span style={{ color: 'var(--green)' }}>{ticker || 'DASHBOARD'}</span>
          </h1>
          <p style={{ color: 'var(--muted)', fontSize: '0.875rem', marginTop: '0.25rem', lineHeight: 1.6 }}>
            Live OHLCV data via Twelve Data · Java backtesting engine · Signal overlay
          </p>
        </div>

        {/* Mode toggle */}
        <div style={{ display: 'flex', gap: '0.5rem', marginBottom: '1.5rem' }}>
          {[
            { key: 'backtest',    label: 'Single Backtest' },
            { key: 'walkforward', label: 'Walk-Forward Validation' },
          ].map(({ key, label }) => (
            <button
              key={key}
              onClick={() => { setMode(key); setError(''); }}
              style={{
                padding: '0.5rem 1.25rem',
                borderRadius: '6px',
                fontFamily: 'var(--font-mono)',
                fontSize: '0.78rem',
                letterSpacing: '0.04em',
                border: mode === key ? '1px solid var(--green)' : '1px solid var(--border)',
                background: mode === key ? 'rgba(0,255,136,0.08)' : 'transparent',
                color: mode === key ? 'var(--green)' : 'var(--muted)',
                cursor: 'pointer',
                transition: 'all 0.2s',
              }}
            >
              {label}
            </button>
          ))}
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

        {/* Single backtest layout */}
        {mode === 'backtest' && (
          <div className="backtest-layout">
            <div className="backtest-form-col">
              <BacktestForm
                onLoadChart={handleLoadChart}
                onRunBacktest={handleRunBacktest}
                loading={loading}
              />
            </div>

            <div className="backtest-chart-col">
              <CandlestickChart candles={candles} signals={signals} height={460} />
              {signals.length > 0 && (
                <div style={{ display: 'flex', gap: '1.5rem', marginTop: '0.75rem' }}>
                  <span style={{ fontFamily: 'var(--font-mono)', fontSize: '0.8rem', color: 'var(--green)' }}>
                    ▲ BUY ({signals.filter(s => s.type === 'BUY').length})
                  </span>
                  <span style={{ fontFamily: 'var(--font-mono)', fontSize: '0.8rem', color: 'var(--red)' }}>
                    ▼ SELL ({signals.filter(s => s.type === 'SELL').length})
                  </span>
                </div>
              )}
            </div>

            {metrics && (
              <div className="backtest-metrics-col">
                <MetricsTable metrics={metrics} />
              </div>
            )}
          </div>
        )}

        {/* Walk-forward layout */}
        {mode === 'walkforward' && (
          <div className="backtest-layout">
            <div className="backtest-form-col">
              <WalkForwardForm
                onRun={handleRunWalkForward}
                loading={loading === 'walkforward'}
              />
            </div>

            <div className="backtest-chart-col">
              {!wfResult && !loading && (
                <div style={{
                  height: '460px', display: 'flex', flexDirection: 'column',
                  alignItems: 'center', justifyContent: 'center',
                  background: 'var(--panel)', border: '1px solid var(--border)',
                  borderRadius: '12px', gap: '1rem',
                }}>
                  <div style={{ fontSize: '2rem' }}>📊</div>
                  <p style={{ color: 'var(--muted)', fontFamily: 'var(--font-mono)', fontSize: '0.85rem', textAlign: 'center', lineHeight: 1.8 }}>
                    Set a ticker and train/test split,<br />then run Walk-Forward Validation.
                  </p>
                </div>
              )}
              {loading === 'walkforward' && (
                <div style={{
                  height: '460px', display: 'flex', flexDirection: 'column',
                  alignItems: 'center', justifyContent: 'center',
                  background: 'var(--panel)', border: '1px solid var(--border)',
                  borderRadius: '12px', gap: '1rem',
                }}>
                  <div style={{
                    width: '40px', height: '40px', borderRadius: '50%',
                    border: '3px solid var(--border)', borderTopColor: 'var(--green)',
                    animation: 'spin 0.8s linear infinite',
                  }} />
                  <p style={{ color: 'var(--muted)', fontFamily: 'var(--font-mono)', fontSize: '0.82rem' }}>
                    Grid-searching parameters…
                  </p>
                </div>
              )}
              {wfResult && <WalkForwardResults result={wfResult} />}
            </div>
          </div>
        )}
      </main>
    </div>
  );
}
