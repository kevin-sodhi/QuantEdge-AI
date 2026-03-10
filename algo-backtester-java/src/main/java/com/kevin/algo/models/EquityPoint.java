package com.kevin.algo.models;

import java.time.LocalDate;

public class EquityPoint {
    public LocalDate date;
    public double equity;

    public EquityPoint(LocalDate date, double equity) {
        this.date = date;
        this.equity = equity;
    }
}
