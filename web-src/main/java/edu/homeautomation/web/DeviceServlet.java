package edu.homeautomation.web;

import edu.homeautomation.model.HomeValidationException;
import edu.homeautomation.model.LightDevice;
import edu.homeautomation.model.SmartDevice;
import edu.homeautomation.model.ThermostatDevice;
import edu.homeautomation.persistence.HomeRepository;
import edu.homeautomation.service.DeviceService;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/** Accepts homeowner device commands and stores each state change through JDBC. */
public final class DeviceServlet extends HttpServlet {
    @Override protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        HomeRepository repository = (HomeRepository) getServletContext().getAttribute(HomeAppListener.REPOSITORY);
        String id = request.getParameter("deviceId");
        String operation = request.getParameter("operation");
        SmartDevice device = repository.findDevices().stream().filter(item -> item.getId().equals(id)).findFirst().orElse(null);
        try {
            if (device == null) throw new IllegalArgumentException("That device could not be found.");
            if ("toggle".equals(operation)) {
                String message = devices().toggle(device);
                WebHtml.flash(request, message, false);
            } else if ("adjust".equals(operation)) {
                if (!device.isCompatible()) throw new IllegalArgumentException("This device needs administrator approval before it can be changed.");
                int value = Integer.parseInt(request.getParameter("value"));
                devices().adjust(device, value);
                WebHtml.flash(request, "Settings saved for " + device.getName() + ".", false);
            } else {
                throw new IllegalArgumentException("Choose a valid device action.");
            }
        } catch (HomeValidationException | IllegalArgumentException exception) {
            WebHtml.flash(request, exception.getMessage(), true);
        }
        response.sendRedirect(request.getContextPath() + "/dashboard");
    }

    private DeviceService devices() {
        return (DeviceService) getServletContext().getAttribute(HomeAppListener.DEVICES);
    }
}
