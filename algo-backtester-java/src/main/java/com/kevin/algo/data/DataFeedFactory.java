package com.kevin.algo.data;

/**
 * Factory pattern: creates DataFeed instances by source type.
 * Add new feed types (database, REST API, WebSocket) here.
 */
public class DataFeedFactory {

    public static DataFeed create(String type, String source) {
        return switch (type.toLowerCase()) {
            case "csv" -> new CsvDataFeed(source);
            default -> throw new IllegalArgumentException("Unknown feed type: " + type);
        };
    }
}
