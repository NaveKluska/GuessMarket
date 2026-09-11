package guessmarket.dto.orderbook;

import guessmarket.dto.EventDetailsDTO;

import java.util.List;

public class OrderBookEventDetailsDTO implements EventDetailsDTO
{
    private final int id;
    private final String name;
    private final String description;
    private final int commission;
    private final String commissionType;
    private final String status;
    private final double accountBalance;
    private final int baseValue;
    private final boolean allowMint;
    private final List<OptionBookDTO> optionBooks;
    private final List<ParticipantHoldingDTO> participants;
    private final String winningOptionName;

    public OrderBookEventDetailsDTO(final int id, final String name, final String description, final int commission, final String commissionType, final String status, final double accountBalance, final int baseValue, final boolean allowMint, final List<OptionBookDTO> optionBooks, final List<ParticipantHoldingDTO> participants, final String winningOptionName)
    {
        this.id = id;
        this.name = name;
        this.description = description;
        this.commission = commission;
        this.commissionType = commissionType;
        this.status = status;
        this.accountBalance = accountBalance;
        this.baseValue = baseValue;
        this.allowMint = allowMint;
        this.optionBooks = optionBooks;
        this.participants = participants;
        this.winningOptionName = winningOptionName;
    }

    @Override
    public int getId()
    {
        return id;
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
        return "ORDER_BOOK";
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

    public int getBaseValue()
    {
        return baseValue;
    }

    public boolean isAllowMint()
    {
        return allowMint;
    }

    public List<OptionBookDTO> getOptionBooks()
    {
        return optionBooks;
    }

    public List<ParticipantHoldingDTO> getParticipants()
    {
        return participants;
    }
}
