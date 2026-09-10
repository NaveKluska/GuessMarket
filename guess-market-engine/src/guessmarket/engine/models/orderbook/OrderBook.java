package guessmarket.engine.models.orderbook;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * The resting buy/sell orders for a single option. Matching only - no mint, no money movement,
 * no knowledge of events or users beyond the name attached to each order.
 */
public class OrderBook
{
    private final List<Order> buyOrders = new ArrayList<>();
    private final List<Order> sellOrders = new ArrayList<>();
    private long nextSequence = 0;

    /**
     * Submits a new order: matches it against the opposite side, then rests whatever's left.
     * Equivalent to calling createOrder + match + rest yourself; use those three directly
     * (as OrderBookMarket does) when something needs to happen between matching and resting,
     * e.g. attempting a mint first.
     *
     * @return the list of fills produced by this submission, in the order they occurred.
     */
    public List<Fill> submit(final String userName, final OrderSide side, final double price, final int quantity)
    {
        final Order incoming = createOrder(userName, side, price, quantity);
        final List<Fill> fills = match(incoming);
        rest(incoming);
        return fills;
    }

    public Order createOrder(final String userName, final OrderSide side, final double price, final int quantity)
    {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Order quantity must be positive.");
        }
        return new Order(userName, side, price, quantity, nextSequence++);
    }

    /**
     * Matches the given order against resting orders on the opposite side (best price first,
     * then earliest first among equal prices), reducing both sides as it goes. Does NOT rest
     * whatever's left unfilled - call rest() afterward if that's still wanted.
     */
    public List<Fill> match(final Order incoming)
    {
        final OrderSide side = incoming.getSide();
        final List<Order> oppositeBook = (side == OrderSide.BUY) ? sellOrders : buyOrders;
        oppositeBook.sort(priorityComparator(side == OrderSide.BUY ? OrderSide.SELL : OrderSide.BUY));

        final List<Fill> fills = new ArrayList<>();
        final List<Order> fullyFilled = new ArrayList<>();

        for (final Order resting : oppositeBook) {
            if (incoming.isFullyFilled()) {
                break;
            }
            if (!priceCrosses(incoming, resting)) {
                break;
            }

            final int fillQuantity = Math.min(incoming.getQuantity(), resting.getQuantity());
            // TRADE PRICE ASSUMPTION - CONFIRM WITH PROFESSOR:
            // We always execute at the RESTING order's price, whether resting is a buy or a sell.
            // Rationale: the resting order already locked in its price; the incoming order's price
            // only decides whether a trade happens at all (see priceCrosses), never what price it
            // happens at. This is the standard "maker's price" convention, but the PDF doesn't say
            // this explicitly - worth double-checking this assumption is what's expected.
            fills.add(new Fill(resting, incoming, fillQuantity, resting.getPrice()));
            incoming.reduceQuantity(fillQuantity);
            resting.reduceQuantity(fillQuantity);

            if (resting.isFullyFilled()) {
                fullyFilled.add(resting);
            }
        }
        oppositeBook.removeAll(fullyFilled);

        return fills;
    }

    /** Adds the order to the resting book on its own side, unless it's already fully filled. */
    public void rest(final Order order)
    {
        if (order.isFullyFilled()) {
            return;
        }
        (order.getSide() == OrderSide.BUY ? buyOrders : sellOrders).add(order);
    }

    /** This book's resting BUY orders, best price first (then earliest first) - a snapshot copy, not the live list. */
    public List<Order> bestBuyOrdersFirst()
    {
        final List<Order> sorted = new ArrayList<>(buyOrders);
        sorted.sort(priorityComparator(OrderSide.BUY));
        return sorted;
    }

    public void removeBuyOrder(final Order order)
    {
        buyOrders.remove(order);
    }

    private boolean priceCrosses(final Order incoming, final Order resting)
    {
        if (incoming.getSide() == OrderSide.BUY) {
            return incoming.getPrice() >= resting.getPrice();
        }
        return incoming.getPrice() <= resting.getPrice();
    }

    private Comparator<Order> priorityComparator(final OrderSide restingSide)
    {
        final Comparator<Order> byPrice = (restingSide == OrderSide.BUY)
            ? Comparator.comparingDouble(Order::getPrice).reversed()
            : Comparator.comparingDouble(Order::getPrice);
        return byPrice.thenComparingLong(Order::getSequence);
    }

    public List<Order> getBuyOrders()
    {
        return buyOrders;
    }

    public List<Order> getSellOrders()
    {
        return sellOrders;
    }
}
