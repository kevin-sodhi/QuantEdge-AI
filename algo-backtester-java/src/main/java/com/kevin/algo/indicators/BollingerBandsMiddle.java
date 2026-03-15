package com.kevin.algo.indicators;

/**
 * BollingerBandsMiddle — middle band = 20-day SMA (the mean)
 */
public class BollingerBandsMiddle extends BollingerBandsBase {

    public BollingerBandsMiddle() { super(); }
    public BollingerBandsMiddle(int period, double multiplier) { super(period, multiplier); }

    @Override
    public double value() { return mean(); }
}
