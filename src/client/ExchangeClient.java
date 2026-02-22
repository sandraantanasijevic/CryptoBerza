package client;

import common.*;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.*;

public class ExchangeClient {

    private static final String HOST = "localhost";
    private static final int RMI_PORT = 1099;

    private ExchangeService service;
    private int clientId;
    private final Scanner scanner = new Scanner(System.in);
    private TcpMarketReceiver receiver;
    private MarketDisplay display;

    public void start() {
        try {
            System.out.println("Digital Crypto Exchange Client");
            System.out.println("Connecting to server...");

            Registry registry = LocateRegistry.getRegistry(HOST, RMI_PORT);
            service = (ExchangeService) registry.lookup("ExchangeService"); //da dobije stub 
            clientId = service.registerClient();

            System.out.println("Connected! Your Client ID: " + clientId);

            mainMenu();

        } catch (Exception e) {
            System.err.println("Error connecting to server: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void mainMenu() throws Exception {
        boolean running = true;
        while (running) {
            System.out.println("MAIN MENU (Client #" + clientId + ")");
            System.out.println("1. View Market Snapshot");
            System.out.println("2. Subscribe to Live Market Feed");
            System.out.println("3. View Order Book (Bids/Asks)");
            System.out.println("4. Place Buy Order");
            System.out.println("5. Place Sell Order");
            System.out.println("6. View My Account");
            System.out.println("7. View Trade History");
            System.out.println("8. Exit");
            System.out.print("Choice: ");

            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1": viewSnapshot(); break;
                case "2": subscribeToFeed(); break;
                case "3": viewOrderBook(); break;
                case "4": placeBuyOrder(); break;
                case "5": placeSellOrder(); break;
                case "6": viewAccount(); break;
                case "7": viewTradeHistory(); break;
                case "8": running = false; break;
                default:  System.out.println("Invalid choice.");
            }
        }

        if (receiver != null) receiver.disconnect();
        System.out.println("Goodbye!");
    }

    private void viewSnapshot() throws Exception {
        List<FinancialInstrument> snapshot = service.getMarketSnapshot();
        System.out.println("\n" + ConsoleColors.BOLD + ConsoleColors.CYAN +
                String.format("%-6s %-18s %14s %12s", "SYMBOL", "NAME", "PRICE (USD)", "CHANGE") +
                ConsoleColors.RESET);
        System.out.println(ConsoleColors.CYAN + "─".repeat(55) + ConsoleColors.RESET);

        for (FinancialInstrument fi : snapshot) {
            double change = fi.getChangeFromOpen();
            String arrow  = ConsoleColors.arrow(change);
            String sign   = ConsoleColors.sign(change);
            String changeStr = String.format("%s%s%.2f%%", arrow, sign, change);
            String priceStr = fi.getCurrentPrice() >= 1 ?
                    String.format("%,.4f", fi.getCurrentPrice()) :
                    String.format("%.6f", fi.getCurrentPrice());

            System.out.printf("%-6s %-18s %14s %12s%n",
                    ConsoleColors.BOLD + fi.getSymbol() + ConsoleColors.RESET,
                    fi.getName(),
                    ConsoleColors.WHITE + priceStr + ConsoleColors.RESET,
                    ConsoleColors.colorize(change, changeStr));
        }
    }

    private void subscribeToFeed() throws Exception {
        List<FinancialInstrument> snapshot = service.getMarketSnapshot();
        System.out.println("\nAvailable symbols:");
        List<String> allSymbols = new ArrayList<>();
        for (FinancialInstrument fi : snapshot) {
            System.out.printf("  %s (%s)%n", fi.getSymbol(), fi.getName());
            allSymbols.add(fi.getSymbol());
        }
        System.out.println("Enter symbols to watch (comma-separated, or ALL for all):");
        System.out.print("> ");
        String input = scanner.nextLine().trim().toUpperCase();

        List<String> watchList;
        if (input.equals("ALL") || input.isEmpty()) {
            watchList = allSymbols;
        } else {
            watchList = new ArrayList<>(Arrays.asList(input.split(",")));
            for (int i = 0; i < watchList.size(); i++) watchList.set(i, watchList.get(i).trim());
        }

        // Stop previous receiver
        if (receiver != null) receiver.disconnect();

        display = new MarketDisplay();
        display.initFromSnapshot(snapshot, watchList);

        int tcpPort = service.getTcpPort();
        receiver = new TcpMarketReceiver(HOST, tcpPort, watchList, display);
        Thread t = new Thread(receiver, "TcpReceiver");
        t.setDaemon(true);
        t.start();

        System.out.println("\nLive feed started. Press ENTER to return to menu.");
        scanner.nextLine();
    }

    private void viewOrderBook() throws Exception {
        System.out.print("Enter symbol: ");
        String symbol = scanner.nextLine().trim().toUpperCase();

        List<Order> bids = service.getBidOrders(symbol);
        List<Order> asks = service.getAskOrders(symbol);

        System.out.println("\n" + ConsoleColors.BOLD + "ORDER BOOK: " + symbol + " " + ConsoleColors.RESET);

        System.out.println(ConsoleColors.GREEN + ConsoleColors.BOLD +
                String.format("%-5s %-15s %-15s", "#", "BID PRICE", "QUANTITY") + ConsoleColors.RESET);
        int i = 1;
        for (Order o : bids) {
            System.out.printf(ConsoleColors.GREEN + "%-5d %-15.4f %-15.4f%n" + ConsoleColors.RESET,
                    i++, o.getPrice(), o.getRemainingQuantity());
            if (i > 10) { System.out.println("(more)"); break; }
        }

        System.out.println(ConsoleColors.RED + ConsoleColors.BOLD +
                String.format("%-5s %-15s %-15s", "#", "ASK PRICE", "QUANTITY") + ConsoleColors.RESET);
        i = 1;
        for (Order o : asks) {
            System.out.printf(ConsoleColors.RED + "%-5d %-15.4f %-15.4f%n" + ConsoleColors.RESET,
                    i++, o.getPrice(), o.getRemainingQuantity());
            if (i > 10) { System.out.println("(more)"); break; }
        }
    }

    private void placeBuyOrder() throws Exception {
        System.out.print("Symbol: ");
        String symbol = scanner.nextLine().trim().toUpperCase();
        System.out.print("Price (USD): ");
        double price = Double.parseDouble(scanner.nextLine().trim());
        System.out.print("Quantity: ");
        double qty = Double.parseDouble(scanner.nextLine().trim());

        String result = service.placeBuyOrder(clientId, symbol, price, qty);
        if (result.equals("OK")) {
            System.out.println(ConsoleColors.GREEN + "Buy order placed successfully!" + ConsoleColors.RESET);
        } else {
            System.out.println(ConsoleColors.RED + "Error: " + result + ConsoleColors.RESET);
        }
    }

    private void placeSellOrder() throws Exception {
        System.out.print("Symbol: ");
        String symbol = scanner.nextLine().trim().toUpperCase();
        System.out.print("Price (USD): ");
        double price = Double.parseDouble(scanner.nextLine().trim());
        System.out.print("Quantity: ");
        double qty = Double.parseDouble(scanner.nextLine().trim());

        String result = service.placeSellOrder(clientId, symbol, price, qty);
        if (result.equals("OK")) {
            System.out.println(ConsoleColors.GREEN + "Sell order placed successfully!" + ConsoleColors.RESET);
        } else {
            System.out.println(ConsoleColors.RED + "Error: " + result + ConsoleColors.RESET);
        }
    }

    private void viewAccount() throws Exception {
        ClientAccount acc = service.getClientAccount(clientId);
        if (acc == null) { System.out.println("Account not found."); return; }

        System.out.println("\n" + ConsoleColors.BOLD + "YOUR ACCOUNT" + ConsoleColors.RESET);
        System.out.printf("Client ID:     %d%n", acc.getClientId());
        System.out.printf("Cash Balance:  %s%.2f USD%n", ConsoleColors.GREEN, acc.getCashBalance());
        System.out.print(ConsoleColors.RESET);
        System.out.println(ConsoleColors.BOLD + "Holdings:" + ConsoleColors.RESET);
        if (acc.getHoldings().isEmpty()) {
            System.out.println("(none)");
        } else {
            for (Map.Entry<String, Double> e : acc.getHoldings().entrySet()) {
                System.out.printf("%-8s %.6f%n", e.getKey(), e.getValue());
            }
        }
    }

    private void viewTradeHistory() throws Exception {
        System.out.print("Symbol: ");
        String symbol = scanner.nextLine().trim().toUpperCase();
        System.out.print("Day (yyyy-MM-dd): ");
        String day = scanner.nextLine().trim();

        List<Trade> trades = service.getTradesForDay(symbol, day);
        System.out.println("\n" + ConsoleColors.BOLD + "TRADE HISTORY: " + symbol + " on " + day + " " + ConsoleColors.RESET);
        if (trades.isEmpty()) {
            System.out.println("No trades found.");
        } else {
            for (Trade t : trades) {
                System.out.println(t);
            }
        }
    }

    public static void main(String[] args) {
        new ExchangeClient().start();
    }
}
