package guessmarket.dto;

import java.util.List;

public class EventSummaryDTO
{
    private final int id;
    private final String name;
    private final String description;
    private final int commission;
    private final String commissionType;
    private final List<String> options;
    private final boolean activeStatus;

    public EventSummaryDTO(final int id, final String name, final String description, final int commission, final String commissionType, final List<String> options, final boolean activeStatus)
    {
        this.id = id;
        this.name = name;
        this.description = description;
        this.commission = commission;
        this.commissionType = commissionType;
        this.options = options;
        this.activeStatus = activeStatus;
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

    public String getCommissionType()
    {
        return commissionType;
    }

    public String getDescription()
    {
        return description;
    }

    public List<String> getOptions()
    {
        return options;
    }
}
