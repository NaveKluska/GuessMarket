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
        // Confirmed with the professor: 0.01 is a flat hard-coded amount, not a percentage of d.
        final double minPrice = 0.01;
        final double maxPrice = d - 0.01;
        final double epsilon = 1e-9;
        if (price < minPrice - epsilon || price > maxPrice + epsilon) {
            throw new IllegalArgumentException("Order price must be between " + minPrice + " and " + maxPrice + " (got " + price + ").");
        }

        final OrderBook book = booksByOptionIndex[optionIndex];
        final Order incoming = book.createOrder(userName, side, price, quantity);

        final List<Fill> fills = new ArrayList<>(book.match(incoming));
        for (final Fill fill : fills) {
            book.recordLastPrice(fill.getPrice());
        }

        final List<Mint> mints = new ArrayList<>();
        if (allowMint && side == OrderSide.BUY) {
            for (int otherIndex = 0; otherIndex < booksByOptionIndex.length; otherIndex++) {
                if (otherIndex == optionIndex || incoming.isFullyFilled()) {
                    continue;
                }
                final List<Mint> newMints = mintAgainst(optionIndex, incoming, otherIndex, booksByOptionIndex[otherIndex]);
                for (final Mint mint : newMints) {
                    booksByOptionIndex[mint.getOptionIndexA()].recordLastPrice(mint.getPriceA());
                    booksByOptionIndex[mint.getOptionIndexB()].recordLastPrice(mint.getPriceB());
                }
                mints.addAll(newMints);
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
            // Confirmed with the professor: mint triggers on >= d, including exact equality.
            if (incoming.getPrice() + counterpart.getPrice() < d) {
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
