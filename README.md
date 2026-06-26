# QuantEdge

A systematic algorithmic trading research platform for backtesting trading strategies on live market data. Built to showcase production-quality quant engineering for the Baruch MFE application.

**Live:** `http://52.60.86.82` · **Stack:** React 18 · FastAPI · Java Spring Boot · AWS EC2

---

## What It Does

QuantEdge lets you test a trading strategy against real historical price data and see honest performance metrics — not just "did it make money" but whether the strategy actually generalises or is just memorising the past.

**Single Backtest** — Pick a stock, period, and strategy. The platform fetches live OHLCV data, runs the strategy through the Java engine, and returns a candlestick chart with BUY/SELL signal overlays, equity curve, and full performance metrics (Sharpe ratio, max drawdown, win rate, net P&L).

**Walk-Forward Validation** — The quant-standard method to detect overfitting. Splits historical data into a training window and a held-out test window. Grid-searches MA parameters on training data only, then evaluates the best parameters on data the optimizer never saw. The train vs test Sharpe comparison tells you whether the strategy is real or just curve-fitted noise.

---

## Architecture

```
Browser
  └── React 18 + Vite (port 5173 dev / 80 prod)
        │
        └── FastAPI Python (port 8000)
              ├── Fetches live OHLCV from Twelve Data API
              ├── POST /api/backtest  ──► Java Spring Boot (port 8080)
              └── POST /api/walk-forward ──► Java Spring Boot (port 8080)
                        │
                        └── BacktestEngine
                              ├── IndicatorFactory (SMA, EMA, RSI, Bollinger Bands)
                              ├── StrategyFactory (MA Crossover, Momentum, Mean Reversion)
                              ├── Portfolio (State pattern: Flat ↔ Long)
                              ├── MetricsCalculator (Sharpe, Drawdown, Win Rate)
                              └── WalkForwardValidator (Grid search + train/test split)
```

React never talks to Java directly. Python is the bridge — it fetches market data, normalises it, and orchestrates the Java engine.

---

## Strategies

| Strategy | Signal Logic |
|---|---|
| **MA Crossover** | BUY when fast MA crosses above slow MA (golden cross). SELL on death cross. |
| **Momentum** | BUY when EMA50 > EMA200 + RSI 50–70 + volume above 20-day avg. SELL on death cross or RSI > 70. |
| **Mean Reversion** | BUY when price ≤ lower Bollinger Band + RSI < 30 (oversold). SELL when price returns to middle band. |

## Indicators

