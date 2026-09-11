package guessmarket.engine.models;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public abstract class Event implements Serializable
{
    private static final long serialVersionUID = 1L;
    private final int id;
    private String name;
    private String description;
    private int commission;
    private CommissionType commissionType;
    private final List<Option> options;
    private EventStatus status;
    protected double accountBalance;
    private double totalCommissionCollected;
    private final List<Transaction> transactions;
    private String winningOptionName;
    private final Set<String> participants = new HashSet<>();

    public Event(int id, String name, String description, int commission, CommissionType commissionType, List<Option> options)
    {
        this.id = id;
        this.name = name;
        this.description = description;
        this.commission = commission;
        this.commissionType = commissionType;
        this.options = options;
        this.status = EventStatus.NOT_ACTIVE;
        this.accountBalance = 0.0;
        this.totalCommissionCollected = 0.0;
        this.transactions = new ArrayList<>();
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

    public List<Option> getOptions()
    {
        return options;
    }

    public EventStatus getStatus()
    {
        return status;
    }

    public boolean getActiveStatus()
    {
        return status == EventStatus.ACTIVE;
    }

    public void activate()
    {
        if (this.status != EventStatus.NOT_ACTIVE) {
            throw new IllegalStateException("Event can only be opened from NOT_ACTIVE status (current: " + status + ").");
        }
        this.status = EventStatus.ACTIVE;
    }

    public abstract double getOptionProbability(int optionIndex);
    
    public abstract double calculateCost(int optionIndex, int quantity);

    public double getAccountBalance()
    {
        return accountBalance;
    }

    public double getTotalCommissionCollected()
    {
        return totalCommissionCollected;
    }

    public List<Transaction> getTransactions()
    {
        return transactions;
    }

    public void executePurchase(final String memberName, final int optionIndex, final int quantity, final double cost, final double commission) {
        if (optionIndex < 0 || optionIndex >= options.size()) {
            throw new IllegalArgumentException("Invalid option index.");
        }
        final Option option = options.get(optionIndex);
        option.addShares(quantity);
        if (commission > 0) {
            this.totalCommissionCollected += commission;
        }

        final Transaction transaction = new Transaction(memberName, option.getName(), quantity, cost);
        this.transactions.add(transaction);
    }

    public void close(final int winningOptionIndex) {
        if (this.status != EventStatus.ACTIVE) {
            throw new IllegalStateException("Event can only be closed from ACTIVE status (current: " + status + ").");
        }
        if (winningOptionIndex < 0 || winningOptionIndex >= options.size()) {
            throw new IllegalArgumentException("Invalid option index.");
        }
        this.status = EventStatus.CLOSED;
        this.winningOptionName = options.get(winningOptionIndex).getName();
    }

    public String getWinningOptionName() {
        return winningOptionName;
    }

    public void addParticipant(final String userName) {
        participants.add(userName);
    }

    public Set<String> getParticipants() {
        return participants;
    }

    public void collectCommission(final double commission) {
        this.totalCommissionCollected += commission;
    }

    public void increaseAccountBalance(final double amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("Amount to increase account balance by cannot be negative.");
        }
        this.accountBalance += amount;
    }

    public void decreaseAccountBalance(final double amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("Amount to decrease account balance by cannot be negative.");
        }
        this.accountBalance -= amount;
    }
}
