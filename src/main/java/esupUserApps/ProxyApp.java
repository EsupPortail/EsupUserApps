package esupUserApps;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static esupUserApps.Utils.*;

class ProxyApp {
    static class App {
        String url;
        String requestTimeout;
    }
    static class Conf {
        Map<String,App> apps;
        String requestTimeout;
    }

    esupUserApps.Conf.Main conf;
    ComputeApps computeApps;

    Logger log = LoggerFactory.getLogger(ProxyApp.class);

    ProxyApp(esupUserApps.Conf.Main conf) {
        this.conf = conf;
        computeApps = new ComputeApps(conf);
    }

    void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (conf.proxyApp.apps == null) { bad_request(response, "config-proxyApp.json is missing"); return; }

        var attrs = userAttrs(request, response);
        if (attrs == null || attrs.isEmpty()) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "you must authenticate first");
            return;
        }

        var appId = request.getParameter("id");
        if (appId == null) { bad_request(response, "missing 'id=xxx' parameter"); return; }
        var app = conf.proxyApp.apps.get(appId);
        if (app == null) { bad_request(response, "invalid appId " + appId); return; }

        var url = handle_url_vars(request, attrs, app.url);
        var conn = urlGET_raw(new URL(url), app.requestTimeout != null ? app.requestTimeout : conf.proxyApp.requestTimeout);
        forwardRequest(conn, response);
    }

    private Ldap.Attrs userAttrs(HttpServletRequest request, HttpServletResponse response) {
        if (hasValidBearerToken(request, conf.shibboleth)) {
            return computeApps.getShibbolethUserInfo(request);
        } else {
            var userId = get_CAS_userId(request);
            if (userId == null) return null;
            return computeApps.getLdapPeopleInfo(userId);
        }
    }

    private void forwardRequest(HttpURLConnection conn, HttpServletResponse response) throws IOException {
        var inputStream = conn.getResponseCode() < 400 ? conn.getInputStream() : conn.getErrorStream();
        try {
            response.setStatus(conn.getResponseCode());
            copyHeaders(conn, response);
            if (response.getHeader("Cache-Control") == null) response.setHeader("Cache-Control", "private, no-cache"); // be safe (in case someone forces a default cache)
            if (inputStream != null) IOUtils.copy(inputStream, response.getOutputStream());
        } finally {
            if (inputStream != null) inputStream.close();
        }
    }

    // inspired by https://gist.github.com/rafaeltuelho/9376341
    private void copyHeaders(HttpURLConnection conn, HttpServletResponse response) {
        for (var i = 1; true; ++i) {
            var key = conn.getHeaderFieldKey(i);
            if (key == null) break;
            var value = conn.getHeaderField(i);

            // skip headers handled by tomcat
            if (key.equalsIgnoreCase("Content-Length")
             || key.equalsIgnoreCase("Connection")
             || key.equalsIgnoreCase("Server")
             || key.equalsIgnoreCase("Transfer-Encoding")
             || key.equalsIgnoreCase("Content-Length")) {
                continue;
            }

            // skip Set-Cookie with a warning
            if (key.equalsIgnoreCase("Set-Cookie")) {
                log.warn("proxied url wants to set a cookie which is not the way it should work! Ignoring the cookie...");
                continue;
            }
            response.setHeader(key, value);
        }
    }

    private String handle_url_vars(HttpServletRequest request, Ldap.Attrs attrs, String url) {
        var m = Pattern.compile("\\{([^}.]+)[.]([^}.]+)\\}").matcher(url);
        var sb = new StringBuilder();
        while (m.find()) {
            var source = m.group(1);
            var name = m.group(2);
            var val = source.equals("query") ? request.getParameter(name) :
                      source.equals("userAttrs") ? Ldap.getFirst(attrs, name) :
                      (String) throwRuntimeException("invalid url variable " + source + "." + name + " in url " + url);
            if (val == null) {
                log.warn("no value for variable " + source + "." + name + " in url" + url + ". Using empty value");
                val = "";
            }
            m.appendReplacement(sb, val);
        }
        m.appendTail(sb);
        return sb.toString();
    }
}
