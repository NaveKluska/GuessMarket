package guessmarket.engine.models;

import java.io.Serializable;

public class Option implements Serializable
{
    private static final long serialVersionUID = 1L;
    private final String name;
    private int sharesBought;

    public Option(final String name)
    {
        this.name = name;
        this.sharesBought = 0;
    }

    public String getName()
    {
        return name;
    }

    public int getSharesBought()
    {
        return sharesBought;
    }

    public void addShares(int amount)
    {
        if (amount < 0) {
            throw new IllegalArgumentException("Amount of shares to add cannot be negative.");
        }
        this.sharesBought += amount;
    }
}
