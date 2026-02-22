package common;

import java.io.Serializable;

public class MarketUpdate implements Serializable {
    private static final long serialVersionUID = 1L;

    public enum UpdateType { PRICE_UPDATE, TRADE_EXECUTED, ORDER_MATCHED }

    private UpdateType type;
    private String symbol;
    private double price;
    private double changePercent;
    private String simulationTime;
    private String message;

    public MarketUpdate(UpdateType type, String symbol, double price, double changePercent, String simulationTime) {
        this.type = type;
        this.symbol = symbol;
        this.price = price;
        this.changePercent = changePercent;
        this.simulationTime = simulationTime;
        this.message = "";
    }

    public MarketUpdate(UpdateType type, String message) {
        this.type = type;
        this.message = message;
        this.symbol = "";
        this.price = 0;
        this.changePercent = 0;
        this.simulationTime = "";
    }

    public UpdateType getType() { return type; }
    public String getSymbol() { return symbol; }
    public double getPrice() { return price; }
    public double getChangePercent() { return changePercent; }
    public String getSimulationTime() { return simulationTime; }
    public String getMessage() { return message; }

    @Override
    public String toString() {
        return String.format("MarketUpdate{type=%s, symbol='%s', price=%.2f, change=%.2f%%, time='%s'}",
                type, symbol, price, changePercent, simulationTime);
    }
}
