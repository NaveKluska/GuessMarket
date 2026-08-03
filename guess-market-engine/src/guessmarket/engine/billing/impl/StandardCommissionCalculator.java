package guessmarket.engine.billing.impl;

import guessmarket.engine.billing.api.CommissionCalculator;
import guessmarket.engine.models.CommissionType;
import guessmarket.engine.models.Event;

public class StandardCommissionCalculator implements CommissionCalculator {
    @Override
    public double calculate(double amount, int commissionPercentage, CommissionType eventCommissionType, CommissionType calculationPhase) {
        if (eventCommissionType == calculationPhase) {
            return amount * ((double) commissionPercentage / 100.0);
        }
        return 0.0;
    }
}
