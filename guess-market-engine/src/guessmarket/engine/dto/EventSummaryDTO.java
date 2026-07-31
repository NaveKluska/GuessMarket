package guessmarket.engine.dto;

import guessmarket.engine.models.Event;
import guessmarket.engine.models.CommissionType;
import guessmarket.engine.models.Option;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class EventSummaryDTO
{
    private final int id;
    private final String name;
    private final boolean activeStatus;
    private final int commission;
    private final CommissionType commissionType;
    private final String description;
    private final List<String> options;

    public EventSummaryDTO(final Event event)
    {
        this.id = event.getId();
        this.name = event.getName();
        this.activeStatus = event.getActiveStatus();
        this.commission = event.getCommission();
        this.commissionType = event.getCommissionType();
        this.description = event.getDescription();
        this.options = extractOptionNames(event.getOptions());
    }

    private static List<String> extractOptionNames(final List<Option> options) {
        if (options == null) {
            return Collections.emptyList();
        }
        final List<String> optionNames = new ArrayList<>();
        for (final Option option : options) {
            optionNames.add(option.getName());
        }
        return Collections.unmodifiableList(optionNames);
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

    public String getDescription()
    {
        return description;
    }

    public List<String> getOptions()
    {
        return options;
    }
}
