import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import Navbar     from '../components/Navbar.jsx';
import TickerTape from '../components/TickerTape.jsx';

const TERMINAL_OUTPUT = `$ quantedge backtest --ticker AAPL --strategy momentum

[INFO] Fetching OHLCV from Python FastAPI...
[INFO] 252 bars loaded  (2024-01-02 → 2025-01-02)
[INFO] Running MomentumStrategy on Java engine...

{
  "ok": true,
  "metrics": {
    "totalReturnPct": 24.81,
    "sharpe": 1.74,
    "maxDrawdown": -8.32,
    "trades": 7
  },
  "signals": [
    { "date": "2024-02-14", "type": "BUY",  "price": 184.15 },
    { "date": "2024-08-05", "type": "SELL", "price": 209.82 },
    ...
  ]
}

[OK] Backtest complete in 312ms`;

const FEATURES = [
  { icon: '📈', title: 'Price Strategy Engine',    tag: 'Java Backend',          desc: 'MA Crossover, Momentum, Mean Reversion strategies with walk-forward validation.' },
  { icon: '🧠', title: 'ML Signal Generation',     tag: 'Python · PyTorch',      desc: 'XGBoost classifier, LSTM price predictor, CNN pattern recognition.' },
  { icon: '💬', title: 'NLP Sentiment Layer',      tag: 'FinBERT · HuggingFace', desc: 'FinBERT scores earnings transcripts, news headlines, and Reddit sentiment.' },
  { icon: '⚡', title: 'Real-Time Data Feed',      tag: 'yfinance · Alpaca API', desc: 'Live OHLCV via Yahoo Finance. Paper trading via Alpaca.' },
  { icon: '📊', title: 'Portfolio Optimizer',      tag: 'NumPy · SciPy',         desc: 'Markowitz mean-variance optimization, Kelly criterion position sizing.' },
  { icon: '🔬', title: 'Research Dashboard',       tag: 'React · Lightweight Charts', desc: 'Candlestick charts with signal overlays, equity curves, and walk-forward results.' },
];

const STATS = [
  { value: '3',      label: 'Strategies Live'  },
  { value: '1.74',   label: 'Avg Sharpe Ratio' },
  { value: 'Live',   label: 'Data Source'      },
  { value: 'FinBERT',label: 'NLP Model'        },
  { value: 'Java',   label: 'Execution Engine' },
];

