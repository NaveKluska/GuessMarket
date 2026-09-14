package guessmarket.engine.models;

import java.util.List;

public class User {
    private final String name;
    private double initialCash;
    private boolean blocked;

    private final List<Integer> marketMakerForEvents;

    public User(String name, double initialCash, List<Integer> marketMakerForEvents) {
        this.name = name;
        this.initialCash = initialCash;
        this.blocked = false;
        this.marketMakerForEvents = marketMakerForEvents;
    }

    public String getName() {
        return name;
    }

    public double getBalance() {
        return initialCash;
    }

    public void increaseBalance(double amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("Amount to increase balance by cannot be negative.");
        }
        this.initialCash += amount;
        // Being blocked is a consequence of a negative balance, not a permanent status - once a
        // payout or other credit brings the balance back to zero or above, there is no longer any
        // reason to keep refusing this user's actions.
        if (this.initialCash >= 0) {
            this.blocked = false;
        }
    }

    public void decreaseBalance(double amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("Amount to decrease balance by cannot be negative.");
        }
        this.initialCash -= amount;
    }

    public List<Integer> getMarketMakerForEvents() {
        return marketMakerForEvents;
    }

    /**
     * Records that this user is now the Market Maker of another event - used when a user creates
     * an event at runtime, so their role there matches one assigned in the loaded file.
     */
    public void addMarketMakerEvent(final int eventId) {
        if (!marketMakerForEvents.contains(eventId)) {
            marketMakerForEvents.add(eventId);
        }
    }

    public boolean isBlocked() {
        return blocked;
    }

    public void setBlocked(boolean blocked) {
        this.blocked = blocked;
    }
}
