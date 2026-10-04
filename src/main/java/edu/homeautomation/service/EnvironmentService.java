package edu.homeautomation.service;

import edu.homeautomation.model.EnvironmentReading;
import java.time.LocalDateTime;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public final class EnvironmentService {
    private final Random random = new Random();
    private final CopyOnWriteArrayList<Consumer<EnvironmentReading>> listeners = new CopyOnWriteArrayList<>();
    private volatile EnvironmentReading latest = new EnvironmentReading(22.4, "All secure", LocalDateTime.now());
    private Thread worker;

    public EnvironmentReading latest() { return latest; }
    public void addListener(Consumer<EnvironmentReading> listener) { listeners.add(listener); }
    public void removeListener(Consumer<EnvironmentReading> listener) { listeners.remove(listener); }

    public synchronized void start() {
        if (worker != null && worker.isAlive()) return;
        worker = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try { Thread.sleep(5000); }
                catch (InterruptedException exception) { Thread.currentThread().interrupt(); break; }
                EnvironmentReading reading = new EnvironmentReading(21.8 + random.nextDouble() * 1.5, "All secure", LocalDateTime.now());
                latest = reading;
                listeners.forEach(listener -> listener.accept(reading));
            }
        }, "environment-monitor");
        worker.setDaemon(true);
        worker.start();
    }
}
