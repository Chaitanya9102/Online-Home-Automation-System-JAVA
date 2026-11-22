package edu.homeautomation.web;

import edu.homeautomation.model.AppUser;
import edu.homeautomation.model.SmartDevice;
import edu.homeautomation.persistence.HomeRepository;
import edu.homeautomation.service.EnvironmentService;
import edu.homeautomation.service.PasswordSecurity;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/** Role-protected admin pages for users, compatibility, platform settings, and system status. */
public final class AdminServlet extends HttpServlet {
    @Override protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        HomeRepository repository = repository();
        String tab = request.getParameter("tab");
        if (tab == null) tab = "overview";
        String path = WebHtml.escape(request.getContextPath());
        StringBuilder body = new StringBuilder("<div class=\"page-heading\"><div><div class=\"eyebrow\">Platform management</div><h1>Administration</h1><p>Manage accounts, review device compatibility, and configure the home platform.</p></div><span class=\"state\">Admin access</span></div>")
                .append("<nav class=\"tabs\"><a class=\"").append("overview".equals(tab) ? "active" : "").append("\" href=\"").append(path).append("/admin?tab=overview\">System overview</a><a class=\"").append("users".equals(tab) ? "active" : "").append("\" href=\"").append(path).append("/admin?tab=users\">User management</a><a class=\"").append("devices".equals(tab) ? "active" : "").append("\" href=\"").append(path).append("/admin?tab=devices\">Device compatibility</a><a class=\"").append("settings".equals(tab) ? "active" : "").append("\" href=\"").append(path).append("/admin?tab=settings\">System settings</a></nav>");
        switch (tab) {
            case "users" -> body.append(usersPage(request, repository));
            case "devices" -> body.append(devicesPage(request, repository));
            case "settings" -> body.append(settingsPage(request, repository));
            default -> body.append(overviewPage(repository));
        }
        if ("overview".equals(tab)) body.append(WebHtml.activityPanel(activity(), 10));
        WebHtml.sendPage(response, WebHtml.page(request, "Administration", "admin", body.toString()));
    }

    @Override protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        HomeRepository repository = repository();
        String action = request.getParameter("action");
        String tab = "overview";
        try {
            switch (action == null ? "" : action) {
                case "save-user" -> { saveUser(request, repository); tab = "users"; }
                case "delete-user" -> { deleteUser(request, repository); tab = "users"; }
                case "compatibility" -> { updateCompatibility(request, repository); tab = "devices"; }
                case "settings" -> { updateSettings(request, repository); tab = "settings"; }
                default -> throw new IllegalArgumentException("Choose a valid admin action.");
            }
        } catch (IllegalArgumentException exception) {
            WebHtml.flash(request, exception.getMessage(), true);
            if ("save-user".equals(action) || "delete-user".equals(action)) tab = "users";
            else if ("compatibility".equals(action)) tab = "devices";
            else if ("settings".equals(action)) tab = "settings";
        }
        response.sendRedirect(request.getContextPath() + "/admin?tab=" + tab);
    }

    private String overviewPage(HomeRepository repository) {
        EnvironmentService environment = (EnvironmentService) getServletContext().getAttribute(HomeAppListener.ENVIRONMENT);
        List<SmartDevice> devices = repository.findDevices();
        long users = repository.findUsers().size();
        long unapproved = devices.stream().filter(device -> !device.isCompatible()).count();
        double temperature = environment.latest().temperature();
        return "<div class=\"metrics\"><article class=\"metric\"><div class=\"metric-label\">Registered accounts</div><div class=\"metric-value\">" + users + "</div><div class=\"metric-note\">Homeowner and admin profiles</div></article><article class=\"metric\"><div class=\"metric-label\">Connected devices</div><div class=\"metric-value\">" + devices.size() + "</div><div class=\"metric-note\">" + unapproved + " awaiting compatibility approval</div></article><article class=\"metric\"><div class=\"metric-label\">Environment monitor</div><div class=\"metric-value\">" + String.format(java.util.Locale.ROOT, "%.1f", temperature) + "°</div><div class=\"metric-note\">Simulated sensor reading · updates every few seconds</div></article></div><section class=\"panel\"><div class=\"panel-head\"><h2>System status</h2><span class=\"state\">Online</span></div><p class=\"muted\">The web app is connected to its SQLite home database. Device commands and environment readings are simulated.</p><div class=\"actions\"><a class=\"btn secondary\" href=\"admin?tab=users\">Manage users</a><a class=\"btn secondary\" href=\"admin?tab=devices\">Review devices</a><a class=\"btn secondary\" href=\"admin?tab=settings\">Update settings</a></div></section>";
    }

    private String usersPage(HttpServletRequest request, HomeRepository repository) {
        String path = WebHtml.escape(request.getContextPath());
        String editEmail = request.getParameter("edit");
        AppUser editing = editEmail == null ? null : repository.findUserByEmail(editEmail);
        StringBuilder rows = new StringBuilder();
        for (AppUser user : repository.findUsers()) {
            rows.append("<tr><td><strong>").append(WebHtml.escape(user.name())).append("</strong><div class=\"small muted\">").append(WebHtml.escape(user.email())).append("</div></td><td><span class=\"state\">").append(WebHtml.escape(user.role())).append("</span></td><td><a class=\"btn ghost small-btn\" href=\"").append(path).append("/admin?tab=users&amp;edit=").append(java.net.URLEncoder.encode(user.email(), java.nio.charset.StandardCharsets.UTF_8)).append("\">Edit</a> <form class=\"inline-form\" method=\"post\" action=\"").append(path).append("/admin\">").append(WebHtml.csrf(request)).append("<input type=\"hidden\" name=\"action\" value=\"delete-user\"><input type=\"hidden\" name=\"email\" value=\"").append(WebHtml.escape(user.email())).append("\"><button class=\"btn danger small-btn\" type=\"submit\">Delete</button></form></td></tr>");
        }
        String name = editing == null ? "" : editing.name();
        String email = editing == null ? "" : editing.email();
        String role = editing == null ? "HOMEOWNER" : editing.role();
        String emailField = editing == null ? WebHtml.input("Email address", "email", email, "email", true)
                : "<div class=\"field\"><label for=\"email\">Email address</label><input id=\"email\" name=\"email\" type=\"email\" value=\"" + WebHtml.escape(email) + "\" readonly></div>";
        return "<section class=\"panel\"><div class=\"panel-head\"><h2>Accounts</h2><span class=\"small muted\">" + repository.findUsers().size() + " accounts</span></div><div class=\"table-wrap\"><table><thead><tr><th>User</th><th>Role</th><th>Actions</th></tr></thead><tbody>" + rows + "</tbody></table></div></section>"
                + "<section class=\"panel\"><h2>" + (editing == null ? "Add an account" : "Edit account") + "</h2><form method=\"post\" action=\"" + path + "/admin\">" + WebHtml.csrf(request) + "<input type=\"hidden\" name=\"action\" value=\"save-user\"><div class=\"form-grid\">"
                + WebHtml.input("Full name", "name", name, "text", true) + emailField
                + "<div class=\"field\"><label for=\"role\">Role</label><select id=\"role\" name=\"role\"><option value=\"HOMEOWNER\" " + ("HOMEOWNER".equals(role) ? "selected" : "") + ">Homeowner</option><option value=\"ADMIN\" " + ("ADMIN".equals(role) ? "selected" : "") + ">Admin</option></select></div>"
                + WebHtml.input(editing == null ? "Password (8+ characters)" : "New password (leave blank to keep current)", "password", "", "password", editing == null)
                + "</div><div class=\"actions\"><button class=\"btn\" type=\"submit\">" + (editing == null ? "Create account" : "Save account") + "</button>" + (editing == null ? "" : "<a class=\"btn ghost\" href=\"" + path + "/admin?tab=users\">Cancel</a>") + "</div></form></section>";
    }

    private String devicesPage(HttpServletRequest request, HomeRepository repository) {
        StringBuilder rows = new StringBuilder();
        String path = WebHtml.escape(request.getContextPath());
        for (SmartDevice device : repository.findDevices()) {
            rows.append("<tr><td><strong>").append(WebHtml.escape(device.getName())).append("</strong></td><td>").append(WebHtml.escape(device.getRoom())).append("</td><td>").append(WebHtml.escape(device.getKind())).append("</td><td><span class=\"state ").append(device.isCompatible() ? "" : "pending").append("\">").append(device.isCompatible() ? "Approved" : "Pending").append("</span></td><td><form class=\"inline-form\" method=\"post\" action=\"").append(path).append("/admin\">").append(WebHtml.csrf(request)).append("<input type=\"hidden\" name=\"action\" value=\"compatibility\"><input type=\"hidden\" name=\"deviceId\" value=\"").append(WebHtml.escape(device.getId())).append("\"><button class=\"btn secondary small-btn\" name=\"compatible\" value=\"true\" type=\"submit\">Approve</button><button class=\"btn danger small-btn\" name=\"compatible\" value=\"false\" type=\"submit\">Reject</button></form></td></tr>");
        }
        return "<section class=\"panel\"><div class=\"panel-head\"><h2>Device compatibility</h2><span class=\"small muted\">Approval enables homeowner controls.</span></div><div class=\"table-wrap\"><table><thead><tr><th>Device</th><th>Room</th><th>Type</th><th>Decision</th><th>Review</th></tr></thead><tbody>" + rows + "</tbody></table></div></section>";
    }

    private String settingsPage(HttpServletRequest request, HomeRepository repository) {
        String path = WebHtml.escape(request.getContextPath());
        return "<section class=\"panel\"><div class=\"panel-head\"><h2>Home settings</h2><span class=\"state\">Saved in SQLite</span></div><form method=\"post\" action=\"" + path + "/admin\">" + WebHtml.csrf(request) + "<input type=\"hidden\" name=\"action\" value=\"settings\"><div class=\"form-grid\">"
                + WebHtml.input("Home name", "homeName", repository.getSetting("home_name", "Alex's Home"), "text", true)
                + WebHtml.input("Temperature alert limit (10–40 °C)", "temperatureLimit", repository.getSetting("temperature_limit", "28"), "number", true)
                + "</div><div class=\"actions\"><button class=\"btn\" type=\"submit\">Save settings</button></div></form></section><section class=\"panel\"><h2>About this demo</h2><p class=\"muted\">The temperature monitor updates in the background. This project simulates the home network and does not send commands to physical devices.</p></section>";
    }

    private void saveUser(HttpServletRequest request, HomeRepository repository) {
        String name = value(request, "name"), email = value(request, "email"), role = value(request, "role").toUpperCase(java.util.Locale.ROOT), passwordText = request.getParameter("password");
        if (name.isBlank() || !email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$") || !(role.equals("HOMEOWNER") || role.equals("ADMIN"))) throw new IllegalArgumentException("Enter a valid name, email address, and role.");
        AppUser existing = repository.findUserByEmail(email);
        AppUser activeUser = (AppUser) request.getSession().getAttribute(WebSecurityFilter.USER);
        if (existing != null && activeUser.email().equalsIgnoreCase(existing.email()) && !role.equals(existing.role())) {
            throw new IllegalArgumentException("You cannot change the role of the account you are using.");
        }
        if (existing != null && "ADMIN".equals(existing.role()) && "HOMEOWNER".equals(role)) {
            long admins = repository.findUsers().stream().filter(user -> "ADMIN".equals(user.role())).count();
            if (admins <= 1) throw new IllegalArgumentException("The last administrator account cannot lose its admin role.");
        }
        if (existing == null && (passwordText == null || passwordText.length() < 8)) throw new IllegalArgumentException("New accounts need a password with at least 8 characters.");
        String hash = existing == null ? "" : existing.passwordHash();
        if (passwordText != null && !passwordText.isBlank()) {
            if (passwordText.length() < 8) throw new IllegalArgumentException("Passwords must contain at least 8 characters.");
            char[] chars = passwordText.toCharArray();
            hash = PasswordSecurity.hash(chars);
            Arrays.fill(chars, '\0');
        }
        repository.saveUser(new AppUser(name, email, role, hash));
        activity().record(activeName(request), existing == null ? "User created" : "User updated", name + " · " + role);
        WebHtml.flash(request, existing == null ? "Account created." : "Account updated.", false);
    }

    private void deleteUser(HttpServletRequest request, HomeRepository repository) {
        String email = value(request, "email");
        AppUser current = (AppUser) request.getSession().getAttribute(WebSecurityFilter.USER);
        AppUser target = repository.findUserByEmail(email);
        if (target == null) throw new IllegalArgumentException("That account could not be found.");
        if (current.email().equalsIgnoreCase(email)) throw new IllegalArgumentException("You cannot delete the account you are using.");
        long admins = repository.findUsers().stream().filter(user -> "ADMIN".equals(user.role())).count();
        if ("ADMIN".equals(target.role()) && admins <= 1) throw new IllegalArgumentException("The last administrator account cannot be deleted.");
        repository.deleteUser(email);
        activity().record(current.name(), "User deleted", target.name() + " · " + target.email());
        WebHtml.flash(request, "Account deleted.", false);
    }

    private void updateCompatibility(HttpServletRequest request, HomeRepository repository) {
        String id = value(request, "deviceId"), compatible = value(request, "compatible");
        if (!"true".equals(compatible) && !"false".equals(compatible)) throw new IllegalArgumentException("Choose approve or reject.");
        SmartDevice device = repository.findDevices().stream().filter(item -> item.getId().equals(id)).findFirst().orElseThrow(() -> new IllegalArgumentException("Device not found."));
        ((edu.homeautomation.service.DeviceService) getServletContext().getAttribute(HomeAppListener.DEVICES))
                .setCompatibility(device, Boolean.parseBoolean(compatible));
        WebHtml.flash(request, device.getName() + " marked " + (device.isCompatible() ? "approved." : "not compatible."), false);
    }

    private void updateSettings(HttpServletRequest request, HomeRepository repository) {
        String homeName = value(request, "homeName"), temperature = value(request, "temperatureLimit");
        if (homeName.isBlank() || homeName.length() > 80) throw new IllegalArgumentException("Enter a home name with 1 to 80 characters.");
        double limit;
        try { limit = Double.parseDouble(temperature); }
        catch (NumberFormatException exception) { throw new IllegalArgumentException("Enter a temperature limit between 10 and 40 °C."); }
        if (!Double.isFinite(limit)) throw new IllegalArgumentException("Enter a temperature limit between 10 and 40 °C.");
        EnvironmentService environment = (EnvironmentService) getServletContext().getAttribute(HomeAppListener.ENVIRONMENT);
        environment.setTemperatureLimit(limit);
        repository.saveSetting("home_name", homeName);
        repository.saveSetting("temperature_limit", Double.toString(limit));
        activity().record(activeName(request), "System settings updated", homeName + " · alert at " + limit + "°C");
        WebHtml.flash(request, "System settings saved.", false);
    }

    private edu.homeautomation.service.ActivityHistory activity() {
        return (edu.homeautomation.service.ActivityHistory) getServletContext().getAttribute(HomeAppListener.ACTIVITY);
    }

    private String activeName(HttpServletRequest request) {
        return ((AppUser) request.getSession().getAttribute(WebSecurityFilter.USER)).name();
    }

    private HomeRepository repository() { return (HomeRepository) getServletContext().getAttribute(HomeAppListener.REPOSITORY); }
    private String value(HttpServletRequest request, String key) { return request.getParameter(key) == null ? "" : request.getParameter(key).trim(); }
}
