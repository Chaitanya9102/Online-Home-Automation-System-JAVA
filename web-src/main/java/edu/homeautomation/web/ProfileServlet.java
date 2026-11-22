package edu.homeautomation.web;

import edu.homeautomation.model.AppUser;
import edu.homeautomation.persistence.HomeRepository;
import edu.homeautomation.service.PasswordSecurity;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;

/** Profile editor with current-password verification for password changes. */
public final class ProfileServlet extends HttpServlet {
    @Override protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        AppUser user = (AppUser) request.getSession().getAttribute(WebSecurityFilter.USER);
        String body = "<div class=\"page-heading\"><div><div class=\"eyebrow\">Account preferences</div><h1>Your profile</h1><p>Keep your account information up to date.</p></div></div><section class=\"panel\"><form method=\"post\" action=\"" + WebHtml.escape(request.getContextPath()) + "/profile\">" + WebHtml.csrf(request)
                + "<h2>Personal details</h2><div class=\"form-grid\">" + WebHtml.input("Name", "name", user.name(), "text", true)
                + WebHtml.input("Email address", "email", user.email(), "email", true)
                + "</div><div class=\"section-head\"><h2>Update password</h2><span class=\"small muted\">Leave all password fields blank to keep the current password.</span></div><div class=\"form-grid\">"
                + WebHtml.input("Current password", "currentPassword", "", "password", false)
                + WebHtml.input("New password (8+ characters)", "newPassword", "", "password", false)
                + WebHtml.input("Confirm new password", "confirmPassword", "", "password", false)
                + "</div><div class=\"actions\"><button class=\"btn\" type=\"submit\">Save profile</button></div></form></section>";
        WebHtml.sendPage(response, WebHtml.page(request, "My profile", "profile", body));
    }

    @Override protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        HomeRepository repository = (HomeRepository) getServletContext().getAttribute(HomeAppListener.REPOSITORY);
        AppUser current = (AppUser) request.getSession().getAttribute(WebSecurityFilter.USER);
        String name = value(request, "name"), email = value(request, "email");
        String currentPassword = request.getParameter("currentPassword"), newPassword = request.getParameter("newPassword"), confirm = request.getParameter("confirmPassword");
        try {
            if (name.isBlank() || !email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) throw new IllegalArgumentException("Enter your name and a valid email address.");
            AppUser collision = repository.findUserByEmail(email);
            if (collision != null && !collision.email().equalsIgnoreCase(current.email())) throw new IllegalArgumentException("That email address is already used by another account.");
            boolean changePassword = (newPassword != null && !newPassword.isBlank()) || (currentPassword != null && !currentPassword.isBlank()) || (confirm != null && !confirm.isBlank());
            String passwordHash = current.passwordHash();
            if (changePassword) {
                char[] currentChars = currentPassword == null ? new char[0] : currentPassword.toCharArray();
                boolean verified = PasswordSecurity.verify(currentChars, current.passwordHash());
                Arrays.fill(currentChars, '\0');
                if (!verified) throw new IllegalArgumentException("Enter your current password to make a password change.");
                if (newPassword == null || newPassword.length() < 8 || !newPassword.equals(confirm)) throw new IllegalArgumentException("Use at least 8 characters and make sure the new passwords match.");
                char[] chars = newPassword.toCharArray();
                passwordHash = PasswordSecurity.hash(chars);
                Arrays.fill(chars, '\0');
            }
            AppUser updated = new AppUser(name, email, current.role(), passwordHash);
            repository.saveUser(updated);
            if (!current.email().equalsIgnoreCase(updated.email())) repository.deleteUser(current.email());
            request.getSession().setAttribute(WebSecurityFilter.USER, updated);
            WebHtml.flash(request, "Your profile has been saved.", false);
        } catch (IllegalArgumentException exception) {
            WebHtml.flash(request, exception.getMessage(), true);
        }
        response.sendRedirect(request.getContextPath() + "/profile");
    }

    private String value(HttpServletRequest request, String key) { return request.getParameter(key) == null ? "" : request.getParameter(key).trim(); }
}
