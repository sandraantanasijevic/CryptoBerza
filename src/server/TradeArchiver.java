package server;

import common.Trade;

import java.io.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class TradeArchiver implements Runnable {

    private final BlockingQueue<Trade> queue = new LinkedBlockingQueue<>();
    private final String archiveDir;
    private volatile boolean running = true;
    private static final DateTimeFormatter DAY_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public TradeArchiver(String archiveDir) {
        this.archiveDir = archiveDir;
        new File(archiveDir).mkdirs();
    }

    public void archive(Trade trade) {
        queue.offer(trade);
    }

    public void stop() {
        running = false;
        Thread.currentThread().interrupt();
    }

    @Override
    public void run() {
        while (running) {
            try {
                Trade trade = queue.take();
                writeTrade(trade);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        Trade trade;
        while ((trade = queue.poll()) != null) {
            writeTrade(trade);
        }
    }

    private void writeTrade(Trade trade) {
        String day = trade.getTimestamp().toLocalDate().format(DAY_FMT);
        String filename = archiveDir + File.separator + trade.getSymbol() + "_" + day + ".csv";
        try (FileWriter fw = new FileWriter(filename, true);
             PrintWriter pw = new PrintWriter(fw)) {
            pw.println(trade.toString());  //1 red - 1 trade
        } catch (IOException e) {
            System.err.println("[TradeArchiver] Error writing trade: " + e.getMessage());
        }
    }

    //Read trades for a given symbol and day from the archive.
    
    public List<Trade> getTradesForDay(String symbol, String day) {
        List<Trade> result = new ArrayList<>();
        String filename = archiveDir + File.separator + symbol + "_" + day + ".csv";
        File f = new File(filename);
        if (!f.exists()) return result;

        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (!line.trim().isEmpty()) { //preskace prazne linije
                    result.add(parseTradeFromLine(line, symbol)); //pretvara text u trade obj
                }
            }
        } catch (IOException e) {
            System.err.println("[TradeArchiver] Error reading trades: " + e.getMessage());
        }
        return result;
    }

    private Trade parseTradeFromLine(String line, String symbol) {
        // Format: timestamp | symbol | PRICE: p | QTY: q | BUYER: b | SELLER: s
        try {
            String[] parts = line.split("\\|");
            String tsStr = parts[0].trim();
            double price = Double.parseDouble(parts[2].replace("PRICE:", "").trim());
            double qty = Double.parseDouble(parts[3].replace("QTY:", "").trim());
            int buyer = Integer.parseInt(parts[4].replace("BUYER:", "").trim());
            int seller = Integer.parseInt(parts[5].replace("SELLER:", "").trim());
            java.time.LocalDateTime ts = java.time.LocalDateTime.parse(tsStr,
                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            return new Trade(symbol, price, qty, buyer, seller, ts);
        } catch (Exception e) {
            return new Trade(symbol, 0, 0, -1, -1, java.time.LocalDateTime.now());
        }
    }
}
