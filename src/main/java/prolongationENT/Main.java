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

public class Main extends HttpServlet {	   
    MainConf conf = null;
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
    	conf = getMainConf(request.getSession().getServletContext());
        loaderJs = new LoaderJs(request, conf);
        computeBandeau = new ComputeBandeau(conf);
    }   

    static MainConf getMainConf(ServletContext sc) {
    	Gson gson = new Gson();
    	MainConf conf = gson.fromJson(getConf(sc, "config.json"), MainConf.class);
    	conf.merge(gson.fromJson(getConf(sc, "config-auth.json"), AuthConf.class));
    	conf.merge(gson.fromJson(getConf(sc, "config-apps.json"), AppsConf.class).init());
        conf.init();
        return conf;
    }

    static JsonObject getConf(ServletContext sc, String jsonFile) {
        String s = file_get_contents(sc, "WEB-INF/" + jsonFile);
        // allow trailing commas
        s = s.replaceAll(",(\\s*[\\]}])", "$1");
    	return new JsonParser().parse(s).getAsJsonObject();
    }
	
}

