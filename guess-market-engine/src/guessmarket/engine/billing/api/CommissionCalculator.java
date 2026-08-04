package guessmarket.engine.billing.api;

import guessmarket.engine.models.CommissionType;

public interface CommissionCalculator {
    /**
     * Calculates the commission based on the event's commission phase and the current phase.
     * If the phases match, it calculates the commission using the amount and percentage.
     * If they do not match, it returns 0.0.
     * 
     * @param amount The transaction cost or winning amount.
     * @param commissionPercentage The commission rate (e.g. 5 for 5%).
     * @param eventCommissionType The phase when this event collects commission.
     * @param calculationPhase The phase currently executing in the engine.
     * @return The calculated commission, or 0.0 if phases do not match.
     */
    double calculate(double amount, int commissionPercentage, CommissionType eventCommissionType, CommissionType calculationPhase);
}
