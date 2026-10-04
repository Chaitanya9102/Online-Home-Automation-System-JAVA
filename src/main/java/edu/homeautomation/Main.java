package edu.homeautomation;

import edu.homeautomation.persistence.HomeRepository;
import edu.homeautomation.persistence.InMemoryHomeRepository;
import edu.homeautomation.persistence.JdbcHomeRepository;
import edu.homeautomation.service.DeviceService;
import edu.homeautomation.service.EnvironmentService;
import edu.homeautomation.ui.LoginFrame;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public final class Main {
    private Main() { }

    public static void main(String[] args) {
        HomeRepository repository = createRepository();
        DeviceService devices = new DeviceService(repository);
        EnvironmentService environment = new EnvironmentService();
        environment.start();

        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
            // Swing's cross-platform look and feel remains available.
        }

        SwingUtilities.invokeLater(() -> new LoginFrame(repository, devices, environment).setVisible(true));
    }

    private static HomeRepository createRepository() {
        String jdbcUrl = System.getenv("HOME_AUTOMATION_DB");
        if (jdbcUrl != null && !jdbcUrl.isBlank()) {
            try {
                return new JdbcHomeRepository(jdbcUrl);
            } catch (RuntimeException exception) {
                System.err.println("Database unavailable; starting demo mode: " + exception.getMessage());
            }
        }
        return new InMemoryHomeRepository();
    }
}
