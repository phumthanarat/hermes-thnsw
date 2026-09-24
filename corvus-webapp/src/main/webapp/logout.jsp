<%@ page session="false" %><%--
  Signs out of the admin console and returns to the sign-in page.
--%><%
    request.logout();
    HttpSession session = request.getSession(false);
    if (session != null) {
        session.invalidate();
    }
    response.sendRedirect(request.getContextPath() + "/login.jsp?signed_out=1");
%>
