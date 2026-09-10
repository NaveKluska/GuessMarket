package guessmarket.engine.models;

import java.io.Serializable;

public class PortfolioItem implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private final int eventId;
    private final String optionName;
    private int quantity;

    public PortfolioItem(int eventId, String optionName, int quantity) {
        this.eventId = eventId;
        this.optionName = optionName;
        this.quantity = quantity;
    }

    public int getEventId() {
        return eventId;
    }

    public String getOptionName() {
        return optionName;
    }

    public int getQuantity() {
        return quantity;
    }

    public void addQuantity(int amount) {
        this.quantity += amount;
    }
}
