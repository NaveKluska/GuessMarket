package guessmarket.engine.models;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * One row in a user's account history: a single balance change, labeled with why it happened.
 * Replaces the old balance-only snapshot - a number alone can't answer "why did my balance
 * change," a description can.
 *
 * @param at            when this change happened
 * @param description   what caused it (e.g. "Bought 5 shares of Yes in \"Will it rain\"")
 * @param amount        the change itself - positive for a credit, negative for a debit
 * @param balanceAfter  the balance immediately after this change
 */
public record AccountEntry(LocalDateTime at, String description, double amount, double balanceAfter) implements Serializable {
}
