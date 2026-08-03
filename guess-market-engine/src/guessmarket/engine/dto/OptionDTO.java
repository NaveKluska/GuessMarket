package guessmarket.engine.dto;

import guessmarket.engine.models.Option;

public class OptionDTO
{
    private final String name;
    private final int sharesBought;
    private final double currentProbability;

    public OptionDTO(final Option option, final double currentProbability)
    {
        this.name = option.getName();
        this.sharesBought = option.getSharesBought();
        this.currentProbability = currentProbability;
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
}
