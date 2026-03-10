package com.kevin.algo.core;

import java.util.ArrayList;
import java.util.List;

/**
 * Result
 * ------
 * Final container for backtest outputs.
 * JSON-serializable with Gson.
 */
public class Result {

    /** Params about this run */
    public static class Params {
        public String csv, strategy;
        public int fast, slow;
        public double cash, fee, slip;
    }

    /** Core metrics */
    public static class Metrics {
        public int trades;
        public double netPnl;
        public double sharpe;
        public double maxDrawdown;
        public int barsRead;
        public double winRatePct;
        public double totalReturnPct;
    }

    /** One equity point (time label + equity value) */
    public static class EquityPoint {
        public String t;
        public double eq;
        public EquityPoint() {}
        public EquityPoint(String t, double eq) { this.t = t; this.eq = eq; }
    }

    // ---- Main result body ----
    public boolean ok;
    public String message;
    public Params params = new Params();
    public Metrics metrics = new Metrics();

    // These lists connect directly to the front-end
    public List<com.kevin.algo.models.BarOut> series = new ArrayList<>();
    public List<com.kevin.algo.models.Signal> signals = new ArrayList<>();
    public List<com.kevin.algo.models.EquityPoint> equity = new ArrayList<>();
}