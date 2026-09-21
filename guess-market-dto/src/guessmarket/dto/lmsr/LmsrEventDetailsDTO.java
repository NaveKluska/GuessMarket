package guessmarket.dto.lmsr;

import guessmarket.dto.EventDetailsDTO;
import guessmarket.dto.OptionDTO;
import guessmarket.dto.TransactionDTO;

import java.util.List;
import java.util.Map;

public class LmsrEventDetailsDTO implements EventDetailsDTO
{
    private final String name;
    private final String description;
    private final int commission;
    private final String commissionType;
    private final String status;
    private final double accountBalance;
    private final double totalCommissionCollected;
    private final List<OptionDTO> options;
    private final List<TransactionDTO> transactions;
    private final String winningOptionName;
    private final Map<String, Double> commissionPaidByUser;
    private final String marketMakerName;

    public LmsrEventDetailsDTO(final String name, final String description, final int commission, final String commissionType, final String status, final double accountBalance, final double totalCommissionCollected, final List<OptionDTO> options, final List<TransactionDTO> transactions, final String winningOptionName, final Map<String, Double> commissionPaidByUser, final String marketMakerName)
    {
        this.name = name;
        this.description = description;
        this.commission = commission;
        this.commissionType = commissionType;
        this.status = status;
        this.accountBalance = accountBalance;
        this.totalCommissionCollected = totalCommissionCollected;
        this.options = options;
        this.transactions = transactions;
        this.winningOptionName = winningOptionName;
        this.commissionPaidByUser = commissionPaidByUser;
        this.marketMakerName = marketMakerName;
    }

    @Override
    public String getMarketMakerName()
    {
        return marketMakerName;
    }

    @Override
    public String getName()
    {
        return name;
    }

    @Override
    public String getDescription()
    {
        return description;
    }

    @Override
    public int getCommission()
    {
        return commission;
    }

    @Override
    public String getCommissionType()
    {
        return commissionType;
    }

    @Override
    public String getStatus()
    {
        return status;
    }

    @Override
    public String getType()
    {
        return "LMSR";
    }

    @Override
    public double getAccountBalance()
    {
        return accountBalance;
    }

    @Override
    public String getWinningOptionName()
    {
        return winningOptionName;
    }

    public double getTotalCommissionCollected()
    {
        return totalCommissionCollected;
    }

    public List<OptionDTO> getOptions()
    {
        return options;
    }

    public List<TransactionDTO> getTransactions()
    {
        return transactions;
    }

    /** Total commission a given user has paid on this event so far (0 if none). */
    public double getCommissionPaidBy(final String userName)
    {
        return commissionPaidByUser.getOrDefault(userName, 0.0);
    }
}
