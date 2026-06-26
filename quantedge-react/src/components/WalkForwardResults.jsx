const fmt    = (v, d = 2) => (v == null ? '—' : Number(v).toFixed(d));
const fmtPct = (v)        => (v == null ? '—' : `${Number(v).toFixed(2)}%`);

function SharpeBar({ label, value, color }) {
  // Clamp bar width: 0 = 0%, Sharpe 3+ = 100%
  const pct = Math.min(Math.max((value / 3) * 100, 0), 100);
  return (
    <div style={{ flex: 1 }}>
      <div style={{
        fontSize: '0.7rem', color: 'var(--muted)',
        fontFamily: 'var(--font-mono)', letterSpacing: '0.06em',
        marginBottom: '0.4rem', textTransform: 'uppercase',
      }}>
        {label} Sharpe
      </div>
      <div style={{
        background: 'var(--border)', borderRadius: '4px',
        height: '6px', overflow: 'hidden', marginBottom: '0.4rem',
      }}>
        <div style={{
          width: `${pct}%`, height: '100%',
          background: color, borderRadius: '4px',
          transition: 'width 0.6s ease',
        }} />
      </div>
      <div style={{
        fontSize: '1.4rem', fontFamily: 'var(--font-mono)',
        fontWeight: 700, color,
      }}>
        {fmt(value)}
      </div>
    </div>
  );
}

function Row({ label, value, color }) {
  return (
    <tr style={{ borderBottom: '1px solid var(--border)' }}>
      <td style={{
        padding: '0.65rem 0', color: 'var(--muted)',
        fontSize: '0.8rem', fontFamily: 'var(--font-mono)', letterSpacing: '0.04em',
      }}>
        {label}
      </td>
      <td style={{
        padding: '0.65rem 0', textAlign: 'right',
        fontFamily: 'var(--font-mono)', fontWeight: 700,
        fontSize: '0.95rem', color: color || 'var(--text)',
      }}>
        {value}
      </td>
    </tr>
  );
}

export default function WalkForwardResults({ result }) {
  if (!result) return null;

  const wf = result.walkForward;
  if (!wf) return null;

  const sharpeDrop = wf.trainSharpe > 0
    ? ((wf.trainSharpe - wf.testSharpe) / wf.trainSharpe) * 100
    : null;

  const degradationColor = sharpeDrop == null
    ? 'var(--muted)'
    : sharpeDrop < 20 ? 'var(--green)' : sharpeDrop < 50 ? 'var(--amber)' : 'var(--red)';

  const testSharpeColor = wf.testSharpe >= 1 ? 'var(--green)'
    : wf.testSharpe >= 0 ? 'var(--amber)'
    : 'var(--red)';

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>

      {/* Sharpe comparison */}
      <div className="card">
        <h3 style={{
          fontFamily: 'var(--font-head)', fontSize: '1.1rem',
          letterSpacing: '0.08em', color: 'var(--white)', marginBottom: '1.25rem',
        }}>
          SHARPE COMPARISON
        </h3>
        <div style={{ display: 'flex', gap: '1.5rem', alignItems: 'flex-start' }}>
          <SharpeBar label="Train" value={wf.trainSharpe} color="var(--green)" />
          <div style={{ width: '1px', background: 'var(--border)', alignSelf: 'stretch' }} />
          <SharpeBar label="Test"  value={wf.testSharpe}  color={testSharpeColor} />
        </div>

        {sharpeDrop != null && (
          <div style={{
            marginTop: '1rem', padding: '0.6rem 0.9rem',
            background: 'rgba(0,0,0,0.3)', borderRadius: '6px',
            fontSize: '0.78rem', fontFamily: 'var(--font-mono)',
            color: degradationColor,
          }}>
            Sharpe degraded {fmt(sharpeDrop, 1)}% train→test
            {sharpeDrop < 20 && ' · Low overfitting'}
            {sharpeDrop >= 20 && sharpeDrop < 50 && ' · Moderate overfitting'}
            {sharpeDrop >= 50 && ' · High overfitting — strategy may not generalise'}
          </div>
        )}
      </div>

      {/* Test metrics */}
      <div className="card">
        <h3 style={{
          fontFamily: 'var(--font-head)', fontSize: '1.1rem',
          letterSpacing: '0.08em', color: 'var(--white)', marginBottom: '1rem',
        }}>
          TEST WINDOW RESULTS
        </h3>
        <table style={{ width: '100%', borderCollapse: 'collapse' }}>
          <tbody>
            <Row
              label="Test Return"
              value={fmtPct(wf.testReturnPct)}
              color={wf.testReturnPct >= 0 ? 'var(--green)' : 'var(--red)'}
            />
            <Row
              label="Test Max Drawdown"
              value={fmtPct(wf.testMaxDrawdown)}
              color="var(--red)"
            />
            <Row
              label="Best Fast MA"
              value={wf.bestFast}
              color="var(--blue)"
            />
            <Row
              label="Best Slow MA"
              value={wf.bestSlow}
              color="var(--blue)"
            />
          </tbody>
        </table>
      </div>

      {/* Period breakdown */}
      <div className="card">
        <h3 style={{
          fontFamily: 'var(--font-head)', fontSize: '1.1rem',
          letterSpacing: '0.08em', color: 'var(--white)', marginBottom: '1rem',
        }}>
          PERIOD BREAKDOWN
        </h3>
        <div style={{ display: 'flex', flexDirection: 'column', gap: '0.75rem' }}>
          <div style={{
            display: 'flex', justifyContent: 'space-between', alignItems: 'center',
            padding: '0.6rem 0.9rem', background: 'rgba(0,255,136,0.06)',
            border: '1px solid rgba(0,255,136,0.15)', borderRadius: '6px',
          }}>
            <span style={{ fontSize: '0.75rem', color: 'var(--green)', fontFamily: 'var(--font-mono)', textTransform: 'uppercase' }}>
              Train
            </span>
            <span style={{ fontSize: '0.8rem', color: 'var(--text)', fontFamily: 'var(--font-mono)' }}>
              {wf.trainStart} → {wf.trainEnd}
            </span>
          </div>
          <div style={{
            display: 'flex', justifyContent: 'space-between', alignItems: 'center',
            padding: '0.6rem 0.9rem', background: 'rgba(255,181,71,0.06)',
            border: '1px solid rgba(255,181,71,0.2)', borderRadius: '6px',
          }}>
            <span style={{ fontSize: '0.75rem', color: 'var(--amber)', fontFamily: 'var(--font-mono)', textTransform: 'uppercase' }}>
              Test
            </span>
            <span style={{ fontSize: '0.8rem', color: 'var(--text)', fontFamily: 'var(--font-mono)' }}>
              {wf.testStart} → {wf.testEnd}
            </span>
          </div>
        </div>
      </div>

    </div>
  );
}
