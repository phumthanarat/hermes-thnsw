<%@ page session="false" %><%-- Sign-out moved into the admin console: /admin/logout --%><%
    response.sendRedirect(request.getContextPath() + "/admin/logout");
%>
