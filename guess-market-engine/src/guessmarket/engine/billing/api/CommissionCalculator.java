package guessmarket.engine.billing.api;

import guessmarket.engine.models.CommissionType;

public interface CommissionCalculator {
    /**
     * Calculates the commission for a given transaction amount.
     */
    double calculate(double amount, int commissionPercentage, CommissionType eventCommissionType, CommissionType calculationPhase);
}
