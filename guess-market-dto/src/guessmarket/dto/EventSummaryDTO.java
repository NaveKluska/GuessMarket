package guessmarket.dto;

import java.util.List;

public class EventSummaryDTO
{
    private final int id;
    private final String name;
    private final boolean activeStatus;
    private final int commission;
    private final String commissionType;
    private final String description;
    private final List<String> options;

    public EventSummaryDTO(final int id, final String name, final boolean activeStatus, final int commission, final String commissionType, final String description, final List<String> options)
    {
        this.id = id;
        this.name = name;
        this.activeStatus = activeStatus;
        this.commission = commission;
        this.commissionType = commissionType;
        this.description = description;
        this.options = options;
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
