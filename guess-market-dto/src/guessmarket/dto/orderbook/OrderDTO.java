package guessmarket.dto.orderbook;

public class OrderDTO
{
    private final String userName;
    private final int quantity;
    private final double price;

    public OrderDTO(final String userName, final int quantity, final double price)
    {
        this.userName = userName;
        this.quantity = quantity;
        this.price = price;
    }

    public String getUserName()
    {
        return userName;
    }

    public int getQuantity()
    {
        return quantity;
    }

    public double getPrice()
    {
        return price;
    }
}
