# QuantEdge Project Synopsis

QuantEdge is a systematic trading research platform that connects a Java backtesting engine, a Python market-data layer, and a React dashboard. The project has evolved from an earlier Node/Express coursework-style webserver into a three-service architecture designed for live OHLCV data, strategy backtesting, signal visualization, and future ML/NLP expansion.

## Current Architecture

The current app is organized into three main services:

- `quantedge-react`: React 18 + Vite frontend running on port `5173` in development and served through Nginx in production.
- `quantedge-python`: FastAPI service running on port `8000`, responsible for fetching market data from Twelve Data, computing technical indicators, and orchestrating backtests.
- `algo-backtester-java`: Java Spring Boot service running on port `8080`, responsible for the actual backtesting engine, strategies, portfolio simulation, metrics, and walk-forward validation.

The main runtime flow is:

1. The user opens the React dashboard and enters a ticker, period, and strategy.
2. React sends requests to the Python FastAPI service.
3. Python fetches OHLCV market data from Twelve Data.
4. Python sends normalized candle data to the Java Spring Boot engine.
5. Java runs the strategy backtest and returns metrics, trades, signals, chart series, and equity data.
6. React displays the candlestick chart, BUY/SELL signal markers, and performance metrics.

## Java Backtesting Engine

The Java service is the core quantitative engine. It contains the reusable backtesting logic and exposes it through a Spring Boot REST API.

Important parts:

- `BacktestEngine`: Coordinates data iteration, indicator updates, strategy signal generation, portfolio updates, and event notifications.
- `BacktestService`: Converts incoming API data into engine objects, runs backtests, and builds JSON responses.
- `BacktestController`: Exposes REST endpoints under `/api`.
- `Portfolio`: Tracks cash, shares, open trades, closed trades, and equity.
- `MetricsCalculator`: Computes net P&L, win rate, total return, max drawdown, Sharpe ratio, and bars read.
- `WalkForwardValidator`: Supports train/test walk-forward validation.

Implemented strategies:

- Moving Average Crossover
- Momentum
- Mean Reversion

Implemented indicators include:

- SMA
- EMA
- RSI
- Bollinger Bands
- Volume SMA

The Java engine also demonstrates several software design patterns:

- Facade pattern in `BacktestEngine`
- Observer pattern through `BacktestListener`
- Factory Method pattern in `StrategyFactory` and `IndicatorFactory`
- State pattern in the portfolio position system

## Python FastAPI Data Layer

The Python service acts as the bridge between live market data and the Java engine.

Main responsibilities:

- Loads `TWELVE_DATA_KEY` from environment variables or local `.env`.
- Fetches OHLCV data from Twelve Data.
- Normalizes data into `Date`, `Open`, `High`, `Low`, `Close`, and `Volume`.
- Computes indicators using the `ta` Python library.
- Generates simple live BUY/SELL/HOLD price signals.
- Calls the Java `/api/backtest` endpoint with candle data.

Important endpoints:

- `GET /health`: Python service health check.
- `GET /data`: Returns OHLCV data as JSON.
- `GET /data/csv`: Returns OHLCV data as CSV.
- `GET /indicators`: Returns latest computed indicator values.
- `GET /price-signal`: Returns a simple strategy signal.
- `POST /api/backtest`: Fetches market data, calls Java, and returns the backtest result.

This layer replaced older assumptions around yfinance with Twelve Data as the active market data provider.

## React Frontend

The React app provides the user-facing dashboard.

Current pages:

- `/`: Home/landing page for QuantEdge.
- `/backtest`: Interactive backtesting dashboard.

Key components:

- `BacktestForm`: Lets the user choose ticker, period, strategy, and moving-average parameters.
- `CandlestickChart`: Uses `lightweight-charts` to display OHLC candles and BUY/SELL markers.
- `MetricsTable`: Displays performance metrics returned by the Java engine.
- `Navbar` and `TickerTape`: UI/navigation components.

In development, Vite proxies:

- `/api` to the Python service on `localhost:8000`
- `/python` to the Python service on `localhost:8000`

The frontend does not call Java directly. It calls Python, and Python coordinates with Java.

## Legacy Node/Express Webserver

The repository still contains an older `Algo-backtester-webserver` project. This appears to be an earlier coursework or milestone implementation.

It includes:

- Express server
- Pug views
- Authentication routes
- MongoDB/Mongoose user model
- CSV upload routes
- Legacy file and dataset routes
- A Java integration layer that can call Python and Java services

This Node service is not the primary current app path. The active architecture is now React + FastAPI + Java Spring Boot. The Node project is useful historical work and may still contain assignment-specific features, but the main product direction has moved to the newer three-service system.

## Deployment Work

The project includes Docker and CI/CD setup:

- Root `docker-compose.yml` builds and runs Java, Python, and React locally.
- `docker-compose.prod.yml` is set up for production containers using ECR image tags.
- `.github/workflows/deploy.yml` builds Docker images, pushes them to Amazon ECR, SSHes into an EC2 server, pulls the images, and restarts services with Docker Compose.
- `scripts/server-setup.sh` appears intended for server provisioning.

One issue to fix: both Docker Compose files currently use `/actuator/health` for the Java health check, but the Java service exposes health at `/api/health`. Unless Spring Actuator is added, the health check should be updated.

## What Has Been Completed So Far

Completed work includes:

- A working Java backtesting engine with strategies, indicators, metrics, portfolio simulation, and REST endpoints.
- A Python FastAPI service that fetches live OHLCV data from Twelve Data.
- A Python-to-Java orchestration path for running backtests on live market data.
- A React dashboard that loads charts, runs backtests, overlays signals, and displays metrics.
- Dockerfiles and Docker Compose configuration for multi-service deployment.
- GitHub Actions deployment pipeline targeting AWS ECR and EC2.
- Legacy Node/Express webserver with authentication, uploads, templates, and Java integration.
- Architecture diagrams and planning documents under `Diagrams`.

## Current Runtime Status

The local development system runs with:

- Java Spring Boot: `http://localhost:8080`
- Python FastAPI: `http://localhost:8000`
- React/Vite frontend: `http://localhost:5173`

Useful health/documentation URLs:

- React app: `http://localhost:5173`
- FastAPI docs: `http://localhost:8000/docs`
- Java health: `http://localhost:8080/api/health`

## Remaining Gaps and Next Steps

The project is functional as a backtesting platform, but several areas still need cleanup or expansion:

- Fix Java health checks in Docker Compose from `/actuator/health` to `/api/health`, or add Spring Actuator.
- Decide whether the legacy Node/Express service should be archived, removed, or reintegrated.
- Add automated tests for Java strategies, Python data handling, and frontend behavior.
- Improve frontend support for walk-forward validation results.
- Add explicit environment variable documentation for local and production deployment.
- Remove generated or local-only files from version control where appropriate, such as `.DS_Store`, `node_modules`, local IDE files, and compiled outputs.
- Reconcile roadmap claims on the homepage with implemented functionality. ML, FinBERT, Alpaca, and portfolio optimization are described in the UI/README as future goals, but they are not yet implemented in the active code.

## One-Sentence Summary

QuantEdge is currently a working full-stack algorithmic trading backtesting platform: React provides the dashboard, Python fetches and prepares live market data, and Java runs the strategy engine and returns trading performance results.
