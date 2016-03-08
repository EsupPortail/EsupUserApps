package prolongationENT;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.logging.LogFactory;

import static prolongationENT.Utils.*;

public class ComputeBandeau {	   
    MainConf conf = null;
    ComputeLayout handleGroups;
    Stats stats;

    static String prev_host_attr = "prev_host";
    static String prev_time_attr = "prev_time";    

    class AppsDTO extends HashMap<String, AppDTO> {}
    class LayoutDTO {
    	public LayoutDTO(String title, Collection<String> fnames) {
			this.title = title;
			this.apps = fnames;
		}
		String title;
    	Collection<String> apps;
    }
    
    org.apache.commons.logging.Log log = LogFactory.getLog(ComputeBandeau.class);

    public ComputeBandeau(MainConf conf, ComputeLayout handleGroups) {
    	this.conf = conf;
    	this.handleGroups = handleGroups;
    	stats = new Stats(conf);      
	}

	void js(HttpServletRequest request, HttpServletResponse response, String userId, String realUserId) throws ServletException, IOException {
	//prev = 0;

	Ldap.Attrs attrs = handleGroups.getLdapPeopleInfo(userId);

	AppsDTO userChannels = userChannels(userId, attrs);
	List<LayoutDTO> userLayout = userLayout(conf.LAYOUT, userChannels.keySet());

	stats.log(request, realUserId, userChannels.keySet());
	
	boolean is_old =
	    !conf.isCasSingleSignOutWorking &&
	    is_old(request) && request.getParameter("auth_checked") == null; // checking auth_checked should not be necessary since having "auth_checked" implies having gone through cleanupSession & CAS and so prev_time should not be set. But it seems firefox can bypass the initial redirect and go straight to CAS without us having cleaned the session... and then a dead-loop always asking for not-old version
	String bandeauHeader = computeBandeauHeader(request, attrs, userChannels);
	String static_js = file_get_contents(request, "static.js");

	Map<String, Object> js_conf =
	   asMapO("bandeau_ENT_url", conf.bandeau_ENT_url)
	     .add("ent_logout_url", via_CAS(conf.cas_logout_url, conf.ent_base_url + "/Logout")) // nb: esup logout may not logout of CAS if user was not logged in esup portail, so forcing CAS logout in case
             .add("cas_impersonate", conf.cas_impersonate)
	     .add("time_before_checking_browser_cache_is_up_to_date", conf.time_before_checking_browser_cache_is_up_to_date);


	Map<String, Object> js_data =
	   asMapO("person", exportAttrs(userId, attrs))
	     .add("bandeauHeader", bandeauHeader)
	     .add("apps", userChannels)
	     .add("layout", userLayout);
	if (!realUserId.equals(userId)) js_data.put("realUserId", realUserId);
        if (getCookie(request, conf.cas_impersonate.cookie_name) != null) {
            js_data.put("canImpersonate", handleGroups.computeValidApps(realUserId, true));
        }

	Map<String, String> js_css =
	    asMap("base",    get_css_with_absolute_url(request, "main.css"))
	     .add("desktop", get_css_with_absolute_url(request, "desktop.css"));
	    
	String js_text_middle = static_js;
	js_text_middle = js_text_middle.replace("var CONF = undefined", "var CONF = " + json_encode(js_conf));
	js_text_middle = js_text_middle.replace("var DATA = undefined", "var DATA = " + json_encode(js_data));
	js_text_middle = js_text_middle.replace("var CSS = undefined", "var CSS = " + json_encode(js_css));

	String hash = computeMD5(js_text_middle);
	Map<String, Object> js_params =
	    asMapO("is_old", is_old)
	     .add("hash", hash);

	String js_text = js_text_middle.replace("var PARAMS = undefined", "var PARAMS = " + json_encode(js_params));

	response.setContentType("application/javascript; charset=utf8");
	PrintWriter out = response.getWriter();
	
	if (hash.equals(request.getParameter("if_none_match"))) {
	    out.println("// not update needed");
	    return;
	}

	out.println("window.bandeau_ENT.notFromLocalStorage = true;");

	if (conf.disableLocalStorage) {
	    out.println(js_text);
	    js_text = "";
	}
	out.println("window.bandeau_ENT.js_text = " + json_encode(js_text) + ";");
	out.println("eval(window.bandeau_ENT.js_text);");
    }
        
