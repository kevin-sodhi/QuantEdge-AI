package com.kevin.algo.portfolio;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.kevin.algo.models.Trade;

public class Portfolio {
    private double cash;
    private final double fee, slip;
    private int shares = 0;
    private Trade open;
    private final List<Trade> closed = new ArrayList<>();

    public Portfolio(double startingCash, double fee, double slippage) {
        this.cash = startingCash;
        this.fee = fee;
        this.slip = slippage;
    }

    public boolean inPosition() { return shares > 0; }

    public void onBuy(LocalDate date, double price) {
        if (shares > 0) return;
        int qty = (int)(cash / (price + fee + slip));
        if (qty <= 0) return;
        cash -= qty * price + fee + slip;
        shares = qty;
        open = new Trade(date, price);
    }

    public void onSell(LocalDate date, double price) {
        if (shares == 0 || open == null) return;
        cash += shares * price - fee - slip;
        open.exitDate = date;
        open.exitPrice = price;
        open.pnl = (open.exitPrice - open.entryPrice) * shares - 2*(fee + slip);
        closed.add(open);
        open = null;
        shares = 0;
    }

    public double equityAt(double price) {
        return cash + shares * price;
    }

    public List<Trade> closedTrades() { return closed; }

    public double finalEquity(double lastClose) {
        return equityAt(lastClose);
    }
}