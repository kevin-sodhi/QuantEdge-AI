# QuantEdge System Design

## 1. High-Level Architecture

```mermaid
graph TB
    subgraph Client["Client (Browser)"]
        UI[React 18 + Vite\nport 5173 dev / 80 prod]
    end

    subgraph Python["Python Service (FastAPI)\nport 8000"]
        API["/api/backtest\n/data\n/indicators\n/price-signal\n/health"]
        DS[data_service.py\nfetch_ohlcv\nfetch_indicators\ngenerate_signal]
    end

    subgraph Java["Java Service (Spring Boot)\nport 8080"]
        BC[BacktestController\n/api/backtest\n/api/walkforward\n/api/health]
        BS[BacktestService]
        BE[BacktestEngine\nFacade Pattern]
        SF[StrategyFactory]
        IF[IndicatorFactory]
        PORT[Portfolio\nState Pattern]
        MC[MetricsCalculator]
        WFV[WalkForwardValidator]
    end

    subgraph External["External"]
        TD[(Twelve Data API\nOHLCV market data)]
    end

    UI -->|POST /api/backtest\nGET /data\nGET /indicators| API
    API --> DS
    DS -->|HTTPS| TD
    API -->|POST /api/backtest\nnormalized candles| BC
    BC --> BS
    BS --> BE
    BE --> SF
    BE --> IF
    BE --> PORT
    BE --> MC
    BS --> WFV
```

---

## 2. Request Flow — Backtest

```mermaid
sequenceDiagram
    actor User
    participant React
    participant Python as FastAPI (Python)
    participant TwelveData as Twelve Data
    participant Java as Spring Boot (Java)

    User->>React: Enter ticker, period, strategy
    React->>Python: POST /api/backtest {ticker, strategy, fast, slow}
    Python->>TwelveData: GET /time_series?symbol=AAPL
    TwelveData-->>Python: Raw OHLCV JSON
    Python->>Python: Normalize → candles[]
    Python->>Java: POST /api/backtest {candles[], strategy, fast, slow}
    Java->>Java: BacktestEngine iterates bars
    Java->>Java: IndicatorFactory warms up SMA/EMA/RSI/BB
    Java->>Java: Strategy emits BUY/SELL signals
    Java->>Java: Portfolio tracks trades + equity
    Java->>Java: MetricsCalculator computes Sharpe, drawdown, etc.
    Java-->>Python: {metrics, signals, series, equity}
    Python-->>React: Forward Java response
    React->>React: Render candlestick chart + signal markers + metrics table
    React-->>User: Dashboard
```

---

## 3. Java Engine Internals

```mermaid
graph LR
    subgraph Engine["BacktestEngine (Facade)"]
        LOOP[Bar iteration loop]
    end

    subgraph Indicators["IndicatorFactory"]
        SMA[SMA\nCircular buffer O-1]
        EMA[EMA\nWilder smoothing]
        RSI[RSI\nWilder smoothing]
        BB[BollingerBands\nUpper / Middle / Lower]
        VSMA[VolumeSMA]
    end

    subgraph Strategies["StrategyFactory"]
        MAC[MovingAverageCrossover\nfast vs slow MA]
        MOM[MomentumStrategy\nGolden cross + RSI + Volume]
        MR[MeanReversionStrategy\nBB lower + RSI oversold]
    end

    subgraph Portfolio["Portfolio (State Pattern)"]
        FLAT[FlatState\nno position]
        LONG[LongState\nin position]
    end

    subgraph Output["Output"]
        TRADES[Closed Trades]
        EQUITY[Equity Curve]
        SIGNALS[BUY/SELL Signals]
        METRICS[MetricsCalculator\nSharpe · Drawdown\nWin Rate · PnL]
    end

    LOOP -->|price per bar| SMA & EMA & RSI & BB & VSMA
    SMA & EMA & RSI & BB & VSMA -->|indicator map| MAC & MOM & MR
    MAC & MOM & MR -->|Optional Signal| Portfolio
    FLAT -->|BUY signal| LONG
    LONG -->|SELL signal| FLAT
    Portfolio --> TRADES & EQUITY & SIGNALS
    TRADES & EQUITY --> METRICS
```

---

## 4. Design Patterns Used

```mermaid
graph TD
    subgraph Patterns["Design Patterns in QuantEdge"]
        FAC["Facade\nBacktestEngine hides\nengine complexity from BacktestService"]
        OBS["Observer\nBacktestListener notifies\non each bar event"]
        FM["Factory Method\nStrategyFactory → Strategy\nIndicatorFactory → Indicator"]
        STATE["State\nPortfolio: FlatState ↔ LongState\ncontrols buy/sell transitions"]
        TM["Template Method\nBaseMovingAverage.accumulate\nSMA/EMA override update-hook"]
        STRAT["Strategy Pattern\nStrategy interface\nSwappable signal logic"]
        DTO["DTO\nResult, BacktestRequest\nClean API boundaries"]
    end
```

---

## 5. Deployment Architecture

```mermaid
graph TB
    subgraph Dev["Local Development"]
        DCL[docker-compose.yml\nBuilds from source]
        J_L[Java :8080]
        P_L[Python :8000]
        R_L[React/Nginx :80]
        DCL --> J_L & P_L & R_L
    end

    subgraph CICD["CI/CD — GitHub Actions"]
        PUSH[git push → main]
        BUILD[Build Docker images\nJava · Python · React]
        ECR[Push to AWS ECR\nca-central-1]
        PUSH --> BUILD --> ECR
    end

    subgraph Prod["Production — AWS EC2 (52.60.86.82)"]
        DCP[docker-compose.prod.yml\nPulls from ECR]
        J_P[quantedge-java\n:8080]
        P_P[quantedge-python\n:8000]
        R_P[quantedge-react/Nginx\n:80]
        DCP --> J_P & P_P & R_P
    end

    ECR -->|SSH deploy\nappleboy/ssh-action| DCP
```

---

## 6. Test Coverage

```mermaid
graph LR
    subgraph JavaTests["Java Tests — 51 tests / JUnit 5"]
        T1[SMATest · 5]
        T2[EMATest · 5]
        T3[RSITest · 6]
        T4[MovingAverageCrossoverTest · 7]
        T5[MomentumStrategyTest · 8]
        T6[MeanReversionStrategyTest · 9]
        T7[MetricsCalculatorTest · 11]
    end

    subgraph PythonTests["Python Tests — 20 passes / pytest + anyio"]
        P1[/health shape]
        P2[/api/backtest validation × 4]
        P3[Happy path — mocked Java]
        P4[502 — Java unreachable]
        P5[/data shape + error propagation]
    end
```
