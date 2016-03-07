package prolongationENT;

import java.net.URL;
import java.util.regex.Pattern;

class App extends ACLs {
    String text;
    String title;
    String description;
    String url;
    String shibbolethSPPrefix;
    boolean hideFromMobile = false;
    boolean hashelp = false;

    String inherit;
    String serviceRegex;
    ACLs admins;
    Cookies cookies = new Cookies();
    

    void merge(App app) {
        if (app.serviceRegex != null) serviceRegex = app.serviceRegex;
        if (app.admins != null) admins = app.admins;
        if (app.cookies != null) cookies = app.cookies;
    }

    App init() {
        if (url != null && admins != null) {
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
}
