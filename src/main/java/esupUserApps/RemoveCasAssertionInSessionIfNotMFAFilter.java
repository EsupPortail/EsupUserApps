package esupUserApps;

import java.io.IOException;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.jasig.cas.client.util.AbstractCasFilter;

/**
 * java-cas-client AuthenticationFilter is not taking into account casServerLoginUrl to know if the user has logged
 * => this filter is used to remove non-MFA logins CAS Assertion, so that AuthenticationFilter does the right thing
 */
public class RemoveCasAssertionInSessionIfNotMFAFilter implements Filter {

    public RemoveCasAssertionInSessionIfNotMFAFilter() {
    }

    @Override
    public final void doFilter(final ServletRequest servletRequest, final ServletResponse servletResponse,
                               final FilterChain filterChain) throws IOException, ServletException {

        var request = (HttpServletRequest) servletRequest;
        var response = (HttpServletResponse) servletResponse;

        var session = request.getSession(false);
        if (session != null && session.getAttribute(AbstractCasFilter.CONST_CAS_ASSERTION) != null && session.getAttribute("MFA") == null) {
            // user is logged as non MFA, we must ignore the previous login
            session.removeAttribute(AbstractCasFilter.CONST_CAS_ASSERTION);
        }

        filterChain.doFilter(request, response);
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    @Override
    public void destroy() {
    }

}
