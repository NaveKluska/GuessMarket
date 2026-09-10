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
    private boolean isStarted;
    private boolean activeStatus;
    protected double accountBalance;
    private double totalCommissionCollected;
    private final List<Transaction> transactions;
    private String winningOptionName;
    // priceHistory.get(optionIndex) = list of recorded probabilities after each trade
    private final List<List<Double>> priceHistory;

    public Event(int id, String name, String description, int commission, CommissionType commissionType, List<Option> options)
    {
        this.id = id;
        this.name = name;
        this.description = description;
        this.commission = commission;
        this.commissionType = commissionType;
        this.options = options;
        this.isStarted = false;
        this.activeStatus = true;
        this.accountBalance = 0.0;
        this.totalCommissionCollected = 0.0;
        this.transactions = new ArrayList<>();
        this.priceHistory = new ArrayList<>();
        for (int i = 0; i < options.size(); i++) {
            priceHistory.add(new ArrayList<>());
        }
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

    public boolean isStarted()
    {
        return isStarted;
    }

    public void setStarted(boolean started)
    {
        this.isStarted = started;
    }

    public boolean getActiveStatus()
    {
        return activeStatus && isStarted;
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
        
        // Record price snapshot for chart after each trade
        recordPriceSnapshot();
    }
    
    public void recordPriceSnapshot() {
        for (int i = 0; i < options.size(); i++) {
            if (priceHistory.size() > i) {
                try {
                    priceHistory.get(i).add(getOptionProbability(i));
                } catch (UnsupportedOperationException e) {
                    // Order Book events may not have a price yet
                }
            }
        }
    }
    
    public List<List<Double>> getPriceHistory() {
        return priceHistory;
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

    public void injectFunds(final double amount) {
        this.accountBalance += amount;
    }
}
