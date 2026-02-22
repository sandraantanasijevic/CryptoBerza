package common;

import java.io.Serializable;
import java.time.LocalDateTime;

public class FinancialInstrument implements Serializable {
    private static final long serialVersionUID = 1L;

    private String symbol;
    private String name;
    private double openPrice;
    private double currentPrice;
    private double change1h;
    private double change24h;
    private double change7d;
    private LocalDateTime lastUpdated;

    public FinancialInstrument(String symbol, String name, double openPrice) {
        this.symbol = symbol;
        this.name = name;
        this.openPrice = openPrice;
        this.currentPrice = openPrice;
        this.change1h = 0.0;
        this.change24h = 0.0;
        this.change7d = 0.0;
        this.lastUpdated = LocalDateTime.now();
    }

    public String getSymbol() { return symbol; }
    public String getName() { return name; }
    public double getOpenPrice() { return openPrice; }
    public double getCurrentPrice() { return currentPrice; }
    public void setCurrentPrice(double currentPrice) { this.currentPrice = currentPrice; }
    public double getChange1h() { return change1h; }
    public void setChange1h(double change1h) { this.change1h = change1h; }
    public double getChange24h() { return change24h; }
    public void setChange24h(double change24h) { this.change24h = change24h; }
    public double getChange7d() { return change7d; }
    public void setChange7d(double change7d) { this.change7d = change7d; }
    public LocalDateTime getLastUpdated() { return lastUpdated; }
    public void setLastUpdated(LocalDateTime lastUpdated) { this.lastUpdated = lastUpdated; }

    public double getChangeFromOpen() {
        return ((currentPrice - openPrice) / openPrice) * 100.0;
    }

    @Override
    public String toString() {
        return String.format("FinancialInstrument{symbol='%s', name='%s', openPrice=%.2f, currentPrice=%.2f}",
                symbol, name, openPrice, currentPrice);
    }
}
