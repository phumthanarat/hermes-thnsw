<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" session="true" %><%--
  Sign-in page of the admin console. Credentials are checked by the
  container realm (request.login), so the same users, password digests and
  lock-out apply as before; the signed-in user is then kept in the session
  until Sign out or the session times out (web.xml session-config).
--%><%!
    private static final String[] CONSOLE_ROLES = { "admin", "corvus", "operator", "viewer" };

    private static boolean hasConsoleRole(HttpServletRequest request) {
        for (String role : CONSOLE_ROLES) {
            if (request.isUserInRole(role)) {
                return true;
            }
        }
        return false;
    }

    private static String escape(String text) {
        return text == null ? "" : text.replace("&", "&amp;").replace("<", "&lt;")
                .replace(">", "&gt;").replace("\"", "&quot;");
    }
%><%
    // same HTTPS rule as the admin console's AccessControlFilter
    if (Boolean.getBoolean("hermes.admin.requireHttps") && !request.isSecure()) {
        String port = System.getProperty("hermes.admin.httpsPort", "443");
        String query = "GET".equalsIgnoreCase(request.getMethod()) && request.getQueryString() != null
                ? "?" + request.getQueryString() : "";
        response.sendRedirect("https://" + request.getServerName() + ("443".equals(port) ? "" : ":" + port)
                + request.getRequestURI() + query);
        return;
    }
    String adminHome = request.getContextPath() + "/admin/home";
    // only ever return to a page of this console
    String next = request.getParameter("next");
    if (next == null || !next.startsWith(request.getContextPath() + "/admin/")
            || next.contains("//") || next.contains("\\")) {
        next = adminHome;
    }
    String error = null;
    if ("POST".equalsIgnoreCase(request.getMethod())) {
        String username = request.getParameter("username");
        String password = request.getParameter("password");
        try {
            if (request.getUserPrincipal() != null) {
                request.logout();
            }
            request.getSession(true);
            request.login(username == null ? "" : username.trim(), password == null ? "" : password);
            if (hasConsoleRole(request) && !request.isUserInRole("disabled")) {
                response.sendRedirect(next);
                return;
            }
            request.logout();
            error = "This account cannot use the admin console.";
        } catch (ServletException e) {
            // wrong password, unknown user or locked out: the same answer
            error = "Invalid username or password.";
        }
    }
    boolean signedOut = "1".equals(request.getParameter("signed_out"));
    boolean expired = "1".equals(request.getParameter("expired"));
%><!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>Sign in - Hermes2+</title>
<style>
  :root { --bg:#f5f7fb; --card:#fff; --text:#1f2937; --soft:#6b7280; --border:#e5e7eb; --accent:#2563eb; --danger:#b91c1c; }
  @media (prefers-color-scheme: dark) { :root { --bg:#0f172a; --card:#111827; --text:#e5e7eb; --soft:#9ca3af; --border:#1f2937; --accent:#60a5fa; --danger:#f87171; } }
  * { box-sizing: border-box; }
  body { margin:0; min-height:100vh; display:flex; align-items:center; justify-content:center; background:var(--bg); color:var(--text); font-family:-apple-system,BlinkMacSystemFont,"Segoe UI",Roboto,sans-serif; padding:16px; }
  .card { width:100%; max-width:360px; background:var(--card); border:1px solid var(--border); border-radius:12px; padding:28px; }
  .brand { display:flex; align-items:center; gap:10px; margin-bottom:20px; }
  .mark { width:36px; height:36px; border-radius:8px; background:var(--accent); color:#fff; display:flex; align-items:center; justify-content:center; font-weight:700; }
  .name { font-weight:700; } .tagline { font-size:12px; color:var(--soft); }
  label { display:block; font-size:13px; margin:14px 0 6px; }
  input[type=text], input[type=password] { width:100%; padding:9px 10px; border:1px solid var(--border); border-radius:8px; background:var(--card); color:var(--text); font-size:14px; }
  button { width:100%; margin-top:20px; padding:10px; border:0; border-radius:8px; background:var(--accent); color:#fff; font-size:14px; font-weight:600; cursor:pointer; }
  .note { font-size:13px; margin:0 0 6px; padding:8px 10px; border-radius:8px; border:1px solid var(--border); }
  .error { color:var(--danger); }
</style>
</head>
<body>
<form class="card" method="post" action="<%= escape(request.getContextPath() + "/login.jsp") %>">
  <div class="brand"><div class="mark">H2</div><div><div class="name">Hermes2+</div><div class="tagline">Administration Console</div></div></div>
  <% if (error != null) { %><p class="note error"><%= escape(error) %></p><% } %>
  <% if (signedOut) { %><p class="note">You have signed out.</p><% } %>
  <% if (expired) { %><p class="note">Your session has ended. Please sign in again.</p><% } %>
  <input type="hidden" name="next" value="<%= escape(next) %>">
  <label for="username">Username</label>
  <input type="text" id="username" name="username" autocomplete="username" autofocus required
         value="<%= escape(request.getParameter("username")) %>">
  <label for="password">Password</label>
  <input type="password" id="password" name="password" autocomplete="current-password" required>
  <button type="submit">Sign in</button>
</form>
</body>
</html>
