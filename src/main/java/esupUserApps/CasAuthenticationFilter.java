package esupUserApps;

import org.apereo.cas.client.util.AbstractCasFilter;
import org.apereo.cas.client.util.CommonUtils;

import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

import jakarta.servlet.Filter;

import static esupUserApps.Utils.*;

public class CasAuthenticationFilter implements Filter {

    String EsupUserApps_url;
    String EsupUserApps_url_other_domain;
    String cas_login_url;
    String cas_login_url_other_domain;
    String cas_mfa_login_url;

    public CasAuthenticationFilter() {}

    @Override
    public void destroy() {}

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        EsupUserApps_url              = filterConfig.getInitParameter("EsupUserApps_url");
        EsupUserApps_url_other_domain = filterConfig.getInitParameter("EsupUserApps_url_other_domain");
        cas_login_url                 = filterConfig.getInitParameter("cas_login_url");
        cas_login_url_other_domain    = filterConfig.getInitParameter("cas_login_url_other_domain");
        cas_mfa_login_url             = filterConfig.getInitParameter("cas_mfa_login_url");
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse,
                               FilterChain filterChain) throws IOException, ServletException {

        var request = (HttpServletRequest) servletRequest;
        var response = (HttpServletResponse) servletResponse;

        var want_mfa = request.getServletPath().equals("/login-mfa");

        var session = request.getSession(false);
        var valid_session = session != null && session.getAttribute(AbstractCasFilter.CONST_CAS_ASSERTION) != null &&
            !(want_mfa && session.getAttribute("MFA") == null);

        if (valid_session || CommonUtils.isNotBlank(request.getParameter("ticket"))) {
            filterChain.doFilter(request, response);
            return;
        }

        var is_other_domain = EsupUserApps_url_other_domain != null && request.getServerName().equals(url2host(EsupUserApps_url_other_domain));
        var final_url = (is_other_domain ? EsupUserApps_url_other_domain : EsupUserApps_url)
            + request.getServletPath()
            + "?" + request.getQueryString();
        response.sendRedirect(via_CAS(want_mfa ? cas_mfa_login_url : is_other_domain ? cas_login_url_other_domain : cas_login_url, final_url));
    }


}
