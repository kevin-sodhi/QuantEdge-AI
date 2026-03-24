package com.kevin.algo.indicators;

/**
 * BollingerBandsUpper — upper band = mean + 2 * stdDev
 */
public class BollingerBandsUpper extends BollingerBandsBase {

    public BollingerBandsUpper() { super(); }
    public BollingerBandsUpper(int period, double multiplier) { super(period, multiplier); }

    @Override
    public double value() { return mean() + multiplier * stdDev(); }
}
