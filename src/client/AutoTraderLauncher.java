package client;

public class AutoTraderLauncher {

    private static final String HOST     = "localhost";
    private static final int    RMI_PORT = 1099;
    private static final int    NUM_BOTS = 5;

    public static void main(String[] args) {
        System.out.println("AutoTrader Launcher - Starting " + NUM_BOTS + " bots");

        for (int i = 1; i <= NUM_BOTS; i++) {
            String botName = "Bot-" + i;
            AutoTrader bot = new AutoTrader(HOST, RMI_PORT, botName);
            Thread t = new Thread(bot, botName);
            t.setDaemon(false);
            t.start();
            System.out.println("Started: " + botName);

            try { Thread.sleep(500); } catch (InterruptedException e) { break; }
        }

        System.out.println("All bots started. Press Ctrl+C to stop.");
        try { Thread.currentThread().join(); } catch (InterruptedException ignored) {}
    }
}
