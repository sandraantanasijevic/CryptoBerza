package client;

public class ConsoleColors {
    public static final String RESET   = "\u001B[0m";
    public static final String RED     = "\u001B[31m";
    public static final String GREEN   = "\u001B[32m";
    public static final String YELLOW  = "\u001B[33m";
    public static final String CYAN    = "\u001B[36m";
    public static final String WHITE   = "\u001B[37m";
    public static final String BOLD    = "\u001B[1m";
    public static final String BLUE    = "\u001B[34m";
    public static final String MAGENTA = "\u001B[35m";

    // Move cursor up N lines
    public static String moveUp(int n) {
        return "\u001B[" + n + "A";
    }

    // Clear line
    public static final String CLEAR_LINE = "\u001B[2K\r";

    public static String colorize(double value, String text) {
        if (value > 0) return GREEN + text + RESET;
        if (value < 0) return RED   + text + RESET;
        return WHITE + text + RESET;
    }

    public static String arrow(double value) {
        if (value > 0) return "▲";
        if (value < 0) return "▼";
        return "─";
    }

    public static String sign(double value) {
        return value >= 0 ? "+" : "";
    }
}
