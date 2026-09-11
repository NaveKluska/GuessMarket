package guessmarket.engine.models.orderbook;

/**
 * A snapshot of one option's current market: last trade price, best bid, best ask, and the
 * derived mid-price and spread. Any of these can be null if there isn't enough data yet
 * (e.g. no trade has happened, or one side of the book is empty).
 */
public class MarketQuote
{
    private final Double last;
    private final Double bid;
    private final Double ask;

    public MarketQuote(final Double last, final Double bid, final Double ask)
    {
        this.last = last;
        this.bid = bid;
        this.ask = ask;
    }

    public Double getLast()
    {
        return last;
    }

    public Double getBid()
    {
        return bid;
    }

    public Double getAsk()
    {
        return ask;
    }

    public Double getMid()
    {
        return (bid == null || ask == null) ? null : (bid + ask) / 2.0;
    }

    public Double getSpread()
    {
        return (bid == null || ask == null) ? null : ask - bid;
    }
}
