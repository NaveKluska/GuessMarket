package guessmarket.engine.models.orderbook;

/**
 * The result of one resting order being matched against an incoming order, at the resting order's price.
 */
public class Fill
{
    private final Order restingOrder;
    private final Order incomingOrder;
    private final int quantity;
    private final double price;

    public Fill(final Order restingOrder, final Order incomingOrder, final int quantity, final double price)
    {
        this.restingOrder = restingOrder;
        this.incomingOrder = incomingOrder;
        this.quantity = quantity;
        this.price = price;
    }

    public Order getRestingOrder()
    {
        return restingOrder;
    }

    public Order getIncomingOrder()
    {
        return incomingOrder;
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
