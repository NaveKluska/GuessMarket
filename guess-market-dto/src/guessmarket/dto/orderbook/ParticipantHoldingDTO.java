package guessmarket.dto.orderbook;

import java.util.List;

/** One participant's current holdings in an Order Book event - required for the "participations" view. */
public class ParticipantHoldingDTO
{
    private final String userName;
    private final List<Integer> holdingsByOption;
    private final List<Double> paidByOption;
    private final double estimatedValue;
    private final double commissionPaid;
    private final Double profitOrLoss;

    public ParticipantHoldingDTO(final String userName, final List<Integer> holdingsByOption, final List<Double> paidByOption, final double estimatedValue, final double commissionPaid, final Double profitOrLoss)
    {
        this.userName = userName;
        this.holdingsByOption = holdingsByOption;
        this.paidByOption = paidByOption;
        this.estimatedValue = estimatedValue;
        this.commissionPaid = commissionPaid;
        this.profitOrLoss = profitOrLoss;
    }

    public String getUserName()
    {
        return userName;
    }

    public List<Integer> getHoldingsByOption()
    {
        return holdingsByOption;
    }

    /** Gross amount this user has spent buying into each option (by index), never netted against sells. */
    public List<Double> getPaidByOption()
    {
        return paidByOption;
    }

    public double getEstimatedValue()
    {
        return estimatedValue;
    }

    public double getCommissionPaid()
    {
        return commissionPaid;
    }

    /** Realized profit/loss from this participation, only meaningful once the event is CLOSED; null otherwise. */
    public Double getProfitOrLoss()
    {
        return profitOrLoss;
    }
}
