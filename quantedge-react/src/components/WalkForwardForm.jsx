import { useState } from 'react';

const PERIODS = [
  { value: '1y',  label: '1 Year'  },
  { value: '2y',  label: '2 Years' },
  { value: '5y',  label: '5 Years' },
];

const fieldStyle  = { display: 'flex', flexDirection: 'column', gap: '0.3rem' };
const labelStyle  = {
  fontSize: '0.72rem', color: 'var(--muted)',
  fontFamily: 'var(--font-mono)', letterSpacing: '0.06em', textTransform: 'uppercase',
};
const inputStyle  = { width: '100%' };

export default function WalkForwardForm({ onRun, loading }) {
  const [ticker,     setTicker]     = useState('AAPL');
  const [period,     setPeriod]     = useState('2y');
  const [trainRatio, setTrainRatio] = useState(0.7);

  const testPct  = Math.round((1 - trainRatio) * 100);
  const trainPct = Math.round(trainRatio * 100);

  return (
    <div className="card" style={{ display: 'flex', flexDirection: 'column', gap: '1.25rem' }}>

      {/* Info banner */}
      <div style={{
        background: 'rgba(0,255,136,0.06)', border: '1px solid rgba(0,255,136,0.2)',
        borderRadius: '8px', padding: '0.75rem 1rem',
        fontSize: '0.78rem', color: 'var(--muted)', lineHeight: 1.6,
        fontFamily: 'var(--font-mono)',
      }}>
        Grid-searches fast/slow MA periods on the <span style={{ color: 'var(--green)' }}>train</span> window,
        then evaluates the best params on unseen <span style={{ color: 'var(--amber)' }}>test</span> data.
      </div>

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

      {/* Train ratio slider */}
      <div style={fieldStyle}>
        <label style={labelStyle}>
          Train / Test Split
        </label>
        <input
          type="range"
          min="0.5" max="0.9" step="0.05"
          value={trainRatio}
          onChange={e => setTrainRatio(parseFloat(e.target.value))}
          style={{ width: '100%', accentColor: 'var(--green)', cursor: 'pointer' }}
        />
        <div style={{ display: 'flex', justifyContent: 'space-between', marginTop: '0.25rem' }}>
          <span style={{ fontSize: '0.75rem', color: 'var(--green)', fontFamily: 'var(--font-mono)' }}>
            TRAIN {trainPct}%
          </span>
          <span style={{ fontSize: '0.75rem', color: 'var(--amber)', fontFamily: 'var(--font-mono)' }}>
            TEST {testPct}%
          </span>
        </div>
      </div>

      <button
        className="btn-primary"
        onClick={() => onRun({ ticker, period, trainRatio, strategy: 'macrossover', indicator: 'sma' })}
        disabled={loading || !ticker}
        style={{ width: '100%' }}
      >
        {loading ? 'Running…' : 'Run Walk-Forward →'}
      </button>
    </div>
  );
}
