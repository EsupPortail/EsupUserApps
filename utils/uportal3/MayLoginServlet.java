package org.jasig.portal;

/*

/MayLogin?uP_fname=xxx :
- if no session, redirects to CAS then uportal /Login
- otherwise skip login
(rationale: less useless sessions in uportal + speed-up going to another portlet via prolongationENT)
*/

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.*;

public class MayLoginServlet extends HttpServlet {
    String uportal_url = "https://esup.univ.fr";
    String cas_url = "https://cas.univ.fr/cas/login";

    public void service (HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        HttpSession s = request.getSession(false);
        String fname = request.getParameter("uP_fname");

        if (s == null || s.getAttribute("org.jasig.portal.UserPreferencesManager") == null) {
            response.sendRedirect(cas_url + "?service=" + uportal_url + "/Login%3FuP_fname%3D" + fname);
        } else {
            response.sendRedirect(uportal_url + "/render.userLayoutRootNode.uP?uP_fname=" + fname);
        }
    }

}
