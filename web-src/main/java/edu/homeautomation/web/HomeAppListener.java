package edu.homeautomation.web;

import edu.homeautomation.persistence.HomeRepository;
import edu.homeautomation.persistence.JdbcHomeRepository;
import edu.homeautomation.service.EnvironmentService;
import edu.homeautomation.service.ActivityHistory;
import edu.homeautomation.service.DeviceService;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;

/** Builds the shared application services when the web application starts. */
public final class HomeAppListener implements ServletContextListener {
    public static final String REPOSITORY = HomeAppListener.class.getName() + ".repository";
    public static final String ENVIRONMENT = HomeAppListener.class.getName() + ".environment";
    public static final String ACTIVITY = HomeAppListener.class.getName() + ".activity";
    public static final String DEVICES = HomeAppListener.class.getName() + ".devices";

    @Override
    public void contextInitialized(ServletContextEvent event) {
        String database = System.getenv().getOrDefault("HOME_AUTOMATION_DB", "jdbc:sqlite:home-automation-web.db");
        HomeRepository repository = new JdbcHomeRepository(database);
        EnvironmentService environment = new EnvironmentService();
        ActivityHistory activity = new ActivityHistory();
        DeviceService devices = new DeviceService(repository, activity);
        environment.setTemperatureLimit(parseLimit(repository.getSetting("temperature_limit", "28")));
        environment.start();
        ServletContext context = event.getServletContext();
        context.setAttribute(REPOSITORY, repository);
        context.setAttribute(ENVIRONMENT, environment);
        context.setAttribute(ACTIVITY, activity);
        context.setAttribute(DEVICES, devices);
        context.log("Haven web application is ready. Database: " + database);
    }

    @Override
    public void contextDestroyed(ServletContextEvent event) {
        Object service = event.getServletContext().getAttribute(ENVIRONMENT);
        if (service instanceof EnvironmentService environment) environment.stop();
    }

    private double parseLimit(String value) {
        try { return Double.parseDouble(value); }
        catch (NumberFormatException ignored) { return 28.0; }
    }
}
