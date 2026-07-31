package guessmarket.engine.dto;

import guessmarket.engine.models.Option;

public class OptionDTO
{
    private final String name;
    private final int sharesBought;

    public OptionDTO(final Option option)
    {
        this.name = option.getName();
        this.sharesBought = option.getSharesBought();
    }

    public String getName()
    {
        return name;
    }

    public int getSharesBought()
    {
        return sharesBought;
    }
}
