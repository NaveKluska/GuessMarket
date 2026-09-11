package guessmarket.engine.models.orderbook;

import guessmarket.engine.models.CommissionType;
import guessmarket.engine.models.Event;
import guessmarket.engine.models.Option;

import java.util.List;

public class OrderBookEvent extends Event {

    private final boolean allowMint;
    private final int initial;
    private final int d;
    private final OrderBookMarket market;
    private final Holdings holdings;

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
