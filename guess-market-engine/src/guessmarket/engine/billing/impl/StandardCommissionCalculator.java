package guessmarket.engine.billing.impl;

import guessmarket.engine.billing.api.CommissionCalculator;
import guessmarket.engine.models.CommissionType;
import guessmarket.engine.models.Event;

public class StandardCommissionCalculator implements CommissionCalculator {

    @Override
    public double calculate(final Event event, final double cost) {
        if (event.getCommissionType() == CommissionType.ON_PURCHASE) {
            return cost * ((double) event.getCommission() / 100.0);
        }
        return 0.0;
    }
}
