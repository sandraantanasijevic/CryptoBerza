package common;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Trade implements Serializable {
    private static final long serialVersionUID = 1L;

    private String symbol;
    private double price;
    private double quantity;
    private int buyerClientId;
    private int sellerClientId;
    private LocalDateTime timestamp;

    public Trade(String symbol, double price, double quantity, int buyerClientId, int sellerClientId, LocalDateTime timestamp) {
        this.symbol = symbol;
        this.price = price;
        this.quantity = quantity;
        this.buyerClientId = buyerClientId;
        this.sellerClientId = sellerClientId;
        this.timestamp = timestamp;
    }

    public String getSymbol() { return symbol; }
    public double getPrice() { return price; }
    public double getQuantity() { return quantity; }
    public int getBuyerClientId() { return buyerClientId; }
    public int getSellerClientId() { return sellerClientId; }
    public LocalDateTime getTimestamp() { return timestamp; }

    @Override
    public String toString() {
        return String.format("%s | %s | PRICE: %.2f | QTY: %.4f | BUYER: %d | SELLER: %d",
                timestamp.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                symbol, price, quantity, buyerClientId, sellerClientId);
    }
}
