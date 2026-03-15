package com.kevin.algo.indicators;

/**
 * BollingerBandsLower — lower band = mean - 2 * stdDev
 */
public class BollingerBandsLower extends BollingerBandsBase {

    public BollingerBandsLower() { super(); }
    public BollingerBandsLower(int period, double multiplier) { super(period, multiplier); }

    @Override
    public double value() { return mean() - multiplier * stdDev(); }
}
