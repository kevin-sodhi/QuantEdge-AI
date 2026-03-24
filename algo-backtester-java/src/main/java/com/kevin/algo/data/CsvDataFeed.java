package com.kevin.algo.data;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import com.kevin.algo.core.Candle;

/**
 * CsvDataFeed
 * -----------
 * Reads stock-market data (like Yahoo Finance CSVs) and streams each row
 * as a Candle object: (date, open, high, low, close, volume).
 *
 * Features:
 *  • Handles quoted / comma-separated numbers.
 *  • Skips header line automatically.
 *  • Accepts multiple date formats (e.g., 2025-10-29 or 10/29/2025).
 *  • Ignores header case (DATE vs Date).
 *  • Automatically reverses data if sorted newest → oldest.
 *  • Optionally uses "Adj Close" instead of "Close".
 */
public class CsvDataFeed implements DataFeed {

    // --- Config ---
    private static final boolean FLAG_USE_ADJ_CLOSE = false;

    // Supported date formats
    private static final DateTimeFormatter[] DATE_FORMATS = new DateTimeFormatter[] {
        DateTimeFormatter.ISO_LOCAL_DATE,                      // 2025-10-29
        DateTimeFormatter.ofPattern("M/d/yyyy", Locale.US),    // 10/9/2025
        DateTimeFormatter.ofPattern("MM/dd/yyyy", Locale.US),  // 10/29/2025
        DateTimeFormatter.ofPattern("yyyy-M-d", Locale.US)     // 2025-7-1
    };

    // --- Internal state ---
    private final java.util.List<Candle> candles = new java.util.ArrayList<>();
    private int idx = 0;
    private Reader fileReader;
    private CSVParser parser;

    // Column header names (detected dynamically)
    private String colDate = "Date";
    private String colOpen = "Open";
    private String colHigh = "High";
    private String colLow  = "Low";
    private String colClose = "Close";
    private String colAdjClose = "Adj Close";
    private String colVolume = "Volume";

    /**
     * Constructor
     * Opens and parses the CSV file into memory, reversing order if needed.
     * @param csvPath path to CSV file (e.g., data/TSLA.csv)
     */
    public CsvDataFeed(String csvPath) {
        try {
            this.fileReader = Files.newBufferedReader(Path.of(csvPath));
            this.parser = CSVFormat.DEFAULT
                    .withFirstRecordAsHeader()
                    .withIgnoreHeaderCase()
                    .withTrim()
                    .parse(fileReader);

            // Detect columns dynamically
            var headerMap = parser.getHeaderMap();
            for (String h : headerMap.keySet()) {
                String lower = h.trim().toLowerCase(Locale.ROOT);
                if (lower.equals("date")) colDate = h;
                else if (lower.equals("open")) colOpen = h;
                else if (lower.equals("high")) colHigh = h;
                else if (lower.equals("low")) colLow = h;
                else if (lower.equals("close")) colClose = h;
                else if (lower.equals("adj close") || lower.equals("adjclose")) colAdjClose = h;
                else if (lower.equals("volume")) colVolume = h;
            }

            // Load all rows into memory
            for (CSVRecord r : parser) {
                Candle c = toCandle(r);
                candles.add(c);
            }

            // Auto-reverse if newest → oldest
            if (candles.size() >= 2) {
                LocalDate first = candles.get(0).getDate();
                LocalDate last  = candles.get(candles.size() - 1).getDate();
                if (first.isAfter(last)) {
                    java.util.Collections.reverse(candles);
                }
            }

            parser.close();
            fileReader.close();

        } catch (IOException e) {
            throw new RuntimeException("Failed to open/parse CSV: " + csvPath, e);
        }
    }

    /** True if more candles are available */
    @Override
    public boolean hasNext() {
        return idx < candles.size();
    }

    /** Returns the next Candle and advances the pointer */
    @Override
    public Candle next() {
        if (!hasNext()) throw new IllegalStateException("No more rows");
        Candle c = candles.get(idx++);
        if (!hasNext()) {
            try { if (parser != null) parser.close(); if (fileReader != null) fileReader.close(); } catch (IOException ignored) {}
        }
        return c;
    }

    /** Convert a CSVRecord into a Candle object */
    private Candle toCandle(CSVRecord r) {
        LocalDate date = parseDate(r.get(colDate));
        double open  = parseDouble(r.get(colOpen));
        double high  = parseDouble(r.get(colHigh));
        double low   = parseDouble(r.get(colLow));
        double close = FLAG_USE_ADJ_CLOSE && hasColumn(r, colAdjClose)
                ? parseDouble(r.get(colAdjClose))
                : parseDouble(r.get(colClose));
        long volume  = parseLong(r.get(colVolume));

        return new Candle(date, open, high, low, close, volume);
    }

    /** Check if a column exists */
    private static boolean hasColumn(CSVRecord r, String name) {
        try {
            r.get(name);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /** Parse multiple possible date formats */
    private static LocalDate parseDate(String s) {
        String in = s == null ? "" : s.trim();
        for (DateTimeFormatter fmt : DATE_FORMATS) {
            try {
                return LocalDate.parse(in, fmt);
            } catch (Exception ignored) {}
        }
        throw new IllegalArgumentException("Unrecognized date: " + s);
    }

    /** Clean and parse doubles */
    private static double parseDouble(String s) {
        String cleaned = cleanNumber(s);
        if (cleaned.isEmpty()) return Double.NaN;
        return Double.parseDouble(cleaned);
    }

    /** Clean and parse longs */
    private static long parseLong(String s) {
        String cleaned = cleanNumber(s);
        if (cleaned.isEmpty()) return 0L;
        return Long.parseLong(cleaned);
    }

    /** Remove commas, quotes, and whitespace */
    private static String cleanNumber(String s) {
        return (s == null ? "" :
                s.replace(",", "")
                 .replace("\"", "")
                 .trim());
    }
}