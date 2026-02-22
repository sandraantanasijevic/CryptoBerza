package server;

import common.ExchangeService;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ExchangeServer {

    private static final int RMI_PORT  = 1099;
    private static final int TCP_PORT  = 5000;
    private static final String SERVICE_NAME = "ExchangeService";

    public static void main(String[] args) throws Exception {
        System.out.println("Digital Exchange Server Starting");

        SimulationClock clock = new SimulationClock(); 
        TradeArchiver archiver = new TradeArchiver("trades_archive");
        Thread archiverThread = new Thread(archiver, "TradeArchiver");
        archiverThread.setDaemon(true);
        archiverThread.start();

        MarketEngine engine = new MarketEngine(archiver, clock);

        // Start TCP server
        TcpMarketServer tcpServer = new TcpMarketServer(TCP_PORT, engine);
        Thread tcpThread = new Thread(tcpServer, "TcpMarketServer");
        tcpThread.setDaemon(true);
        tcpThread.start();

        // Start RMI registry and bind service
        Registry registry = LocateRegistry.createRegistry(RMI_PORT);
        ExchangeServiceImpl service = new ExchangeServiceImpl(engine, TCP_PORT);
        registry.rebind(SERVICE_NAME, service); // registruje servis
        System.out.println("[ExchangeServer] RMI service bound: " + SERVICE_NAME + " on port " + RMI_PORT);

        //price movements (every 2 real seconds)
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(() -> {
            try {
                engine.simulatePriceMovements();
            } catch (Exception e) {
                System.err.println("[Scheduler] Error: " + e.getMessage());
            }
        }, 2, 2, TimeUnit.SECONDS);

        System.out.println("[ExchangeServer] Ready. Simulation clock started.");
        System.out.println("[ExchangeServer] Simulation time: " + clock.getSimulationTimeString());
        System.out.println("[ExchangeServer] Scale: 1 real second = " + clock.getScaleFactor() + " simulation seconds");

        Thread.currentThread().join();
    }
}
