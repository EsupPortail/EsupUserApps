package prolongationENT;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.logging.LogFactory;

import static prolongationENT.Utils.*;

public class ComputeBandeau {	   
    MainConf conf = null;
    ComputeApps computeApps;
    Stats stats;

    static String prev_host_attr = "prev_host";
    static String prev_time_attr = "prev_time";    

    class LayoutDTO {
    	public LayoutDTO(String title, Collection<AppDTO> portlets) {
			this.title = title;
			this.portlets = portlets;
		}
		String title;
    	Collection<AppDTO> portlets;
    }
    
    org.apache.commons.logging.Log log = LogFactory.getLog(ComputeBandeau.class);

    public ComputeBandeau(MainConf conf) {
    	this.conf = conf;
    	computeApps = new ComputeApps(conf);
    	stats = new Stats(conf);      
	}
    
    void layout(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
	boolean noCache = request.getParameter("noCache") != null;
	String userId = noCache ? null : get_CAS_userId(request);
	String forcedId = request.getParameter("uid");

	if (noCache || userId == null) {
	    if (request.getParameter("auth_checked") == null) {
		cleanupSession(request);
		String final_url = conf.bandeau_ENT_url + "/layout?auth_checked"
		    + (request.getQueryString() != null ? "&" + request.getQueryString() : "");
		response.sendRedirect(via_CAS(conf.cas_login_url, final_url) + "&gateway=true");
	    } else {
		// user is not authenticated.
                respond_js(response,
			   String.format(file_get_contents(request, "templates/notLogged.html"),
					 json_encode(asMap("cas_login_url", conf.cas_login_url))));
	    }
	    return;
	}

	if (forcedId != null) {
	    List<String> memberOf = computeApps.getLdapPeopleInfo(userId).get("memberOf");
	    if (conf.admins.contains(userId) ||
		memberOf != null && firstCommonElt(memberOf, conf.admins) != null) {
		// ok
	    } else {
		forcedId = null;
	    }
	}
	if (forcedId == null) forcedId = userId;
	layout(request, response, forcedId, userId);
    }
    
	void layout(HttpServletRequest request, HttpServletResponse response, String userId, String realUserId) throws ServletException, IOException {
	//prev = 0;

	Ldap.Attrs attrs = computeApps.getLdapPeopleInfo(userId);

	Map<String,AppDTO> userChannels = userChannels(userId, attrs);
	List<LayoutDTO> userLayout = userLayout(conf.LAYOUT, userChannels);

	stats.log(request, realUserId, userChannels.keySet());
	
	boolean is_old =
	    !conf.isCasSingleSignOutWorking &&
	    is_old(request) && request.getParameter("auth_checked") == null; // checking auth_checked should not be necessary since having "auth_checked" implies having gone through cleanupSession & CAS and so prev_time should not be set. But it seems firefox can bypass the initial redirect and go straight to CAS without us having cleaned the session... and then a dead-loop always asking for not-old version

	Map<String, Object> js_data =
            asMapO("user", userId)
            .add("userAttrs", exportAttrs(userId, attrs))
            .add("layout", asMap("folders", userLayout));
	if (!realUserId.equals(userId)) js_data.put("realUserId", realUserId);
        if (getCookie(request, conf.cas_impersonate.cookie_name) != null) {
            js_data.put("canImpersonate", computeApps.computeValidApps(realUserId, true));
        }

        String callback = request.getParameter("callback");
        if (callback == null) {
            // mostly for debugging purpose
	    respond_json(response, js_data);
            return;
        }

	String js_data_ = json_encode(js_data);
	String hash = computeMD5(js_data_);
	Map<String, Object> js_params =
	    asMapO("is_old", is_old)
	     .add("hash", hash);
	
	respond_js(response,
		   hash.equals(request.getParameter("if_none_match")) ?
		     "// not update needed" :
		     callback + "(\n\n" + js_data_ + ",\n\n" + json_encode(js_params) + "\n\n)");
    }

    void canImpersonate(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String uid = request.getParameter("uid");
        String service = request.getParameter("service");
        Collection<String> appIds = computeApps.canImpersonate(uid, service);
        if (appIds.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        } else {
            respond_json(response, appIds);
        }
    }
    
    void redirect(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

	String activeTab = request.getParameter("uportalActiveTab");
	String appId = null;
	String location = null;
	if (activeTab != null) {
		// gasp, there is a http:// iframe, display only one channel (nb: if the first channel is http-only, it will be displayed outside of uportal)
		appId = request.getParameter("firstId");
	} else {
	    appId = request.getParameter("id");
	    if (appId == null) throw new RuntimeException("missing 'id=xxx' parameter");
	}
	if (appId != null) { 
            App app = conf.APPS.get(appId);
    	    if (app == null) throw new RuntimeException("invalid appId " + appId);
            boolean isGuest = !hasParameter(request, "login") && !hasParameter(request, "relog");
	    location = get_url(app, appId, hasParameter(request, "guest"), isGuest, conf.current_idpAuthnRequest_url);

            // Below rely on /ProlongationENT/redirect proxied in applications.
            // Example for Apache:
            //   ProxyPass /ProlongationENT https://ent.univ.fr/ProlongationENT
            if (hasParameter(request, "relog")) {
                removeCookies(request, response, app.cookies);
            }
            if (hasParameter(request, "impersonate")) {
                String wantedUid = getCookie(request, conf.cas_impersonate.cookie_name);
                response.addCookie(newCookie("CAS_IMPERSONATED", wantedUid, app.cookies.path));
            }
	}
	response.sendRedirect(location);
    }

    void removeCookies(HttpServletRequest request, HttpServletResponse response, Cookies toRemove) {
        for (String prefix : toRemove.name_prefixes()) {
            for(Cookie c : getCookies(request)) { 
                if (!c.getName().startsWith(prefix)) continue;
                response.addCookie(newCookie(c.getName(), null, toRemove.path()));
            }
        }
        for (String name : toRemove.names()) {
            response.addCookie(newCookie(name, null, toRemove.path()));
        }
    }
        
    static long time_before_forcing_CAS_authentication_again(boolean different_referrer) {
  	return different_referrer ? 10 : 120; // seconds
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
    
    List<LayoutDTO> userLayout(Map<String, List<String>> layout, Map<String, AppDTO> userChannels) {        
	List<LayoutDTO> rslt = new ArrayList<>();
	for (Map.Entry<String, List<String>> e : layout.entrySet()) {
	    List<AppDTO> l = new ArrayList<>();
	    for (String fname : e.getValue()) {
                AppDTO app = userChannels.get(fname);
		if (app != null)
		    l.add(app);
            }
	    if (!l.isEmpty())
		rslt.add(new LayoutDTO(e.getKey(), l));
	}
 	return rslt;  
    }
    
    Map<String, AppDTO> userChannels(final String userId, Ldap.Attrs person) {
        Map<String, AppDTO> rslt = new HashMap<>();
	
	for (String fname : computeApps.computeValidApps(person, false)) {
		App app = conf.APPS.get(fname);
		rslt.put(fname, new AppDTO(fname, app, get_user_url(app, fname, conf.current_idpAuthnRequest_url)));
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

