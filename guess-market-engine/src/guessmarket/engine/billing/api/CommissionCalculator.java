package guessmarket.engine.billing.api;

import guessmarket.engine.models.Event;

public interface CommissionCalculator {
    /**
     * Calculates the commission for a given transaction cost, based on the event's rules.
     */
    double calculate(Event event, double cost);
}
