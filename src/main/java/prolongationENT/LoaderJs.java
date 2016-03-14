package prolongationENT;

import java.io.IOException;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.logging.LogFactory;

import static prolongationENT.Utils.*;

public class LoaderJs {
    MainConf conf;
    String js;
    String jsHash;
    
    org.apache.commons.logging.Log log = LogFactory.getLog(LoaderJs.class);

    public LoaderJs(HttpServletRequest request, MainConf conf) {
        this.conf = conf;
        js = compute(request);
        jsHash = computeMD5(js);
    }
    
    void loader_js(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        if (!jsHash.equals(request.getParameter("v"))) {
            // redirect to versioned loader.js which has long cache time
            int one_hour = 60 * 60;
            response.setHeader("Cache-Control", "max-age=" + one_hour);
            response.sendRedirect(conf.prolongationENT_url + "/loader.js?v=" + jsHash);
        } else {
            int one_year = 60 * 60 * 24 * 365;
	    response.setHeader("Etag", jsHash);
	    respond_js(response, one_year, js);
    	}
    }
    
    String compute(HttpServletRequest request) {
    	String helpers_js = file_get_contents(request, "lib/helpers.ts");
    	String main_js = file_get_contents(request, "lib/main.ts");
        String loader_js = file_get_contents(request, "lib/loader.ts");

    	String js_css = json_encode(
    	    asMap("base",    get_css_with_absolute_url(request, "main.css"))
    	     .add("desktop", get_css_with_absolute_url(request, "desktop.css"))
    	);

    	String templates = json_encode(
            asMap("header", theme_file_contents(request, "templates/header.html"))
             .add("footer", theme_file_contents(request, "templates/footer.html"))
    	);

    	Map<String, Object> js_conf =
    	    objectFieldsToMap(conf, "prolongationENT_url", "cas_login_url", "uportal_base_url", "layout_url", "theme",
                              "cas_impersonate", "disableLocalStorage", 
    	    		"time_before_checking_browser_cache_is_up_to_date", "ent_logout_url");

    	return
            "(function () {\n" +
            "if (!window.prolongation_ENT) window.prolongation_ENT = {};\n" +
            file_get_contents(request, "lib/init.ts") +
            "pE.CONF = " + json_encode(js_conf) + "\n\n" +
            "pE.CSS = " + js_css + "\n\n" +
            "pE.TEMPLATES = " + templates + "\n\n" +
            helpers_js + main_js + "\n\n" +
            file_get_contents(request, "lib/plugins.ts") +
            file_get_contents(request, "lib/" + conf.theme + ".ts") + ";" +
            loader_js +
            "})()";
    }

    String theme_file_contents(HttpServletRequest request, String file) {
        return file_get_contents(request, conf.theme + "/" + file);
    }
    
    String get_css_with_absolute_url(HttpServletRequest request, String css_file) {
	String s = theme_file_contents(request, css_file);
	return s.replaceAll("(url\\(['\" ]*)(?!['\" ])(?!https?:|/)", "$1" + conf.prolongationENT_url + "/" + conf.theme + "/");
    }
	
}

