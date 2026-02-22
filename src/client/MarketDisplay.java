package client;

import common.FinancialInstrument;
import common.MarketUpdate;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class MarketDisplay {

    private final Map<String, DisplayRow> rows = new LinkedHashMap<>();
    private final List<String> symbolOrder = new ArrayList<>();
    private boolean initialized = false;

    private static class DisplayRow {
        String symbol;
        String name;
        double price;
        double change1h;
        double change24h;
        double change7d;
        String lastTime;

        DisplayRow(String symbol, String name, double price, double c1h, double c24h, double c7d, String time) {
            this.symbol = symbol;
            this.name = name;
            this.price = price;
            this.change1h = c1h;
            this.change24h = c24h;
            this.change7d = c7d;
            this.lastTime = time;
        }
    }

    public synchronized void initFromSnapshot(List<FinancialInstrument> instruments, List<String> watched) {
        rows.clear();
        symbolOrder.clear();
        for (FinancialInstrument fi : instruments) {
            if (watched.isEmpty() || watched.contains(fi.getSymbol())) {
                rows.put(fi.getSymbol(), new DisplayRow(
                        fi.getSymbol(), fi.getName(),
                        fi.getCurrentPrice(),
                        fi.getChange1h(), fi.getChange24h(), fi.getChange7d(),
                        "N/A"
                ));
                symbolOrder.add(fi.getSymbol());
            }
        }
        printHeader();
        for (String sym : symbolOrder) {
            printRow(rows.get(sym));
        }
        initialized = true;
    }

    public synchronized void update(MarketUpdate upd) {
        String symbol = upd.getSymbol();
        DisplayRow row = rows.get(symbol);
        if (row == null) return;

        //double oldPrice = row.price;
        row.price = upd.getPrice();
        row.lastTime = upd.getSimulationTime();
        row.change24h = upd.getChangePercent();

        int rowIdx = symbolOrder.indexOf(symbol);
        if (rowIdx < 0) return;

        int linesFromBottom = symbolOrder.size() - rowIdx;
        System.out.print(ConsoleColors.moveUp(linesFromBottom));
        System.out.print(ConsoleColors.CLEAR_LINE);
        printRow(row);
        for (int i = 0; i < linesFromBottom - 1; i++) {
            System.out.println();
        }
    }

    private void printHeader() {
        System.out.println();
        System.out.printf(ConsoleColors.BOLD + ConsoleColors.CYAN +
                "%-6s %-18s %14s %10s %10s %10s  %-20s%n" + ConsoleColors.RESET,
                "SYMBOL", "NAME", "PRICE (USD)", "1h %", "24h %", "7d %", "LAST UPDATE");
        System.out.println(ConsoleColors.CYAN + "─".repeat(95) + ConsoleColors.RESET);
    }

    private void printRow(DisplayRow row) {
        String priceStr = formatPrice(row.price);
        String c1h  = formatChange(row.change1h);
        String c24h = formatChange(row.change24h);
        String c7d  = formatChange(row.change7d);

        System.out.printf("%-6s %-18s %14s %10s %10s %10s  %-20s%n",
                ConsoleColors.BOLD + row.symbol + ConsoleColors.RESET,
                row.name,
                priceStr,
                c1h, c24h, c7d,
                ConsoleColors.YELLOW + row.lastTime + ConsoleColors.RESET);
    }

    private String formatPrice(double price) {
        if (price >= 1000)      
        	return ConsoleColors.WHITE + String.format("%,.2f", price) + ConsoleColors.RESET;
        else if (price >= 1)   
        	return ConsoleColors.WHITE + String.format("%.4f", price)  + ConsoleColors.RESET;
        else                   
        	return ConsoleColors.WHITE + String.format("%.6f", price)  + ConsoleColors.RESET;
    }

    private String formatChange(double change) {
        String arrow = ConsoleColors.arrow(change);
        String sign  = ConsoleColors.sign(change);
        String text  = String.format("%s%s%.2f%%", arrow, sign, change);
        return ConsoleColors.colorize(change, text);
    }

    public boolean isInitialized() { return initialized; }
}
