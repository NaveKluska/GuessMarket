package guessmarket.engine.models;

import java.io.Serializable;
import java.time.LocalDateTime;

public class Transaction implements Serializable
{
    private static final long serialVersionUID = 1L;
    private final String userName;
    private final String optionName;
    private final int quantity;
    private final double pricePaid;
    private final LocalDateTime timestamp;

    public Transaction(String userName, String optionName, int quantity, double pricePaid)
    {
        this.userName = userName;
        this.optionName = optionName;
        this.quantity = quantity;
        this.pricePaid = pricePaid;
        this.timestamp = LocalDateTime.now();
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
