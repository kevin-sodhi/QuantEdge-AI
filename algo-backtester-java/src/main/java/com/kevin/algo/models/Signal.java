package com.kevin.algo.models;

import java.time.LocalDate;

public class Signal {
    public enum Type { BUY, SELL }

    public LocalDate date;
    public double price;
    public Type type;

    public Signal(LocalDate date, double price, Type type) {
        this.date = date;
        this.price = price;
        this.type = type;
    }
}