export default function Home() {
  const navigate  = useNavigate();
  const [ctaTicker, setCtaTicker] = useState('');

  return (
    <div style={{ background: 'var(--black)', minHeight: '100vh' }}>
      <Navbar />
      <TickerTape />

      {/* Hero */}
      <section className="hero-section">
        {/* Left copy */}
        <div>
          <div style={{
            display: 'inline-block', marginBottom: '1.5rem',
            background: 'var(--green-glow)', border: '1px solid rgba(0,255,136,0.25)',
            borderRadius: '20px', padding: '0.35rem 1rem',
            fontFamily: 'var(--font-mono)', fontSize: '0.72rem',
            color: 'var(--green)', letterSpacing: '0.08em',
          }}>
            ◆ SYSTEMATIC ALPHA RESEARCH PLATFORM
          </div>

          <h1 style={{
            fontFamily: 'var(--font-head)', fontSize: 'clamp(3rem, 6vw, 5rem)',
            lineHeight: 0.95, letterSpacing: '0.02em', color: 'var(--white)',
            marginBottom: '1.5rem',
          }}>
            TRADE SMARTER<br />
            <span style={{ color: 'var(--green)' }}>NOT HARDER</span>
          </h1>

          <p style={{ color: 'var(--muted)', fontSize: '1rem', lineHeight: 1.7, marginBottom: '2rem', maxWidth: '420px' }}>
            Multi-signal alpha research platform combining Java execution,
            Python ML, and FinBERT NLP into one systematic trading framework.
          </p>

          <div style={{ display: 'flex', gap: '1rem' }}>
            <button className="btn-primary" onClick={() => navigate('/backtest')}>
              Launch Backtest →
            </button>
            <button className="btn-ghost">View Strategies</button>
          </div>
        </div>

        {/* Right: fake terminal */}
        <div style={{
          background: '#1a1a1a', borderRadius: '12px',
          border: '1px solid var(--border)', overflow: 'hidden',
          boxShadow: '0 24px 80px rgba(0,0,0,0.6)',
        }}>
          {/* Terminal chrome */}
          <div style={{
            background: '#2a2a2a', padding: '0.75rem 1rem',
            display: 'flex', alignItems: 'center', gap: '0.5rem',
            borderBottom: '1px solid var(--border)',
          }}>
            <span style={{ width: '12px', height: '12px', borderRadius: '50%', background: '#ff5f57', display: 'block' }} />
            <span style={{ width: '12px', height: '12px', borderRadius: '50%', background: '#febc2e', display: 'block' }} />
            <span style={{ width: '12px', height: '12px', borderRadius: '50%', background: '#28c840', display: 'block' }} />
            <span style={{ marginLeft: '1rem', fontFamily: 'var(--font-mono)', fontSize: '0.72rem', color: 'var(--muted)' }}>
              quantedge — bash
            </span>
          </div>
          <pre style={{
            padding: '1.25rem', margin: 0,
            fontFamily: 'var(--font-mono)', fontSize: '0.72rem',
            color: '#a8d8a8', lineHeight: 1.6, overflowX: 'auto',
          }}>
            {TERMINAL_OUTPUT}
          </pre>
        </div>
      </section>

      {/* end of hero section */}

      {/* Stats bar */}
      <div style={{ borderTop: '1px solid var(--border)', borderBottom: '1px solid var(--border)', background: 'var(--deep)' }}>
        <div className="stats-grid">
          {STATS.map(s => (
            <div key={s.label}>
              <div style={{ fontFamily: 'var(--font-head)', fontSize: '2rem', color: 'var(--green)', letterSpacing: '0.04em' }}>
                {s.value}
              </div>
              <div style={{ fontSize: '0.78rem', color: 'var(--muted)', fontFamily: 'var(--font-mono)', marginTop: '0.25rem' }}>
                {s.label}
              </div>
            </div>
          ))}
        </div>
      </div>

      {/* Features grid */}
      <section style={{ maxWidth: '1200px', margin: '0 auto', padding: '5rem 2rem' }}>
        <h2 style={{
          fontFamily: 'var(--font-head)', fontSize: '2.5rem', letterSpacing: '0.06em',
          color: 'var(--white)', marginBottom: '3rem', textAlign: 'center',
        }}>
          EVERYTHING YOU NEED TO <span style={{ color: 'var(--green)' }}>FIND ALPHA</span>
        </h2>
        <div className="features-grid">
          {FEATURES.map(f => (
            <div key={f.title} className="card" style={{
              transition: 'border-color 0.2s, box-shadow 0.2s',
              cursor: 'default',
            }}
              onMouseEnter={e => {
                e.currentTarget.style.borderColor = 'var(--green)';
                e.currentTarget.style.boxShadow = '0 0 24px var(--green-glow)';
              }}
              onMouseLeave={e => {
                e.currentTarget.style.borderColor = 'var(--border)';
                e.currentTarget.style.boxShadow = 'none';
              }}
            >
              <div style={{ fontSize: '1.75rem', marginBottom: '0.75rem' }}>{f.icon}</div>
              <div style={{
                display: 'inline-block', marginBottom: '0.75rem',
                background: 'var(--green-glow)', border: '1px solid rgba(0,255,136,0.2)',
                borderRadius: '4px', padding: '0.2rem 0.5rem',
                fontFamily: 'var(--font-mono)', fontSize: '0.65rem', color: 'var(--green)',
              }}>
                {f.tag}
              </div>
              <h3 style={{ fontFamily: 'var(--font-head)', fontSize: '1.1rem', letterSpacing: '0.05em', color: 'var(--white)', marginBottom: '0.5rem' }}>
                {f.title}
              </h3>
              <p style={{ color: 'var(--muted)', fontSize: '0.825rem', lineHeight: 1.6 }}>{f.desc}</p>
            </div>
          ))}
        </div>
      </section>

      {/* CTA */}
      <section style={{
        background: 'var(--deep)', borderTop: '1px solid var(--border)',
        padding: '5rem 2rem', textAlign: 'center',
      }}>
        <h2 style={{ fontFamily: 'var(--font-head)', fontSize: '3rem', letterSpacing: '0.06em', color: 'var(--white)', marginBottom: '0.5rem' }}>
          FIND YOUR <span style={{ color: 'var(--green)' }}>EDGE</span>
        </h2>
        <p style={{ color: 'var(--muted)', marginBottom: '2rem' }}>
          Enter a ticker and run a backtest in seconds
        </p>
        <div style={{ display: 'flex', gap: '0.75rem', justifyContent: 'center', maxWidth: '420px', margin: '0 auto' }}>
          <input
            placeholder="AAPL, NVDA, TSLA…"
            value={ctaTicker}
            onChange={e => setCtaTicker(e.target.value.toUpperCase())}
            onKeyDown={e => e.key === 'Enter' && ctaTicker && navigate(`/backtest?ticker=${ctaTicker}`)}
            style={{ flex: 1 }}
          />
          <button
            className="btn-primary"
            onClick={() => ctaTicker && navigate(`/backtest?ticker=${ctaTicker}`)}
          >
            RUN BACKTEST
          </button>
        </div>
      </section>

      {/* Footer */}
      <footer style={{
        borderTop: '1px solid var(--border)', padding: '2rem',
        textAlign: 'center', color: 'var(--muted)', fontSize: '0.8rem',
        fontFamily: 'var(--font-mono)',
      }}>
        <p>© 2026 QuantEdge · Kevin Sodhi · Research tool</p>
      </footer>
    </div>
  );
}
