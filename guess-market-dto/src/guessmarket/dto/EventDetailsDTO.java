package guessmarket.dto;

import java.util.List;

public class EventDetailsDTO
{
    private final int id;
    private final String name;
    private final String description;
    private final int commission;
    private final String commissionType;
    private final List<OptionDTO> options;
    private final boolean activeStatus;
    private final boolean isStarted;
    private final double accountBalance;
    private final double totalCommissionCollected;
    private final List<TransactionDTO> transactions;
    private final String winningOptionName;
    private final List<List<Double>> priceHistory;

    public EventDetailsDTO(final int id, final String name, final String description, final int commission, final String commissionType, final boolean activeStatus, final boolean isStarted, final double accountBalance, final double totalCommissionCollected, final List<OptionDTO> options, final List<TransactionDTO> transactions, final String winningOptionName, final List<List<Double>> priceHistory)
    {
        this.id = id;
        this.name = name;
        this.description = description;
        this.commission = commission;
        this.commissionType = commissionType;
        this.activeStatus = activeStatus;
        this.isStarted = isStarted;
        this.accountBalance = accountBalance;
        this.totalCommissionCollected = totalCommissionCollected;
        this.options = options;
        this.transactions = transactions;
        this.winningOptionName = winningOptionName;
        this.priceHistory = priceHistory;
    }

    public int getId()
    {
        return id;
    }

    public String getName()
    {
        return name;
    }

    public String getDescription()
    {
        return description;
    }

    public int getCommission()
    {
        return commission;
    }

    public String getCommissionType()
    {
        return commissionType;
    }

    public List<OptionDTO> getOptions()
    {
        return options;
    }

    public boolean getActiveStatus()
    {
        return activeStatus;
    }

    public boolean isStarted()
    {
        return isStarted;
    }

    public double getAccountBalance()
    {
        return accountBalance;
    }

    public double getTotalCommissionCollected()
    {
        return totalCommissionCollected;
    }

    public List<TransactionDTO> getTransactions()
    {
        return transactions;
    }

    public String getWinningOptionName()
    {
        return winningOptionName;
    }

    public List<List<Double>> getPriceHistory()
    {
        return priceHistory;
    }
}
