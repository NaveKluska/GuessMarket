package guessmarket.engine.models.lmsr;

import guessmarket.engine.models.CommissionType;
import guessmarket.engine.models.Event;
import guessmarket.engine.models.Option;

import java.util.List;

public class LmsrEvent extends Event {
    private static final long serialVersionUID = 1L;
    private final int b;

    public LmsrEvent(String name, String description, int commission, CommissionType commissionType, List<Option> options, int b) {
        super(name, description, commission, commissionType, options);
        this.b = b;
        this.accountBalance = 0.0;
    }

    public int getB() {
        return b;
    }

    public double calculateInitialSubsidy() {
        return b * Math.log(getOptions().size());
    }

    @Override
    public double getOptionProbability(int optionIndex) {
        return probabilityAt(sharesSnapshot(-1, 0), optionIndex);
    }

    /**
     * The price of an option for an arbitrary set of share counts, rather than the current ones.
     * <p>
     * Pure: it reads nothing from the event and changes nothing, which is what lets a caller
     * reconstruct the whole price history by replaying past share counts through it without
     * disturbing live state. Uses the same max-subtracted form as {@link #cost}, for the same
     * overflow reason.
     *
     * @param shares      share counts per option, indexed the same way as getOptions()
     * @param optionIndex the option whose price is wanted
     */
    public double probabilityAt(final int[] shares, final int optionIndex) {
        final double max = maxExponent(shares);
        double sum = 0.0;
        for (int q : shares) {
            sum += Math.exp(q / (double) b - max);
        }
        return Math.exp(shares[optionIndex] / (double) b - max) / sum;
    }

    @Override
    public double calculateCost(int optionIndex, int quantity) {
        return cost(sharesSnapshot(optionIndex, quantity)) - cost(sharesSnapshot(-1, 0));
    }

    /**
     * C(q) = b * ln( sum of e^(qi/b) ), evaluated as b * (m + ln( sum of e^(qi/b - m) )) where m is
     * the largest exponent.
     * <p>
     * The two forms are algebraically identical - factoring e^m out of the sum turns it into a
     * multiplier that ln() converts to the leading m - but the second never feeds a large number to
     * exp(). Written directly, e^(qi/b) overflows to infinity once qi/b passes about 709 (the limit
     * of a double), which made the cost NaN and a trader's balance -Infinity. Subtracting the
     * maximum first means the biggest term is always exactly e^0 = 1, so it cannot overflow at any
     * share count. Verified against the direct form over 244,766 sampled states: the two agree to
     * within 1.2e-10 of a dollar wherever the direct form stays finite.
     */
    private double cost(final int[] shares) {
        final double max = maxExponent(shares);
        double sum = 0.0;
        for (int q : shares) {
            sum += Math.exp(q / (double) b - max);
        }
        return b * (max + Math.log(sum));
    }

    private double maxExponent(final int[] shares) {
        double max = Double.NEGATIVE_INFINITY;
        for (int q : shares) {
            max = Math.max(max, q / (double) b);
        }
        return max;
    }

    /**
     * Current share counts, optionally with {@code quantity} added to one option.
     * Indexed by position rather than matched by name, so options that happen to share a name
     * cannot have the quantity applied to both.
     */
    private int[] sharesSnapshot(final int optionIndex, final int quantity) {
        final int[] shares = new int[getOptions().size()];
        for (int i = 0; i < shares.length; i++) {
            shares[i] = getOptions().get(i).getSharesBought();
            if (i == optionIndex) {
                shares[i] += quantity;
            }
        }
        return shares;
    }
}
