package esupUserApps;

import jakarta.servlet.*;

import org.jasig.cas.client.session.SingleSignOutFilter;
import org.jasig.cas.client.validation.Cas20ProxyReceivingTicketValidationFilter;

import static esupUserApps.Utils.*;

import java.util.LinkedList;
import java.util.List;

public class WebXml implements ServletContextListener {
        
    public void contextDestroyed(ServletContextEvent event) {}

    public void contextInitialized(ServletContextEvent event) {
        configure(event.getServletContext());
    }

    private void configure(ServletContext sc) {
        // force sane value (avoid ;jessionid in urls)
        sc.setSessionTrackingModes(java.util.EnumSet.of(SessionTrackingMode.COOKIE));

        Conf.Main conf = Main.getConf(sc);

        List<String> serverNames = new LinkedList<String>();
        serverNames.add(url2host(conf.EsupUserApps_url));
        if (conf.EsupUserApps_url_other_domain != null) {
            serverNames.add(url2host(conf.EsupUserApps_url_other_domain));
        }
        serverNames.addAll(conf.EsupUserApps_vhost_aliases);
                
        addFilter(sc, "CAS Single Sign Out", SingleSignOutFilter.class, null,
                  "/layout", "/login", "/login-mfa");

        addFilter(sc, "CAS Authentication", CasAuthenticationFilter.class,
                  asMap("cas_login_url", conf.cas_login_url)
                   .add("cas_login_url_other_domain", conf.cas_login_url_other_domain)
                   .add("cas_mfa_login_url", conf.cas_mfa_login_url)
                   .add("EsupUserApps_url", conf.EsupUserApps_url)
                   .add("EsupUserApps_url_other_domain", conf.EsupUserApps_url_other_domain),
                  "/login", "/login-mfa");

        addFilter(sc, "CAS Validate", Cas20ProxyReceivingTicketValidationFilter.class,
                  asMap("casServerUrlPrefix", conf.cas_base_url)
                   .add("serverName", String.join(" ", serverNames))
                   .add("redirectAfterValidation", "false"), 
                  "/layout", "/login");
    
        addFilter(sc, "CAS MFA Validate", Cas20ProxyReceivingTicketValidationFilter.class,
                  asMap("casServerUrlPrefix", conf.cas_mfa_base_url)
                   .add("serverName", String.join(" ", serverNames))
                   .add("redirectAfterValidation", "false"), 
                  "/login-mfa");
    
        addServlet(sc, "EsupUserApps", Main.class, null, Main.mappings);
    }
}
