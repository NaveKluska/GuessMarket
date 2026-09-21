package guessmarket.dto;

import java.util.List;

public class UserSummaryDTO
{
    private final String name;
    private final double balance;
    private final boolean blocked;
    private final boolean marketMaker;
    private final List<String> relevantEventNames;
    /** How this user's account balance has moved over time, oldest first. */
    private final List<ChartPointDTO> balanceHistory;

    public UserSummaryDTO(final String name, final double balance, final boolean blocked, final boolean marketMaker, final List<String> relevantEventNames, final List<ChartPointDTO> balanceHistory)
    {
        this.name = name;
        this.balance = balance;
        this.blocked = blocked;
        this.marketMaker = marketMaker;
        this.relevantEventNames = relevantEventNames;
        this.balanceHistory = balanceHistory;
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
    public List<String> getRelevantEventNames()
    {
        return relevantEventNames;
    }

    public List<ChartPointDTO> getBalanceHistory()
    {
        return balanceHistory;
    }
}
