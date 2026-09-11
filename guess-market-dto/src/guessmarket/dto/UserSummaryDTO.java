package guessmarket.dto;

import java.util.List;

public class UserSummaryDTO
{
    private final String name;
    private final double balance;
    private final boolean blocked;
    private final boolean marketMaker;
    private final List<Integer> activeEventIds;

    public UserSummaryDTO(final String name, final double balance, final boolean blocked, final boolean marketMaker, final List<Integer> activeEventIds)
    {
        this.name = name;
        this.balance = balance;
        this.blocked = blocked;
        this.marketMaker = marketMaker;
        this.activeEventIds = activeEventIds;
    }

    public String getName()
    {
        return name;
    }

    public double getBalance()
    {
        return balance;
    }

    public boolean isBlocked()
    {
        return blocked;
    }

    public boolean isMarketMaker()
    {
        return marketMaker;
    }

    public List<Integer> getActiveEventIds()
    {
        return activeEventIds;
    }
}
