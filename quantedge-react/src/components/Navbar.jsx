import { useNavigate } from 'react-router-dom';

const styles = {
  nav: {
    position: 'fixed', top: 0, left: 0, right: 0, zIndex: 1000,
    display: 'flex', alignItems: 'center', justifyContent: 'space-between',
    padding: '0 2rem', height: '64px',
    background: 'rgba(8,10,14,0.92)',
    borderBottom: '1px solid var(--border)',
    backdropFilter: 'blur(12px)',
  },
  logo: {
    fontFamily: 'var(--font-head)',
    fontSize: '1.5rem',
    letterSpacing: '0.08em',
    color: 'var(--white)',
  },
  links: { display: 'flex', gap: '2rem', alignItems: 'center' },
  link: {
    color: 'var(--muted)', fontSize: '0.875rem',
    letterSpacing: '0.04em', cursor: 'pointer',
    transition: 'color 0.2s',
  },
  status: {
    display: 'flex', alignItems: 'center', gap: '0.5rem',
    fontSize: '0.75rem', fontFamily: 'var(--font-mono)',
    color: 'var(--muted)',
  },
  dot: {
    width: '6px', height: '6px', borderRadius: '50%',
    background: 'var(--green)', boxShadow: '0 0 6px var(--green)',
  },
};

export default function Navbar() {
  const navigate = useNavigate();

  return (
    <nav style={styles.nav}>
      <div style={styles.logo}>
        QUANT<span style={{ color: 'var(--green)' }}>EDGE</span>
      </div>

      {/* Hidden on mobile */}
      <div className="navbar-links" style={styles.links}>
        <span style={styles.link} onClick={() => navigate('/')}>Platform</span>
        <span style={styles.link} onClick={() => navigate('/')}>Strategies</span>
        <span style={styles.link} onClick={() => navigate('/')}>Research</span>
        <span style={styles.link} onClick={() => navigate('/')}>Docs</span>
      </div>

      <div style={{ display: 'flex', alignItems: 'center', gap: '1.5rem' }}>
        {/* Hidden on mobile */}
        <div className="navbar-status" style={styles.status}>
          <div style={styles.dot} />
          MARKETS OPEN · NYSE
        </div>
        <button className="btn-primary" style={{ padding: '0.5rem 1.25rem', fontSize: '0.875rem' }}
          onClick={() => navigate('/backtest')}>
          Launch App →
        </button>
      </div>
    </nav>
  );
}
