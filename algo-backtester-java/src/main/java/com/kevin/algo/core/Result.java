package com.kevin.algo.core;

import java.util.ArrayList;
import java.util.List;

import com.kevin.algo.models.BarOut;
import com.kevin.algo.models.EquityPoint;
import com.kevin.algo.models.Signal;

/**
 * DESIGN PATTERN: Data Transfer Object / DTO (Architectural)
 * -----------------------------------------------------------
 * Result is a plain data holder with no behaviour. Its sole purpose is to
 * carry all backtest output fields in one object so Gson can serialise them
 * to JSON in a single call.
 *
 * Why DTO?
 *   Main.java fills in the fields; Gson reads them. Nothing else touches Result.
 *   Keeping it a dumb data bag separates "what data" from "how it's computed"
 *   (MetricsCalculator) and "how it's printed" (Main → Gson).
 *
 * Inner classes:
 *   Params  → echo of CLI flags for debugging / audit trail in the JSON
 *   Metrics → computed performance numbers (filled by MetricsCalculator)
 */
public class Result {

    public static class Params {
        public String csv, strategy, indicator;
        public int fast, slow;
        public double cash, fee, slip;
    }

    public static class Metrics {
        public int barsRead;
        public int trades;
        public double netPnl;
        public double totalReturnPct;
        public double winRatePct;
        public double maxDrawdown;
        public double sharpe;
    }

    // ---- Main result body ----
    public boolean ok;
    public String message;
    public Params params = new Params();
    public Metrics metrics = new Metrics();

    public List<BarOut> series = new ArrayList<>();
    public List<Signal> signals = new ArrayList<>();
    public List<EquityPoint> equity = new ArrayList<>();
}
