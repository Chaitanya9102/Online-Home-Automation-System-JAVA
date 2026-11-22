package edu.homeautomation.web;

import edu.homeautomation.model.AppUser;
import edu.homeautomation.model.ActivityEvent;
import edu.homeautomation.model.EnvironmentReading;
import edu.homeautomation.service.ActivityHistory;
import edu.homeautomation.service.EnvironmentService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/** Small HTML helpers shared by the servlet-rendered pages. */
final class WebHtml {
    private WebHtml() { }

    static String escape(String value) {
        if (value == null) return "";
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }

    static String csrf(HttpServletRequest request) {
        HttpSession session = request.getSession();
        Object token = session.getAttribute(WebSecurityFilter.CSRF);
        if (token == null) {
            token = java.util.UUID.randomUUID().toString();
            session.setAttribute(WebSecurityFilter.CSRF, token);
        }
        return "<input type=\"hidden\" name=\"_csrf\" value=\"" + escape(token.toString()) + "\">";
    }

    static String flash(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) return "";
        Object value = session.getAttribute("haven.flash");
        Object kind = session.getAttribute("haven.flash.kind");
        session.removeAttribute("haven.flash");
        session.removeAttribute("haven.flash.kind");
        return value == null ? "" : "<div class=\"flash " + ("error".equals(kind) ? "error" : "") + "\" role=\"status\">" + escape(value.toString()) + "</div>";
    }

    static void flash(HttpServletRequest request, String message, boolean error) {
        HttpSession session = request.getSession();
        session.setAttribute("haven.flash", message);
        session.setAttribute("haven.flash.kind", error ? "error" : "success");
    }

    static String activityPanel(ActivityHistory history, int count) {
        List<ActivityEvent> events = history.latest(count);
        StringBuilder html = new StringBuilder("<section class=\"panel\"><div class=\"panel-head\"><h2>Recent activity</h2><span class=\"small muted\">This session</span></div>");
        if (events.isEmpty()) return html.append("<p class=\"muted\">No actions yet. Device controls and admin changes will appear here.</p></section>").toString();
        html.append("<div class=\"table-wrap\"><table><thead><tr><th>Time</th><th>User</th><th>Action</th><th>Details</th></tr></thead><tbody>");
        DateTimeFormatter time = DateTimeFormatter.ofPattern("h:mm a");
        for (ActivityEvent event : events) html.append("<tr><td>").append(event.occurredAt().format(time)).append("</td><td>")
                .append(escape(event.actor())).append("</td><td>").append(escape(event.action())).append("</td><td>")
                .append(escape(event.details())).append("</td></tr>");
        return html.append("</tbody></table></div></section>").toString();
    }

    static String temperaturePanel(EnvironmentService environment) {
        List<EnvironmentReading> readings = environment.temperatureHistory();
        double current = environment.latest().temperature();
        boolean alert = current > environment.getTemperatureLimit();
        String status = alert ? "Alert · above " + String.format(Locale.ROOT, "%.0f", environment.getTemperatureLimit()) + "°C limit"
                : "Within range · limit " + String.format(Locale.ROOT, "%.0f", environment.getTemperatureLimit()) + "°C";
        StringBuilder html = new StringBuilder("<section class=\"panel temperature-panel\"><div class=\"panel-head\"><h2>Temperature history</h2><span class=\"state ")
                .append(alert ? "pending" : "").append("\">").append(escape(status)).append("</span></div><p class=\"small muted\">Simulated readings · most recent ").append(readings.size()).append(" samples</p>");
        if (readings.size() < 2) return html.append("<p class=\"muted\">History will appear after another simulated reading.</p></section>").toString();
        double min = readings.stream().mapToDouble(EnvironmentReading::temperature).min().orElse(0);
        double max = readings.stream().mapToDouble(EnvironmentReading::temperature).max().orElse(1);
        if (max - min < 1) { min -= .5; max += .5; }
        StringBuilder points = new StringBuilder();
        for (int i=0; i<readings.size(); i++) {
            int x = 30 + (700 * i / Math.max(1, readings.size()-1));
            double normalized = (readings.get(i).temperature() - min) / (max-min);
            int y = 145 - (int)Math.round(normalized * 115);
            if (i > 0) points.append(' ');
            points.append(x).append(',').append(y);
        }
        html.append("<svg class=\"temperature-chart\" viewBox=\"0 0 760 180\" role=\"img\" aria-label=\"Simulated temperature history from ")
                .append(String.format(Locale.ROOT,"%.1f",min)).append(" to ").append(String.format(Locale.ROOT,"%.1f",max)).append(" degrees Celsius\"><line x1=\"30\" y1=\"150\" x2=\"730\" y2=\"150\"/><polyline points=\"")
                .append(points).append("\"/><text x=\"30\" y=\"172\">").append(String.format(Locale.ROOT,"%.1f°C",min)).append("</text><text x=\"670\" y=\"22\">")
                .append(String.format(Locale.ROOT,"%.1f°C",max)).append("</text></svg></section>");
        return html.toString();
    }

    static String page(HttpServletRequest request, String title, String active, String body) {
        String path = escape(request.getContextPath());
        AppUser user = (AppUser) request.getSession().getAttribute(WebSecurityFilter.USER);
        String initials = user.name().trim().isEmpty() ? "H" : user.name().trim().substring(0, 1).toUpperCase();
        String homeownerLinks = "HOMEOWNER".equals(user.role())
                ? "<a class=\"" + ("automations".equals(active) ? "active" : "") + "\" href=\"" + path + "/automations\">⟳ &nbsp;Automations</a>"
                : "";
        String adminLink = "ADMIN".equals(user.role())
                ? "<a class=\"" + ("admin".equals(active) ? "active" : "") + "\" href=\"" + path + "/admin\">▦ &nbsp;Admin console</a>"
                : "";
        return "<!doctype html><html lang=\"en\"><head><meta charset=\"utf-8\"><meta name=\"viewport\" content=\"width=device-width,initial-scale=1\"><meta name=\"theme-color\" content=\"#14283b\"><title>" + escape(title) + " · Haven</title><link rel=\"stylesheet\" href=\"" + path + "/assets/style.css\"></head><body>"
                + "<div class=\"shell\"><aside class=\"sidebar\"><a class=\"brand\" href=\"" + path + "/dashboard\"><span class=\"brand-mark\">⌂</span> HAVEN</a>"
                + "<div><div class=\"nav-label\">Your home</div><nav class=\"nav\"><a class=\"" + ("dashboard".equals(active) ? "active" : "") + "\" href=\"" + path + "/dashboard\">⌂ &nbsp;Overview</a>" + homeownerLinks + "<a class=\"" + ("profile".equals(active) ? "active" : "") + "\" href=\"" + path + "/profile\">◉ &nbsp;My profile</a></nav></div>"
                + (adminLink.isEmpty() ? "" : "<div><div class=\"nav-label\">Management</div><nav class=\"nav\">" + adminLink + "</nav></div>")
                + "<div class=\"side-foot\">● &nbsp;Home hub online<br>Simulated device network</div></aside>"
                + "<main class=\"main\"><header class=\"topbar\"><span class=\"topbar-title\">" + escape(title) + "</span><div class=\"user-chip\"><span class=\"avatar\">" + escape(initials) + "</span><span class=\"user-name\">" + escape(user.name()) + "</span><form method=\"post\" action=\"" + path + "/auth/logout\">" + csrf(request) + "<button class=\"btn ghost small-btn\" type=\"submit\">Sign out</button></form></div></header>"
                + "<section class=\"content\">" + flash(request) + body + "</section></main></div></body></html>";
    }

    static String input(String label, String name, String value, String type, boolean required) {
        return "<div class=\"field\"><label for=\"" + escape(name) + "\">" + escape(label) + "</label><input id=\"" + escape(name) + "\" name=\"" + escape(name) + "\" type=\"" + escape(type) + "\" value=\"" + escape(value) + "\"" + (required ? " required" : "") + "></div>";
    }

    static void sendPage(jakarta.servlet.http.HttpServletResponse response, String html) throws java.io.IOException {
        response.setContentType("text/html;charset=UTF-8");
        response.getWriter().write(html);
    }
}
