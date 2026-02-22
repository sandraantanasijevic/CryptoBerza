package common;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class ClientAccount implements Serializable {
    private static final long serialVersionUID = 1L;

    private int clientId;
    private double cashBalance;
    private Map<String, Double> holdings; 

    public ClientAccount(int clientId, double initialCash) {
        this.clientId = clientId;
        this.cashBalance = initialCash;
        this.holdings = new HashMap<>();
    }

    public int getClientId() { return clientId; }
    public double getCashBalance() { return cashBalance; }
    public void setCashBalance(double cashBalance) { this.cashBalance = cashBalance; }
    public Map<String, Double> getHoldings() { return holdings; }

    public double getHolding(String symbol) {
        return holdings.getOrDefault(symbol, 0.0);
    }

    public void addHolding(String symbol, double quantity) {
        holdings.merge(symbol, quantity, Double::sum);
    }

    public void subtractHolding(String symbol, double quantity) {
        double current = getHolding(symbol);
        if (current - quantity < 0.000001) {
            holdings.remove(symbol);
        } else {
            holdings.put(symbol, current - quantity);
        }
    }

    @Override
    public String toString() {
        return String.format("ClientAccount{clientId=%d, cash=%.2f, holdings=%s}", clientId, cashBalance, holdings);
    }
}
