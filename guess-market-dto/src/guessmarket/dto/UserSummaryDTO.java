package guessmarket.dto;

import java.util.List;

public class UserSummaryDTO
{
    private final String name;
    private final double balance;
    private final boolean blocked;
    private final boolean marketMaker;
    private final List<Integer> relevantEventIds;

    public UserSummaryDTO(final String name, final double balance, final boolean blocked, final boolean marketMaker, final List<Integer> relevantEventIds)
    {
        this.name = name;
        this.balance = balance;
        this.blocked = blocked;
        this.marketMaker = marketMaker;
        this.relevantEventIds = relevantEventIds;
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

    /** Events this user owns (is MM for, regardless of status) or participates in - "participation / owner" per the spec. */
    public List<Integer> getRelevantEventIds()
    {
        return relevantEventIds;
    }
}
