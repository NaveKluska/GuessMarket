package guessmarket.client.net;

/**
 * Client-side copy of the engine's own CommissionType - see OrderSide for why the client keeps
 * its own copy instead of depending on the engine module.
 */
public enum CommissionType
{
    ON_PURCHASE,
    ON_CLOSE
}
