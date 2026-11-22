package edu.homeautomation.web;

import edu.homeautomation.model.AutomationRule;
import edu.homeautomation.persistence.HomeRepository;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/** Lets homeowners create routines and turn existing routines on or off. */
public final class AutomationServlet extends HttpServlet {
    @Override protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        HomeRepository repository = repository();
        var rules = repository.findRules();
        StringBuilder body = new StringBuilder("<div class=\"page-heading\"><div><div class=\"eyebrow\">Make your home feel effortless</div><h1>Your automations</h1><p>Set a condition and an action for a routine. Routines are simulated in this project.</p></div></div>")
                .append("<div class=\"section-head\"><h2>Your routines</h2><span class=\"state\">").append(rules.size()).append(" routines</span></div><div class=\"rule-list\">");
        if (rules.isEmpty()) body.append("<div class=\"empty\">No routines yet. Add one below to get started.</div>");
        for (AutomationRule rule : rules) {
            body.append("<article class=\"rule-card\"><div class=\"rule-copy\"><div class=\"rule-icon\">⟳</div><div><div class=\"rule-name\">").append(WebHtml.escape(rule.getName())).append("</div><div class=\"rule-detail\">").append(WebHtml.escape(rule.getCondition())).append(" → ").append(WebHtml.escape(rule.getAction())).append("</div></div></div><div class=\"actions\">");
            if (rule.isEnabled()) body.append("<form method=\"post\" action=\"").append(WebHtml.escape(request.getContextPath())).append("/automations\">").append(WebHtml.csrf(request)).append("<input type=\"hidden\" name=\"action\" value=\"run\"><input type=\"hidden\" name=\"name\" value=\"").append(WebHtml.escape(rule.getName())).append("\"><button class=\"btn secondary small-btn\" type=\"submit\">Run now</button></form>");
            body.append("<form method=\"post\" action=\"").append(WebHtml.escape(request.getContextPath())).append("/automations\">").append(WebHtml.csrf(request)).append("<input type=\"hidden\" name=\"action\" value=\"toggle\"><input type=\"hidden\" name=\"name\" value=\"").append(WebHtml.escape(rule.getName())).append("\"><input type=\"hidden\" name=\"enabled\" value=\"").append(!rule.isEnabled()).append("\"><button class=\"btn ").append(rule.isEnabled() ? "secondary" : "ghost").append(" small-btn\" type=\"submit\">").append(rule.isEnabled() ? "Pause" : "Enable").append("</button></form></div></article>");
        }
        body.append("</div><div class=\"section-head\"><h2>Create a routine</h2></div><section class=\"panel\"><form method=\"post\" action=\"").append(WebHtml.escape(request.getContextPath())).append("/automations\">").append(WebHtml.csrf(request)).append("<input type=\"hidden\" name=\"action\" value=\"create\"><div class=\"form-grid\">")
                .append(WebHtml.input("Routine name", "name", "", "text", true))
                .append(WebHtml.input("When should it happen?", "condition", "", "text", true))
                .append(WebHtml.input("What should happen?", "actionText", "", "text", true))
                .append("</div><div class=\"actions\"><button class=\"btn\" type=\"submit\">Save routine</button></div></form></section>");
        WebHtml.sendPage(response, WebHtml.page(request, "Automations", "automations", body.toString()));
    }

    @Override protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        HomeRepository repository = repository();
        String action = request.getParameter("action");
        try {
            if ("create".equals(action)) {
                String name = value(request, "name"), condition = value(request, "condition"), actionText = value(request, "actionText");
                if (name.isBlank() || condition.isBlank() || actionText.isBlank()) throw new IllegalArgumentException("Add a name, condition, and action for the routine.");
                if (repository.findRules().stream().anyMatch(item -> item.getName().equalsIgnoreCase(name))) throw new IllegalArgumentException("A routine already uses that name.");
                ((edu.homeautomation.service.DeviceService) getServletContext().getAttribute(HomeAppListener.DEVICES))
                        .validateAutomationAction(actionText);
                repository.saveRule(new AutomationRule(name, condition, actionText, true));
                ((edu.homeautomation.service.ActivityHistory) getServletContext().getAttribute(HomeAppListener.ACTIVITY))
                        .record(((edu.homeautomation.model.AppUser) request.getSession().getAttribute(WebSecurityFilter.USER)).name(), "Automation created", name + " → " + actionText);
                WebHtml.flash(request, "Routine added and enabled.", false);
            } else if ("toggle".equals(action)) {
                String name = value(request, "name");
                AutomationRule rule = repository.findRules().stream().filter(item -> item.getName().equals(name)).findFirst().orElseThrow(() -> new IllegalArgumentException("That routine could not be found."));
                rule.setEnabled(Boolean.parseBoolean(request.getParameter("enabled")));
                repository.updateRule(rule);
                ((edu.homeautomation.service.ActivityHistory) getServletContext().getAttribute(HomeAppListener.ACTIVITY))
                        .record(((edu.homeautomation.model.AppUser) request.getSession().getAttribute(WebSecurityFilter.USER)).name(), "Automation updated", name + (rule.isEnabled() ? " enabled." : " paused."));
                WebHtml.flash(request, "Routine " + (rule.isEnabled() ? "enabled" : "paused") + ".", false);
            } else if ("run".equals(action)) {
                String name = value(request, "name");
                AutomationRule rule = repository.findRules().stream().filter(item -> item.getName().equals(name)).findFirst().orElseThrow(() -> new IllegalArgumentException("That routine could not be found."));
                if (!rule.isEnabled()) throw new IllegalArgumentException("Enable this routine before running it.");
                String result = ((edu.homeautomation.service.DeviceService) getServletContext().getAttribute(HomeAppListener.DEVICES))
                        .runAutomation(rule.getAction());
                WebHtml.flash(request, result, false);
            } else throw new IllegalArgumentException("Choose a valid routine action.");
        } catch (IllegalArgumentException | edu.homeautomation.model.HomeValidationException exception) {
            WebHtml.flash(request, exception.getMessage(), true);
        }
        response.sendRedirect(request.getContextPath() + "/automations");
    }

    private HomeRepository repository() { return (HomeRepository) getServletContext().getAttribute(HomeAppListener.REPOSITORY); }
    private String value(HttpServletRequest request, String key) { return request.getParameter(key) == null ? "" : request.getParameter(key).trim(); }
}
