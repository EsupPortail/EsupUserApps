package prolongationENT;

import javax.servlet.*;

import org.jasig.cas.client.session.SingleSignOutFilter;
import org.jasig.cas.client.validation.Cas20ProxyReceivingTicketValidationFilter;

import static prolongationENT.Utils.*;

public class WebXml implements ServletContextListener {
        
    public void contextDestroyed(ServletContextEvent event) {}

    public void contextInitialized(ServletContextEvent event) {
        configure(event.getServletContext());
    }

    private void configure(ServletContext sc) {
        MainConf conf = Main.getMainConf(sc);
                
        addFilter(sc, "CAS Single Sign Out", SingleSignOutFilter.class, null, "/layout");

        addFilter(sc, "CAS Validate", Cas20ProxyReceivingTicketValidationFilter.class,
                  asMap("casServerUrlPrefix", conf.cas_base_url)
                   .add("serverName", url2host(conf.prolongationENT_url))
                   .add("redirectAfterValidation", "false"), 
                  "/layout");
    
        addServlet(sc, "ProlongationENT", Main.class, null, Main.mappings);
    }
}