    static long time_before_forcing_CAS_authentication_again(boolean different_referrer) {
  	return different_referrer ? 10 : 120; // seconds
    }

    String computeBandeauHeaderLinks(HttpServletRequest request, Ldap.Attrs user, Map<String,AppDTO> validApps) {
	String template = file_get_contents(request, "templates/headerLinks.html");
	String myAccount = file_get_contents(request, "templates/headerLinkMyAccount.html");

	String login = user.containsKey("supannAliasLogin") ? user.get("supannAliasLogin").get(0) : user.get("uid").get(0);
	return String.format(template,
			     user.containsKey("displayName") ? user.get("displayName").get(0) : user.get("mail").get(0), 
			     user.containsKey("displayName") ? user.get("mail").get(0) + " (" + login + ")" : login, 
			     myAccount);
    }

    String computeBandeauHeader(HttpServletRequest request, Ldap.Attrs user, AppsDTO userChannels) {
	String template = file_get_contents(request, "templates/header.html");

	String portalPageBarLinks = user != null ? computeBandeauHeaderLinks(request, user, userChannels) : "";

	return String.format(template, portalPageBarLinks);
    }
    
    String get_css_with_absolute_url(HttpServletRequest request, String css_file) {
	String s = file_get_contents(request, css_file);
	return s.replaceAll("(url\\(['\" ]*)(?!['\" ])(?!https?:|/)", "$1" + conf.bandeau_ENT_url + "/");
    }
   
    /* ******************************************************************************** */
    /* heuristics to detect if user may have changed (unneeded if CAS single logout?) */
    /* ******************************************************************************** */   
    boolean referer_hostname_changed(HttpServletRequest request, HttpSession session) {
	String referer = (String) request.getHeader("Referer");
	if (referer == null) return false;

	String current_host = url2host(referer);
	debug_msg("current_host " + current_host);
	boolean changed = false;
	String prev_host = (String) session.getAttribute(prev_host_attr);
	if (prev_host != null) {
	    debug_msg("prev_host " + prev_host);
	    changed = !prev_host.equals(current_host);
	    if (changed) debug_msg("referer_hostname_changed: previous=" + session.getAttribute(prev_host_attr) + " current=" + current_host);
	}
	session.setAttribute(prev_host_attr, current_host);
	return changed;
    }

    boolean is_old(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
	long max_age = time_before_forcing_CAS_authentication_again(referer_hostname_changed(request, session));
	long now = now();
	boolean is_old = false;
	if (session.getAttribute(prev_time_attr) != null) {
	    long age = now - (Long) session.getAttribute(prev_time_attr);
	    is_old = age > max_age;
	    if (is_old) debug_msg("response is potentially old: age is " + age + " (more than " + max_age + ")");
	} else {
	    session.setAttribute(prev_time_attr, now);
	}
	return is_old;
    }
    
    /* ******************************************************************************** */
    /* compute user's layout & channels */
    /* ******************************************************************************** */   
    Ldap.Attrs exportAttrs(String userId, Ldap.Attrs attrs) {
	Ldap.Attrs user = new Ldap.Attrs();
	for (String attr: conf.wanted_user_attributes) {
	    List<String> val = attrs.get(attr);
	    if (val != null)
		user.put(attr, val);
	}
	user.put("id", java.util.Collections.singletonList(userId));
	return user;
    }
    
