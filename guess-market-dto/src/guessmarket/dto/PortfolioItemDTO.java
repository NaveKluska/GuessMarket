package guessmarket.dto;

import java.io.Serializable;

public class PortfolioItemDTO implements Serializable {
    private final int eventId;
    private final String optionName;
    private final int quantity;

    public PortfolioItemDTO(int eventId, String optionName, int quantity) {
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
}
