package prolongationENT;

import java.io.IOException;
import java.util.List;
import java.util.HashSet;
import java.util.Set;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.apache.commons.logging.LogFactory;

import static prolongationENT.Utils.*;

public class Main extends HttpServlet {	   
    MainConf conf = null;
    String mainJs;
    String mainJsHash;
    ComputeLayout handleGroups;
	ComputeBandeau computeBandeau;
    
    org.apache.commons.logging.Log log = LogFactory.getLog(Main.class);

    static String[] mappings = new String[] { "/layout", "/loader.js", "/logout", "/detectReload", "/redirect", "/canImpersonate", "/purgeCache" };
    
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        if (conf == null) initConf(request);
        switch (request.getServletPath()) {
           case "/layout":         layout        (request, response); break;       
           case "/loader.js":      loader_js     (request, response); break;
           case "/logout":         logout        (request, response); break;
           case "/detectReload":   detectReload  (request, response); break;
           case "/redirect":       redirect      (request, response); break;
           case "/canImpersonate": canImpersonate(request, response); break;
           case "/purgeCache":     purgeCache    (request, response); break;
        }
    }
    
    void purgeCache(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
	    log.warn("purging cache");
	    initConf(request);
    }
    
    void layout(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
	boolean noCache = request.getParameter("noCache") != null;
	String userId = noCache ? null : get_CAS_userId(request);
	String forcedId = request.getParameter("uid");

	if (noCache || userId == null) {
	    if (request.getParameter("auth_checked") == null) {
		computeBandeau.cleanupSession(request);
		String final_url = conf.bandeau_ENT_url + "/layout?auth_checked"
		    + (request.getQueryString() != null ? "&" + request.getQueryString() : "");
		response.sendRedirect(via_CAS(conf.cas_login_url, final_url) + "&gateway=true");
	    } else {
		// user is not authenticated.
                String template = file_get_contents(request, "templates/notLogged.html");
                response.setContentType("application/javascript; charset=utf8");
                response.getWriter().println(String.format(template, json_encode(asMap("cas_login_url", conf.cas_login_url))));
	    }
	    return;
	}

	if (forcedId != null) {
	    List<String> memberOf = handleGroups.getLdapPeopleInfo(userId).get("memberOf");
	    if (conf.admins.contains(userId) ||
		memberOf != null && firstCommonElt(memberOf, conf.admins) != null) {
		// ok
	    } else {
		forcedId = null;
	    }
	}
	if (forcedId == null) forcedId = userId;
	computeBandeau.layout(request, response, forcedId, userId);
    }
    
    void loader_js(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        if (!mainJsHash.equals(request.getParameter("v"))) {
            // redirect to versioned loader.js which has long cache time
            int one_hour = 60 * 60;
            response.setHeader("Cache-Control", "max-age=" + one_hour);
            response.sendRedirect(conf.bandeau_ENT_url + "/loader.js?v=" + mainJsHash);
        } else {
            int one_year = 60 * 60 * 24 * 365;
            response.setHeader("Cache-Control", "max-age=" + one_year);
	    response.setHeader("Etag", mainJsHash);
            response.setContentType("application/javascript; charset=utf8");
            response.getWriter().write(mainJs);
    	}
    }
    
    void detectReload(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
	long five_days = 432000;
	response.setDateHeader("Expires", (now() + five_days) * 1000L);
	response.setContentType("application/javascript; charset=utf8");
	response.getWriter().println("window.prolongation_ENT.detectReload(" + now() + ");");
    }
    
    void logout(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
	session_invalidate(request);

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
	    location = computeBandeau.get_url(app, appId, hasParameter(request, "guest"), isGuest, conf.current_idpAuthnRequest_url);

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
    
    synchronized void initConf(HttpServletRequest request) {
    	conf = getMainConf(request.getSession().getServletContext());
        handleGroups = new ComputeLayout(conf);
        computeBandeau = new ComputeBandeau(conf, handleGroups);
        mainJs = computeBandeau.computeMainJs(request);
        mainJsHash = computeMD5(mainJs);
    }   

    static MainConf getMainConf(ServletContext sc) {
    	Gson gson = new Gson();
    	MainConf conf = gson.fromJson(getConf(sc, "config.json"), MainConf.class);
    	conf.merge(gson.fromJson(getConf(sc, "config-auth.json"), AuthConf.class));
    	conf.merge(gson.fromJson(getConf(sc, "config-apps.json"), AppsConf.class).init());
        conf.init();
        return conf;
    }

    /* ******************************************************************************** */
    /* simple helper functions */
    /* ******************************************************************************** */   

    static JsonObject getConf(ServletContext sc, String jsonFile) {
        String s = file_get_contents(sc, "WEB-INF/" + jsonFile);
        // allow trailing commas
        s = s.replaceAll(",(\\s*[\\]}])", "$1");
    	return new JsonParser().parse(s).getAsJsonObject();
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

