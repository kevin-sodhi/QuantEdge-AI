const express = require('express');
const router  = express.Router();

const { fetchTickerJson, runJavaBacktest } = require('../services/javaIntegration');

module.exports = function createBacktestRouter({ getUploadedFile }) {

  // GET /backtest/run — no params → show form
  router.get('/backtest/run', async (req, res) => {
    if (!req.query.ticker) {
      return res.render('backtest-form', {
        title:     'Run Backtest',
        pageClass: 'backtest-page',
      });
    }

    try {
      const result = await handleBacktest(req.query);
      return res.render('backtest-results', buildViewData(result, req.query));
    } catch (err) {
      console.error(err);
      res.status(500).render('error', { title: 'Server Error', code: 500, message: err.message });
    }
  });

  // POST /backtest/run — form submission
  router.post('/backtest/run', async (req, res) => {
    try {
      const result = await handleBacktest(req.body);
      return res.render('backtest-results', buildViewData(result, req.body));
    } catch (err) {
      console.error(err);
      res.status(500).render('error', { title: 'Server Error', code: 500, message: err.message });
    }
  });

  return router;
};

// ---------------------------------------------------------------------------
// Core handler — shared by GET and POST
// ---------------------------------------------------------------------------

async function handleBacktest(params) {
  const ticker   = (params.ticker || '').trim().toUpperCase();
  const period   = params.period    || '1y';
  const strategy = params.strategy  || 'macrossover';
  const indicator = params.indicator || 'sma';
  const fast     = parseInt(params.fast || '5', 10);
  const slow     = parseInt(params.slow || '20', 10);
  const initialCapital = parseFloat(params.initialCapital || '10000');

  if (!ticker) throw new Error('Ticker symbol is required.');

  if (strategy === 'macrossover' && (
    !Number.isFinite(fast) || !Number.isFinite(slow) || fast <= 0 || slow <= 0 || fast >= slow
  )) {
    throw new Error('MA Crossover requires positive integers with fast < slow.');
  }

  // Step 1: fetch live OHLCV from Python FastAPI
  const candles = await fetchTickerJson(ticker, period);

  // Step 2: run backtest via Java Spring Boot — all in memory, no CSV
  return runJavaBacktest({ candles, ticker, period, strategy, indicator, fast, slow, initialCapital });
}

// ---------------------------------------------------------------------------
// View-model builder
// ---------------------------------------------------------------------------

function buildViewData(result, params) {
  const ticker   = (params.ticker || '').toUpperCase();
  const period   = params.period   || '1y';
  const strategy = params.strategy || 'macrossover';
  const fast     = parseInt(params.fast || '5', 10);
  const slow     = parseInt(params.slow || '20', 10);

  const m      = result?.metrics || {};
  const series = Array.isArray(result?.series) ? result.series : [];
  let lastFast = 'N/A', lastSlow = 'N/A', lastDate = 'N/A';

  for (let i = series.length - 1; i >= 0; i--) {
    const row = series[i];
    if (row?.date) lastDate = row.date;
    if (row?.smaFast !== undefined || row?.smaSlow !== undefined) {
      if (row?.smaFast !== undefined) lastFast = row.smaFast;
      if (row?.smaSlow !== undefined) lastSlow = row.smaSlow;
      break;
    }
  }

  return {
    title:     'Backtest Results',
    pageClass: 'backtest-page',
    params:    { ticker, period, strategy, fast, slow },
    metrics: {
      barsRead:       m.barsRead       ?? 'N/A',
      trades:         m.trades         ?? 'N/A',
      totalReturnPct: m.totalReturnPct ?? 'N/A',
      sharpe:         m.sharpe         ?? 'N/A',
      maxDrawdown:    m.maxDrawdown     ?? 'N/A',
      lastDate,
      lastFast,
      lastSlow,
    },
    signals: Array.isArray(result?.signals) ? result.signals : [],
    rawJson: JSON.stringify(result, null, 2),
  };
}
