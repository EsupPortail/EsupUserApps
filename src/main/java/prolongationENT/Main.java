package prolongationENT;

import java.io.IOException;
import java.util.List;
import java.util.HashSet;
import java.util.Set;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.apache.commons.logging.LogFactory;

public class Main extends HttpServlet {	   
    MainConf conf = null;
    ComputeLayout handleGroups;
	ComputeBandeau computeBandeau;
    
    org.apache.commons.logging.Log log = LogFactory.getLog(Main.class);

    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
	if (request.getServletPath().endsWith("detectReload")) {
	    detectReload(request, response);
	} else if (request.getServletPath().endsWith("logout")) {
	    logout(request, response);
	} else if (request.getServletPath().endsWith("canImpersonate")) {
	    canImpersonate(request, response);
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
	String userId = noCache ? null : Utils.get_CAS_userId(request);
	String app = request.getParameter("app");
	String res = request.getParameter("res");
	String time = request.getParameter("time");
	String forcedId = request.getParameter("uid");

	if (noCache || userId == null) {
	    if (request.getParameter("auth_checked") == null) {
		computeBandeau.cleanupSession(request);
		String final_url = conf.bandeau_ENT_url + "/js?auth_checked"
		    + (app != null ? "&app=" + urlencode(app) : "")
		    + (res != null ? "&res=" + urlencode(res) : "")
		    + (time != null ? "&time=" + urlencode(time) : "")
		    + (forcedId != null ? "&uid=" + urlencode(forcedId) : "");
		response.sendRedirect(Utils.via_CAS(conf.cas_login_url, final_url) + "&gateway=true");
	    } else {
		// user is not authenticated.
                String template = Utils.file_get_contents(request, "templates/notLogged.html");
                response.setContentType("application/javascript; charset=utf8");
                response.getWriter().println(String.format(template, json_encode(asMap("cas_login_url", conf.cas_login_url))));
	    }
	    return;
	}

	if (forcedId != null) {
	    List<String> memberOf = handleGroups.getLdapPeopleInfo(userId).get("memberOf");
	    if (conf.admins.contains(userId) ||
		memberOf != null && Utils.firstCommonElt(memberOf, conf.admins) != null) {
		// ok
	    } else {
		forcedId = null;
	    }
	}
	if (forcedId == null) forcedId = userId;
	computeBandeau.js(request, response, forcedId, userId);
    }
    
    void detectReload(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
	long five_days = 432000;
	response.setDateHeader("Expires", (now() + five_days) * 1000L);
	response.setContentType("application/javascript; charset=utf8");
	response.getWriter().println("window.bandeau_ENT_detectReload(" + now() + ");");
    }
    
    void logout(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
	Utils.session_invalidate(request);

	String callback = request.getParameter("callback");
	response.setContentType("application/javascript; charset=utf8");
	response.getWriter().println(callback + "();");
    }
    
    void canImpersonate(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String uid = request.getParameter("uid");
        String service = request.getParameter("service");
        Set<String> appIds = handleGroups.computeValidApps(uid, true);
        if (service != null) {
            // cleanup url
	    service = service.replace(":443/", "/");
            for (String appId : new HashSet<>(appIds)) {
                App app = conf.APPS.get(appId);
                boolean keep = app.serviceRegex != null && service.matches(app.serviceRegex);
                if (!keep) appIds.remove(appId);
            }
        }

        if (appIds.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        } else {
            response.setContentType("application/json; charset=utf8");
            response.getWriter().println(json_encode(appIds));
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
	    location = computeBandeau.get_url(app, appId, hasParameter(request, "guest"), isGuest, null);

            // Below rely on /ProlongationENT/redirect proxied in applications.
            // Example for Apache:
            //   ProxyPass /ProlongationENT https://ent.univ.fr/ProlongationENT
            if (hasParameter(request, "relog")) {
                removeCookies(request, response, app.cookies);
            }
            if (hasParameter(request, "impersonate")) {
                String wantedUid = Utils.getCookie(request, conf.cas_impersonate.cookie_name);
                response.addCookie(Utils.newCookie("CAS_IMPERSONATED", wantedUid, app.cookies.path));
            }
	}
	response.sendRedirect(location);
    }

    void removeCookies(HttpServletRequest request, HttpServletResponse response, Cookies toRemove) {
        for (String prefix : toRemove.name_prefixes()) {
            for(Cookie c : request.getCookies()) { 
                if (!c.getName().startsWith(prefix)) continue;
                response.addCookie(Utils.newCookie(c.getName(), null, toRemove.path()));
            }
        }
        for (String name : toRemove.names()) {
            response.addCookie(Utils.newCookie(name, null, toRemove.path()));
        }
    }
    
    synchronized void initConf(HttpServletRequest request) {
    	Gson gson = new Gson();
    	conf = gson.fromJson(getConf(request, "config.json"), MainConf.class);
    	conf.merge(gson.fromJson(getConf(request, "config-auth.json"), AuthConf.class));
    	conf.merge(gson.fromJson(getConf(request, "config-apps.json"), AppsConf.class).init());
        conf.init();
        handleGroups = new ComputeLayout(conf);
        computeBandeau = new ComputeBandeau(conf, handleGroups);
    }   

    String private_file_get_contents(HttpServletRequest request, String file) {
        return Utils.file_get_contents(request, "WEB-INF/" + file);
    }
    
    /* ******************************************************************************** */
    /* simple helper functions */
    /* ******************************************************************************** */   

    JsonObject getConf(HttpServletRequest request, String jsonFile) {
        String s = private_file_get_contents(request, jsonFile);
        // allow trailing commas
        s = s.replaceAll(",(\\s*[\\]}])", "$1");
    	return new JsonParser().parse(s).getAsJsonObject();
    }

    private static String json_encode(Object o) {
	return Utils.json_encode(o);
    }
    private static String urlencode(String s) {
	return Utils.urlencode(s);
    }
    private static boolean hasParameter(HttpServletRequest request, String attrName) {
	return Utils.hasParameter(request, attrName);
    }
    private static Utils.MapBuilder<Object> asMap(String k, Object v) {
	return Utils.asMap(k, v);
    }
    private long now() { return Utils.now(); }

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

