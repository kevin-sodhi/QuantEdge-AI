package com.kevin.algo.portfolio;

import java.time.LocalDate;

/**
 * DESIGN PATTERN: State (Behavioural) — State interface
 * -------------------------------------------------------
 * Represents what the portfolio is allowed to do depending on whether
 * it currently holds a position or not.
 *
 * How it fits here:
 *   Context        → Portfolio (holds a PositionState field, delegates calls)
 *   State          → this interface
 *   ConcreteStates → FlatState (no position), LongState (holding shares)
 *
 * Instead of an if/else on a boolean flag inside Portfolio, the behaviour
 * for onBuy and onSell changes automatically when the state transitions.
 * Portfolio never needs to know which state it is in — it just delegates.
 */
public interface PositionState {
    void onBuy(Portfolio ctx, LocalDate date, double price);
    void onSell(Portfolio ctx, LocalDate date, double price);
    boolean inPosition();
}
