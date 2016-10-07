package prolongationENT;

import java.net.URL;
import java.util.Collection;
import java.util.List;
import java.util.LinkedList;
import java.util.Map;
import java.util.regex.Pattern;

import static prolongationENT.Utils.firstNonNull;

class App extends ACLs {
    
    static class Cookies {
        static String[] default_names = new String[] { "JSESSIONID", "PHPSESSID" };
        static String[] default_name_prefixes = new String[] { "_shibsession_" };

        String path;
        List<String> names;
        List<String> name_prefixes;

        String path() {
            return firstNonNull(path, "/");
        }

        List<String> names() {
            List<String> r = new LinkedList<>();
            if (names != null) r.addAll(names);
            for (String name : default_names) r.add(name);
            return r;
        }
    
        List<String> name_prefixes() {
            List<String> r = new LinkedList<>();
            if (name_prefixes != null) r.addAll(name_prefixes);
            for (String prefix : default_name_prefixes) r.add(prefix);
            return r;
        }
    }

    String shortText;
    String text;
    String title;
    String description;
    String url;
    Integer position;
    Map<String,String> url_vars;
    String shibbolethSPPrefix;
    boolean hideFromMobile = false;
    boolean hashelp = false;
    Collection<String> tags;

    List<String> inherit;
    String serviceRegex;
    ACLs admins;
    Cookies cookies = new Cookies();
    

    void merge(App app) {
        if (app.url != null) url = app.url;
        if (app.groups != null) groups = app.groups;

        if (app.serviceRegex != null) serviceRegex = app.serviceRegex;
        if (app.admins != null) admins = app.admins;
        if (app.cookies != null) cookies = app.cookies;
        if (app.shibbolethSPPrefix != null) shibbolethSPPrefix = app.shibbolethSPPrefix;
    }

    App init(Map<String,String> global_url_vars) {
        if (url == null) throw new RuntimeException("invalid config-apps.json: missing url for " + title);
        if (url_vars != null) handle_urls_vars(url_vars);
        if (global_url_vars != null) handle_urls_vars(global_url_vars);

        if (admins != null) {
            compute_default_cookies_path_and_serviceRegex();
        }
        return this;
    }
    
    private void compute_default_cookies_path_and_serviceRegex() {
        URL url = Utils.toURL(this.url);

        // default path is:
        // /     for http://foo/bar
        // /bar/ for http://foo/bar/ or http://foo/bar/boo/xxx
        String path = url.getPath().replaceFirst("/[^/]*$", "").replaceFirst("^(/[^/]*)/.*", "$1") + "/";
        
        if (cookies.path == null) cookies.path = path;
        if (serviceRegex == null) serviceRegex = Pattern.quote(url.getProtocol() + "://" + url.getHost() + path) + ".*";
    }

    private void handle_urls_vars(Map<String,String> url_vars) {
        for (Map.Entry<String, String> e : url_vars.entrySet()) {
            url = url.replace("{" + e.getKey() + "}", e.getValue());
        }
    }
}
