import java.util.List;

public class EventDetailsDTO
{
    private final int id;
    private final String name;
    private final String description;
    private final int commission;
    private final CommissionType commissionType;
    private final List<Option> options;
    private final boolean activeStatus;
    private final int b;
    private final double accountBalance;
    private final double totalCommissionCollected;
    private final List<Transaction> transactions;

    public EventDetailsDTO(final Event event)
    {
        this.id = event.getId();
        this.name = event.getName();
        this.description = event.getDescription();
        this.commission = event.getCommission();
        this.commissionType = event.getCommissionType();
        this.options = event.getOptions();
        this.activeStatus = event.getActiveStatus();
        this.b = event.getB();
        this.accountBalance = event.getAccountBalance();
        this.totalCommissionCollected = event.getTotalCommissionCollected();
        this.transactions = event.getTransactions();
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
}