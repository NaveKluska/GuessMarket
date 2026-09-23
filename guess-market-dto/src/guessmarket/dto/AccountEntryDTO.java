package guessmarket.dto;

import java.time.LocalDateTime;

public class AccountEntryDTO
{
    private final LocalDateTime at;
    private final String description;
    private final double amount;
    private final double balanceAfter;

    public AccountEntryDTO(final LocalDateTime at, final String description, final double amount, final double balanceAfter)
    {
        this.at = at;
        this.description = description;
        this.amount = amount;
        this.balanceAfter = balanceAfter;
    }

    public LocalDateTime getAt()
    {
        return at;
    }

    public String getDescription()
    {
        return description;
    }

    public double getAmount()
    {
        return amount;
    }

    public double getBalanceAfter()
    {
        return balanceAfter;
    }
}
