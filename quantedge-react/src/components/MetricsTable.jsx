const fmt    = (v, suffix = '') =>
  v === undefined || v === null ? '—' : `${Number(v).toFixed(2)}${suffix}`;

const sharpeColor = (v) => v >= 1 ? 'var(--green)' : v >= 0 ? 'var(--amber)' : 'var(--red)';

const rows = [
  { label: 'Total Return',  key: 'totalReturnPct', suffix: '%', color: (v) => v >= 0 ? 'var(--green)' : 'var(--red)' },
  { label: 'Sharpe Ratio',  key: 'sharpe',         suffix: '',  color: sharpeColor },
  { label: 'Sortino Ratio', key: 'sortino',        suffix: '',  color: sharpeColor },
  { label: 'Calmar Ratio',  key: 'calmar',         suffix: '',  color: (v) => v >= 1 ? 'var(--green)' : v >= 0 ? 'var(--amber)' : 'var(--red)' },
  { label: 'Max Drawdown',  key: 'maxDrawdown',    suffix: '%', color: () => 'var(--red)' },
  { label: 'Net P&L',       key: 'netPnl',         suffix: '',  color: (v) => v >= 0 ? 'var(--green)' : 'var(--red)' },
  { label: 'Win Rate',      key: 'winRatePct',     suffix: '%', color: (v) => v >= 50 ? 'var(--green)' : 'var(--amber)' },
  { label: 'Trades',        key: 'trades',         suffix: '',  color: () => 'var(--text)', raw: true },
  { label: 'Bars Read',     key: 'barsRead',       suffix: '',  color: () => 'var(--muted)', raw: true },
];

function MetricRow({ label, value, color, raw, suffix = '' }) {
  const display = raw ? (value ?? '—') : fmt(value, suffix);
  const c = value !== undefined && value !== null ? color(Number(value)) : 'var(--muted)';
  return (
    <tr style={{ borderBottom: '1px solid var(--border)' }}>
      <td style={{ padding: '0.6rem 0', color: 'var(--muted)', fontSize: '0.78rem', fontFamily: 'var(--font-mono)', letterSpacing: '0.04em' }}>
        {label}
      </td>
      <td style={{ padding: '0.6rem 0', textAlign: 'right', color: c, fontFamily: 'var(--font-mono)', fontWeight: 700, fontSize: '0.92rem' }}>
        {display}
      </td>
    </tr>
  );
}

function SharpeWithCI({ metrics }) {
  const lo  = metrics.sharpeCI95Low;
  const hi  = metrics.sharpeCI95High;
  const val = metrics.sharpe;
  if (lo === undefined || lo === null) return null;

  const c = val >= 1 ? 'var(--green)' : val >= 0 ? 'var(--amber)' : 'var(--red)';
  return (
    <tr style={{ borderBottom: '1px solid var(--border)' }}>
      <td style={{ padding: '0.6rem 0', color: 'var(--muted)', fontSize: '0.78rem', fontFamily: 'var(--font-mono)', letterSpacing: '0.04em' }}>
        Sharpe 95% CI
      </td>
      <td style={{ padding: '0.6rem 0', textAlign: 'right', fontFamily: 'var(--font-mono)', fontWeight: 700, fontSize: '0.82rem', color: 'var(--muted)' }}>
        <span style={{ color: c }}>{Number(val).toFixed(2)}</span>
        {' '}
        <span style={{ fontSize: '0.72rem' }}>
          [{Number(lo).toFixed(2)} – {Number(hi).toFixed(2)}]
        </span>
      </td>
    </tr>
  );
}

export default function MetricsTable({ metrics }) {
  if (!metrics) return null;

  return (
    <div className="card">
      <h3 style={{ fontFamily: 'var(--font-head)', fontSize: '1.2rem', letterSpacing: '0.08em', color: 'var(--white)', marginBottom: '1rem' }}>
        RESULTS
      </h3>
      <table style={{ width: '100%', borderCollapse: 'collapse' }}>
        <tbody>
          {rows.map(({ label, key, suffix, color, raw }) => (
            <MetricRow key={key} label={label} value={metrics[key]} color={color} raw={raw} suffix={suffix} />
          ))}
          <SharpeWithCI metrics={metrics} />
        </tbody>
      </table>
    </div>
  );
}
