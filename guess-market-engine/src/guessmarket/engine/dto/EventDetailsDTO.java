package guessmarket.engine.dto;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import guessmarket.engine.models.Event;
import guessmarket.engine.models.CommissionType;
import guessmarket.engine.models.Option;
import guessmarket.engine.models.Transaction;

public class EventDetailsDTO
{
    private final int id;
    private final String name;
    private final String description;
    private final int commission;
    private final CommissionType commissionType;
    private final List<OptionDTO> options;
    private final boolean activeStatus;
    private final int b;
    private final double accountBalance;
    private final double totalCommissionCollected;
    private final List<TransactionDTO> transactions;

    public EventDetailsDTO(final Event event)
    {
        this.id = event.getId();
        this.name = event.getName();
        this.description = event.getDescription();
        this.commission = event.getCommission();
        this.commissionType = event.getCommissionType();
        this.activeStatus = event.getActiveStatus();
        this.b = event.getB();
        this.accountBalance = event.getAccountBalance();
        this.totalCommissionCollected = event.getTotalCommissionCollected();
        this.options = mapOptions(event.getOptions());
        this.transactions = mapTransactions(event.getTransactions());
    }

    private static List<OptionDTO> mapOptions(final List<Option> options) {
        if (options == null) {
            return Collections.emptyList();
        }
        final List<OptionDTO> optionDTOs = new ArrayList<>();
        for (final Option option : options) {
            optionDTOs.add(new OptionDTO(option));
        }
        return Collections.unmodifiableList(optionDTOs);
    }

    private static List<TransactionDTO> mapTransactions(final List<Transaction> transactions) {
        if (transactions == null) {
            return Collections.emptyList();
        }
        final List<TransactionDTO> transactionDTOs = new ArrayList<>();
        for (final Transaction transaction : transactions) {
            transactionDTOs.add(new TransactionDTO(transaction));
        }
        return Collections.unmodifiableList(transactionDTOs);
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

    public CommissionType getCommissionType()
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

    public int getB()
    {
        return b;
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
}