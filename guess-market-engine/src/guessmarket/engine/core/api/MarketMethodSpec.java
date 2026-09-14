package guessmarket.engine.core.api;

/**
 * The trading-method-specific settings needed to create a new event, as supplied by a caller.
 * <p>
 * Sealed so the engine can exhaustively handle every supported market type: an LMSR event needs
 * only its liquidity parameter b, whereas an Order Book event needs its mint policy, initial share
 * allocation and base value. Keeping these in one sealed type means {@code createEvent} takes a
 * single method argument instead of a long list of primitives where most are irrelevant to
 * whichever type is actually being created.
 */
public sealed interface MarketMethodSpec {

    /** LMSR settings. {@code b} is the liquidity parameter; the initial subsidy is b * ln(optionCount). */
    record Lmsr(int b) implements MarketMethodSpec {
    }

    /**
     * Order Book settings.
     *
     * @param allowMint whether two opposing buy orders whose prices together reach d may mint new shares
     * @param initial   how many shares of every option the Market Maker buys when opening the event
     * @param d         the base value a winning share pays out at close
     */
    record OrderBook(boolean allowMint, int initial, int d) implements MarketMethodSpec {
    }
}
