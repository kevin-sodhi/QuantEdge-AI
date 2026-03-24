import { useState } from 'react';

const STRATEGIES = [
  { value: 'macrossover',  label: 'MA Crossover' },
  { value: 'momentum',     label: 'Momentum' },
  { value: 'meanreversion',label: 'Mean Reversion' },
];

const PERIODS = [
  { value: '3mo', label: '3 Months' },
  { value: '6mo', label: '6 Months' },
  { value: '1y',  label: '1 Year'   },
  { value: '2y',  label: '2 Years'  },
  { value: '5y',  label: '5 Years'  },
];

export default function BacktestForm({ onLoadChart, onRunBacktest, loading }) {
  const [ticker,   setTicker]   = useState('AAPL');
  const [period,   setPeriod]   = useState('1y');
  const [strategy, setStrategy] = useState('momentum');
  const [fast,     setFast]     = useState(5);
  const [slow,     setSlow]     = useState(20);

  const isMaCross = strategy === 'macrossover';

  const fieldStyle = {
    display: 'flex', flexDirection: 'column', gap: '0.3rem',
  };
  const labelStyle = {
    fontSize: '0.72rem', color: 'var(--muted)',
    fontFamily: 'var(--font-mono)', letterSpacing: '0.06em', textTransform: 'uppercase',
  };
  const inputStyle = { width: '100%' };

  return (
    <div className="card" style={{ display: 'flex', flexDirection: 'column', gap: '1.25rem' }}>
      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1rem' }}>
        <div style={fieldStyle}>
          <label style={labelStyle}>Ticker</label>
          <input
            style={inputStyle}
            value={ticker}
            onChange={e => setTicker(e.target.value.toUpperCase())}
            placeholder="AAPL"
            maxLength={10}
          />
        </div>
        <div style={fieldStyle}>
          <label style={labelStyle}>Period</label>
          <select style={inputStyle} value={period} onChange={e => setPeriod(e.target.value)}>
            {PERIODS.map(p => <option key={p.value} value={p.value}>{p.label}</option>)}
          </select>
        </div>
      </div>

      <div style={fieldStyle}>
        <label style={labelStyle}>Strategy</label>
        <select style={inputStyle} value={strategy} onChange={e => setStrategy(e.target.value)}>
          {STRATEGIES.map(s => <option key={s.value} value={s.value}>{s.label}</option>)}
        </select>
      </div>

      {isMaCross && (
        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1rem' }}>
          <div style={fieldStyle}>
            <label style={labelStyle}>Fast MA</label>
            <input style={inputStyle} type="number" min="1" value={fast} onChange={e => setFast(+e.target.value)} />
          </div>
          <div style={fieldStyle}>
            <label style={labelStyle}>Slow MA</label>
            <input style={inputStyle} type="number" min="2" value={slow} onChange={e => setSlow(+e.target.value)} />
          </div>
        </div>
      )}

      <div style={{ display: 'flex', gap: '0.75rem' }}>
        <button
          className="btn-ghost"
          style={{ flex: 1 }}
          onClick={() => onLoadChart({ ticker, period })}
          disabled={loading || !ticker}
        >
          {loading === 'chart' ? 'Loading…' : 'Load Chart'}
        </button>
        <button
          className="btn-primary"
          style={{ flex: 1 }}
          onClick={() => onRunBacktest({ ticker, period, strategy, fast, slow })}
          disabled={loading || !ticker}
        >
          {loading === 'backtest' ? 'Running…' : 'Run Backtest →'}
        </button>
      </div>
    </div>
  );
}
