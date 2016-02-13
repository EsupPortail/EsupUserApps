package org.esupportail.portal.services;

import java.io.InputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import net.sf.json.util.JSONUtils;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.io.IOUtils;
import org.jasig.cas.client.util.AbstractCasFilter;
import org.jasig.cas.client.validation.Assertion;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import javax.xml.bind.annotation.adapters.HexBinaryAdapter;


public class ProlongationENT extends HttpServlet {	   
    JSONObject conf = null;

    String bandeau_ENT_url, ent_base_url, ent_base_url_guest, current_idpAuthnRequest_url;
    List<String> url_bandeau_compatible, apps_no_bandeau, wanted_user_attributes;
    String cas_login_url, cas_logout_url;
    ProlongationENTGroups handleGroups;

    org.apache.commons.logging.Log log = LogFactory.getLog(ProlongationENT.class);

    static String prev_host_attr = "org.esupportail.portal.services.ProlongationENT.prev_host";
    static String prev_time_attr = "org.esupportail.portal.services.ProlongationENT.prev_time";
    static String request_last_time_attr_prefix = "org.esupportail.portal.services.ProlongationENT.request_last_time_";
    static String visit_id_attr = "org.esupportail.portal.services.ProlongationENT.visit_id";    
    static String global_visit_nb_attr = "org.esupportail.portal.services.ProlongationENT.global_visit_nb";
    static String app_visit_nb_attr_prefix = "org.esupportail.portal.services.ProlongationENT.app_visit_nb_";
    
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
	if (request.getServletPath().endsWith("detectReload")) {
	    detectReload(request, response);
	} else if (request.getServletPath().endsWith("logout")) {
	    logout(request, response);
	} else if (request.getServletPath().endsWith("redirect")) {
	    if (conf == null) initConf(request);
	    redirect(request, response);
	} else if (request.getServletPath().endsWith("purgeCache")) {
	    log.warn("purging cache");
	    initConf(request);
	} else {
	    if (conf == null) initConf(request);
	    js(request, response);	   
	}
    }
    
    void js(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
	boolean noCache = request.getParameter("noCache") != null;
	String userId = noCache ? null : get_CAS_userId(request);
	String app = request.getParameter("app");
	String res = request.getParameter("res");
	String time = request.getParameter("time");
	String forcedId = request.getParameter("uid");

	if (noCache || userId == null) {
	    if (request.getParameter("auth_checked") == null) {
		cleanupSession(request);
		String final_url = bandeau_ENT_url + "/js?auth_checked"
		    + (app != null ? "&app=" + urlencode(app) : "")
		    + (res != null ? "&res=" + urlencode(res) : "")
		    + (time != null ? "&time=" + urlencode(time) : "")
		    + (forcedId != null ? "&uid=" + urlencode(forcedId) : "");
		response.sendRedirect(via_CAS(cas_login_url, final_url) + "&gateway=true");
	    } else {
		// user is not authenticated.
                String template = file_get_contents(request, "templates/notLogged.html");
                response.setContentType("application/javascript; charset=utf8");
                response.getWriter().println(String.format(template, json_encode(array("cas_login_url", cas_login_url))));
	    }
	    return;
	}

	if (forcedId != null) {
	    List<String> memberOf = handleGroups.getLdapPeopleInfo(userId).get("memberOf");
	    if (getConfList("admins").contains(userId) ||
		memberOf != null && !org.apache.commons.collections.CollectionUtils.intersection(memberOf, getConfList("admins")).isEmpty()) {
		// ok
	    } else {
		forcedId = null;
	    }
	}
	if (forcedId == null) forcedId = userId;
	js(request, response, forcedId, userId);
    }

    void js(HttpServletRequest request, HttpServletResponse response, String userId, String realUserId) throws ServletException, IOException {
	//prev = 0;

	Map<String,List<String>> attrs = handleGroups.getLdapPeopleInfo(userId);
	Map<String,List<String>> user = getUser(userId, attrs);

	Map<String,Map<String,String>> userChannels = userChannels(userId, attrs);
	List<Map<String, Object>> userLayout = userLayout(handleGroups.LAYOUT, userChannels.keySet());

	stats(request, realUserId, userChannels.keySet());
	
	boolean is_old =
	    !conf.getBoolean("isCasSingleSignOutWorking") &&
	    is_old(request) && request.getParameter("auth_checked") == null; // checking auth_checked should not be necessary since having "auth_checked" implies having gone through cleanupSession & CAS and so prev_time should not be set. But it seems firefox can bypass the initial redirect and go straight to CAS without us having cleaned the session... and then a dead-loop always asking for not-old version
	String bandeauHeader = computeBandeauHeader(request, user, userChannels);
	String static_js = file_get_contents(request, "static.js");

	Map<String, Object> js_conf =
	    array("bandeau_ENT_url", bandeau_ENT_url,
		  "ent_logout_url", via_CAS(cas_logout_url, ent_base_url + "/Logout"), // nb: esup logout may not logout of CAS if user was not logged in esup portail, so forcing CAS logout in case
                  "cas_impersonate", conf.get("cas_impersonate"),
		  "time_before_checking_browser_cache_is_up_to_date", conf.get("time_before_checking_browser_cache_is_up_to_date"));


	Map<String, Object> js_data =
	    array("person", user,
		  "bandeauHeader", bandeauHeader,
		  "apps", userChannels,
		  "layout", userLayout);
	if (!realUserId.equals(userId)) js_data.put("realUserId", realUserId);

	Map<String, Object> js_css =
	    array("base",    get_css_with_absolute_url(request, "main.css"),
		  "desktop", get_css_with_absolute_url(request, "desktop.css"));
	    
	String js_text_middle = static_js;
	js_text_middle = js_text_middle.replace("var CONF = undefined", "var CONF = " + json_encode(js_conf));
	js_text_middle = js_text_middle.replace("var DATA = undefined", "var DATA = " + json_encode(js_data));
	js_text_middle = js_text_middle.replace("var CSS = undefined", "var CSS = " + json_encode(js_css));

	String hash = computeMD5(js_text_middle);
	Map<String, Object> js_params =
	    array("is_old", is_old,
		  "hash", hash);

	String js_text = js_text_middle.replace("var PARAMS = undefined", "var PARAMS = " + json_encode(js_params));

	response.setContentType("application/javascript; charset=utf8");
	PrintWriter out = response.getWriter();
	
	if (hash.equals(request.getParameter("if_none_match"))) {
	    out.println("// not update needed");
	    return;
	}

	out.println("window.bandeau_ENT.notFromLocalStorage = true;");

	if (conf.getBoolean("disableLocalStorage")) {
	    out.println(js_text);
	    js_text = "";
	}
	out.println("window.bandeau_ENT.js_text = " + json_encode(js_text) + ";");
	out.println("eval(window.bandeau_ENT.js_text);");
    }
    
    void detectReload(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
	long five_days = 432000;
	response.setDateHeader("Expires", (now() + five_days) * 1000L);
	response.setContentType("application/javascript; charset=utf8");
	response.getWriter().println("window.bandeau_ENT_detectReload(" + now() + ");");
    }
    
    void logout(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
	session_invalidate(request);

	String callback = request.getParameter("callback");
	response.setContentType("application/javascript; charset=utf8");
	response.getWriter().println(callback + "();");
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
	    Map<String,String> app = get_app(appId);
	    if (app == null) throw new RuntimeException("invalid appId " + appId);
	    location = get_url(app, appId, hasParameter(request, "guest"), !hasParameter(request, "login"), current_idpAuthnRequest_url);
	}
	response.sendRedirect(location);
    }
    
    static long time_before_forcing_CAS_authentication_again(boolean different_referrer) {
  	return different_referrer ? 10 : 120; // seconds
    }

    synchronized void initConf(HttpServletRequest request) {
	String raw_conf = private_file_get_contents(request, "config.json");
	String apps_conf = private_file_get_contents(request, "config-apps.json");
	String auth_conf = private_file_get_contents(request, "config-auth.json");
	conf = JSONObject.fromObject(raw_conf);
	
	handleGroups = new ProlongationENTGroups(raw_conf, apps_conf, auth_conf);

	ent_base_url       = conf.getString("ent_base_url");
	ent_base_url_guest = conf.getString("ent_base_url_guest");
	current_idpAuthnRequest_url = conf.getString("current_idpAuthnRequest_url");

	url_bandeau_compatible = getConfList("url_bandeau_compatible");
	apps_no_bandeau     = getConfList("apps_no_bandeau");

	wanted_user_attributes = getConfList("wanted_user_attributes");

	bandeau_ENT_url    = ent_base_url_guest + "/ProlongationENT";

	cas_login_url      = conf.getString("cas_base_url") + "/login";
	cas_logout_url     = conf.getString("cas_base_url") + "/logout";		
    }   

    String computeBandeauHeaderLinkMyAccount(HttpServletRequest request, Map<String,Map<String,String>> validApps) {
	Map<String,String> def_activation = validApps.get("CActivation");
	if (def_activation == null) return "";
      
	String template = file_get_contents(request, "templates/headerLinkMyAccount.html");      
	String activation_url = get_user_url(def_activation, "CActivation", null);

	return String.format(template, activation_url);
    }

    String computeBandeauHeaderLinks(HttpServletRequest request, Map<String,List<String>> user, Map<String,Map<String,String>> validApps) {
	String template = file_get_contents(request, "templates/headerLinks.html");

	String myAccount = computeBandeauHeaderLinkMyAccount(request, validApps);

	String login = user.containsKey("supannAliasLogin") ? user.get("supannAliasLogin").get(0) : user.get("id").get(0);
	return String.format(template,
			     user.containsKey("displayName") ? user.get("displayName").get(0) : user.get("mail").get(0), 
			     user.containsKey("displayName") ? user.get("mail").get(0) + " (" + login + ")" : login, 
			     myAccount);
    }

    String computeBandeauHeader(HttpServletRequest request, Map<String,List<String>> user, Map<String,Map<String,String>> validApps) {
	String template = file_get_contents(request, "templates/header.html");

	String portalPageBarLinks = user != null ? computeBandeauHeaderLinks(request, user, validApps) : "";

	return String.format(template, portalPageBarLinks);
    }
    
    String get_css_with_absolute_url(HttpServletRequest request, String css_file) {
	String s = file_get_contents(request, css_file);
	return s.replaceAll("(url\\(['\" ]*)(?!['\" ])(?!https?:|/)", "$1" + bandeau_ENT_url + "/");
    }

    InputStream file_get_stream(HttpServletRequest request, String file) {
	return request.getSession().getServletContext().getResourceAsStream(file);
    }

    String file_get_contents(HttpServletRequest request, String file) {
	try {
            InputStream in = file_get_stream(request, "/" + file);
            if (in == null) {
                log.error("error reading file " + file);
                return null;
            }
	    return IOUtils.toString(in, "UTF-8");
	} catch (IOException e) {
	    log.error("error reading file " + file, e);
	    return null;
	}
    }

    String private_file_get_contents(HttpServletRequest request, String file) {
        return file_get_contents(request, "WEB-INF/" + file);
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

    void cleanupSession(HttpServletRequest request) {
	HttpSession session = request.getSession(false);
	if (session != null) {
	    session.removeAttribute(prev_time_attr);
	    session.removeAttribute(prev_host_attr);
	}
    }

    List<String> intersect(String[] l1, Set<String> l2) {
	List<String> r = new ArrayList<String>();
	for (String e : l1)
	    if (l2.contains(e))
		r.add(e);
	return r;	    
    }
    
    void stats(HttpServletRequest request, String userId, Set<String> userChannels) {
        HttpSession session = request.getSession(false);
	String app = request.getParameter("app");
	if (app == null) return;
	String[] app_ = app.split(",");
	List<String> apps = intersect(app_, userChannels);
        app = apps.isEmpty() ? app_[0] : apps.get(0);
        
	log.info(
		"[" + new java.util.Date() + "] " +
		"[IP:" + request.getRemoteAddr() + "] " +
		"[ID:" + userId + "] " +
		"[APP:" + app + "] " +
		"[URL:" + request.getHeader("Referer") + "] " +
		"[USER-AGENT:" + request.getHeader("User-Agent") +"] " +
		"[RES:" + request.getParameter("res") +"] " +
		"[VISIT:" + get_visit_id(session) + ":" + get_app_visit_nb(session, app) +"] " +
                "[LOAD-TIME:" + request.getParameter("time") + "]");
            
    }
    
    /* ******************************************************************************** */
    /* compute user's layout & channels using uportal API */
    /* ******************************************************************************** */   
    Map<String,List<String>> getUser(String userId, Map<String, List<String>> attrs) {
	Map<String,List<String>> user = new HashMap<String,List<String>>();
	for (String attr: wanted_user_attributes) {
	    List<String> val = attrs.get(attr);
	    if (val != null)
		user.put(attr, val);
	}
	user.put("id", java.util.Collections.singletonList(userId));
	return user;
    }
    
    List<Map<String, Object>> userLayout(Map<String, List<String>> layout, Set<String> userChannels) {
	List<Map<String, Object>> rslt = new ArrayList<Map<String, Object>>();
	for (Map.Entry<String, List<String>> e : layout.entrySet()) {
	    List<String> fnames = new ArrayList<String>();
	    for (String fname : e.getValue())
		if (userChannels.contains(fname))
		    fnames.add(fname);
	    if (!fnames.isEmpty())
		rslt.add(array("title", e.getKey(), "apps", fnames));
	}
 	return rslt;  
    }
    
    Map<String,Map<String,String>> userChannels(final String userId, Map<String, List<String>> person) {
        Map<String,Map<String,String>> rslt = new HashMap<String,Map<String,String>>();
	
	for (String fname : handleGroups.computeValidAppsRaw(person)) {
		Map<String,String> def = get_app(fname);

		def.put("url", get_user_url(def, fname, current_idpAuthnRequest_url));

		rslt.put(fname, def);
	  }
	return rslt;
    }
    
    /* ******************************************************************************** */
    /* generate links */
    /* ******************************************************************************** */   
    String via_CAS(String cas_login_url, String href) {
	return cas_login_url + "?service="  + urlencode(href);
    }

    String ent_url(Map<String,String> app, String fname, boolean isGuest, boolean noLogin, String idpAuthnRequest_url) {
	String url = isGuest ? ent_base_url_guest + "/Guest" : ent_base_url + (noLogin ? "/render.userLayoutRootNode.uP" : "/MayLogin");
	return url + "?uP_fname=" + fname;
    }

    // quick'n'dirty version: it expects a simple mapping from url to SP entityId and SP SAML v1 url
    public static String via_idpAuthnRequest_url(String idpAuthnRequest_url, String url, String shibbolethSPPrefix) {
	String spId = url.replaceFirst("(://[^/]*)(.*)", "$1");
	String shire = spId + shibbolethSPPrefix + "Shibboleth.sso/SAML/POST";
	return String.format("%s?shire=%s&target=%s&providerId=%s", idpAuthnRequest_url, shire, urlencode(url), spId);
    }

    public static String url_maybe_adapt_idp(String idpAuthnRequest_url, String url, String shibbolethSPPrefix) {
        if (idpAuthnRequest_url != null && shibbolethSPPrefix != null) {
            String realUrl = url;
            url = via_idpAuthnRequest_url(idpAuthnRequest_url, url, shibbolethSPPrefix);
            
            // HACK for test ProlongationENT: handle apps using production federation
            if (!realUrl.contains("test")) url = url.replace("idp-test", "idp");
	    //debug_msg("personalized shib url is now " + url);
	}
	return url;
    }

    String enhance_url(String url, String appId, Set<String> options) {
	if (options.contains("useExternalURLStats"))
	    url = ent_base_url + "/ExternalURLStats?fname=" + appId + "&service=" + urlencode(url);

	if (options.contains("force_CAS"))
	    url = via_CAS(cas_login_url, url);
	
	return url;
    }

    String get_url(Map<String,String> app, String appId, boolean isGuest, boolean noLogin, String idpAuthnRequest_url) {
	String url = app.get("url");
	//log.warn(json_encode(app));
	if (url != null && (!apps_no_bandeau.contains(appId) || idpAuthnRequest_url != null && url_bandeau_compatible.contains(appId))) {
	    url = url_maybe_adapt_idp(idpAuthnRequest_url, app.get("url"), app.get("shibbolethSPPrefix"));
	    return enhance_url(url, appId, app.keySet());
	} else {
	    return ent_url(app, appId, isGuest, noLogin, null);
	}
    }

    String get_user_url(Map<String,String> app, String appId, String idpAuthnRequest_url) {
	return get_url(app, appId, false, false, idpAuthnRequest_url);
    }

    
    Map<String,String> get_app(String appId) {
    	return handleGroups.getApp(appId);
    }

    /* ******************************************************************************** */
    /* simple helper functions */
    /* ******************************************************************************** */   

    List<String> getConfList(String key) {
	return JSONArray.toList(conf.getJSONArray(key));
    }   

    void debug_msg(String msg) {
	//log.warn("DEBUG " + msg);
    }
    
    // inspired from java-cas-client code
    String get_CAS_userId(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        Assertion assertion = (Assertion) (session == null ? request
					   .getAttribute(AbstractCasFilter.CONST_CAS_ASSERTION) : session
					   .getAttribute(AbstractCasFilter.CONST_CAS_ASSERTION));
        return assertion == null ? null : assertion.getPrincipal().getName();
    }

    boolean isNewVisit(HttpSession session, String app) {
        String attr = request_last_time_attr_prefix + app;
        Long last = (Long) session.getAttribute(attr);
        long current = System.currentTimeMillis();

        session.setAttribute(attr, current);

        long max_inactive = conf.getInt("visit_max_inactive") * 1000;
        return last == null || current - last > max_inactive;
    }

    Long counter(HttpSession session, String attr) {
        Long nb = (Long) session.getAttribute(attr);
        nb = (nb == null ? 0 : nb) + 1;
        session.setAttribute(attr, nb);
        return nb;
    }

    String get_visit_id(HttpSession session) {
	String id = (String) session.getAttribute(visit_id_attr);
        if (isNewVisit(session, "") || id == null) {
            id = UUID.randomUUID().toString();
            session.setAttribute(visit_id_attr, id);
        }
        return id;
    }

    Long get_app_visit_nb(HttpSession session, String app) {
        String attr = app_visit_nb_attr_prefix + app;
	Long nb = (Long) session.getAttribute(attr);
        if (isNewVisit(session, app) || nb == null) {
            nb = counter(session, global_visit_nb_attr);
            session.setAttribute(attr, nb);
        }
        return nb;
    }
    
    long now() {
	return System.currentTimeMillis() / 1000L;
    }   

    String url2host(String url) {
	try {
	    return new URL(url).getHost();
	} catch (java.net.MalformedURLException e) {
	    log.error(e, e);
	    return null;
	}
    }

    void session_invalidate(HttpServletRequest request) {
	HttpSession session = request.getSession(false);
	if (session == null) return;
	try {
	    session.invalidate();
	} catch (IllegalStateException ise) {
	    // IllegalStateException indicates session was already invalidated.
	    // This is fine.  LogoutServlet is looking to guarantee the logged out session is invalid;
	    // it need not insist that it be the one to perform the invalidating.
	}
    }

    String computeMD5(String s) {
	try {
	    //System.out.println("computing digest of " + file);
	    MessageDigest md = MessageDigest.getInstance("MD5");
	    byte[] digest = md.digest(s.getBytes());
	    return (new HexBinaryAdapter()).marshal(digest);
	} catch (NoSuchAlgorithmException e) {
	    throw new RuntimeException(e);
	}
    }
    
    private static String json_encode(Object o) {
	if (o instanceof String) {
	    return JSONUtils.valueToString(o);
	} else {    
	    return "" + JSONObject.fromObject(o);
	}
    }

    static private String urlencode(String s) {
	try {
	    return java.net.URLEncoder.encode(s, "UTF-8");
	}
	catch (java.io.UnsupportedEncodingException uee) {
	    return s;
	}
    }

    static String urldecode(String s) {
       try {
           return java.net.URLDecoder.decode(s, "UTF-8");
       }
       catch (java.io.UnsupportedEncodingException uee) {
           return s;
       }
    }

    static private boolean hasParameter(HttpServletRequest request, String attrName) {
	return request.getParameter(attrName) != null;
    }
    
    static String removePrefixOrNull(String s, String prefix) {
	return s.startsWith(prefix) ? s.substring(prefix.length()) : null;
    }

    static Map<String, Object> array(String key1, Object val1) {
	Map<String, Object> r = new HashMap<String, Object>();
	r.put(key1, val1);
	return r;
    }
    static Map<String, Object> array(String key1, Object val1, String key2, Object val2) {
	Map<String, Object> r = array(key1, val1);
	r.put(key2, val2);
	return r;
    }
    static Map<String, Object> array(String key1, Object val1, String key2, Object val2, String key3, Object val3) {
	Map<String, Object> r = array(key1, val1, key2, val2);
	r.put(key3, val3);
	return r;
    }
    static Map<String, Object> array(String key1, Object val1, String key2, Object val2, String key3, Object val3, String key4, Object val4) {
	Map<String, Object> r = array(key1, val1, key2, val2, key3, val3);
	r.put(key4, val4);
	return r;
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

