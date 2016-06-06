package prolongationENT;

import java.io.IOException;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.apache.commons.logging.LogFactory;

import static prolongationENT.Utils.*;

@SuppressWarnings("serial")
public class Main extends HttpServlet {           
    Conf.Main conf = null;
    LoaderJs loaderJs;
    ComputeBandeau computeBandeau;
    
    org.apache.commons.logging.Log log = LogFactory.getLog(Main.class);

    static String[] mappings = new String[] {
        "/loader.js", "/detectReload", "/purgeCache",
        "/layout", "/logout", "/redirect", "/canImpersonate",
    };
    
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        if (conf == null || conf.disableServerCache) initConf(request);
        switch (request.getServletPath()) {
            case "/loader.js":      loader_js     (request, response); break;
            case "/detectReload":   detectReload  (request, response); break;
            case "/purgeCache":     purgeCache    (request, response); break;

            case "/layout":         layout        (request, response); break;       
            case "/logout":         logout        (request, response); break;
            case "/redirect":       redirect      (request, response); break;
            case "/canImpersonate": canImpersonate(request, response); break;
        }
    }
    
    void loader_js(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        loaderJs.loader_js(request, response);
    }
    
    void detectReload(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        long five_days = 60 * 60 * 24 * 5;
        respond_js(response, five_days, "window.prolongation_ENT.detectReload(" + now() + ");");
    }
    
    void purgeCache(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        log.warn("purging cache");
        initConf(request);
    }

    
    void layout(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        computeBandeau.layout(request, response);
    }
    
    void logout(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        session_invalidate(request);

        String callback = request.getParameter("callback");
        respond_js(response, callback + "();");
    }
    
    void canImpersonate(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        computeBandeau.canImpersonate(request, response);
    }
    
    void redirect(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        computeBandeau.redirect(request, response);
    }
    
    synchronized void initConf(HttpServletRequest request) {
        ServletContext sc = request.getServletContext();
        conf = getConf(sc);
        loaderJs = new LoaderJs(request, conf);
        computeBandeau = new ComputeBandeau(conf);
    }   

    static Conf.Main getConf(ServletContext sc) {
        Gson gson = new Gson();
        Conf.Main conf = gson.fromJson(getConf(sc, "config.json"), Conf.Main.class);
        conf.merge(gson.fromJson(getConf(sc, "config-auth.json"), Conf.Auth.class));
        conf.merge(gson.fromJson(getConf(sc, "config-apps.json"), Conf.Apps.class).init());
        conf.merge(gson.fromJson(getConf(sc, "config-shibboleth.json"), Shibboleth.Conf.class));
        conf.topApps = gson.fromJson(getConf(sc, "config-topApps.json"), TopAppsAgimus.GlobalConf.class).init();
        conf.init();
        return conf;
    }

    static String getConf(ServletContext sc, String jsonFile) {
        String s = file_get_contents(sc, "WEB-INF/" + jsonFile);
        if (s == null) return "{}"; // do not fail here, checks are done on required attrs
        // allow trailing commas
        s = s.replaceAll(",(\\s*[\\]}])", "$1");
        return s;
    }
        
}

