package guessmarket.engine.models;

import java.util.ArrayList;
import java.util.List;

public class User {
    private final String name;
    private double balance;
    private final List<Double> balanceHistory;
    
    // As discussed, we are storing all info from the XML exactly as it is given 
    // to avoid losing data that might be needed by the engine later.
    private final List<Integer> marketMakerForEvents;
    private final List<PortfolioItem> portfolio;

    private boolean isBlocked = false;

    public User(String name, double initialCash, List<Integer> marketMakerForEvents) {
        this.name = name;
        this.balance = initialCash;
        this.marketMakerForEvents = marketMakerForEvents;
        this.portfolio = new ArrayList<>();
        this.balanceHistory = new ArrayList<>();
        this.balanceHistory.add(initialCash); // record starting balance
    }

    public String getName() {
        return name;
    }

    public double getBalance() {
        return balance;
    }

    public void deductBalance(double amount) {
        this.balance -= amount;
        this.balanceHistory.add(this.balance);
        if (this.balance < 0) {
            this.isBlocked = true;
        }
    }

    public void addFunds(double amount) {
        this.balance += amount;
        this.balanceHistory.add(this.balance);
    }

    public List<Integer> getMarketMakerForEvents() {
        return marketMakerForEvents;
    }

    public List<PortfolioItem> getPortfolio() {
        return portfolio;
    }

    public List<Double> getBalanceHistory() {
        return java.util.Collections.unmodifiableList(this.balanceHistory);
    }

    public boolean isBlocked() {
        return isBlocked;
    }

    public int getPortfolioQuantity(int eventId, String optionName) {
        for (PortfolioItem item : portfolio) {
            if (item.getEventId() == eventId && item.getOptionName().equals(optionName)) {
                return item.getQuantity();
            }
        }
        return 0;
    }

    public void addShares(int eventId, String optionName, int quantity) {
        for (PortfolioItem item : portfolio) {
            if (item.getEventId() == eventId && item.getOptionName().equals(optionName)) {
                item.addQuantity(quantity);
                return;
            }
        }
        portfolio.add(new PortfolioItem(eventId, optionName, quantity));
    }
}
