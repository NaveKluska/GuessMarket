// Superseded by AccountEntry - a balance snapshot alone lost the "why" (deposit, trade,
// commission...), so the whole account log switched to AccountEntry (which carries a description
// and the amount that moved, not just the resulting balance). Kept for reference rather than
// deleted; not compiled, not called from anywhere.
// package guessmarket.engine.models;
//
// import java.io.Serializable;
// import java.time.LocalDateTime;
//
// /**
//  * One reading of a user's account balance at a moment in time, used to plot how their money has
//  * moved over the course of their trading.
//  *
//  * @param at      when the balance reached this value
//  * @param balance the balance at that moment
//  */
// public record BalancePoint(LocalDateTime at, double balance) implements Serializable {
// }
