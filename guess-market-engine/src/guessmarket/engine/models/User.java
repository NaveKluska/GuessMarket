package guessmarket.engine.models;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class User {
    private final String name;
    private double initialCash;
    private boolean blocked;

    private final List<String> marketMakerForEvents;

    /**
     * Every value this user's balance has held, oldest first, for plotting their account over time.
     * Recorded inside increaseBalance/decreaseBalance rather than at the call sites: those two
     * methods are the only way a balance can ever move, so anything that touches the money is
     * captured automatically and no future caller can forget to record itself.
     */
    private final List<BalancePoint> balanceHistory = new ArrayList<>();

    public User(String name, double initialCash, List<String> marketMakerForEvents) {
        this.name = name;
        this.initialCash = initialCash;
        this.blocked = false;
        this.marketMakerForEvents = marketMakerForEvents;
        // The opening balance is a real data point - without it a chart would start at the user's
        // first trade rather than at what they began with.
        this.balanceHistory.add(new BalancePoint(LocalDateTime.now(), initialCash));
    }

    /** This user's balance over time, oldest first. Unmodifiable - the history is the engine's to write. */
    public List<BalancePoint> getBalanceHistory() {
        return Collections.unmodifiableList(balanceHistory);
    }

    private void recordBalance() {
        balanceHistory.add(new BalancePoint(LocalDateTime.now(), this.initialCash));
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
        recordBalance();
    }

    public void decreaseBalance(double amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("Amount to decrease balance by cannot be negative.");
        }
        this.initialCash -= amount;
        recordBalance();
    }

    public List<String> getMarketMakerForEvents() {
        return marketMakerForEvents;
    }

    /**
     * Records that this user is now the Market Maker of another event - used when a user creates
     * an event at runtime, so their role there matches one assigned in the loaded file.
     */
    public void addMarketMakerEvent(final String eventName) {
        if (!marketMakerForEvents.contains(eventName)) {
            marketMakerForEvents.add(eventName);
        }
    }

    public boolean isBlocked() {
        return blocked;
    }

    public void setBlocked(boolean blocked) {
        this.blocked = blocked;
    }
}
