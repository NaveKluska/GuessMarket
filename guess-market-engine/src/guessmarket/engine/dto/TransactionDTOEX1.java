package guessmarket.engine.dto;

import guessmarket.engine.models.Transaction;

public class TransactionDTOEX1 
{
    private final String optionName;
    private final int quantity;
    private final double pricePaid;

    public TransactionDTOEX1(final Transaction transaction)
    {
        this.optionName = transaction.getOptionName();
        this.quantity = transaction.getQuantity();
        this.pricePaid = transaction.getPricePaid();
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
}
