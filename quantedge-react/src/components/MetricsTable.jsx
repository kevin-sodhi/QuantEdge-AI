const fmt = (v, suffix = '') =>
  v === undefined || v === null || v === 'N/A' ? '—' : `${Number(v).toFixed(2)}${suffix}`;

const rows = [
  { label: 'Total Return',   key: 'totalReturnPct', suffix: '%',  color: (v) => v >= 0 ? 'var(--green)' : 'var(--red)' },
  { label: 'Sharpe Ratio',   key: 'sharpe',         suffix: '',   color: (v) => v >= 1 ? 'var(--green)' : v >= 0 ? 'var(--amber)' : 'var(--red)' },
  { label: 'Max Drawdown',   key: 'maxDrawdown',     suffix: '%',  color: () => 'var(--red)' },
  { label: 'Trades',         key: 'trades',          suffix: '',   color: () => 'var(--text)', raw: true },
  { label: 'Bars Read',      key: 'barsRead',        suffix: '',   color: () => 'var(--muted)', raw: true },
  { label: 'Net P&L',        key: 'netPnl',          suffix: '',   color: (v) => v >= 0 ? 'var(--green)' : 'var(--red)' },
];

export default function MetricsTable({ metrics }) {
  if (!metrics) return null;

  return (
    <div className="card">
      <h3 style={{
        fontFamily: 'var(--font-head)', fontSize: '1.2rem',
        letterSpacing: '0.08em', color: 'var(--white)', marginBottom: '1rem',
      }}>
        RESULTS
      </h3>
      <table style={{ width: '100%', borderCollapse: 'collapse' }}>
        <tbody>
          {rows.map(({ label, key, suffix, color, raw }) => {
            const val = metrics[key];
            const display = raw
              ? (val ?? '—')
              : fmt(val, suffix);
            const c = (val !== undefined && val !== null && val !== 'N/A')
              ? color(Number(val))
              : 'var(--muted)';
            return (
              <tr key={key} style={{ borderBottom: '1px solid var(--border)' }}>
                <td style={{
                  padding: '0.65rem 0',
                  color: 'var(--muted)',
                  fontSize: '0.8rem',
                  fontFamily: 'var(--font-mono)',
                  letterSpacing: '0.04em',
                }}>
                  {label}
                </td>
                <td style={{
                  padding: '0.65rem 0',
                  textAlign: 'right',
                  color: c,
                  fontFamily: 'var(--font-mono)',
                  fontWeight: 700,
                  fontSize: '0.95rem',
                }}>
                  {display}
                </td>
              </tr>
            );
          })}
        </tbody>
      </table>
    </div>
  );
}
