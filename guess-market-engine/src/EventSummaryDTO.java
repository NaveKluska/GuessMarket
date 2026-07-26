public class EventSummaryDTO
{
    private final int id;
    private final String name;
    private final boolean activeStatus;
    private final int commission;
    private final CommissionType commissionType;

    public EventSummaryDTO(final Event event)
    {
        this.id = event.getId();
        this.name = event.getName();
        this.activeStatus = event.getActiveStatus();
        this.commission = event.getCommission();
        this.commissionType = event.getCommissionType();
    }

    public int getId()
    {
        return id;
    }

    public String getName()
    {
        return name;
    }

    public boolean getActiveStatus()
    {
        return activeStatus;
    }

    public int getCommission()
    {
        return commission;
    }

    public CommissionType getCommissionType()
    {
        return commissionType;
    }
}
