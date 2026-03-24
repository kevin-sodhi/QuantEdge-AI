package com.kevin.algo.web.dto;

import java.util.List;

/**
 * WalkForwardRequest — body of POST /api/walk-forward.
 */
public class WalkForwardRequest {
    public List<CandleData> candles;

    public String ticker          = "";
    public String period          = "1y";
    public String strategy        = "macrossover";
    public String indicator       = "sma";
    public double trainRatio      = 0.7;
    public double initialCapital  = 10_000.0;
}
