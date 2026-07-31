package guessmarket.engine.dto;

import java.time.LocalDateTime;
import guessmarket.engine.models.Transaction;

public class TransactionDTO 
{
    private final String userName;
    private final String optionName;
    private final int quantity;
    private final double pricePaid;
    private final LocalDateTime timestamp;

    public TransactionDTO(final Transaction transaction)
    {
        this.userName = transaction.getUserName();
        this.optionName = transaction.getOptionName();
        this.quantity = transaction.getQuantity();
        this.pricePaid = transaction.getPricePaid();
        this.timestamp = transaction.getTimestamp();
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
