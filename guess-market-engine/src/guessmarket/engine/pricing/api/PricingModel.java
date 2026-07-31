package guessmarket.engine.pricing.api;

import guessmarket.engine.models.Event;

public interface PricingModel {
    /**
     * Calculates the dynamic cost of buying a specific quantity of shares for a given option.
     *
     * @param event       The event the shares belong to.
     * @param optionIndex The index of the option being purchased.
     * @param quantity    The number of shares being purchased.
     * @return The calculated cost before commission.
     */
    double calculateCost(Event event, int optionIndex, int quantity);
}
