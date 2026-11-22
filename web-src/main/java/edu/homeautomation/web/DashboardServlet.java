package edu.homeautomation.web;

import edu.homeautomation.model.EnvironmentReading;
import edu.homeautomation.model.SecurityDevice;
import edu.homeautomation.model.SmartDevice;
import edu.homeautomation.persistence.HomeRepository;
import edu.homeautomation.service.EnvironmentService;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/** Home overview for both roles. */
public final class DashboardServlet extends HttpServlet {
    @Override protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        HomeRepository repository = (HomeRepository) getServletContext().getAttribute(HomeAppListener.REPOSITORY);
        EnvironmentService environment = (EnvironmentService) getServletContext().getAttribute(HomeAppListener.ENVIRONMENT);
        List<SmartDevice> devices = repository.findDevices();
        EnvironmentReading reading = environment.latest();
        long securityCount = devices.stream().filter(SecurityDevice.class::isInstance).count();
        boolean secure = devices.stream().filter(SecurityDevice.class::isInstance).allMatch(SmartDevice::isOn);
        String homeName = repository.getSetting("home_name", "Your Home");
        String temperature = String.format(Locale.ROOT, "%.1f", reading.temperature());
        String updated = reading.recordedAt().format(DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault()));

        StringBuilder body = new StringBuilder("<div class=\"page-heading\"><div><div class=\"eyebrow\">Home overview</div><h1>Good to see you, ")
                .append(WebHtml.escape(((edu.homeautomation.model.AppUser) request.getSession().getAttribute(WebSecurityFilter.USER)).name().split(" ")[0]))
                .append("</h1><p>Here’s what’s happening around ").append(WebHtml.escape(homeName)).append(".</p></div></div>")
                .append("<div class=\"metrics\"><article class=\"metric\"><span class=\"metric-icon\">◉</span><div class=\"metric-label\">Indoor temperature</div><div class=\"metric-value\">").append(temperature).append("° C</div><div class=\"metric-note\">Comfortable range · live simulation · updated ").append(WebHtml.escape(updated)).append("</div>");
        if (reading.temperature() > environment.getTemperatureLimit()) {
            body.append("<div class=\"status-alert small\">Temperature alert · above your ").append(String.format(Locale.ROOT, "%.0f", environment.getTemperatureLimit())).append("° limit</div>");
        } else {
            body.append("<div class=\"status-good small\">Within your temperature limit</div>");
        }
        body.append("</article><article class=\"metric\"><span class=\"metric-icon\">⌑</span><div class=\"metric-label\">Home security</div><div class=\"metric-value\">").append(secure ? "All secure" : "Check devices").append("</div><div class=\"metric-note\">").append(securityCount).append(" security devices · ").append(secure ? "all armed" : "one or more disarmed").append("</div></article>")
                .append("<article class=\"metric\"><span class=\"metric-icon\">⌘</span><div class=\"metric-label\">Connected devices</div><div class=\"metric-value\">").append(devices.size()).append(" devices</div><div class=\"metric-note\">Your simulated home hub is responding</div></article></div>")
                .append(WebHtml.temperaturePanel(environment))
                .append("<div class=\"section-head\"><h2>Your devices</h2><span class=\"home-name\">Changes are saved to your home database</span></div><div class=\"device-grid\">");
        boolean canControl = "HOMEOWNER".equals(((edu.homeautomation.model.AppUser) request.getSession().getAttribute(WebSecurityFilter.USER)).role());
        for (SmartDevice device : devices) body.append(deviceCard(request, device, canControl));
        body.append("</div>")
                .append(WebHtml.activityPanel((edu.homeautomation.service.ActivityHistory) getServletContext()
                        .getAttribute(HomeAppListener.ACTIVITY), 8));
        WebHtml.sendPage(response, WebHtml.page(request, "Overview", "dashboard", body.toString()));
    }

    static String deviceCard(HttpServletRequest request, SmartDevice device, boolean canControl) {
        String path = WebHtml.escape(request.getContextPath());
        String icon = switch (device.getKind()) { case "Light" -> "☼"; case "Thermostat" -> "♨"; default -> "⌑"; };
        String status = device instanceof SecurityDevice ? device.statusText() : device.statusText();
        StringBuilder html = new StringBuilder("<article class=\"device-card\"><div class=\"device-icon\" aria-hidden=\"true\">").append(icon).append("</div><div><div class=\"device-title\">")
                .append(WebHtml.escape(device.getName())).append("</div><div class=\"device-meta\">").append(WebHtml.escape(device.getRoom())).append(" · ").append(WebHtml.escape(status)).append("</div>");
        if (!device.isCompatible()) html.append("<div class=\"small status-alert\">Waiting for administrator approval</div>");
        html.append("</div><div class=\"device-actions\"><span class=\"state ").append(device.isOn() ? "" : "off").append("\">").append(device.isOn() ? "On" : "Off").append("</span>");
        if (canControl) html.append("<form method=\"post\" action=\"" + path + "/device\">").append(WebHtml.csrf(request)).append("<input type=\"hidden\" name=\"deviceId\" value=\"").append(WebHtml.escape(device.getId())).append("\"><input type=\"hidden\" name=\"operation\" value=\"toggle\"><button class=\"btn secondary small-btn\" type=\"submit\" ").append(device.isCompatible() ? "" : "disabled").append(">").append(device.isOn() ? "Turn off" : "Turn on").append("</button></form>");
        html.append("</div>");
        if (canControl && device instanceof edu.homeautomation.model.LightDevice light) {
            html.append("<form class=\"device-adjust\" method=\"post\" action=\"" + path + "/device\">").append(WebHtml.csrf(request)).append("<input type=\"hidden\" name=\"deviceId\" value=\"").append(WebHtml.escape(device.getId())).append("\"><input type=\"hidden\" name=\"operation\" value=\"adjust\"><label for=\"level-").append(WebHtml.escape(device.getId())).append("\">Brightness</label><input id=\"level-").append(WebHtml.escape(device.getId())).append("\" name=\"value\" type=\"number\" min=\"0\" max=\"100\" value=\"").append(light.getBrightness()).append("\" required><span class=\"small muted\">0–100%</span><button class=\"btn ghost small-btn\" type=\"submit\" ").append(device.isCompatible() ? "" : "disabled").append(">Save</button></form>");
        } else if (canControl && device instanceof edu.homeautomation.model.ThermostatDevice thermostat) {
            html.append("<form class=\"device-adjust\" method=\"post\" action=\"" + path + "/device\">").append(WebHtml.csrf(request)).append("<input type=\"hidden\" name=\"deviceId\" value=\"").append(WebHtml.escape(device.getId())).append("\"><input type=\"hidden\" name=\"operation\" value=\"adjust\"><label for=\"level-").append(WebHtml.escape(device.getId())).append("\">Target temperature</label><input id=\"level-").append(WebHtml.escape(device.getId())).append("\" name=\"value\" type=\"number\" min=\"10\" max=\"32\" value=\"").append(thermostat.getTargetTemperature()).append("\" required><span class=\"small muted\">10–32° C</span><button class=\"btn ghost small-btn\" type=\"submit\" ").append(device.isCompatible() ? "" : "disabled").append(">Save</button></form>");
        }
        return html.append("</article>").toString();
    }
}
