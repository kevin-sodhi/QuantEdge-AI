import { useEffect, useRef } from 'react';
import { createChart } from 'lightweight-charts';

/**
 * CandlestickChart
 * ----------------
 * Props:
 *   candles  — array of { time, open, high, low, close }
 *   signals  — array of { date, type, price } from backtest result
 *   height   — chart height in px (default 420)
 */
export default function CandlestickChart({ candles = [], signals = [], height = 420 }) {
  const containerRef = useRef(null);
  const chartRef     = useRef(null);
  const seriesRef    = useRef(null);

  // Create chart once on mount
  useEffect(() => {
    if (!containerRef.current) return;

    const chart = createChart(containerRef.current, {
      width:  containerRef.current.clientWidth,
      height,
      layout: {
        background: { color: '#0c0f15' },
        textColor:  '#c8d4e8',
      },
      grid: {
        vertLines: { color: '#1e2535' },
        horzLines: { color: '#1e2535' },
      },
      crosshair: { mode: 1 },
      rightPriceScale: { borderColor: '#1e2535' },
      timeScale: {
        borderColor: '#1e2535',
        timeVisible: true,
        secondsVisible: false,
      },
    });

    const series = chart.addCandlestickSeries({
      upColor:        '#00ff88',
      downColor:      '#ff3d5a',
      borderUpColor:  '#00ff88',
      borderDownColor:'#ff3d5a',
      wickUpColor:    '#00cc6a',
      wickDownColor:  '#cc2d47',
    });

    chartRef.current  = chart;
    seriesRef.current = series;

    // Responsive resize
    const ro = new ResizeObserver(() => {
      chart.applyOptions({ width: containerRef.current.clientWidth });
    });
    ro.observe(containerRef.current);

    return () => { ro.disconnect(); chart.remove(); };
  }, [height]);

  // Update candle data when it changes
  useEffect(() => {
    if (!seriesRef.current || !candles.length) return;
    seriesRef.current.setData(candles);
    chartRef.current.timeScale().fitContent();
  }, [candles]);

  // Overlay BUY/SELL signal markers when signals change
  useEffect(() => {
    if (!seriesRef.current) return;
    if (!signals.length) {
      seriesRef.current.setMarkers([]);
      return;
    }
    const markers = signals
      .filter(s => s.date)
      .map(s => ({
        time:     s.date,
        position: s.type === 'BUY' ? 'belowBar' : 'aboveBar',
        color:    s.type === 'BUY' ? '#00ff88'  : '#ff3d5a',
        shape:    s.type === 'BUY' ? 'arrowUp'  : 'arrowDown',
        text:     s.type,
        size:     1,
      }))
      .sort((a, b) => (a.time > b.time ? 1 : -1));

    seriesRef.current.setMarkers(markers);
  }, [signals]);

  return (
    <div style={{ position: 'relative' }}>
      {!candles.length && (
        <div style={{
          position: 'absolute', inset: 0, display: 'flex',
          alignItems: 'center', justifyContent: 'center',
          color: 'var(--muted)', fontFamily: 'var(--font-mono)', fontSize: '0.875rem',
          background: '#0c0f15', borderRadius: '8px', zIndex: 1,
        }}>
          Enter a ticker and click Load Chart
        </div>
      )}
      <div ref={containerRef} style={{ width: '100%', height, borderRadius: '8px', overflow: 'hidden' }} />
    </div>
  );
}
