package guessmarket.dto.orderbook;

import guessmarket.dto.ChartPointDTO;
import java.util.List;

public class OptionBookDTO
{
    private final String optionName;
    private final Double last;
    private final Double bid;
    private final Double ask;
    private final Double mid;
    private final Double spread;
    private final List<OrderDTO> bids;
    private final List<OrderDTO> asks;
    /** How this option's traded price has moved over time, oldest first. */
    private final List<ChartPointDTO> priceHistory;

    public OptionBookDTO(final String optionName, final Double last, final Double bid, final Double ask, final Double mid, final Double spread, final List<OrderDTO> bids, final List<OrderDTO> asks, final List<ChartPointDTO> priceHistory)
    {
        this.optionName = optionName;
        this.last = last;
        this.bid = bid;
        this.ask = ask;
        this.mid = mid;
        this.spread = spread;
        this.bids = bids;
        this.asks = asks;
        this.priceHistory = priceHistory;
    }

    public String getOptionName()
    {
        return optionName;
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
        return mid;
    }

    public Double getSpread()
    {
        return spread;
    }

    public List<OrderDTO> getBids()
    {
        return bids;
    }

    public List<OrderDTO> getAsks()
    {
        return asks;
    }

    public List<ChartPointDTO> getPriceHistory()
    {
        return priceHistory;
    }
}
