package client;

import common.MarketUpdate;

import java.io.*;
import java.net.Socket;
import java.util.List;

public class TcpMarketReceiver implements Runnable {

    private final String host;
    private final int port;
    private final List<String> subscribedSymbols;
    private final MarketDisplay display;
    private Socket socket;
    private ObjectOutputStream oos;
    private ObjectInputStream ois;
    private volatile boolean running = true;

    public TcpMarketReceiver(String host, int port, List<String> subscribedSymbols, MarketDisplay display) {
        this.host = host;
        this.port = port;
        this.subscribedSymbols = subscribedSymbols;
        this.display = display;
    }

    @Override
    public void run() {
        try {
            socket = new Socket(host, port); //otvara konekciju
            oos = new ObjectOutputStream(socket.getOutputStream());
            oos.flush();
            ois = new ObjectInputStream(socket.getInputStream());

            // Send subscription
            String subscription = "SUBSCRIBE:" + String.join(",", subscribedSymbols);
            oos.writeObject(subscription);
            oos.flush();

            System.out.println("[TcpReceiver] Connected. Subscribed to: " + subscribedSymbols);

            while (running) {
                try {
                    Object obj = ois.readObject();
                    if (obj instanceof MarketUpdate) {
                        MarketUpdate upd = (MarketUpdate) obj;
                        if (display.isInitialized()) {
                            display.update(upd);
                        }
                    }
                } catch (EOFException | java.net.SocketException e) {
                    System.out.println("[TcpReceiver] Connection closed.");
                    break;
                } catch (ClassNotFoundException e) {
                    System.err.println("[TcpReceiver] Unknown class: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            System.err.println("[TcpReceiver] Error: " + e.getMessage());
        } finally {
            disconnect();
        }
    }

    public void disconnect() {
        running = false;
        try {
            if (oos != null) {
                oos.writeObject("DISCONNECT");
                oos.flush();
            }
        } catch (IOException ignored) {}
        try { if (socket != null) socket.close(); } catch (IOException ignored) {}
    }
}
