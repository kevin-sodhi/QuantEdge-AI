package com.kevin.algo.strategy;

/**
 * DESIGN PATTERN: Factory Method (Creational)
 * --------------------------------------------
 * Centralises object creation for Strategy implementations.
 * Callers (Main.java) ask for a strategy by name string and receive
 * a Strategy interface — they never depend on concrete classes directly.
 *
 * How it fits here:
 *   Factory   → StrategyFactory.create(name)
 *   Product   → Strategy interface
 *   Concrete  → MovingAverageCrossover (and future strategies)
 *
 * To add a new strategy: add a new case here and write its class.
 * Nothing else changes.
 */
public class StrategyFactory {

    public static Strategy create(String name) {
        return switch (name.toLowerCase()) {
            case "macrossover"   -> new MovingAverageCrossover();
            case "momentum"      -> new MomentumStrategy();
            case "meanreversion" -> new MeanReversionStrategy();
            default -> throw new IllegalArgumentException("Unknown strategy: " + name);
        };
    }
}
