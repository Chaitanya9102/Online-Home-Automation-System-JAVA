package edu.homeautomation.web;

import edu.homeautomation.model.AppUser;
import edu.homeautomation.persistence.HomeRepository;
import edu.homeautomation.service.PasswordSecurity;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/** Handles homeowner registration, sign-in, and sign-out. */
public final class AuthServlet extends HttpServlet {
    @Override protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String view = request.getPathInfo();
        if (view == null || "/".equals(view) || "/login".equals(view)) {
            WebHtml.sendPage(response, loginPage(request));
        } else if ("/register".equals(view)) {
            WebHtml.sendPage(response, registerPage(request));
        } else {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    @Override protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        String action = request.getPathInfo();
        HomeRepository repository = (HomeRepository) getServletContext().getAttribute(HomeAppListener.REPOSITORY);
        if ("/login".equals(action)) {
            login(request, response, repository);
        } else if ("/register".equals(action)) {
            register(request, response, repository);
        } else if ("/logout".equals(action)) {
            if (request.getSession(false) != null) request.getSession(false).invalidate();
            response.sendRedirect(request.getContextPath() + "/auth/login");
        } else {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    private void login(HttpServletRequest request, HttpServletResponse response, HomeRepository repository) throws IOException {
        String email = value(request, "email");
        char[] password = request.getParameter("password") == null ? new char[0] : request.getParameter("password").toCharArray();
        AppUser user = repository.findUserByEmail(email);
        boolean accepted = user != null && PasswordSecurity.verify(password, user.passwordHash());
        java.util.Arrays.fill(password, '\0');
        if (!accepted) {
            WebHtml.flash(request, "That email and password do not match. Check them and try again.", true);
            response.sendRedirect(request.getContextPath() + "/auth/login");
            return;
        }
        request.changeSessionId();
        request.getSession().setAttribute(WebSecurityFilter.USER, user);
        activity().record(user.name(), "Sign in", "Signed in to Haven web.");
        response.sendRedirect(request.getContextPath() + "/dashboard");
    }

    private void register(HttpServletRequest request, HttpServletResponse response, HomeRepository repository) throws IOException {
        String name = value(request, "name");
        String email = value(request, "email");
        String passwordValue = request.getParameter("password");
        String confirmValue = request.getParameter("confirmPassword");
        if (name.isBlank() || !email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$") || passwordValue == null || passwordValue.length() < 8
                || !passwordValue.equals(confirmValue)) {
            WebHtml.flash(request, "Enter a name and valid email, use a password with at least 8 characters, and make sure both passwords match.", true);
            response.sendRedirect(request.getContextPath() + "/auth/register");
            return;
        }
        if (repository.findUserByEmail(email) != null) {
            WebHtml.flash(request, "An account already uses that email address. Try signing in instead.", true);
            response.sendRedirect(request.getContextPath() + "/auth/register");
            return;
        }
        char[] password = passwordValue.toCharArray();
        repository.saveUser(new AppUser(name, email, "HOMEOWNER", PasswordSecurity.hash(password)));
        activity().record(name, "Account created", "Homeowner account created.");
        java.util.Arrays.fill(password, '\0');
        WebHtml.flash(request, "Your homeowner account is ready. You can sign in now.", false);
        response.sendRedirect(request.getContextPath() + "/auth/login");
    }

    private String loginPage(HttpServletRequest request) {
        String path = WebHtml.escape(request.getContextPath());
        return authShell(request, "Welcome home", "Sign in to Haven", "Use your account to manage your home.",
                "<form class=\"auth-form\" method=\"post\" action=\"" + path + "/auth/login\">" + WebHtml.csrf(request)
                        + WebHtml.input("Email address", "email", "", "email", true)
                        + WebHtml.input("Password", "password", "", "password", true)
                        + "<button class=\"btn\" type=\"submit\">Sign in <span aria-hidden=\"true\">→</span></button></form>"
                        + "<a class=\"auth-link\" href=\"" + path + "/auth/register\">Create a homeowner account</a>"
                        + "<div class=\"demo-box\"><strong>Try the demo</strong><br>Homeowner: alex@example.com / Home123!<br>Administrator: admin@haven.local / Admin123!</div>");
    }

    private String registerPage(HttpServletRequest request) {
        String path = WebHtml.escape(request.getContextPath());
        return authShell(request, "A calmer home", "Create your account", "Set up a homeowner profile to explore the controls.",
                "<form class=\"auth-form\" method=\"post\" action=\"" + path + "/auth/register\">" + WebHtml.csrf(request)
                        + WebHtml.input("Your name", "name", "", "text", true)
                        + WebHtml.input("Email address", "email", "", "email", true)
                        + WebHtml.input("Password (8+ characters)", "password", "", "password", true)
                        + WebHtml.input("Confirm password", "confirmPassword", "", "password", true)
                        + "<button class=\"btn\" type=\"submit\">Create homeowner account <span aria-hidden=\"true\">→</span></button></form>"
                        + "<a class=\"auth-link\" href=\"" + path + "/auth/login\">Already have an account? Sign in</a>");
    }

    private String authShell(HttpServletRequest request, String eyebrow, String title, String subtext, String form) {
        String path = WebHtml.escape(request.getContextPath());
        return "<!doctype html><html lang=\"en\"><head><meta charset=\"utf-8\"><meta name=\"viewport\" content=\"width=device-width,initial-scale=1\"><title>" + WebHtml.escape(title) + " · Haven</title><link rel=\"stylesheet\" href=\"" + path + "/assets/style.css\"></head><body><main class=\"auth-page\"><section class=\"auth-story\"><div class=\"brand\"><span class=\"brand-mark\">⌂</span> HAVEN</div><div class=\"auth-decoration\"><i></i><i></i><i></i></div><h1>A calmer way to run your home.</h1><p>Control your spaces, keep an eye on your environment, and let routines take care of the little things.</p></section><section class=\"auth-card-wrap\"><div class=\"auth-card\"><div class=\"eyebrow\">" + WebHtml.escape(eyebrow) + "</div><h2>" + WebHtml.escape(title) + "</h2><p class=\"muted\">" + WebHtml.escape(subtext) + "</p>" + WebHtml.flash(request) + form + "<p class=\"footer-note\">Home device actions and environment readings are simulated.</p></div></section></main></body></html>";
    }

    private String value(HttpServletRequest request, String key) {
        return request.getParameter(key) == null ? "" : request.getParameter(key).trim();
    }

    private edu.homeautomation.service.ActivityHistory activity() {
        return (edu.homeautomation.service.ActivityHistory) getServletContext().getAttribute(HomeAppListener.ACTIVITY);
    }
}
