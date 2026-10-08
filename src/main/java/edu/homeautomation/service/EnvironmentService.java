package edu.homeautomation.service;

import edu.homeautomation.model.EnvironmentReading;
import java.time.LocalDateTime;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/** Publishes simulated home readings from a synchronized background monitor. */
public final class EnvironmentService {
    private final Random random = new Random();
    private final CopyOnWriteArrayList<Consumer<EnvironmentReading>> listeners = new CopyOnWriteArrayList<>();
    private volatile EnvironmentReading latest = new EnvironmentReading(22.4, "All secure", LocalDateTime.now());
    private volatile double temperatureLimit = 28.0;
    private volatile String securityStatus = "All secure";
    private Thread worker;

    public EnvironmentReading latest() { return latest; }
    public double getTemperatureLimit() { return temperatureLimit; }
    public void addListener(Consumer<EnvironmentReading> listener) { listeners.add(listener); }
    public void removeListener(Consumer<EnvironmentReading> listener) { listeners.remove(listener); }

    public void setTemperatureLimit(double temperatureLimit) {
        if (temperatureLimit < 10 || temperatureLimit > 40) throw new IllegalArgumentException("Temperature limit must be between 10 and 40 °C.");
        this.temperatureLimit = temperatureLimit;
        publish(latest.temperature());
    }

    public void setSecurityStatus(String securityStatus) {
        this.securityStatus = securityStatus;
        publish(latest.temperature());
    }

    private void publish(double temperature) {
        latest = new EnvironmentReading(temperature, securityStatus, LocalDateTime.now());
        listeners.forEach(listener -> listener.accept(latest));
    }

    public synchronized void start() {
        if (worker != null && worker.isAlive()) return;
        worker = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try { Thread.sleep(5000); }
                catch (InterruptedException exception) { Thread.currentThread().interrupt(); break; }
                publish(21.8 + random.nextDouble() * 12.2);
            }
        }, "environment-monitor");
        worker.setDaemon(true);
        worker.start();
    }
}
