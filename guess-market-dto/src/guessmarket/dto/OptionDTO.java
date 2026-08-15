package guessmarket.dto;

public class OptionDTO
{
    private final String name;
    private final int sharesBought;
    private final double currentProbability;

    public OptionDTO(final String name, final int sharesBought, final double currentProbability)
    {
        this.name = name;
        this.sharesBought = sharesBought;
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
