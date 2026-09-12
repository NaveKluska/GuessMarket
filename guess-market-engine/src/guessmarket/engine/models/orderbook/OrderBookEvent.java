package guessmarket.engine.models.orderbook;

import guessmarket.engine.models.CommissionType;
import guessmarket.engine.models.Event;
import guessmarket.engine.models.Option;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OrderBookEvent extends Event {

    private final boolean allowMint;
    private final int initial;
    private final int d;
    private final OrderBookMarket market;
    private final Holdings holdings;
    /** Net cash a user has put into this event so far: positive means spent more than received. Used to derive profit/loss at close. */
    private final Map<String, Double> netCashFlowByUser = new HashMap<>();
    /** Gross amount a user has spent buying into each option (by index), never netted against sells - mirrors how commission is tracked. */
    private final Map<String, double[]> spentByUserPerOption = new HashMap<>();

    public OrderBookEvent(int id, String name, String description, int commissionValue,
                          CommissionType commissionType, List<Option> options,
                          boolean allowMint, int initial, int d) {
        super(id, name, description, commissionValue, commissionType, options);
        this.allowMint = allowMint;
        this.initial = initial;
        this.d = d;
        this.market = new OrderBookMarket(options.size(), d, allowMint);
        this.holdings = new Holdings(options.size());
    }

    public boolean isAllowMint() {
        return allowMint;
    }

    public int getInitial() {
        return initial;
    }

    public int getD() {
        return d;
    }

    public OrderBookMarket getMarket() {
        return market;
    }

    public Holdings getHoldings() {
        return holdings;
    }

    public MarketQuote getQuote(int optionIndex) {
        return market.getBook(optionIndex).getQuote();
    }

    /** Records money the user paid into this event (buy cost, mint cost, commission). */
    public void recordSpent(final String userName, final double amount) {
        if (amount <= 0) {
            return;
        }
        netCashFlowByUser.merge(userName, amount, Double::sum);
    }

    /** Records money the user received from this event (sell proceeds, close-time payout). */
    public void recordReceived(final String userName, final double amount) {
        if (amount <= 0) {
            return;
        }
        netCashFlowByUser.merge(userName, -amount, Double::sum);
    }

    /** Total profit (positive) or loss (negative) the user has realized from participating in this event so far. */
    public double getProfitOrLoss(final String userName) {
        return -netCashFlowByUser.getOrDefault(userName, 0.0);
    }

    /** Records that the user spent this much buying shares of this option (initial allocation, a fill, or a mint). */
    public void recordSpentOnOption(final String userName, final int optionIndex, final double amount) {
        if (amount <= 0) {
            return;
        }
        final double[] perOption = spentByUserPerOption.computeIfAbsent(userName, k -> new double[getOptions().size()]);
        perOption[optionIndex] += amount;
    }

    public double getSpentOnOption(final String userName, final int optionIndex) {
        final double[] perOption = spentByUserPerOption.get(userName);
        return perOption == null ? 0.0 : perOption[optionIndex];
    }

    @Override
    public double getOptionProbability(int optionIndex) {
        final MarketQuote quote = getQuote(optionIndex);
        if (quote.getMid() != null) {
            return quote.getMid() / d;
        }
        if (quote.getLast() != null) {
            return quote.getLast() / d;
        }
        return 0.5;
    }

    @Override
    public double calculateCost(int optionIndex, int quantity) {
        throw new UnsupportedOperationException("Order Book trading goes through submitOrder, not calculateCost.");
    }
}
