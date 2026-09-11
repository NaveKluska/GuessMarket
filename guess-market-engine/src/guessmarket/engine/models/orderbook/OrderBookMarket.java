package guessmarket.engine.models.orderbook;

import java.util.ArrayList;
import java.util.List;

/**
 * Coordinates one option's OrderBook against the others for a single Order Book event.
 * An incoming order tries a normal same-option match first; then, if this event allows mint
 * and the order is a BUY, it tries to mint new shares against resting BUY orders on every
 * OTHER option, whenever the two prices together exceed the base value d; whatever's still
 * unfilled after that rests in its own option's book.
 */
public class OrderBookMarket
{
    private final OrderBook[] booksByOptionIndex;
    private final int d;
    private final boolean allowMint;

    public OrderBookMarket(final int optionCount, final int d, final boolean allowMint)
    {
        this.booksByOptionIndex = new OrderBook[optionCount];
        for (int i = 0; i < optionCount; i++) {
            this.booksByOptionIndex[i] = new OrderBook();
        }
        this.d = d;
        this.allowMint = allowMint;
    }

    public OrderBook getBook(final int optionIndex)
    {
        return booksByOptionIndex[optionIndex];
    }

    public TradeOutcome submit(final String userName, final int optionIndex, final OrderSide side, final double price, final int quantity)
    {
        // PRICE RANGE ASSUMPTION - CONFIRM WITH PROFESSOR:
        // The PDF gives a formula only for the upper bound (d - 0.01); the teacher's reference
        // simulation rejected a price at d itself with "must be between $0.01 and $0.99" for d=1.
        // We treat 0.01 as a flat minimum granularity (not scaled by d), since no scaled formula
        // is given, and treat both endpoints as valid ("between X and Y" read as inclusive).
        final double minPrice = 0.01;
        final double maxPrice = d - 0.01;
        final double epsilon = 1e-9;
        if (price < minPrice - epsilon || price > maxPrice + epsilon) {
            throw new IllegalArgumentException("Order price must be between " + minPrice + " and " + maxPrice + " (got " + price + ").");
        }

        final OrderBook book = booksByOptionIndex[optionIndex];
        final Order incoming = book.createOrder(userName, side, price, quantity);

        final List<Fill> fills = new ArrayList<>(book.match(incoming));
        final List<Mint> mints = new ArrayList<>();

        if (allowMint && side == OrderSide.BUY) {
            for (int otherIndex = 0; otherIndex < booksByOptionIndex.length; otherIndex++) {
                if (otherIndex == optionIndex || incoming.isFullyFilled()) {
                    continue;
                }
                mints.addAll(mintAgainst(optionIndex, incoming, otherIndex, booksByOptionIndex[otherIndex]));
            }
        }

        book.rest(incoming);
        return new TradeOutcome(fills, mints);
    }

    private List<Mint> mintAgainst(final int incomingOptionIndex, final Order incoming, final int otherOptionIndex, final OrderBook otherBook)
    {
        final List<Mint> mints = new ArrayList<>();

        for (final Order counterpart : otherBook.bestBuyOrdersFirst()) {
            if (incoming.isFullyFilled()) {
                break;
            }
            // MINT THRESHOLD ASSUMPTION - CONFIRM WITH PROFESSOR:
            // The PDF says mint triggers when the two prices together are STRICTLY GREATER
            // than d, so we use "<=" here to stop (not "<"). An exact match (== d) is
            // arguably still mint-worthy (the event breaks even instead of profiting) but
            // that's not what's written, so we follow the letter of the spec.
            if (incoming.getPrice() + counterpart.getPrice() <= d) {
                break; // bestBuyOrdersFirst() is sorted best-first, so nothing later qualifies either.
            }

            final int mintQuantity = Math.min(incoming.getQuantity(), counterpart.getQuantity());
            // Resting order (counterpart) gets exactly its own quoted price. Incoming pays the
            // complement (d - counterpart's price), NOT its own quoted price - see Mint's javadoc.
            final double restingPrice = counterpart.getPrice();
            final double incomingPrice = d - restingPrice;
            mints.add(new Mint(incomingOptionIndex, incoming, incomingPrice, otherOptionIndex, counterpart, restingPrice, mintQuantity));
            incoming.reduceQuantity(mintQuantity);
            counterpart.reduceQuantity(mintQuantity);

            if (counterpart.isFullyFilled()) {
                otherBook.removeBuyOrder(counterpart);
            }
        }

        return mints;
    }
}
