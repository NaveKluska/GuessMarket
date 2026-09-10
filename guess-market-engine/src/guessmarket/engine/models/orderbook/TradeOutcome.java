package guessmarket.engine.models.orderbook;

import java.util.List;

/** Everything that happened as a result of one order submission: ordinary matches and mints. */
public class TradeOutcome
{
    private final List<Fill> fills;
    private final List<Mint> mints;

    public TradeOutcome(final List<Fill> fills, final List<Mint> mints)
    {
        this.fills = fills;
        this.mints = mints;
    }

    public List<Fill> getFills()
    {
        return fills;
    }

    public List<Mint> getMints()
    {
        return mints;
    }
}
