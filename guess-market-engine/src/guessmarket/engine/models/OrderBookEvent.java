package guessmarket.engine.models;

import java.util.List;

public class OrderBookEvent extends Event {
    
    // As discussed, we are storing all info from the XML (allowMint, initial, d)
    // exactly as it is given to avoid losing data that might be needed by the engine later.
    private final boolean allowMint;
    private final int initial;
    private final int d;

    public OrderBookEvent(int id, String name, String description, int commissionValue,
                          CommissionType commissionType, List<Option> options, 
                          boolean allowMint, int initial, int d) {
        super(id, name, description, commissionValue, commissionType, options);
        this.allowMint = allowMint;
        this.initial = initial;
        this.d = d;
    }

    public boolean isAllowMint() {
        return allowMint;
    }

    public int getInitial() {
        return initial;
    }

    public int getD() {
        return d;
    }

    @Override
    public double getOptionProbability(int optionIndex) {
        // TODO: Implement Order Book probability logic later
        throw new UnsupportedOperationException("Order Book logic not yet implemented");
    }

    @Override
    public double calculateCost(int optionIndex, int quantity) {
        // TODO: Implement Order Book cost calculation later
        throw new UnsupportedOperationException("Order Book logic not yet implemented");
    }
}
