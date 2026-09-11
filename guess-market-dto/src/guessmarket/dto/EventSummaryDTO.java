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
    private final String status;
    private final String type;
    private final String marketMakerName;

    public EventSummaryDTO(final int id, final String name, final String description, final int commission, final String commissionType, final List<String> options, final String status, final String type, final String marketMakerName)
    {
        this.id = id;
        this.name = name;
        this.description = description;
        this.commission = commission;
        this.commissionType = commissionType;
        this.options = options;
        this.status = status;
        this.type = type;
        this.marketMakerName = marketMakerName;
    }

    public String getMarketMakerName()
    {
        return marketMakerName;
    }

    public int getId()
    {
        return id;
    }

    public String getName()
    {
        return name;
    }

    public String getStatus()
    {
        return status;
    }

    public String getType()
    {
        return type;
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
