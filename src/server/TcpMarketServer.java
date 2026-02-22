package server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TcpMarketServer implements Runnable {

    private final int port;
    private final MarketEngine engine;
    private ServerSocket serverSocket;
    private volatile boolean running = true;
    private final ExecutorService pool = Executors.newCachedThreadPool();

    public TcpMarketServer(int port, MarketEngine engine) {
        this.port = port;
        this.engine = engine;
    }

    @Override
    public void run() {
        try {
            serverSocket = new ServerSocket(port);
            System.out.println("[TcpMarketServer] Listening on port " + port);

            while (running) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    System.out.println("[TcpMarketServer] New TCP client: " + clientSocket.getRemoteSocketAddress());
                    TcpClientHandler handler = new TcpClientHandler(clientSocket, engine);
                    pool.submit(handler);
                } catch (IOException e) {
                    if (running) System.err.println("[TcpMarketServer] Accept error: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            System.err.println("[TcpMarketServer] Could not start: " + e.getMessage());
        }
    }

    public void stop() {
        running = false;
        try { serverSocket.close(); } catch (IOException ignored) {}
        pool.shutdown();
    }
}
