package org.esupportail.portal.services;

import java.io.InputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
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

import org.jasig.portal.groups.IEntity;
import org.jasig.portal.security.IAuthorizationPrincipal;
import org.jasig.portal.security.IPerson;
import org.jasig.portal.services.AuthorizationService;
import org.jasig.portal.services.GroupService;
import org.jasig.portal.spring.locator.PersonAttributeDaoLocator;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import javax.xml.bind.annotation.adapters.HexBinaryAdapter;


public class ProlongationENT extends HttpServlet {	   
    JSONObject conf = null;

    String bandeau_ENT_url, ent_base_url, ent_base_url_guest;// currentIdpId;
    List<String> url_bandeau_compatible, url_bandeau_direct, wanted_user_attributes;
    String cas_login_url, cas_logout_url;
    ProlongationENTGlobalLayout globalLayout = null;

    org.apache.commons.logging.Log log = LogFactory.getLog(ProlongationENT.class);

    static String prev_host_attr = "org.esupportail.portal.services.ProlongationENT.prev_host";
    static String prev_time_attr = "org.esupportail.portal.services.ProlongationENT.prev_time";
    
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
	if (request.getServletPath().endsWith("detectReload")) {
	    detectReload(request, response);
	} else if (request.getServletPath().endsWith("logout")) {
	    logout(request, response);
	} else if (request.getServletPath().endsWith("redirect")) {
	    if (conf == null) initConf(request);
	    if (globalLayout == null) computeGlobalLayout();
	    redirect(request, response);
	} else if (request.getServletPath().endsWith("purgeCache")) {
	    log.warn("purging cache");

	    org.jasig.portal.utils.cache.CacheManagementHelper helper = new org.jasig.portal.utils.cache.CacheManagementHelper();
	    helper.setCacheManager((net.sf.ehcache.CacheManager) org.jasig.portal.spring.PortalApplicationContextLocator.getApplicationContext().getBean("cacheManager"));
	    helper.clearAllCaches();

	    initConf(request);
	    computeGlobalLayout();
	} else {
	    if (conf == null) initConf(request);
	    if (globalLayout == null) computeGlobalLayout();
	    js(request, response);	   
	}
    }
    
    void js(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
	boolean noCache = request.getParameter("noCache") != null;
	String userId = noCache ? null : get_CAS_userId(request);
	String app = request.getParameter("app");
	String forcedId = request.getParameter("uid");

	if (noCache || userId == null) {
	    if (request.getParameter("auth_checked") == null) {
		cleanupSession(request);
		String final_url = bandeau_ENT_url + "/js?auth_checked=true"
		    + (app != null ? "&app=" + urlencode(app) : "")
		    + (forcedId != null ? "&uid=" + urlencode(forcedId) : "");
		response.sendRedirect(via_CAS(cas_login_url, final_url) + "&gateway=true");
	    } else {
		// user is not authenticated. Empty response
	    }
	    return;
	}

	if (forcedId != null) {
	    List<String> memberOf = getUser(userId).get("memberOf");
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
	Map<String,List<String>> user = getUser(userId);

	ProlongationENTGlobalLayout globalLayout = this.globalLayout;
	Map<String,Map<String,String>> userChannels = userChannels(globalLayout.allChannels, userId);
	List<Map<String, Object>> userLayout = userLayout(globalLayout.layout, userChannels.keySet());

	stats(request, realUserId, userChannels.keySet());
	
	boolean is_old =
	    !conf.getBoolean("isCasSingleSignOutWorking") &&
	    is_old(request) && request.getParameter("auth_checked") == null; // checking auth_checked should not be necessary since having "auth_checked" implies having gone through cleanupSession & CAS and so prev_time should not be set. But it seems firefox can bypass the initial redirect and go straight to CAS without us having cleaned the session... and then a dead-loop always asking for not-old version
	String bandeauHeader = computeBandeauHeader(request, user, userChannels);
	String static_js = file_get_contents(request, "static.js");

	Map<String, Object> js_conf =
	    array("bandeau_ENT_url", bandeau_ENT_url,
		  "ent_logout_url", via_CAS(cas_logout_url, ent_base_url + "/Logout"), // nb: esup logout may not logout of CAS if user was not logged in esup portail, so forcing CAS logout in case
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
	
	Map<String, Object> js_params =
	    array("is_old", is_old,
		  "hash", computeMD5(js_text_middle));

	String js_text = js_text_middle.replace("var PARAMS = undefined", "var PARAMS = " + json_encode(js_params));

	response.setContentType("application/javascript; charset=utf8");
	PrintWriter out = response.getWriter();
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
	    if (!tab_has_non_https_url(activeTab, hasParameter(request, "guest"))) {
		// ok: let uportal display all channels
		location = ent_tab_url(activeTab);
	    } else {
		// gasp, there is a http:// iframe, display only one channel (nb: if the first channel is http-only, it will be displayed outside of uportal)
		appId = request.getParameter("firstId");
	    }
	} else {
	    appId = request.getParameter("id");
	    if (appId == null) throw new RuntimeException("missing 'id=xxx' parameter");
	}
	if (appId != null) { 
	    Map<String,String> app = get_app(appId);
	    if (app == null) throw new RuntimeException("invalid appId " + appId);
	    location = get_url(app, appId, hasParameter(request, "guest"), !hasParameter(request, "login"), null);
	}
	response.sendRedirect(location);
    }
    
    static long time_before_forcing_CAS_authentication_again(boolean different_referrer) {
  	return different_referrer ? 10 : 120; // seconds
    }

    ProlongationENTGlobalLayout getGlobalLayout() {
	return globalLayout;
    }

    // better block until we compute one global layout instead of letting different threads compute the same thing concurrently
    synchronized void computeGlobalLayout() {
	globalLayout = new ProlongationENTGlobalLayout();
    }

    synchronized void initConf(HttpServletRequest request) {
	conf = JSONObject.fromObject(file_get_contents(request, "config.json"));

	ent_base_url       = conf.getString("ent_base_url");
	ent_base_url_guest = conf.getString("ent_base_url_guest");
	//currentIdpId       = conf.getString("currentIdpId");

	url_bandeau_compatible = getConfList("url_bandeau_compatible");
	url_bandeau_direct     = getConfList("url_bandeau_direct");

	wanted_user_attributes = getConfList("wanted_user_attributes");

	bandeau_ENT_url    = ent_base_url + "/ProlongationENT";

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
	return request.getSession().getServletContext().getResourceAsStream("/ProlongationENT/" + file);
    }

    String file_get_contents(HttpServletRequest request, String file) {
	try {
	    return IOUtils.toString(file_get_stream(request, file), "UTF-8");
	} catch (IOException e) {
	    log.error("error reading file " + file, e);
	    return null;
	}
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
	String app = request.getParameter("app");
	if (app == null) return;
	String[] app_ = app.split(",");
	List<String> apps = intersect(app_, userChannels);
	log.info(
		"[" + new java.util.Date() + "] " +
		"[IP:" + request.getRemoteAddr() + "] " +
		"[ID:" + userId + "] " +
		"[APP:" + (apps.isEmpty() ? app_[0] : apps.get(0)) + "] " +
		"[URL:" + request.getHeader("Referer") + "] " +
		"[USER-AGENT:" + request.getHeader("User-Agent") +"]");
    }
    
    /* ******************************************************************************** */
    /* compute user's layout & channels using uportal API */
    /* ******************************************************************************** */   
    Map<String,List<String>> getUser(String userId) {
	Map<String,List<Object>> attrs = PersonAttributeDaoLocator.getPersonAttributeDao().getPerson(userId).getAttributes();

	Map<String,List<String>> user = new HashMap<String,List<String>>();
	for (String attr: wanted_user_attributes) {
	    List<Object> val = attrs.get(attr);
	    if (val != null)
		user.put(attr, (List) val);
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
    
    Map<String,Map<String,String>> userChannels(Map<Long,Map<String,String>> channels, final String userId) {
	String idpAuthnRequest_url = null;
	
        Map<String,Map<String,String>> rslt = new HashMap<String,Map<String,String>>();

	if (userId == null) return rslt;
	
	IEntity user = GroupService.getEntity(userId, IPerson.class);

	//EntityIdentifier ei = user.getEntityIdentifier();
	//IAuthorizationPrincipal ap = AuthorizationService.instance().newPrincipal(ei.getKey(), ei.getType());
	IAuthorizationPrincipal ap = AuthorizationService.instance().newPrincipal(user);

	int i = 0;
	for (Map.Entry<Long, Map<String,String>> e : channels.entrySet()) {
	    if (ap.canRender(e.getKey().intValue())) {
		// clone
		Map<String,String> def = new HashMap<String,String>(e.getValue());
		String fname = def.get("fname");
		def.remove("fname");

		def.put("url", get_user_url(def, fname, idpAuthnRequest_url));

		rslt.put(fname, def);
	    }
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
	String url = isGuest ? ent_base_url_guest + "/Guest" : ent_base_url + (noLogin ? "/render.userLayoutRootNode.uP" : "/Login");
	String uportalActiveTab = app.get(isGuest ? "uportalActiveTabGuest": "uportalActiveTab");
	String params = 
	    "?uP_fname=" + fname
	    + (uportalActiveTab != null ? "&uP_sparam=activeTab&activeTab=" + uportalActiveTab : "");
	url = url + params;
	return isGuest || noLogin ? url : 
	    idpAuthnRequest_url != null ? via_idpAuthnRequest_url(idpAuthnRequest_url, url) : via_CAS(cas_login_url, url);
    }

    // quick'n'dirty version: it expects a simple mapping from url to SP entityId and SP SAML v1 url
    String via_idpAuthnRequest_url(String idpAuthnRequest_url, String url) {
	String spId = url.replace("(://[^/]*)(.*)", "$1");
	String shire = spId + "/Shibboleth.sso/SAML/POST";
	return String.format("%s?shire=%s&target=%s&providerId=%s", idpAuthnRequest_url, urlencode(shire), urlencode(url), urlencode(spId));
    }

    String url_maybe_adapt_idp(String url, String idpAuthnRequest_url) {
	if (idpAuthnRequest_url == null) return url;
	String currentAuthnRequest = "xxxx"; //entityID_to_AuthnRequest_url[currentIdpId];
	String url_ = removePrefixOrNull(url, currentAuthnRequest);
	if (url_ != null) {
	    url = idpAuthnRequest_url + url_;
	    debug_msg("personalized shib url is now url");
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
	if (url != null && (url_bandeau_direct.contains(appId) || idpAuthnRequest_url != null && url_bandeau_compatible.contains(appId))) {
	    url = url_maybe_adapt_idp(app.get("url"), idpAuthnRequest_url);
	    return enhance_url(url, appId, app.keySet());
	} else {
	    return ent_url(app, appId, isGuest, noLogin, idpAuthnRequest_url);
	}
    }

    String get_user_url(Map<String,String> app, String appId, String idpAuthnRequest_url) {
	return get_url(app, appId, false, false, idpAuthnRequest_url);
    }

    
    Map<String,String> get_app(String appId) {
	for (Map<String,String> app : globalLayout.allChannels.values()) {
	    if (appId.equals(app.get("fname"))) {
		return app;
	    }
	}
	return null;
    }

    boolean tab_has_non_https_url(String uportalActiveTab, boolean isGuest) {
	String key = isGuest ? "uportalActiveTabGuest" : "uportalActiveTab";
	for (Map<String,String> app : globalLayout.allChannels.values()) {
	    if (uportalActiveTab.equals(app.get(key))) {
		String url = app.get("url");
		if (url != null && urldecode(url).contains("http://")) {
		    return true;
		}
	    }
	}
	return false;
    }

    String ent_tab_url(String uportalActiveTab) {
	String url = ent_base_url + "/render.userLayoutRootNode.uP";
	String params = "?uP_root=root" + "&uP_sparam=activeTab&activeTab=" + uportalActiveTab;
	return url + params;
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
    
    private static String json_encode_raw(Object o) {
	if (o instanceof String) {
	    return JSONUtils.valueToString(o);
	} else {    
	    return "" + JSONObject.fromObject(o);
	}
    }
    
    static String json_encode(Object o) {
	String s = json_encode_raw(o);
	// IE11 does not trust the js script charset and will interpret with html page charset
	// so we must escape accents since json-lib does not do it...
	s = s.replace("É", "\\u00c9");
	s = s.replace("À", "\\u00c0");
	s = s.replace("à", "\\u00e0");
	s = s.replace("â", "\\u00e2");
	s = s.replace("ä", "\\u00e4");
	s = s.replace("ç", "\\u00e7");
	s = s.replace("è", "\\u00e8");
	s = s.replace("é", "\\u00e9");
	s = s.replace("ê", "\\u00ea");
	s = s.replace("ë", "\\u00eb");
	s = s.replace("î", "\\u00ee");
	s = s.replace("ï", "\\u00ef");
	s = s.replace("ô", "\\u00f4");
	s = s.replace("ö", "\\u00f6");
	s = s.replace("ù", "\\u00f9");
	s = s.replace("û", "\\u00fb");
	s = s.replace("ü", "\\u00fc");
	return s;
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
	return ProlongationENTGlobalLayout.urldecode(s);
    }

    static private boolean hasParameter(HttpServletRequest request, String attrName) {
	return request.getParameter(attrName) != null;
    }
    
    static private String removePrefixOrNull(String s, String prefix) {
	return ProlongationENTGlobalLayout.removePrefixOrNull(s, prefix);
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

