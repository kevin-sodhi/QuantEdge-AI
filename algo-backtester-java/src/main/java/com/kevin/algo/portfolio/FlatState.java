package com.kevin.algo.portfolio;

import java.time.LocalDate;

/**
 * DESIGN PATTERN: State (Behavioural) — Concrete State: "Flat"
 * --------------------------------------------------------------
 * Represents the portfolio when it holds NO shares.
 *
 * In this state:
 *   onBuy()  → executes the buy and transitions context to LongState
 *   onSell() → ignored (nothing to sell)
 *
 * The state transition (setState(new LongState())) happens here, inside the
 * state itself — Portfolio never manually flips its own state flag.
 */
public class FlatState implements PositionState {

    @Override
    public void onBuy(Portfolio ctx, LocalDate date, double price) {
        ctx.executeBuy(date, price);
        ctx.setState(new LongState());
    }

    @Override
    public void onSell(Portfolio ctx, LocalDate date, double price) {
        // nothing to sell
    }

    @Override
    public boolean inPosition() {
        return false;
    }
}