    List<LayoutDTO> userLayout(Map<String, List<String>> layout, Set<String> userChannels) {
	List<LayoutDTO> rslt = new ArrayList<>();
	for (Map.Entry<String, List<String>> e : layout.entrySet()) {
	    List<String> fnames = new ArrayList<>();
	    for (String fname : e.getValue())
		if (userChannels.contains(fname))
		    fnames.add(fname);
	    if (!fnames.isEmpty())
		rslt.add(new LayoutDTO(e.getKey(), fnames));
	}
 	return rslt;  
    }
    
    AppsDTO userChannels(final String userId, Ldap.Attrs person) {
        AppsDTO rslt = new AppsDTO();
	
	for (String fname : handleGroups.computeValidApps(person, false)) {
		App app = conf.APPS.get(fname);
		rslt.put(fname, new AppDTO(app, get_user_url(app, fname, conf.current_idpAuthnRequest_url)));
	  }
	return rslt;
    }
    
    /* ******************************************************************************** */
    /* generate links */
    /* ******************************************************************************** */   
    String ent_url(App app, String fname, boolean isGuest, boolean noLogin, String idpAuthnRequest_url) {
	String url = isGuest ? conf.ent_base_url_guest + "/Guest" : conf.ent_base_url + (noLogin ? "/render.userLayoutRootNode.uP" : "/MayLogin");
	return url + "?uP_fname=" + fname;
    }

    // quick'n'dirty version: it expects a simple mapping from url to SP entityId and SP SAML v1 url
    static String via_idpAuthnRequest_url(String idpAuthnRequest_url, String url, String shibbolethSPPrefix) {
	String spId = url.replaceFirst("(://[^/]*)(.*)", "$1");
	String shire = spId + shibbolethSPPrefix + "Shibboleth.sso/SAML/POST";
	return String.format("%s?shire=%s&target=%s&providerId=%s", idpAuthnRequest_url, shire, urlencode(url), spId);
    }

    static String url_maybe_adapt_idp(String idpAuthnRequest_url, String url, String shibbolethSPPrefix) {
        if (idpAuthnRequest_url != null && shibbolethSPPrefix != null) {
            String realUrl = url;
            url = via_idpAuthnRequest_url(idpAuthnRequest_url, url, shibbolethSPPrefix);
            
            // HACK for test ProlongationENT: handle apps using production federation
            if (!realUrl.contains("test")) url = url.replace("idp-test", "idp");
	    //debug_msg("personalized shib url is now " + url);
	}
	return url;
    }

    String get_url(App app, String appId, boolean isGuest, boolean noLogin, String idpAuthnRequest_url) {
	String url = app.url;
	//log.warn(json_encode(app));
	if (url != null && (!conf.apps_no_bandeau.contains(appId) || idpAuthnRequest_url != null && conf.url_bandeau_compatible.contains(appId))) {
	    url = url_maybe_adapt_idp(idpAuthnRequest_url, app.url, app.shibbolethSPPrefix);
	    return url;
	} else {
	    return ent_url(app, appId, isGuest, noLogin, null);
	}
    }

    String get_user_url(App app, String appId, String idpAuthnRequest_url) {
	return get_url(app, appId, false, false, idpAuthnRequest_url);
    }
    
    void cleanupSession(HttpServletRequest request) {
	HttpSession session = request.getSession(false);
	if (session != null) {
	    session.removeAttribute(prev_time_attr);
	    session.removeAttribute(prev_host_attr);
	}
    }

    /* ******************************************************************************** */
    /* simple helper functions */
    /* ******************************************************************************** */   
    void debug_msg(String msg) {
	//log.warn("DEBUG " + msg);
    }
        
    private static Utils.MapBuilder<Object> asMapO(String k, Object v) {
	return asMap(k, v);
    }

    /*
    long prev = 0;
    void logDelta(String step) {
	long current = System.currentTimeMillis();
	if (prev != 0) {
	    log.warn("TIMING: " + step + ": " + (current - prev) + "ms");
	}
	prev = current;
    } 
    */   
	
}

