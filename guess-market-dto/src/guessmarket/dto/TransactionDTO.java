package guessmarket.dto;

import java.time.LocalDateTime;

public class TransactionDTO 
{
    private final String userName;
    private final String optionName;
    private final int quantity;
    private final double pricePaid;
    private final LocalDateTime timestamp;

    public TransactionDTO(final String userName, final String optionName, final int quantity, final double pricePaid, final LocalDateTime timestamp)
    {
        this.userName = userName;
        this.optionName = optionName;
        this.quantity = quantity;
        this.pricePaid = pricePaid;
        this.timestamp = timestamp;
    }

    public String getUserName()
    {
        return userName;
    }

    public String getOptionName()
    {
        return optionName;
    }

    public int getQuantity()
    {
        return quantity;
    }

    public double getPricePaid()
    {
        return pricePaid;
    }

    public LocalDateTime getTimestamp()
    {
        return timestamp;
    }
}
