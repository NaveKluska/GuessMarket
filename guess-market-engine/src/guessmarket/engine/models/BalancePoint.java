package guessmarket.engine.models;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * One reading of a user's account balance at a moment in time, used to plot how their money has
 * moved over the course of their trading.
 *
 * @param at      when the balance reached this value
 * @param balance the balance at that moment
 */
public record BalancePoint(LocalDateTime at, double balance) implements Serializable {
}
