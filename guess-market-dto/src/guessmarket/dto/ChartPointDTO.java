package guessmarket.dto;

import java.time.LocalDateTime;

/**
 * A single plotted point: one value at one moment. Used for both the price-over-time charts on an
 * event and the balance-over-time chart on a user, since the two need exactly the same shape.
 */
public class ChartPointDTO
{
    private final LocalDateTime at;
    private final double value;

    public ChartPointDTO(final LocalDateTime at, final double value)
    {
        this.at = at;
        this.value = value;
    }

    public LocalDateTime getAt()
    {
        return at;
    }

    public double getValue()
    {
        return value;
    }
}
