package prolongationENT;

import javax.servlet.*;

import org.jasig.cas.client.session.SingleSignOutFilter;
import org.jasig.cas.client.validation.Cas20ProxyReceivingTicketValidationFilter;

public class WebXml implements ServletContextListener{
	
	public void contextDestroyed(ServletContextEvent event) {}

	public void contextInitialized(ServletContextEvent event) {
            configure(event.getServletContext());
	}

	private void configure(ServletContext sc) {
		MainConf conf = Main.getMainConf(sc);
		
		Utils.addFilter(sc, "CAS Single Sign Out", SingleSignOutFilter.class, null, "/js");

		Utils.addFilter(sc, "CAS Validate", Cas20ProxyReceivingTicketValidationFilter.class,
				Utils.asMap("casServerUrlPrefix", conf.cas_base_url)
				 	   .add("serverName", Utils.url2host(conf.bandeau_ENT_url))
				       .add("redirectAfterValidation", "false"), 
				"/js");
    
		Utils.addServlet(sc, "ProlongationENT", Main.class, null, Main.mappings);
	}
}
