package guessmarket.dto;

public class PendingOrderDTO {
    private final String userName;
    private final String type;      // "BUY" or "SELL"
    private final String optionName;
    private final double price;
    private final int quantity;

    public PendingOrderDTO(String userName, String type, String optionName, double price, int quantity) {
        this.userName = userName;
        this.type = type;
        this.optionName = optionName;
        this.price = price;
        this.quantity = quantity;
    }

    public String getUserName() { return userName; }
    public String getType() { return type; }
    public String getOptionName() { return optionName; }
    public double getPrice() { return price; }
    public int getQuantity() { return quantity; }
}
