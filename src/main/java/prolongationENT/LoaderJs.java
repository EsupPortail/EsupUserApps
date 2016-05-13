package prolongationENT;

import java.io.IOException;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.logging.LogFactory;

import static prolongationENT.Utils.*;

public class LoaderJs {
    Conf.Main conf;
    String js;
    String jsHash;
    
    org.apache.commons.logging.Log log = LogFactory.getLog(LoaderJs.class);

    public LoaderJs(HttpServletRequest request, Conf.Main conf) {
        this.conf = conf;
        js = compute(request, conf.theme);
        jsHash = computeMD5(js);
    }
    
    void loader_js(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String wantedTheme = request.getParameter("theme");
        if (wantedTheme != null && conf.themes.alternatives != null &&
            conf.themes.alternatives.list.contains(wantedTheme)) {
            // NB: no strong caching for alternative themes
            int one_hour = 60 * 60;
            respond_js(response, one_hour, compute(request, request.getParameter("theme")));
        } else if (!jsHash.equals(request.getParameter("v"))) {
            int one_hour = 60 * 60;
            if (conf.themes.alternatives != null) {
                respond_js(response, one_hour, String.format(file_get_contents(request, "lib/loader-chooser.ts"),
                                                             conf.prolongationENT_url + "/loader.js", jsHash,
                                                             conf.themes.alternatives.cookieName));
            } else {
                // redirect to versioned loader.js which has long cache time
                response.setHeader("Cache-Control", "max-age=" + one_hour);
                response.sendRedirect(conf.prolongationENT_url + "/loader.js?v=" + jsHash);
            }
        } else {
            int one_year = 60 * 60 * 24 * 365;
            response.setHeader("Etag", jsHash);
            respond_js(response, one_year, js);
        }
    }
    
    String compute(HttpServletRequest request, String theme) {
        String helpers_js = file_get_contents(request, "lib/helpers.ts");
        String main_js = file_get_contents(request, "lib/main.ts");
        String loader_js = file_get_contents(request, "lib/loader.ts");

        String js_css = json_encode(
            asMap("base",    get_css_with_absolute_url(request, theme, "main.css"))
             .add("desktop", get_css_with_absolute_url(request, theme, "desktop.css"))
        );

        String templates = json_encode(
            asMap("header", theme_file_contents(request, theme, "templates/header.html"))
             .add("footer", theme_file_contents(request, theme, "templates/footer.html"))
        );

        Map<String, Object> js_conf =
            objectFieldsToMap(conf, "prolongationENT_url", "cas_login_url", "uportal_base_url", "layout_url",
                              "cas_impersonate", "disableLocalStorage", 
                              "time_before_checking_browser_cache_is_up_to_date", "ent_logout_url");
        js_conf.put("theme", theme);

        return
            "(function () {\n" +
            "if (!window.prolongation_ENT) window.prolongation_ENT = {};\n" +
            file_get_contents(request, "lib/init.ts") +
            "pE.CONF = " + json_encode(js_conf) + "\n\n" +
            (conf.disableCSSInlining ? "" : "pE.CSS = " + js_css + "\n\n") +
            "pE.TEMPLATES = " + templates + "\n\n" +
            helpers_js + main_js + "\n\n" +
            file_get_contents(request, "lib/plugins.ts") +
            file_get_contents(request, "lib/" + theme + ".ts") + ";" +
            loader_js +
            "})()";
    }

    String theme_file_contents(HttpServletRequest request, String theme, String file) {
        return file_get_contents(request, theme + "/" + file);
    }
    
    String get_css_with_absolute_url(HttpServletRequest request, String theme, String css_file) {
        String s = theme_file_contents(request, theme, css_file);
        return s.replaceAll("(url\\(['\" ]*)(?!['\" ])(?!https?:|/)", "$1" + conf.prolongationENT_url + "/" + theme + "/");
    }

}

