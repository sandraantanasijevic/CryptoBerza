package client;

import common.*;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.*;

// Automated bot that generates random buy/sell orders
  
public class AutoTrader implements Runnable {

    private final String host;
    private final int rmiPort;
    private final String botName;
    private ExchangeService service;
    private int clientId;
    private final Random rnd = new Random();

    private static final String[] SYMBOLS = {"BTC","ETH","BNB","SOL","XRP","ADA","AVAX","DOGE","DOT","LTC"};

    public AutoTrader(String host, int rmiPort, String botName) {
        this.host = host;
        this.rmiPort = rmiPort;
        this.botName = botName;
    }

    @Override
    public void run() {
        try {
            Registry registry = LocateRegistry.getRegistry(host, rmiPort);
            service = (ExchangeService) registry.lookup("ExchangeService");
            clientId = service.registerClient();
            System.out.println("[AutoTrader:" + botName + "] Registered as client #" + clientId);

            List<FinancialInstrument> snapshot = service.getMarketSnapshot();

            while (!Thread.currentThread().isInterrupted()) {
                try {
                    Thread.sleep(3000 + rnd.nextInt(5000)); // wait 3-8 seconds

                    //random instrument
                    FinancialInstrument fi = snapshot.get(rnd.nextInt(snapshot.size()));
                    String symbol = fi.getSymbol();

                    // Refresh current price
                    List<FinancialInstrument> current = service.getMarketSnapshot();
                    double currentPrice = current.stream()
                            .filter(i -> i.getSymbol().equals(symbol))
                            .findFirst()
                            .map(FinancialInstrument::getCurrentPrice)
                            .orElse(fi.getCurrentPrice());

                    ClientAccount acc = service.getClientAccount(clientId);
                    boolean decideBuy = rnd.nextBoolean();

                    if (decideBuy) {
                        // Buy
                        double priceMod = 1.0 + (rnd.nextDouble() * 0.02); // up to 2%
                        double price = currentPrice * priceMod;
                        double maxSpend = acc.getCashBalance() * (0.01 + rnd.nextDouble() * 0.05); // 1-6% koliko moze da potrosi
                        double qty = Math.max(0.0001, maxSpend / price); //koliko coina kupuje
                        qty = Math.round(qty * 10000.0) / 10000.0; //na 4 decimale 

                        if (acc.getCashBalance() > price * qty && price * qty > 1.0) {
                            String result = service.placeBuyOrder(clientId, symbol, price, qty);
                            if (result.equals("OK")) {
                                System.out.printf("[AutoTrader:%s] BUY %s: %.4f @ %.4f%n", botName, symbol, qty, price);
                            }
                        }
                    } else {
                        // Sell if we have holdings
                        double holding = acc.getHolding(symbol);
                        if (holding > 0.0001) {
                            double sellQty = holding * (0.1 + rnd.nextDouble() * 0.4); // sell 10-50%
                            sellQty = Math.round(sellQty * 10000.0) / 10000.0;
                            if (sellQty < 0.0001) sellQty = holding;

                            double priceMod = 1.0 - (rnd.nextDouble() * 0.02);
                            double price = currentPrice * priceMod;

                            if (sellQty > 0.0001) {
                                String result = service.placeSellOrder(clientId, symbol, price, sellQty);
                                if (result.equals("OK")) {
                                    System.out.printf("[AutoTrader:%s] SELL %s: %.4f @ %.4f%n", botName, symbol, sellQty, price);
                                }
                            }
                        } else {
                            // No holdings
                            double price = currentPrice * 0.99;
                            double qty = Math.max(0.0001, (acc.getCashBalance() * 0.01) / price);
                            qty = Math.round(qty * 10000.0) / 10000.0;
                            if (acc.getCashBalance() > price * qty && price * qty > 1.0) {
                                service.placeBuyOrder(clientId, symbol, price, qty);
                            }
                        }
                    }

                    if (rnd.nextInt(10) == 0) {
                        snapshot = service.getMarketSnapshot();
                    }

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    System.err.println("[AutoTrader:" + botName + "] Error: " + e.getMessage());
                }
            }
        } catch (Exception e) {
            System.err.println("[AutoTrader:" + botName + "] Fatal error: " + e.getMessage());
        }
    }
}
