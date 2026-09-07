package guessmarket.engine.models;

import java.util.List;

public class User {
    private final String name;
    private double initialCash;
    
    // As discussed, we are storing all info from the XML exactly as it is given 
    // to avoid losing data that might be needed by the engine later.
    private final List<Integer> marketMakerForEvents;

    public User(String name, double initialCash, List<Integer> marketMakerForEvents) {
        this.name = name;
        this.initialCash = initialCash;
        this.marketMakerForEvents = marketMakerForEvents;
    }

    public String getName() {
        return name;
    }

    public double getBalance() {
        return initialCash;
    }

    public void setBalance(double initialCash) {
        this.initialCash = initialCash;
    }

    public List<Integer> getMarketMakerForEvents() {
        return marketMakerForEvents;
    }
}
