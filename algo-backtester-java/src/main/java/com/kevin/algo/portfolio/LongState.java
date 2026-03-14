package com.kevin.algo.portfolio;

import java.time.LocalDate;

/**
 * DESIGN PATTERN: State (Behavioural) — Concrete State: "Long"
 * --------------------------------------------------------------
 * Represents the portfolio when it IS holding shares.
 *
 * In this state:
 *   onBuy()  → ignored (already long, no pyramiding)
 *   onSell() → executes the sell and transitions context back to FlatState
 *
 * The transition back (setState(new FlatState())) lives here, keeping
 * Portfolio.java free of branching state-transition logic.
 */
public class LongState implements PositionState {

    @Override
    public void onBuy(Portfolio ctx, LocalDate date, double price) {
        // already long, ignore
    }

    @Override
    public void onSell(Portfolio ctx, LocalDate date, double price) {
        ctx.executeSell(date, price);
        ctx.setState(new FlatState());
    }

    @Override
    public boolean inPosition() {
        return true;
    }
}
