# Algo Backtester Webserver (Tour)
Node + Express front end for the Java moving-average backtester. Users upload a CSV, pick fast/slow MA windows, and the server spawns the Java JAR to calculate signals and metrics, then renders the results with Pug templates.

## Quick start (what to run)
- Requires Node (tested with Express 5) and Java 17.
- Build the Java engine in the sibling repo: `cd ../algo-backtester-java && mvn -q -DskipTests package`. The Node service looks for `../algo-backtester-java/target/algo-backtester-java-1.0.0-jar-with-dependencies.jar`. A copy also lives in `engine/algo-backtester.jar` for submission; point `services/javaIntegration.js` there if needed.
- Install and start the web server: `npm install` then `npm run dev` (nodemon) or `npm start`.
- Visit `http://localhost:3000`. Login/signup first, then go to `/old-home` to upload and backtest.

## Where everything lives
- `AlgoBacktester.js` – Express entry point; configures sessions, Pug views, connects to MongoDB, registers all routers, and custom 404/500 handlers.


- `services/javaIntegration.js` – Spawns the Java JAR with `child_process.spawn`, builds args for `--csv/--fast/--slow/--strategy`, and parses JSON output. Default CSV fallback is `../data/TSLA.csv`. Uploads are saved to `../data/` (path is created if missing).


- `routes/` – Request handlers:
  - `routes/auth.js` handles signup/login/logout with PBKDF2 hashing and session creation.
  - `routes/home.js` protects `/old-home` with `middleware/requireAuth.js` and serves the main HTML form.
  - `routes/uploads.js` streams CSV uploads via Multiparty, enforces CSV-only, and remembers the last uploaded file.
  - `routes/backtest.js` runs the Java engine (GET/POST), then renders `views/backtest-results.pug` with metrics, signals, and raw JSON.
  - `routes/backtestLegacy.js` and `routes/dataset.js` are lightweight demos of query/body and route params for the assignment.
  - `routes/files.js` sanitizes and serves downloads from `../data/`.


- `Database/db.js` – Connects to MongoDB Atlas using `credentials.js`, drops/recreates the `users` collection with JSON Schema validation on startup.

- `models/User.js` – Mongoose schema for users (name, email, hash, salt, createdAt).

- `middleware/requireAuth.js` – Redirects unauthenticated users to `/login`.

- `views/` – Pug templates. `base.pug` sets layout/nav, `login.pug` and `signup.pug` are auth pages, `backtest-results.pug` renders metrics and Chart.js line chart with BUY/SELL markers, `error.pug` for templated errors. Partials in `views/partials/` render signals and table rows.

- `public/` – Static assets and plain HTML pages (`index.html` for the legacy form, custom `404.html`/`500.html`, `sample.csv`, hero images/videos, `style.css`).

- `engine/` – Submitted copy of the backtester JAR (not the path used by default code; see Quick start).

- `../data/` – Upload target and default CSVs. The server’s `FILES_DIR` points here so data is outside the repo root.

- `credentials.js` – MongoDB Atlas credentials (username/password/cluster). No session secret stored here; session secret is hardcoded in `AlgoBacktester.js`.

## Request/response flow
1) Auth: `/signup` hashes the password with PBKDF2 and saves salt/hash; `/login` recomputes hash and stores `req.session.user`; `/logout` destroys the session.  
2) Upload: `/upload` (Multiparty) writes CSV to `../data/` and records the last upload in memory. Non-CSV files are rejected and deleted.  
3) Backtest: `/backtest/run` (GET/POST) resolves the CSV path (uploaded file or default TSLA), spawns the Java JAR with MA parameters, and parses JSON output. Errors surface as rendered error pages.  
4) Results: `views/backtest-results.pug` shows metrics table, signals list, raw JSON, and Chart.js plot (price with BUY/SELL markers).  
5) Downloads: `/files/:name` serves sanitized filenames from `../data/` for grading/inspection.

## Notes
- The Mongo connection drops and recreates the `users` collection on every server start to demonstrate server-side schema validation.
- Default navigation hides headers on login/signup (`hideNav` flag in views). Main content after auth is the static HTML form at `/old-home`.
- If the Java JAR is missing, `services/javaIntegration.js` logs a warning and backtests will fail when called; ensure the path in that file matches where the grader places the JAR.

## Example usage
- Go to `/signup`, create an account, then `/login`.
- Upload `public/sample.csv` (or your own) at the form in `/old-home`.
- Submit fast/slow MA values (example `5` and `20`) to `/backtest/run` and view the rendered metrics, signals, and JSON.
