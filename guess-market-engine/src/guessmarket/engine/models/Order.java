package guessmarket.engine.models;

public class Order
{
    private final String userName;
    private final OrderSide side;
    private final double price;
    private final long sequence;
    private int quantity;

    public Order(final String userName, final OrderSide side, final double price, final int quantity, final long sequence)
    {
        this.userName = userName;
        this.side = side;
        this.price = price;
        this.quantity = quantity;
        this.sequence = sequence;
    }

    public String getUserName()
    {
        return userName;
    }

    public OrderSide getSide()
    {
        return side;
    }

    public double getPrice()
    {
        return price;
    }

    public long getSequence()
    {
        return sequence;
    }

    public int getQuantity()
    {
        return quantity;
    }

    public boolean isFullyFilled()
    {
        return quantity == 0;
    }

    public void reduceQuantity(final int amount)
    {
        if (amount < 0 || amount > quantity) {
            throw new IllegalArgumentException("Invalid fill amount: " + amount + " (order only has " + quantity + " remaining).");
        }
        this.quantity -= amount;
    }
}
