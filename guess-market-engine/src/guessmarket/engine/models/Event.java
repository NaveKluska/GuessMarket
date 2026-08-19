package guessmarket.engine.models;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public abstract class Event implements Serializable
{
    private static final long serialVersionUID = 1L;
    private final int id;
    private String name;
    private String description;
    private int commission;
    private CommissionType commissionType;
    private final List<Option> options;
    private boolean activeStatus;
    protected double accountBalance;
    private double totalCommissionCollected;
    private final List<Transaction> transactions;
    private String winningOptionName;

    public Event(int id, String name, String description, int commission, CommissionType commissionType, List<Option> options)
    {
        this.id = id;
        this.name = name;
        this.description = description;
        this.commission = commission;
        this.commissionType = commissionType;
        this.options = options;
        this.activeStatus = true;
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

    public boolean getActiveStatus()
    {
        return activeStatus;
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
        this.accountBalance += (cost + commission);
        this.totalCommissionCollected += commission;
        
        final Transaction transaction = new Transaction(memberName, option.getName(), quantity, cost);
        this.transactions.add(transaction);
    }

    public void deactivateEvent(final int winningOptionIndex) {
        if (winningOptionIndex < 0 || winningOptionIndex >= options.size()) {
            throw new IllegalArgumentException("Invalid option index.");
        }
        this.activeStatus = false;
        this.winningOptionName = options.get(winningOptionIndex).getName();
    }

    // TODO: For better encapsulation, this method should throw an IllegalStateException if activeStatus is true.
    // Implementing this requires updating MarketEngineImpl.mapToDetailsDTO to check getActiveStatus() 
    // before calling this method, otherwise the engine will crash when mapping active events.
    public String getWinningOptionName() {
        return winningOptionName;
    }

    public void collectCommission(final double commission) {
        this.totalCommissionCollected += commission;
    }

    public void deductFromBalance(final double amount) {
        this.accountBalance -= amount;
    }
}
