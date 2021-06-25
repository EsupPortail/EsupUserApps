package esupUserApps;

import java.io.IOException;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.gson.Gson;

import org.slf4j.LoggerFactory;

import static esupUserApps.Utils.*;

@SuppressWarnings("serial")
public class Main extends HttpServlet {           
    Conf.Main conf = null;
    ComputeBandeau computeBandeau;
    ProxyApp proxyApp;
    
    org.slf4j.Logger log = LoggerFactory.getLogger(Main.class);

    static String[] mappings = new String[] {
        "/detectReload", "/purgeCache", "/purgeUserCache",
        "/layout", "/login", "/logout", "/redirect", "/proxyApp", "/canImpersonate", "/canAccess",
        "/admin/config-apps.json",
    };
    
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        if (conf == null || conf.disableServerCache) initConf(request);
        switch (request.getServletPath()) {
            case "/detectReload":   detectReload  (request, response); break;
            case "/purgeCache":     purgeCache    (request, response); break;
            case "/purgeUserCache": purgeUserCache(request, response); break;

            case "/layout":         layout        (request, response); break;       
            case "/login":          login         (request, response); break;
            case "/logout":         logout        (request, response); break;
            case "/redirect":       redirect      (request, response); break;
            case "/canAccess":      canAccess     (request, response); break;
            case "/canImpersonate": canImpersonate(request, response); break;

            case "/proxyApp":       proxyApp     (request, response); break;

            case "/admin/config-apps.json": show_config_apps(request, response); break;
        }
    }
    
    void detectReload(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        long five_days = 60 * 60 * 24 * 5;
        respond_js(response, five_days, "window.prolongation_ENT.detectReload(" + now() + ");");
    }
    
    void purgeCache(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        log.warn("purging cache");
        initConf(request);
    }

    void purgeUserCache(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String userId = get_CAS_userId(request);
        if (userId == null) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "you must authenticate first");
        } else {
            log.warn("purging cache " + userId);
            computeBandeau.purgeUserCache(userId);
        }
    }
    
    void layout(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        computeBandeau.layout(request, response);
    }
    
    void login(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.sendRedirect(request.getParameter("target"));
    }
    
    void logout(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        session_invalidate(request);

        String callback = request.getParameter("callback");
        respond_js(response, callback + "();");
    }
    
    void canImpersonate(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        computeBandeau.canImpersonate(request, response);
    }
    
    void canAccess(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        computeBandeau.canAccess(request, response);
    }

    void redirect(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        computeBandeau.redirect(request, response);
    }

    void proxyApp(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        proxyApp.doGet(request, response);
    }

    void show_config_apps(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        if (get_CAS_userId(request) == null) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "you must authenticate first");
        } else {
            respond_json(response, objectFieldsToMap(conf, "LAYOUT", "APPS", "GROUPS"));
        }
    }
    
    synchronized void initConf(HttpServletRequest request) {
        ServletContext sc = request.getServletContext();
        conf = getConf(sc);
        computeBandeau = new ComputeBandeau(conf);
        proxyApp = new ProxyApp(conf);
    }   

    static Conf.Main getConf(ServletContext sc) {
        Gson gson = new Gson();
        Conf.Main conf = gson.fromJson(getConf(sc, "config.json", true), Conf.Main.class);
        conf.merge(gson.fromJson(getConf(sc, "config-auth.json", true), Conf.Auth.class));
        conf.merge(gson.fromJson(getConf(sc, "config-apps.json", true), Conf.Apps.class).init());
        conf.merge(gson.fromJson(getConf(sc, "config-shibboleth.json", false), Shibboleth.Conf.class));
        conf.topApps = gson.fromJson(getConf(sc, "config-topApps.json", false), TopAppsAgimus.GlobalConf.class).init();
        conf.proxyApp = gson.fromJson(getConf(sc, "config-proxyApp.json", false), ProxyApp.Conf.class);
        conf.init();
        return conf;
    }

    static String getConf(ServletContext sc, String jsonFile, boolean mustExist) {
        String s = file_get_contents(sc, "WEB-INF/" + jsonFile, mustExist);
        if (s == null) return "{}"; // do not fail here, checks are done on required attrs
        // allow trailing commas
        s = s.replaceAll(",(\\s*[\\]}])", "$1");
        return s;
    }
        
}

