package server;

import common.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;

public class MarketEngine {

    private final Map<String, FinancialInstrument> instruments = new ConcurrentHashMap<>();
    private final Map<String, OrderBook> orderBooks = new ConcurrentHashMap<>();
    private final Map<Integer, ClientAccount> accounts = new ConcurrentHashMap<>();
    private final Map<String, List<Double>> priceHistory1h = new ConcurrentHashMap<>();
    private final Map<String, List<Double>> priceHistory24h = new ConcurrentHashMap<>();
    private final Map<String, List<Double>> priceHistory7d = new ConcurrentHashMap<>();

    private final TradeArchiver archiver;
    private final SimulationClock clock;
    private final List<TcpClientHandler> tcpClients = new CopyOnWriteArrayList<>();

    private static final double INITIAL_CLIENT_BALANCE = 100_000.0;

    public MarketEngine(TradeArchiver archiver, SimulationClock clock) {
        this.archiver = archiver;
        this.clock = clock;
        initializeInstruments();
    }

    private void initializeInstruments() {
        Object[][] data = {
            {"BTC",  "Bitcoin",          67383.0},
            {"ETH",  "Ethereum",          1946.0},
            {"BNB",  "BNB",                614.0},
            {"SOL",  "Solana",             83.0},
            {"XRP",  "XRP",                  1.3},
            {"ADA",  "Cardano",              0.2},
            {"AVAX", "Avalanche",            8.8},
            {"DOGE", "Dogecoin",             0.38},
            {"DOT",  "Polkadot",             1.3},
            {"MATIC","Polygon",              0.52},
            {"LINK", "Chainlink",           8.7},
            {"UNI",  "Uniswap",             3.4},
            {"LTC",  "Litecoin",            53.0},
            {"ATOM", "Cosmos",               2.2},
            {"NEAR", "NEAR Protocol",        1.0},
        };

        for (Object[] row : data) {
            String symbol = (String) row[0];
            String name   = (String) row[1];
            double price  = (Double) row[2];
            FinancialInstrument fi = new FinancialInstrument(symbol, name, price);
            instruments.put(symbol, fi);
            orderBooks.put(symbol, new OrderBook(symbol));
            priceHistory1h.put(symbol, new CopyOnWriteArrayList<>());
            priceHistory24h.put(symbol, new CopyOnWriteArrayList<>());
            priceHistory7d.put(symbol, new CopyOnWriteArrayList<>());
        }
    }

    public synchronized int registerClient() {
        int id = accounts.size() + 1;
        ClientAccount acc = new ClientAccount(id, INITIAL_CLIENT_BALANCE);
        // Daj svim klijentima pocetne holdings za svaki instrument
        for (String symbol : instruments.keySet()) {
            double price = instruments.get(symbol).getCurrentPrice();
            // Daj kolicinu vrednosti oko 5000 USD po instrumentu
            double qty = Math.round((5000.0 / price) * 10000.0) / 10000.0;
            if (qty < 0.0001) qty = 0.0001;
            acc.addHolding(symbol, qty);
        }
        accounts.put(id, acc);
        System.out.println("[MarketEngine] Registered client #" + id + " with initial holdings");
        return id;
    }

    public List<FinancialInstrument> getSnapshot() {
        return new ArrayList<>(instruments.values());
    }

    public List<Order> getBidOrders(String symbol) {
        OrderBook ob = orderBooks.get(symbol);
        if (ob == null) return new ArrayList<>();
        return ob.getBids();
    }

    public List<Order> getAskOrders(String symbol) {
        OrderBook ob = orderBooks.get(symbol);
        if (ob == null) return new ArrayList<>();
        return ob.getAsks();
    }

    public String placeBuyOrder(int clientId, String symbol, double price, double quantity) {
        ClientAccount acc = accounts.get(clientId);
        if (acc == null) return "ERROR: Unknown client";
        if (!instruments.containsKey(symbol)) return "ERROR: Unknown symbol";
        if (price <= 0 || quantity <= 0) return "ERROR: Invalid price or quantity";

        double cost = price * quantity;
        synchronized (acc) {
            if (acc.getCashBalance() < cost) {
                return String.format("ERROR: Insufficient funds. Need %.2f, have %.2f", cost, acc.getCashBalance());
            }
            acc.setCashBalance(acc.getCashBalance() - cost);
        }

        Order order = new Order(clientId, symbol, Order.OrderType.BUY, price, quantity);
        orderBooks.get(symbol).addBid(order);
        matchOrders(symbol);
        return "OK";
    }

    public String placeSellOrder(int clientId, String symbol, double price, double quantity) {
        ClientAccount acc = accounts.get(clientId);
        if (acc == null) return "ERROR: Unknown client";
        if (!instruments.containsKey(symbol)) return "ERROR: Unknown symbol";
        if (price <= 0 || quantity <= 0) return "ERROR: Invalid price or quantity";

        synchronized (acc) {
            double holding = acc.getHolding(symbol);
            if (holding < quantity) {
                return String.format("ERROR: Insufficient holdings. Need %.4f, have %.4f", quantity, holding);
            }
            acc.subtractHolding(symbol, quantity);
        }

        Order order = new Order(clientId, symbol, Order.OrderType.SELL, price, quantity);
        orderBooks.get(symbol).addAsk(order);
        matchOrders(symbol);
        return "OK";
    }

