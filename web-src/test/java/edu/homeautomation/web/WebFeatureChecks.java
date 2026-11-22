package edu.homeautomation.web;

import edu.homeautomation.model.AppUser;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicInteger;

/** Dependency-light checks for shared HTML safety and Servlet access rules. */
public final class WebFeatureChecks {
    private WebFeatureChecks() { }

    public static void main(String[] args) throws Exception {
        check(WebHtml.escape("<script>&\"'").equals("&lt;script&gt;&amp;&quot;&#39;"), "HTML output must escape untrusted text.");
        homeownerCannotOpenAdminServletRoutes();
        System.out.println("Web feature checks passed.");
    }

    private static void homeownerCannotOpenAdminServletRoutes() throws Exception {
        var user = new AppUser("Demo Homeowner", "alex@example.com", "HOMEOWNER", "hash");
        HttpSession session = (HttpSession) Proxy.newProxyInstance(HttpSession.class.getClassLoader(), new Class[]{HttpSession.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getAttribute" -> WebSecurityFilter.USER.equals(args[0]) ? user : null;
                    case "setAttribute", "removeAttribute" -> null;
                    case "getId" -> "test-session";
                    default -> defaultValue(method.getReturnType());
                });
        HttpServletRequest request = (HttpServletRequest) Proxy.newProxyInstance(HttpServletRequest.class.getClassLoader(), new Class[]{HttpServletRequest.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "setCharacterEncoding" -> null;
                    case "getMethod" -> "GET";
                    case "getContextPath" -> "";
                    case "getRequestURI" -> "/admin";
                    case "getSession" -> session;
                    default -> defaultValue(method.getReturnType());
                });
        AtomicInteger deniedStatus = new AtomicInteger();
        HttpServletResponse response = (HttpServletResponse) Proxy.newProxyInstance(HttpServletResponse.class.getClassLoader(), new Class[]{HttpServletResponse.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("sendError")) { deniedStatus.set((Integer) args[0]); return null; }
                    return defaultValue(method.getReturnType());
                });
        AtomicInteger forwarded = new AtomicInteger();
        FilterChain chain = (request1, response1) -> forwarded.incrementAndGet();
        new WebSecurityFilter().doFilter(request, response, chain);
        check(deniedStatus.get() == HttpServletResponse.SC_FORBIDDEN, "Admin route should respond with 403 to a homeowner.");
        check(forwarded.get() == 0, "A homeowner must not reach the admin servlet.");
    }

    private static Object defaultValue(Class<?> type) {
        if (!type.isPrimitive() || type == void.class) return null;
        if (type == boolean.class) return false;
        if (type == char.class) return '\0';
        if (type == byte.class) return (byte) 0;
        if (type == short.class) return (short) 0;
        if (type == int.class) return 0;
        if (type == long.class) return 0L;
        if (type == float.class) return 0f;
        if (type == double.class) return 0d;
        return null;
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
