package edu.homeautomation.web;

import edu.homeautomation.model.AppUser;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.UUID;

/** Protects private pages and checks a per-session CSRF token on every form submission. */
public final class WebSecurityFilter implements Filter {
    static final String USER = "haven.user";
    static final String CSRF = "haven.csrf";

    @Override
    public void doFilter(ServletRequest rawRequest, ServletResponse rawResponse, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) rawRequest;
        HttpServletResponse response = (HttpServletResponse) rawResponse;
        request.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("X-Frame-Options", "DENY");
        response.setHeader("Referrer-Policy", "same-origin");

        HttpSession session = request.getSession(request.getMethod().equals("GET"));
        if (session != null && session.getAttribute(CSRF) == null) {
            session.setAttribute(CSRF, UUID.randomUUID().toString());
        }

        String path = request.getRequestURI().substring(request.getContextPath().length());
        boolean publicPath = path.startsWith("/auth/") || path.startsWith("/assets/");
        if ("POST".equalsIgnoreCase(request.getMethod())) {
            String expected = session == null ? null : (String) session.getAttribute(CSRF);
            String received = request.getParameter("_csrf");
            if (expected == null || received == null || !MessageDigest.isEqual(
                    expected.getBytes(StandardCharsets.UTF_8), received.getBytes(StandardCharsets.UTF_8))) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "The form expired. Refresh the page and try again.");
                return;
            }
        }

        AppUser user = session == null ? null : (AppUser) session.getAttribute(USER);
        if (!publicPath && user == null) {
            response.sendRedirect(request.getContextPath() + "/auth/login");
            return;
        }
        if (path.startsWith("/admin") && (user == null || !"ADMIN".equals(user.role()))) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Administrator access is required.");
            return;
        }
        if (("/device".equals(path) || "/automations".equals(path))
                && (user == null || !"HOMEOWNER".equals(user.role()))) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Homeowner access is required.");
            return;
        }
        chain.doFilter(request, response);
    }
}
