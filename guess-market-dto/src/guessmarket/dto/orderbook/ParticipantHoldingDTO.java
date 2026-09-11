package guessmarket.dto.orderbook;

import java.util.List;

/** One participant's current holdings in an Order Book event - required for the "participations" view. */
public class ParticipantHoldingDTO
{
    private final String userName;
    private final List<Integer> holdingsByOption;
    private final double estimatedValue;

    public ParticipantHoldingDTO(final String userName, final List<Integer> holdingsByOption, final double estimatedValue)
    {
        this.userName = userName;
        this.holdingsByOption = holdingsByOption;
        this.estimatedValue = estimatedValue;
    }

    public String getUserName()
    {
        return userName;
    }

    public List<Integer> getHoldingsByOption()
    {
        return holdingsByOption;
    }

    public double getEstimatedValue()
    {
        return estimatedValue;
    }
}
