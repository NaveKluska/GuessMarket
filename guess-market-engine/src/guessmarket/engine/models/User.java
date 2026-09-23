package guessmarket.engine.models;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class User {
    private final String name;
    private double initialCash;
    private boolean blocked;

    private final List<String> marketMakerForEvents;

    /**
     * Every change this user's balance has ever gone through, oldest first, each one labeled with
     * why it happened - a deposit, a trade, a commission, a payout. Recorded inside
     * increaseBalance/decreaseBalance rather than at the call sites: those two methods are the
     * only way a balance can ever move, so anything that touches the money is captured
     * automatically and no future caller can forget to record itself.
     */
    private final List<AccountEntry> accountHistory = new ArrayList<>();

    public User(String name, double initialCash, List<String> marketMakerForEvents) {
        this.name = name;
        this.initialCash = initialCash;
        this.blocked = false;
        this.marketMakerForEvents = marketMakerForEvents;
        // The opening balance is a real entry - without it a user's history would start at their
        // first transaction rather than at what they began with.
        this.accountHistory.add(new AccountEntry(LocalDateTime.now(), "Initial balance", initialCash, initialCash));
    }

    /**
     * This user's full account history, oldest first, as an independent snapshot - safe to iterate
     * even while another request is concurrently changing this same user's balance elsewhere.
     */
    public synchronized List<AccountEntry> getAccountHistory() {
        return new ArrayList<>(accountHistory);
    }

    private void recordEntry(String description, double amount) {
        accountHistory.add(new AccountEntry(LocalDateTime.now(), description, amount, this.initialCash));
    }

    public String getName() {
        return name;
    }

    public synchronized double getBalance() {
        return initialCash;
    }

    // All of a user's mutable state (balance, blocked, history) is touched here and in
    // decreaseBalance/setBlocked - the only places a balance can ever move. Synchronized per-user
    // (not one lock shared by everyone) so two people trading on different accounts never block
    // each other; only two requests touching the SAME user's money ever have to wait their turn -
    // which matters now that a server can genuinely field two such requests at once, unlike the
    // single-desktop-app world this class was first written for.
    public synchronized void increaseBalance(double amount, String reason) {
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
        recordEntry(reason, amount);
    }

    public synchronized void decreaseBalance(double amount, String reason) {
        if (amount < 0) {
            throw new IllegalArgumentException("Amount to decrease balance by cannot be negative.");
        }
        this.initialCash -= amount;
        recordEntry(reason, -amount);
    }

    /** An independent snapshot of the events this user is MM for - same reasoning as getAccountHistory(). */
    public synchronized List<String> getMarketMakerForEvents() {
        return new ArrayList<>(marketMakerForEvents);
    }

    /**
     * Records that this user is now the Market Maker of another event - used when a user creates
     * an event at runtime, so their role there matches one assigned in the loaded file.
     */
    public synchronized void addMarketMakerEvent(final String eventName) {
        if (!marketMakerForEvents.contains(eventName)) {
            marketMakerForEvents.add(eventName);
        }
    }

    public synchronized boolean isBlocked() {
        return blocked;
    }

    public synchronized void setBlocked(boolean blocked) {
        this.blocked = blocked;
    }
}
