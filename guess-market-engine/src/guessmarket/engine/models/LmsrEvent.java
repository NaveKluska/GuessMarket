package guessmarket.engine.models;

import java.util.List;

public class LmsrEvent extends Event {
    private static final long serialVersionUID = 1L;
    private final int b;

    public LmsrEvent(int id, String name, String description, int commission, CommissionType commissionType, List<Option> options, int b) {
        super(id, name, description, commission, commissionType, options);
        this.b = b;
        this.accountBalance = 0.0;
    }

    public int getB() {
        return b;
    }

    @Override
    public double getOptionProbability(int optionIndex) {
        double sum = 0.0;
        for (Option opt : getOptions()) {
            sum += Math.exp(opt.getSharesBought() / (double) b);
        }
        return Math.exp(getOptions().get(optionIndex).getSharesBought() / (double) b) / sum;
    }

    @Override
    public double calculateCost(int optionIndex, int quantity) {
        double sumBefore = 0.0;
        for (Option opt : getOptions()) {
            sumBefore += Math.exp(opt.getSharesBought() / (double) b);
        }
        double costBefore = b * Math.log(sumBefore);

        double sumAfter = 0.0;
        String choiceName = getOptions().get(optionIndex).getName();
        for (Option opt : getOptions()) {
            int q = opt.getSharesBought();
            if (opt.getName().equals(choiceName)) {
                q += quantity;
            }
            sumAfter += Math.exp(q / (double) b);
        }
        double costAfter = b * Math.log(sumAfter);

        return costAfter - costBefore;
    }
}
