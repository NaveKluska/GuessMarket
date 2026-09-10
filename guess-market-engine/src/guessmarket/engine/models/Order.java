package guessmarket.engine.models;

import java.io.Serializable;
import java.time.LocalDateTime;

public class Order implements Serializable {
    private static final long serialVersionUID = 1L;
    
    public enum Type { BUY, SELL }
    
    private final String userName;
    private final int optionIndex;
    private final Type type;
    private final double price;
    private int quantity;
    private final LocalDateTime timestamp;

    public Order(String userName, int optionIndex, Type type, double price, int quantity) {
        this.userName = userName;
        this.optionIndex = optionIndex;
        this.type = type;
        this.price = price;
        this.quantity = quantity;
        this.timestamp = LocalDateTime.now();
    }

    public String getUserName() { return userName; }
    public int getOptionIndex() { return optionIndex; }
    public Type getType() { return type; }
    public double getPrice() { return price; }
    public int getQuantity() { return quantity; }
    public LocalDateTime getTimestamp() { return timestamp; }
    
    public void reduceQuantity(int amount) {
        if (amount > quantity) {
            throw new IllegalArgumentException("Cannot reduce more than available quantity");
        }
        this.quantity -= amount;
    }
}
