package guessmarket.client.net;

/**
 * Client-side copy of the engine's own OrderSide. The client never depends on the engine module
 * (it only ever talks to the server over HTTP), so it keeps its own tiny copy of the handful of
 * enums it needs to build requests - the constant names match exactly, since they travel over
 * the wire as plain strings the server parses back with OrderSide.valueOf(...).
 */
public enum OrderSide
{
    BUY,
    SELL
}
