package guessmarket.engine.pricing.impl;

import guessmarket.engine.pricing.api.PricingModel;
import guessmarket.engine.models.Event;
import guessmarket.engine.models.Option;

public class LmsrPricingModel implements PricingModel {

    @Override
    public double calculateCost(final Event event, final int optionIndex, final int quantity) {
        final double b = event.getB();
        
        // 1. Calculate LMSR state BEFORE purchase: C = b * ln(sum(e^(q_i / b)))
        double sumBefore = 0.0;
        for (final Option opt : event.getOptions()) {
            sumBefore += Math.exp(opt.getSharesBought() / b);
        }
        final double costBefore = b * Math.log(sumBefore);

        // 2. Calculate LMSR state AFTER purchase
        double sumAfter = 0.0;
        final String choiceName = event.getOptions().get(optionIndex).getName();
        for (final Option opt : event.getOptions()) {
            int q = opt.getSharesBought();
            if (opt.getName().equals(choiceName)) {
                q += quantity; // Pretend we added the new shares
            }
            sumAfter += Math.exp(q / b);
        }
        final double costAfter = b * Math.log(sumAfter);

        // 3. Return the dynamic price difference
        return costAfter - costBefore;
    }
}
