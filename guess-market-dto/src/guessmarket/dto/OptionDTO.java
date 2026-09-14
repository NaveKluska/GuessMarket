package guessmarket.dto;

import java.util.List;

public class OptionDTO
{
    private final String name;
    private final int sharesBought;
    private final double currentProbability;
    /** How this option's price has moved across the event's trading, oldest first. */
    private final List<ChartPointDTO> priceHistory;

    public OptionDTO(final String name, final int sharesBought, final double currentProbability, final List<ChartPointDTO> priceHistory)
    {
        this.name = name;
        this.sharesBought = sharesBought;
        this.currentProbability = currentProbability;
        this.priceHistory = priceHistory;
    }

    public String getName()
    {
        return name;
    }

    public int getSharesBought()
    {
        return sharesBought;
    }

    public double getCurrentProbability()
    {
        return currentProbability;
    }

    public List<ChartPointDTO> getPriceHistory()
    {
        return priceHistory;
    }
}
