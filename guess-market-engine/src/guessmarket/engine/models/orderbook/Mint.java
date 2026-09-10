package guessmarket.engine.models.orderbook;

/**
 * Newly created shares from two BUY orders on opposite options whose combined price exceeded
 * the event's base value (d). Unlike a Fill, no money passes between the two traders here -
 * both sides pay their OWN quoted price into the event's account and each receives brand-new
 * shares of their own option.
 */
public class Mint
{
    private final int optionIndexA;
    private final Order orderA;
    private final int optionIndexB;
    private final Order orderB;
    private final int quantity;

    public Mint(final int optionIndexA, final Order orderA, final int optionIndexB, final Order orderB, final int quantity)
    {
        this.optionIndexA = optionIndexA;
        this.orderA = orderA;
        this.optionIndexB = optionIndexB;
        this.orderB = orderB;
        this.quantity = quantity;
    }

    public int getOptionIndexA()
    {
        return optionIndexA;
    }

    public Order getOrderA()
    {
        return orderA;
    }

    public int getOptionIndexB()
    {
        return optionIndexB;
    }

    public Order getOrderB()
    {
        return orderB;
    }

    public int getQuantity()
    {
        return quantity;
    }
}
