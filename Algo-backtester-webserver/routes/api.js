const express = require('express');
const router  = express.Router();

const { fetchTickerJson, runJavaBacktest } = require('../services/javaIntegration');

/**
 * POST /api/backtest
 * ------------------
 * JSON API endpoint for the React frontend.
 * Same logic as routes/backtest.js but returns raw JSON instead of rendering Pug.
 *
 * Body: { ticker, period, strategy, indicator, fast, slow, initialCapital }
 * Returns: Java Spring Boot response { ok, metrics, trades, signals, equity, series }
 */
router.post('/api/backtest', async (req, res) => {
  try {
    const ticker        = (req.body.ticker || '').trim().toUpperCase();
    const period        = req.body.period        || '1y';
    const strategy      = req.body.strategy      || 'macrossover';
    const indicator     = req.body.indicator     || 'sma';
    const fast          = parseInt(req.body.fast  || '5',  10);
    const slow          = parseInt(req.body.slow  || '20', 10);
    const initialCapital = parseFloat(req.body.initialCapital || '10000');

    if (!ticker) {
      return res.status(400).json({ ok: false, error: 'ticker is required' });
    }

    if (strategy === 'macrossover' && (
      !Number.isFinite(fast) || !Number.isFinite(slow) ||
      fast <= 0 || slow <= 0 || fast >= slow
    )) {
      return res.status(400).json({
        ok: false,
        error: 'MA Crossover requires positive integers with fast < slow',
      });
    }

    const candles = await fetchTickerJson(ticker, period);
    const result  = await runJavaBacktest({ candles, ticker, period, strategy, indicator, fast, slow, initialCapital });

    return res.json(result);
  } catch (err) {
    console.error('[/api/backtest]', err.message);
    return res.status(500).json({ ok: false, error: err.message });
  }
});

module.exports = router;
