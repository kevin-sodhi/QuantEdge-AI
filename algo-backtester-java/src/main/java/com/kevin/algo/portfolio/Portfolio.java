package com.kevin.algo.portfolio;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.kevin.algo.models.Trade;

/**
 * DESIGN PATTERN: State (Behavioural) — Context
 * -----------------------------------------------
 * Portfolio is the "context" in the State pattern.
 * It holds a reference to the current PositionState (starts as FlatState)
 * and delegates onBuy/onSell calls to it. The state itself transitions
 * to the next state when appropriate (Flat → Long on buy, Long → Flat on sell).
 *
 * Before State pattern:   Portfolio had an `inPosition` boolean and used
 *                         if/else branches for every action.
 * After State pattern:    Each state class encapsulates its own behaviour.
 *                         Portfolio.onBuy() is always one line: state.onBuy(...).
 *
 * Package-private methods (executeBuy, executeSell, setState) are the "back
 * channel" that concrete state classes use to mutate Portfolio safely.
 */
public class Portfolio {
    private double cash;
    private final double fee, slip;
    int shares = 0;       // package-private: accessed by state classes
    Trade open;           // package-private: accessed by state classes
    private final List<Trade> closed = new ArrayList<>();
    private PositionState state = new FlatState();

    public Portfolio(double startingCash, double fee, double slippage) {
        this.cash = startingCash;
        this.fee = fee;
        this.slip = slippage;
    }

    // ---- Public API (delegates to state) ----

    public boolean inPosition() { return state.inPosition(); }

    public void onBuy(LocalDate date, double price) {
        state.onBuy(this, date, price);
    }

    public void onSell(LocalDate date, double price) {
        state.onSell(this, date, price);
    }

    public double equityAt(double price) {
        return cash + shares * price;
    }

    public List<Trade> closedTrades() { return closed; }

    public double finalEquity(double lastClose) {
        return equityAt(lastClose);
    }

    // ---- Package-private: called by state classes ----

    void setState(PositionState next) {
        this.state = next;
    }

    void executeBuy(LocalDate date, double price) {
        int qty = (int)(cash / (price + fee + slip));
        if (qty <= 0) return;
        cash -= qty * price + fee + slip;
        shares = qty;
        open = new Trade(date, price);
    }

    void executeSell(LocalDate date, double price) {
        if (open == null) return;
        cash += shares * price - fee - slip;
        open.exitDate = date;
        open.exitPrice = price;
        open.pnl = (open.exitPrice - open.entryPrice) * shares - 2 * (fee + slip);
        closed.add(open);
        open = null;
        shares = 0;
    }
}
