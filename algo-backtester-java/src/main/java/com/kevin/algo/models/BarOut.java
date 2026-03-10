package com.kevin.algo.models;

import java.time.LocalDate;

public class BarOut {
    public LocalDate date;
    public double open, high, low, close;
    public Double smaFast, smaSlow;

    public BarOut(LocalDate date, double open, double high, double low,double close, Double smaFast, Double smaSlow) {
        this.date = date; this.open = open; this.high = high;
        this.low = low; this.close = close;
        this.smaFast = smaFast; this.smaSlow = smaSlow;
    }
}