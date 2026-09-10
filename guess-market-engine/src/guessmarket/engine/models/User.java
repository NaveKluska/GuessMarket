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

    public boolean isBlocked() {
        return blocked;
    }

    public void setBlocked(boolean blocked) {
        this.blocked = blocked;
    }
}
