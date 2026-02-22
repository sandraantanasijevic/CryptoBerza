package server;

import common.Order;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public class OrderBook {

    private final String symbol;
    // Bids sorted descending by price
    private final List<Order> bids = new CopyOnWriteArrayList<>();
    // Asks sorted ascending by price
    private final List<Order> asks = new CopyOnWriteArrayList<>();

    public OrderBook(String symbol) {
        this.symbol = symbol;
    }

    public String getSymbol() { return symbol; }

    public synchronized void addBid(Order order) {
        bids.add(order);
        bids.sort((a, b) -> Double.compare(b.getPrice(), a.getPrice()));
    }

    public synchronized void addAsk(Order order) {
        asks.add(order);
        asks.sort(Comparator.comparingDouble(Order::getPrice));
    }

    public synchronized List<Order> getBids() {
        return new ArrayList<>(bids);
    }

    public synchronized List<Order> getAsks() {
        return new ArrayList<>(asks);
    }


    public synchronized List<double[]> matchOrders() {
        List<double[]> matches = new ArrayList<>();

        while (!bids.isEmpty() && !asks.isEmpty()) {
            Order bestBid = bids.get(0);
            Order bestAsk = asks.get(0);

            if (bestBid.getPrice() >= bestAsk.getPrice()) {
                double matchPrice = bestAsk.getPrice(); // ask price is execution price
                double matchQty = Math.min(bestBid.getRemainingQuantity(), bestAsk.getRemainingQuantity());

                matches.add(new double[]{
                    bestBid.getClientId(),
                    bestAsk.getClientId(),
                    matchPrice,
                    matchQty
                });

                bestBid.setRemainingQuantity(bestBid.getRemainingQuantity() - matchQty);
                bestAsk.setRemainingQuantity(bestAsk.getRemainingQuantity() - matchQty);

                if (bestBid.getRemainingQuantity() < 0.000001) bids.remove(0);
                if (!asks.isEmpty() && bestAsk.getRemainingQuantity() < 0.000001) asks.remove(0);
            } else {
                break;
            }
        }

        return matches;
    }

    public synchronized void removeFilledOrders() {
        bids.removeIf(o -> o.getRemainingQuantity() < 0.000001);
        asks.removeIf(o -> o.getRemainingQuantity() < 0.000001);
    }
}
