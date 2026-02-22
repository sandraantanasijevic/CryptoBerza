package server;

import common.MarketUpdate;

import java.io.*;
import java.net.Socket;
import java.util.HashSet;
import java.util.Set;

public class TcpClientHandler implements Runnable {

    private final Socket socket;
    private final MarketEngine engine;
    private final ObjectOutputStream oos;
    private ObjectInputStream ois;
    private final Set<String> subscribedSymbols = new HashSet<>();
    private volatile boolean running = true;

    public TcpClientHandler(Socket socket, MarketEngine engine) throws IOException {
        this.socket = socket;
        this.engine = engine;
        this.oos = new ObjectOutputStream(socket.getOutputStream());
        this.oos.flush();
    }

    @Override
    public void run() {
        try {
            ois = new ObjectInputStream(socket.getInputStream());
            engine.registerTcpClient(this);

            // Read subscription request: comma-separated symbols
            while (running) {
                try {
                    Object msg = ois.readObject();
                    if (msg instanceof String) {
                        String cmd = (String) msg;
                        if (cmd.startsWith("SUBSCRIBE:")) {
                            String[] syms = cmd.substring(10).split(",");
                            synchronized (subscribedSymbols) {
                                subscribedSymbols.clear();
                                for (String s : syms) {
                                    subscribedSymbols.add(s.trim());
                                }
                            }
                            System.out.println("[TcpClientHandler] Client subscribed to: " + subscribedSymbols);
                        } else if (cmd.equals("DISCONNECT")) {
                            break;
                        }
                    }
                } catch (EOFException | java.net.SocketException e) {
                    break;
                } catch (ClassNotFoundException e) {
                    System.err.println("[TcpClientHandler] Unknown class: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            // client disconnected
        } finally {
            engine.unregisterTcpClient(this);
            try { socket.close(); } catch (IOException ignored) {}
        }
    }

    public synchronized void sendUpdate(MarketUpdate update, String symbol) {
        synchronized (subscribedSymbols) {
            if (!subscribedSymbols.isEmpty() && !subscribedSymbols.contains(symbol)) {
                return;
            }
        }
        try {
            oos.writeObject(update); //pretvara obj u bajtove i salje klijentu
            oos.flush();
            oos.reset(); // prevent memory leak
        } catch (IOException e) {
            running = false;
        }
    }

    public void stop() {
        running = false;
        try { socket.close(); } catch (IOException ignored) {}
    }
}
