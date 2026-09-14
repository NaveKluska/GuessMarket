package guessmarket.engine.models.orderbook;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * One traded price for an option at a moment in time, used to plot how that option's price has
 * moved as trading progressed.
 *
 * @param at    when the trade happened
 * @param price the price it traded at
 */
public record PricePoint(LocalDateTime at, double price) implements Serializable {
}
