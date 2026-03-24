const fs   = require('fs');
const path = require('path');

// Service URLs — override via env vars in Railway
const PYTHON_SERVICE_URL = process.env.PYTHON_SERVICE_URL || 'http://localhost:8000';
const JAVA_SERVICE_URL   = process.env.JAVA_SERVICE_URL   || 'http://localhost:8080';

// FILES_DIR kept for legacy CSV download routes
const FILES_DIR = path.resolve(__dirname, '..', '..', 'data');
if (!fs.existsSync(FILES_DIR)) fs.mkdirSync(FILES_DIR, { recursive: true });

console.log('[INFO] Python service:', PYTHON_SERVICE_URL);
console.log('[INFO] Java service:  ', JAVA_SERVICE_URL);

// ---------------------------------------------------------------------------
// Python helpers
// ---------------------------------------------------------------------------

/**
 * Fetch OHLCV data from Python FastAPI as a JSON candles array.
 * Python returns: { data: [ { Date, Open, High, Low, Close, Volume }, ... ] }
 * We normalise keys to lowercase so Java CandleData can deserialise them.
 *
 * @param {string} ticker  e.g. "AAPL"
 * @param {string} period  e.g. "1y"
 * @returns {Promise<Array>} array of { date, open, high, low, close, volume }
 */
async function fetchTickerJson(ticker, period = '1y') {
  const url = `${PYTHON_SERVICE_URL}/data?ticker=${encodeURIComponent(ticker)}&period=${encodeURIComponent(period)}`;

  let res;
  try {
    res = await fetch(url);
  } catch (err) {
    throw new Error(
      `Cannot reach Python service at ${PYTHON_SERVICE_URL}. ` +
      `Start it with: cd quantedge-python && uvicorn main:app --port 8000\n${err.message}`
    );
  }

  if (!res.ok) {
    const detail = await res.text().catch(() => '');
    throw new Error(`Python /data returned ${res.status} for '${ticker}': ${detail}`);
  }

  const json = await res.json();

  // Normalise capitalised keys (Date, Open...) from yfinance → lowercase for Java
  return (json.data || []).map(r => ({
    date:   r.Date   ?? r.date,
    open:   r.Open   ?? r.open,
    high:   r.High   ?? r.high,
    low:    r.Low    ?? r.low,
    close:  r.Close  ?? r.close,
    volume: r.Volume ?? r.volume,
  }));
}

// ---------------------------------------------------------------------------
// Java Spring Boot helpers
// ---------------------------------------------------------------------------

/**
 * Run a backtest via Java Spring Boot REST API.
 * No CSV files — all data travels in the request body as JSON.
 *
 * @param {Object} opts
 * @param {Array}  opts.candles        - normalised OHLCV array from fetchTickerJson
 * @param {string} opts.ticker         - ticker symbol (audit trail only)
 * @param {string} opts.period         - period string (audit trail only)
 * @param {string} opts.strategy       - macrossover | momentum | meanreversion
 * @param {string} opts.indicator      - sma | ema (macrossover only)
 * @param {number} opts.fast           - fast MA period
 * @param {number} opts.slow           - slow MA period
 * @param {number} opts.initialCapital - starting cash
 * @returns {Promise<Object>} parsed JSON response from Java
 */
async function runJavaBacktest({
  candles,
  ticker        = '',
  period        = '1y',
  strategy      = 'macrossover',
  indicator     = 'sma',
  fast          = 5,
  slow          = 20,
  initialCapital = 10_000,
}) {
  const url  = `${JAVA_SERVICE_URL}/api/backtest`;
  const body = { candles, ticker, period, strategy, indicator, fast, slow, initialCapital };

  let res;
  try {
    res = await fetch(url, {
      method:  'POST',
      headers: { 'Content-Type': 'application/json' },
      body:    JSON.stringify(body),
    });
  } catch (err) {
    throw new Error(
      `Cannot reach Java service at ${JAVA_SERVICE_URL}. ` +
      `Start it with: cd algo-backtester-java && mvn spring-boot:run\n${err.message}`
    );
  }

  if (!res.ok) {
    const detail = await res.text().catch(() => '');
    throw new Error(`Java /api/backtest returned ${res.status}: ${detail}`);
  }

  return res.json();
}

/**
 * Run walk-forward validation via Java Spring Boot.
 */
async function runJavaWalkForward({
  candles,
  ticker       = '',
  period       = '1y',
  strategy     = 'macrossover',
  indicator    = 'sma',
  trainRatio   = 0.7,
  initialCapital = 10_000,
}) {
  const url  = `${JAVA_SERVICE_URL}/api/walk-forward`;
  const body = { candles, ticker, period, strategy, indicator, trainRatio, initialCapital };

  let res;
  try {
    res = await fetch(url, {
      method:  'POST',
      headers: { 'Content-Type': 'application/json' },
      body:    JSON.stringify(body),
    });
  } catch (err) {
    throw new Error(`Cannot reach Java service: ${err.message}`);
  }

  if (!res.ok) {
    const detail = await res.text().catch(() => '');
    throw new Error(`Java /api/walk-forward returned ${res.status}: ${detail}`);
  }

  return res.json();
}

// ---------------------------------------------------------------------------
// Legacy helpers — kept so existing upload-based routes don't break
// ---------------------------------------------------------------------------

function resolveCsvPath(uploadedFile) {
  if (uploadedFile?.path) return uploadedFile.path;
  return path.join(FILES_DIR, 'TSLA.csv');
}

module.exports = {
  FILES_DIR,
  PYTHON_SERVICE_URL,
  JAVA_SERVICE_URL,
  fetchTickerJson,
  runJavaBacktest,
  runJavaWalkForward,
  resolveCsvPath,         // legacy
};
