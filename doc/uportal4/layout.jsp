<%
// wrapper around uportal /layout.json .
// - put it in uportal webapp directory
// - add this to ProlongationENT config.json:
//     "layout_url": "https://uportal.univ.fr/layout.jsp",
//
%><%@ page import="java.io.InputStream"
%><%@ page import="java.util.Properties" %><%!

InputStream file_get_stream(HttpServletRequest request, String file) {
    return request.getSession().getServletContext().getResourceAsStream(file);
}
Properties file_get_properties(HttpServletRequest request, String file) throws Exception {
    Properties prop = new Properties();
    prop.load(file_get_stream(request, file));
    return prop;
}
String urlencode(String s) throws Exception {
    return java.net.URLEncoder.encode(s, "utf-8");
}

%><% 
String callback = request.getParameter("callback");
String user = request.getRemoteUser();
if ((user == null || user.equals("guest")) && request.getParameter("auth_checked") == null) {
    // redirect to CAS
    String conf_file = "/WEB-INF/classes/properties/security.properties";
    String casLoginUrl = file_get_properties(request, conf_file).getProperty("org.jasig.portal.channels.CLogin.CasLoginUrl");

    String currentUrl = request.getServletPath() + "?auth_checked&" + request.getQueryString();
    String loginParam = "?refUrl=" + urlencode(currentUrl);
    response.sendRedirect(casLoginUrl + urlencode(loginParam) + "&gateway=true");
} else {
    response.setContentType((callback != null ? "application/javascript": "application/json") + "; charset=UTF-8");
    if (callback != null) {
	out.print(callback + "(");
	out.flush();
    }
    request.getRequestDispatcher("/layout.json").include(request, response);
    if (callback != null) out.println(")");
}

%>

