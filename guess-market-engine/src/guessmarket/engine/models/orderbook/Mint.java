package guessmarket.engine.models.orderbook;

/**
 * Newly created shares from two BUY orders on opposite options whose combined price exceeded
 * the event's base value (d). Unlike a Fill, no money passes between the two traders here -
 * both sides pay into the event's account and each receives brand-new shares of their own option.
 *
 * IMPORTANT: the RESTING order (B) pays exactly its own quoted price. The INCOMING order (A) does
 * NOT pay its own quoted price - it pays (d - priceB), the complement needed to reach exactly d
 * total. This is price IMPROVEMENT for the incoming side (it's always <= their own quoted price,
 * since the mint eligibility check already confirmed priceA_quoted + priceB > d). Confirmed against
 * the teacher's reference simulation: Alice bids $0.62, Carol rests at $0.42, Alice actually pays
 * $0.58 (not $0.62) so the pair totals exactly $1.00 - nothing extra is collected by the event.
 */
public class Mint
{
    private final int optionIndexA;
    private final Order orderA;
    private final double priceA;
    private final int optionIndexB;
    private final Order orderB;
    private final double priceB;
    private final int quantity;

    public Mint(final int optionIndexA, final Order orderA, final double priceA, final int optionIndexB, final Order orderB, final double priceB, final int quantity)
    {
        this.optionIndexA = optionIndexA;
        this.orderA = orderA;
        this.priceA = priceA;
        this.optionIndexB = optionIndexB;
        this.orderB = orderB;
        this.priceB = priceB;
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

    /** What order A (the incoming order) actually pays per share - NOT necessarily orderA.getPrice(). */
    public double getPriceA()
    {
        return priceA;
    }

    public int getOptionIndexB()
    {
        return optionIndexB;
    }

    public Order getOrderB()
    {
        return orderB;
    }

    /** What order B (the resting order) actually pays per share - always exactly orderB.getPrice(). */
    public double getPriceB()
    {
        return priceB;
    }

    public int getQuantity()
    {
        return quantity;
    }
}
