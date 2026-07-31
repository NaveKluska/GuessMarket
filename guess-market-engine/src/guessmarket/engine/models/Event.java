package guessmarket.engine.models;

import java.util.ArrayList;
import java.util.List;

public class Event
{
    private final int id;
    private String name;
    private String description;
    private int commission;
    private CommissionType commissionType;
    private final List<Option> options;
    private boolean activeStatus;
    private int b;
    private double accountBalance;
    private double totalCommissionCollected;
    private final List<Transaction> transactions;

    public Event(int id, String name, String description, int commission, CommissionType commissionType, List<Option> options, int b)
    {
        this.id = id;
        this.name = name;
        this.description = description;
        this.commission = commission;
        this.commissionType = commissionType;
        this.options = options;
        this.b = b;
        this.activeStatus = false;
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

    public List<Transaction> getTransactions()
    {
        return transactions;
    }

    public void executePurchase(final String memberName, final int optionIndex, final int quantity, final double cost, final double commission) {
        final Option option = options.get(optionIndex);
        option.addShares(quantity);
        this.accountBalance += cost;
        this.totalCommissionCollected += commission;
        
        final Transaction transaction = new Transaction(memberName, option.getName(), quantity, cost);
        this.transactions.add(transaction);
    }

    /**
     * Deactivates the event. We only allow turning it off.
     * If an event has happened and closed, we do not reopen it. 
     * If you want another event, you should create a new event.
     */
    public void deactivateEvent() {
        this.activeStatus = false;
    }

    public void collectCommission(final double commission) {
        this.totalCommissionCollected += commission;
    }
}
