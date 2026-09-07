package guessmarket.engine.models;

import java.util.List;

public class OrderBookEvent extends Event {
    
    // As discussed, we are storing all info from the XML (allowMint, initial, d)
    // exactly as it is given to avoid losing data that might be needed by the engine later.
    private final boolean allowMint;
    private final int initial;
    private final int d;

    public OrderBookEvent(int id, String name, String description, CommissionType commissionType, 
                          int commissionValue, List<Option> options, 
                          boolean allowMint, int initial, int d) {
        super(id, name, description, commissionType, commissionValue, options);
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
}
