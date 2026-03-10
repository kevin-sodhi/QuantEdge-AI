package com.kevin.algo.models;

import java.time.LocalDate;

public class Trade {
    public LocalDate entryDate, exitDate;
    public double entryPrice, exitPrice, pnl;

    public Trade(LocalDate entryDate, double entryPrice) {
        this.entryDate = entryDate;
        this.entryPrice = entryPrice;
    }
}