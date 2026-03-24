import { useEffect, useRef } from 'react';

const TICKERS = [
  { sym: 'AAPL',    price: '192.35', chg: '+1.24%',  up: true  },
  { sym: 'MSFT',    price: '415.20', chg: '+0.87%',  up: true  },
  { sym: 'NVDA',    price: '875.40', chg: '+3.12%',  up: true  },
  { sym: 'TSLA',    price: '178.90', chg: '-1.05%',  up: false },
  { sym: 'GOOGL',   price: '175.60', chg: '+0.54%',  up: true  },
  { sym: 'META',    price: '502.10', chg: '+1.89%',  up: true  },
  { sym: 'AMZN',    price: '185.30', chg: '-0.32%',  up: false },
  { sym: 'JPM',     price: '201.50', chg: '+0.63%',  up: true  },
  { sym: 'BTC/USD', price: '67420',  chg: '+2.41%',  up: true  },
  { sym: 'SPY',     price: '524.80', chg: '+0.71%',  up: true  },
];

const tape = [...TICKERS, ...TICKERS]; // duplicate for seamless loop

export default function TickerTape() {
  const ref = useRef(null);

  useEffect(() => {
    const el = ref.current;
    if (!el) return;
    let x = 0;
    let raf;
    const speed = 0.5;

    function tick() {
      x -= speed;
      if (Math.abs(x) >= el.scrollWidth / 2) x = 0;
      el.style.transform = `translateX(${x}px)`;
      raf = requestAnimationFrame(tick);
    }
    raf = requestAnimationFrame(tick);
    return () => cancelAnimationFrame(raf);
  }, []);

  return (
    <div style={{
      position: 'fixed', top: '64px', left: 0, right: 0, zIndex: 999,
      background: 'var(--deep)', borderBottom: '1px solid var(--border)',
      overflow: 'hidden', height: '36px', display: 'flex', alignItems: 'center',
    }}>
      <div ref={ref} style={{ display: 'flex', gap: '3rem', whiteSpace: 'nowrap', willChange: 'transform' }}>
        {tape.map((t, i) => (
          <span key={i} style={{ display: 'flex', gap: '0.5rem', alignItems: 'center',
            fontFamily: 'var(--font-mono)', fontSize: '0.72rem' }}>
            <span style={{ color: 'var(--muted)' }}>{t.sym}</span>
            <span style={{ color: 'var(--white)' }}>{t.price}</span>
            <span style={{ color: t.up ? 'var(--green)' : 'var(--red)' }}>{t.chg}</span>
          </span>
        ))}
      </div>
    </div>
  );
}
