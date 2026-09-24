<%@ page session="false" %><%-- The sign-in page moved into the admin console: /admin/login --%><%
    String next = request.getParameter("next");
    response.sendRedirect(request.getContextPath() + "/admin/login"
            + (next == null ? "" : "?next=" + java.net.URLEncoder.encode(next, "UTF-8")));
%>
