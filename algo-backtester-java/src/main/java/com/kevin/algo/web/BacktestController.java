package com.kevin.algo.web;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.kevin.algo.web.dto.BacktestRequest;
import com.kevin.algo.web.dto.WalkForwardRequest;

/**
 * BacktestController
 * ------------------
 * Exposes the Java backtesting engine as a REST API on port 8080.
 * Called by Node.js (the API gateway) — never by the browser directly.
 *
 * @CrossOrigin allows requests from Node.js on localhost:3000
 * and from Railway during deployment.
 */
@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class BacktestController {

    private final BacktestService backtestService;

    public BacktestController(BacktestService backtestService) {
        this.backtestService = backtestService;
    }

    /** Health check — Railway uses this to confirm the service is live. */
    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of(
            "status",  "ok",
            "service", "quantedge-java",
            "port",    "8080"
        );
    }

    /** List all available strategy names. */
    @GetMapping("/strategies")
    public Map<String, Object> strategies() {
        return Map.of("strategies", List.of("macrossover", "momentum", "meanreversion"));
    }

    /**
     * Run a full backtest.
     *
     * POST /api/backtest
     * Body: { candles: [...], ticker, period, strategy, fast, slow, initialCapital }
     * Returns: { ok, params, metrics, trades, series, signals, equity }
     */
    @PostMapping("/backtest")
    public ResponseEntity<Map<String, Object>> runBacktest(@RequestBody BacktestRequest req) {
        try {
            return ResponseEntity.ok(backtestService.runBacktest(req));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("ok", false, "error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("ok", false, "error", e.getMessage()));
        }
    }

    /**
     * Run walk-forward validation.
     *
     * POST /api/walk-forward
     * Body: { candles: [...], strategy, trainRatio, indicator }
     * Returns: { ok, walkForward: { trainSharpe, testSharpe, bestFast, bestSlow, ... } }
     */
    @PostMapping("/walk-forward")
    public ResponseEntity<Map<String, Object>> runWalkForward(@RequestBody WalkForwardRequest req) {
        try {
            return ResponseEntity.ok(backtestService.runWalkForward(req));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("ok", false, "error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("ok", false, "error", e.getMessage()));
        }
    }
}