- **SMA** — Simple Moving Average (circular ring buffer, O(1) per bar)
- **EMA** — Exponential Moving Average (Wilder smoothing)
- **RSI** — Relative Strength Index (Wilder's two-phase algorithm)
- **Bollinger Bands** — Upper / Middle / Lower (rolling variance, O(1) per bar)
- **Volume SMA** — 20-day volume average for confirmation

## Performance Metrics

- Sharpe Ratio (annualised, 252 trading days)
- Max Drawdown (peak-to-trough %)
- Total Return %
- Win Rate %
- Net P&L
- Bars Read

---

## Design Patterns (Java Engine)

| Pattern | Where |
|---|---|
| Facade | `BacktestEngine` hides complexity from `BacktestService` |
| Observer | `BacktestListener` notified on each bar event |
| Factory Method | `StrategyFactory`, `IndicatorFactory` |
| State | `Portfolio`: `FlatState` ↔ `LongState` |
| Template Method | `BaseMovingAverage.accumulate()` — SMA/EMA override `update()` hook |
| Strategy | `Strategy` interface — swappable signal logic |
| DTO | `BacktestRequest`, `WalkForwardRequest`, `Result` |

---

## Tech Stack

| Layer | Technology |
|---|---|
| Frontend | React 18, Vite, lightweight-charts, react-router-dom |
| API / Data | Python 3.13, FastAPI, httpx, pandas, ta, Twelve Data |
| Engine | Java 17, Spring Boot 3.2, Maven |
| Testing | JUnit 5 (51 tests), pytest + anyio (20 tests) |
| Infrastructure | Docker, AWS ECR, AWS EC2, GitHub Actions CI/CD |

---

## Running Locally

### Prerequisites

- Java 17+
- Python 3.10+
- Node.js 18+
- Maven 3.8+
- A free [Twelve Data API key](https://twelvedata.com)

### 1. Clone the repo

```bash
git clone https://github.com/kevin-sodhi/QuantEdge-AI.git
cd QuantEdge-AI
```

### 2. Set up the Python environment variable

```bash
echo "TWELVE_DATA_KEY=your_key_here" > quantedge-python/.env
```

### 3. Install frontend dependencies

```bash
cd quantedge-react
npm install
```

### 4. Start all three services with one command

```bash
npm run dev
```

This uses `concurrently` to start:
- Java Spring Boot on `http://localhost:8080`
- Python FastAPI on `http://localhost:8000`
- React/Vite on `http://localhost:5173`

Java takes ~20 seconds to boot. Once all three are ready, open:

```
http://localhost:5173/backtest
```

### Useful local URLs

| Service | URL |
|---|---|
| React app | http://localhost:5173 |
| FastAPI docs (Swagger) | http://localhost:8000/docs |
| Java health check | http://localhost:8080/api/health |
| Python health check | http://localhost:8000/health |

---

## Running with Docker

```bash
# Local — builds images from source
docker compose up --build

# Production — pulls from AWS ECR (requires ECR credentials)
docker compose -f docker-compose.prod.yml up
```

---

## Running Tests

**Java (51 tests):**

```bash
cd algo-backtester-java
mvn test
```

Covers: SMA, EMA, RSI indicators · MA Crossover, Momentum, Mean Reversion strategies · MetricsCalculator (Sharpe, drawdown, win rate, P&L)

**Python (20 tests):**

```bash
cd quantedge-python
pip install -r requirements.txt
pytest tests/ -v
```

Covers: `/health` endpoint · `/api/backtest` input validation · happy path with mocked Java · 502 on Java unreachable · `/data` endpoint shape

No API key or running services needed — all external I/O is mocked.

---

## CI/CD & Deployment

Every push to `main` triggers the GitHub Actions pipeline:

```
Push to main
  └── Build Docker images (Java, Python, React)
        └── Push to AWS ECR (ca-central-1)
              └── SSH into EC2 (52.60.86.82)
                    └── docker compose pull + up
```

Required GitHub secrets:

| Secret | Description |
|---|---|
| `AWS_ACCESS_KEY_ID` | AWS IAM access key |
| `AWS_SECRET_ACCESS_KEY` | AWS IAM secret |
| `EC2_HOST` | EC2 public IP |
| `EC2_SSH_KEY` | EC2 private key (PEM) |

---

## Project Structure

```
QuantEdge-AI/
├── quantedge-react/          # React 18 frontend
│   └── src/
│       ├── pages/
│       │   ├── Home.jsx
│       │   └── Backtest.jsx          # Single backtest + walk-forward toggle
│       └── components/
│           ├── BacktestForm.jsx
│           ├── CandlestickChart.jsx
│           ├── MetricsTable.jsx
│           ├── WalkForwardForm.jsx
│           └── WalkForwardResults.jsx
│
├── quantedge-python/         # FastAPI data + orchestration layer
│   ├── main.py               # Endpoints: /health /data /indicators /api/backtest /api/walk-forward
│   ├── services/
│   │   └── data_service.py   # Twelve Data fetch, indicator computation, signal generation
│   └── tests/
│       └── test_api.py
│
├── algo-backtester-java/     # Spring Boot backtesting engine
│   └── src/main/java/com/kevin/algo/
│       ├── engine/           # BacktestEngine, MetricsCalculator, WalkForwardValidator
│       ├── strategy/         # MovingAverageCrossover, MomentumStrategy, MeanReversionStrategy
│       ├── indicators/       # SMA, EMA, RSI, BollingerBands, VolumeSMA
│       ├── portfolio/        # Portfolio, FlatState, LongState
│       └── web/              # BacktestController, BacktestService, DTOs
│
├── Diagrams/
│   └── system-design.md      # Mermaid architecture diagrams
│
├── docker-compose.yml        # Local development
├── docker-compose.prod.yml   # Production (AWS ECR images)
└── .github/workflows/
    └── deploy.yml            # CI/CD pipeline
```

---

## How Walk-Forward Validation Works

Standard backtesting has a problem: you can search through hundreds of parameter combinations on the same data you measure performance on. The parameters that look best are likely overfit to historical noise — they won't work on future data.

Walk-forward validation solves this:

1. **Split** — 70% of data for training, 30% held out as unseen test data
2. **Optimise** — grid-search fast/slow MA periods on training data only (18 combinations)
3. **Evaluate** — run the winning parameters against the test window that was never touched
4. **Compare** — train Sharpe vs test Sharpe. A small drop means the strategy generalises. A large drop means overfitting.

The UI shows the degradation percentage and labels it: low / moderate / high overfitting.

---

## Roadmap

- [ ] Statistical rigor — Deflated Sharpe, bootstrap confidence intervals, Calmar ratio
- [ ] Transaction cost modeling — commissions, bid-ask spread, slippage
- [ ] Factor exposure — Fama-French regression, alpha decomposition
- [ ] Multi-asset portfolio construction — Markowitz, Ledoit-Wolf shrinkage, risk parity
- [ ] ML signal layer — information coefficient, alpha decay, purged walk-forward CV
- [ ] Regime detection — Hidden Markov Model (bull / bear / high-volatility)
- [ ] Alpaca paper trading — live signal → order execution loop

---

## Author

Kevin Sodhi · [GitHub](https://github.com/kevin-sodhi) · Built for Baruch MFE application