    private void matchOrders(String symbol) {
        OrderBook ob = orderBooks.get(symbol);
        List<double[]> matches = ob.matchOrders();

        for (double[] m : matches) {
            int buyerClientId  = (int) m[0];
            int sellerClientId = (int) m[1];
            double matchPrice  = m[2];
            double matchQty    = m[3];

            // Update buyer account
            ClientAccount buyer = accounts.get(buyerClientId);
            if (buyer != null) {
                synchronized (buyer) {
                    buyer.addHolding(symbol, matchQty);
                }
            }

            ClientAccount seller = accounts.get(sellerClientId);
            if (seller != null) {
                synchronized (seller) {
                    seller.setCashBalance(seller.getCashBalance() + matchPrice * matchQty);
                }
            }

            // Update instrument price
            FinancialInstrument fi = instruments.get(symbol);
            if (fi != null) {
                double oldPrice = fi.getCurrentPrice();
                fi.setCurrentPrice(matchPrice);
                fi.setLastUpdated(LocalDateTime.now());
                updatePriceHistory(symbol, oldPrice, matchPrice);
            }

            Trade trade = new Trade(symbol, matchPrice, matchQty, buyerClientId, sellerClientId,
                    clock.getSimulationTime());
            archiver.archive(trade);

            broadcastUpdate(symbol, matchPrice, MarketUpdate.UpdateType.TRADE_EXECUTED);

            System.out.printf("[MarketEngine] TRADE %s: %.4f @ %.2f (buyer=%d, seller=%d)%n",
                    symbol, matchQty, matchPrice, buyerClientId, sellerClientId);
        }
    }

    private void updatePriceHistory(String symbol, double oldPrice, double newPrice) {
        FinancialInstrument fi = instruments.get(symbol);
        if (fi == null) return;

        List<Double> hist1h  = priceHistory1h.get(symbol);
        List<Double> hist24h = priceHistory24h.get(symbol);
        List<Double> hist7d  = priceHistory7d.get(symbol);

        hist1h.add(oldPrice);
        hist24h.add(oldPrice);
        hist7d.add(oldPrice);

        // Limit history size
        if (hist1h.size() > 60)   hist1h.remove(0);
        if (hist24h.size() > 1440) hist24h.remove(0);
        if (hist7d.size() > 10080) hist7d.remove(0);

        // Update change percentages
        if (!hist1h.isEmpty()) {
            double ref1h = hist1h.get(0);
            fi.setChange1h(ref1h != 0 ? ((newPrice - ref1h) / ref1h) * 100 : 0);
        }
        if (!hist24h.isEmpty()) {
            double ref24h = hist24h.get(0);
            fi.setChange24h(ref24h != 0 ? ((newPrice - ref24h) / ref24h) * 100 : 0);
        }
        if (!hist7d.isEmpty()) {
            double ref7d = hist7d.get(0);
            fi.setChange7d(ref7d != 0 ? ((newPrice - ref7d) / ref7d) * 100 : 0);
        }
    }

    public void broadcastUpdate(String symbol, double price, MarketUpdate.UpdateType type) {
        FinancialInstrument fi = instruments.get(symbol);
        double change = fi != null ? fi.getChangeFromOpen() : 0;
        MarketUpdate update = new MarketUpdate(type, symbol, price, change, clock.getSimulationTimeString());

        for (TcpClientHandler handler : tcpClients) {
            handler.sendUpdate(update, symbol);
        }
    }

    public void simulatePriceMovements() {
        Random rnd = new Random();
        for (FinancialInstrument fi : instruments.values()) {
            // Small random walk: -1% to +1%
            double changePct = (rnd.nextGaussian() * 0.3) / 100.0;
            double newPrice = fi.getCurrentPrice() * (1 + changePct);
            if (newPrice < 0.0001) newPrice = 0.0001;

            double oldPrice = fi.getCurrentPrice();
            fi.setCurrentPrice(newPrice);
            fi.setLastUpdated(LocalDateTime.now());
            updatePriceHistory(fi.getSymbol(), oldPrice, newPrice);

            broadcastUpdate(fi.getSymbol(), newPrice, MarketUpdate.UpdateType.PRICE_UPDATE);
        }
    }

    public void registerTcpClient(TcpClientHandler handler) {
        tcpClients.add(handler);
    }

    public void unregisterTcpClient(TcpClientHandler handler) {
        tcpClients.remove(handler);
    }

    public ClientAccount getClientAccount(int clientId) {
        return accounts.get(clientId);
    }

    public Map<String, FinancialInstrument> getInstruments() {
        return instruments;
    }

    public List<Trade> getTradesForDay(String symbol, String day) {
        return archiver.getTradesForDay(symbol, day);
    }
}
