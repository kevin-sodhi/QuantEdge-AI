package com.kevin.algo.cli;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.kevin.algo.core.Result;
import com.kevin.algo.data.DataFeed;
import com.kevin.algo.data.DataFeedFactory;
import com.kevin.algo.engine.BacktestConfig;
import com.kevin.algo.engine.BacktestEngine;
import com.kevin.algo.engine.MetricsCalculator;
import com.kevin.algo.engine.ResultCollector;
import com.kevin.algo.indicators.Indicator;
import com.kevin.algo.indicators.IndicatorFactory;
import com.kevin.algo.portfolio.Portfolio;
import com.kevin.algo.strategy.Strategy;
import com.kevin.algo.strategy.StrategyFactory;

/**
 * Entry point.
 *
 * Build & run:
 *   mvn -q clean package -DskipTests
 *   java -jar target/algo-backtester-java-1.0.0-jar-with-dependencies.jar \
 *     --csv data/TSLA.csv --fast 3 --slow 5 --strategy macrossover --indicator sma
 *
 * New flags:
 *   --indicator  sma|ema  (default: sma)
 */
public class Main {

    private static final Gson GSON = new GsonBuilder()
        .registerTypeAdapter(LocalDate.class, new TypeAdapter<LocalDate>() {
            // Java → JSON: LocalDate.of(2024,1,15) → "2024-01-15"
            @Override
            public void write(JsonWriter out, LocalDate value) throws java.io.IOException {
                out.value(value != null ? value.toString() : null);
            }
            // JSON → Java: "2024-01-15" → LocalDate
            @Override
            public LocalDate read(JsonReader in) throws java.io.IOException {
                String s = in.nextString();
                return (s == null) ? null : LocalDate.parse(s);
            }
        })
        .setPrettyPrinting()
        .create();

    public static void main(String[] args) {
        // 1) Parse CLI flags
        Map<String, String> flags = parseArgs(args);
        String csv       = flags.getOrDefault("csv", "");
        String strategy  = flags.getOrDefault("strategy", "macrossover");
        String indicator = flags.getOrDefault("indicator", "sma");
        int fast         = tryParseInt(flags.get("fast"), 3);
        int slow         = tryParseInt(flags.get("slow"), 5);
        double cash      = tryParseDouble(flags.get("cash"), 10000.0);
        double fee       = tryParseDouble(flags.get("fee"), 0.0);
        double slip      = tryParseDouble(flags.get("slip"), 0.0);

        // 2) Validate CSV path
        Path csvPath = Path.of(csv).toAbsolutePath();
        Map<String, Object> response = new HashMap<>();
        if (!Files.exists(csvPath)) {
            response.put("ok", false);
            response.put("error", "CSV not found: " + csvPath);
            System.out.println(GSON.toJson(response));
            return;
        }

        // 3) Build components via factories (Factory pattern)
        DataFeed feed   = DataFeedFactory.create("csv", csvPath.toString());
        Indicator iFast = IndicatorFactory.create(indicator, fast);
        Indicator iSlow = IndicatorFactory.create(indicator, slow);
        Strategy  strat = StrategyFactory.create(strategy);
        Portfolio pf    = new Portfolio(cash, fee, slip);

        // 4) Wire config via Builder pattern
        BacktestConfig cfg = new BacktestConfig.Builder()
            .feed(feed)
            .fast(iFast)
            .slow(iSlow)
            .strategy(strat)
            .portfolio(pf)
            .build();

        // 5) Register Observer (ResultCollector) and run engine
        ResultCollector collector = new ResultCollector();
        BacktestEngine engine = new BacktestEngine();
        engine.addListener(collector);
        engine.run(cfg);

        // 6) Compute full metrics
        Result.Metrics metrics = MetricsCalculator.compute(pf.closedTrades(), collector.equity, cash);

        // 7) Assemble params block
        Result.Params params = new Result.Params();
        params.csv      = csv;
        params.strategy = strategy;
        params.indicator = indicator;
        params.fast     = fast;
        params.slow     = slow;
        params.cash     = cash;
        params.fee      = fee;
        params.slip     = slip;

        // 8) Build JSON response
        response.put("ok",      true);
        response.put("message", "Backtest complete");
        response.put("params",  params);
        response.put("metrics", metrics);
        response.put("series",  collector.series);
        response.put("signals", collector.signals);
        response.put("equity",  collector.equity);

        // 9) Print JSON
        System.out.println(GSON.toJson(response));
    }

    // ---- Utility helpers ----

    private static Map<String, String> parseArgs(String[] args) {
        Map<String, String> map = new HashMap<>();
        for (int i = 0; i < args.length; i++) {
            String token = args[i];
            if (token.startsWith("--")) {
                String key = token.substring(2);
                String val = "";
                if (i + 1 < args.length && !args[i + 1].startsWith("--")) {
                    val = args[++i];
                }
                map.put(key, val);
            }
        }
        return map;
    }

    private static int tryParseInt(String s, int fallback) {
        try { return (s == null || s.isEmpty()) ? fallback : Integer.parseInt(s); }
        catch (NumberFormatException e) { return fallback; }
    }

    private static double tryParseDouble(String s, double fallback) {
        try { return (s == null || s.isEmpty()) ? fallback : Double.parseDouble(s); }
        catch (NumberFormatException e) { return fallback; }
    }
}
