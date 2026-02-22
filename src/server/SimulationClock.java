package server;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Simulation clock: 1 real second = SCALE_FACTOR simulation seconds.
 * So 1 simulation hour passes in 60/SCALE_FACTOR real seconds.
 * Default: 1 real minute = 1 simulation hour (scale = 60)
 */
public class SimulationClock {

    // 1 real second = 60 simulation seconds
    private static final int SCALE_FACTOR = 60;

    private final LocalDateTime simulationStart;
    private final long realStartMs;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter DAY_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public SimulationClock() {
        simulationStart = LocalDateTime.of(2025, 1, 1, 9, 0, 0);
        realStartMs = System.currentTimeMillis();
    }

    public LocalDateTime getSimulationTime() {
        long elapsedRealMs = System.currentTimeMillis() - realStartMs;
        long elapsedSimSeconds = (elapsedRealMs / 1000L) * SCALE_FACTOR;
        return simulationStart.plusSeconds(elapsedSimSeconds);
    }

    public String getSimulationTimeString() {
        return getSimulationTime().format(FORMATTER);
    }

    public String getSimulationDayString() {
        return getSimulationTime().format(DAY_FORMATTER);
    }

    public int getScaleFactor() {
        return SCALE_FACTOR;
    }
}
