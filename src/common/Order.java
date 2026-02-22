package common;

import java.io.Serializable;
import java.time.LocalDateTime;

public class Order implements Serializable {
    private static final long serialVersionUID = 1L;

    public enum OrderType { BUY, SELL }

    private static int idCounter = 1;

    private int id;
    private int clientId;
    private String symbol;
    private OrderType type;
    private double price;
    private double quantity;
    private double remainingQuantity;
    private LocalDateTime timestamp;

    public Order(int clientId, String symbol, OrderType type, double price, double quantity) {
        this.id = idCounter++;
        this.clientId = clientId;
        this.symbol = symbol;
        this.type = type;
        this.price = price;
        this.quantity = quantity;
        this.remainingQuantity = quantity;
        this.timestamp = LocalDateTime.now();
    }

    public int getId() { return id; }
    public int getClientId() { return clientId; }
    public String getSymbol() { return symbol; }
    public OrderType getType() { return type; }
    public double getPrice() { return price; }
    public double getQuantity() { return quantity; }
    public double getRemainingQuantity() { return remainingQuantity; }
    public void setRemainingQuantity(double remainingQuantity) { this.remainingQuantity = remainingQuantity; }
    public LocalDateTime getTimestamp() { return timestamp; }

    @Override
    public String toString() {
        return String.format("Order{id=%d, clientId=%d, symbol='%s', type=%s, price=%.2f, qty=%.4f, rem=%.4f}",
                id, clientId, symbol, type, price, quantity, remainingQuantity);
    }
}
